package com.htf.portal.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.htf.portal.dao.JoinRequestDao;
import com.htf.portal.dao.TeamDao;
import com.htf.portal.dao.TeamMemberDao;
import com.htf.portal.dao.TeamSeekerDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.dto.AdminTeamsView;
import com.htf.portal.dto.HackathonTeamsView;
import com.htf.portal.dto.MemberView;
import com.htf.portal.dto.MyTeamView;
import com.htf.portal.dto.OpenTeamView;
import com.htf.portal.dto.RequestView;
import com.htf.portal.dto.TeamForm;
import com.htf.portal.dto.TeamRegistrationView;
import com.htf.portal.dto.TeamView;
import com.htf.portal.dto.TeamViewerRole;
import com.htf.portal.exception.AlreadyInTeamException;
import com.htf.portal.exception.BusinessRuleException;
import com.htf.portal.exception.ConflictException;
import com.htf.portal.exception.DuplicateKeyException;
import com.htf.portal.exception.ForbiddenActionException;
import com.htf.portal.exception.NotFoundException;
import com.htf.portal.model.Hackathon;
import com.htf.portal.model.JoinRequest;
import com.htf.portal.model.Team;
import com.htf.portal.model.TeamMember;
import com.htf.portal.model.TeamStatus;
import com.htf.portal.model.User;
import com.htf.portal.util.DateUtils;
import com.htf.portal.util.SkillUtils;
import com.htf.portal.util.TextUtils;

/**
 * Team rules (section 5.2): create, view, edit, delete, leave, remove a member, the open-team list with
 * match %, and the admin overview. Invites and applications live in RequestService.
 */
@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    // Open teams: best match first; for equal match, newest team first.
    private static final Comparator<OpenTeamView> OPEN_TEAM_ORDER =
            Comparator.comparing(OpenTeamView::matchPercent, Comparator.reverseOrder())
                    .thenComparing(OpenTeamView::createdAt, Comparator.reverseOrder());

    private final TeamDao teamDao;
    private final TeamMemberDao teamMemberDao;
    private final JoinRequestDao joinRequestDao;
    private final TeamSeekerDao seekerDao;
    private final UserDao userDao;
    private final HackathonService hackathonService;
    private final RequestService requestService;
    private final RegistrationService registrationService;
    private final NotificationService notificationService;
    private final ActivityLog activityLog;

    public TeamService(TeamDao teamDao, TeamMemberDao teamMemberDao, JoinRequestDao joinRequestDao,
                       TeamSeekerDao seekerDao, UserDao userDao,
                       HackathonService hackathonService, RequestService requestService,
                       RegistrationService registrationService, NotificationService notificationService,
                       ActivityLog activityLog) {
        this.teamDao = teamDao;
        this.teamMemberDao = teamMemberDao;
        this.joinRequestDao = joinRequestDao;
        this.seekerDao = seekerDao;
        this.userDao = userDao;
        this.hackathonService = hackathonService;
        this.requestService = requestService;
        this.registrationService = registrationService;
        this.notificationService = notificationService;
        this.activityLog = activityLog;
    }

    // ------------------------------------------------------------------ reading

    /** The teams part of a hackathon page: my team, whether I may create/join one, and open teams by match %. */
    public HackathonTeamsView teamsForHackathon(Long viewerId, Long hackathonId) {
        Hackathon hackathon = hackathonService.findHackathon(hackathonId);
        User viewer = findUser(viewerId);
        Optional<Team> myTeam = teamDao.findTeamOfUserInHackathon(hackathonId, viewerId);

        String joinBlockedReason = null;
        if (!viewer.isStudent()) {
            joinBlockedReason = "Only students can create or join teams";
        } else if (myTeam.isPresent()) {
            joinBlockedReason = "You are already in team " + myTeam.get().getName() + " for this hackathon";
        } else if (!hackathon.isRegistrationOpenOn(LocalDate.now())) {
            joinBlockedReason = "Registration closed on " + hackathon.getRegistrationDeadline();
        }

        // Teams I already have a pending invite or application with (a HashSet makes "contains" a quick check).
        Set<Long> pendingTeamIds = new HashSet<>();
        for (JoinRequest r : joinRequestDao.findPendingByStudentInHackathon(viewerId, hackathonId)) {
            pendingTeamIds.add(r.getTeamId());
        }
        Set<String> mySkills = SkillUtils.toSet(viewer.getSkills());
        Map<Long, Integer> memberCounts = teamMemberDao.countMembersPerTeam(hackathonId);

        List<OpenTeamView> openTeams = new ArrayList<>();
        for (Team team : teamDao.findOpenByHackathon(hackathonId)) {
            openTeams.add(new OpenTeamView(team.getId(), team.getName(), team.getDescription(), team.getLookingFor(),
                    findUser(team.getLeaderId()).getName(), memberCounts.getOrDefault(team.getId(), 0),
                    hackathon.getMaxTeamSize(), SkillUtils.matchPercent(team.getLookingFor(), mySkills),
                    pendingTeamIds.contains(team.getId()), team.getCreatedAt()));
        }
        openTeams.sort(OPEN_TEAM_ORDER);

        Long myTeamId = null;
        String myTeamName = null;
        if (myTeam.isPresent()) {
            myTeamId = myTeam.get().getId();
            myTeamName = myTeam.get().getName();
        }
        boolean lookingForTeam = seekerDao.exists(hackathonId, viewerId);
        int seekerCount = (int) seekerDao.countAvailableByHackathon(hackathonId);
        return new HackathonTeamsView(myTeamId, myTeamName, joinBlockedReason, openTeams, lookingForTeam, seekerCount);
    }

    public List<MyTeamView> myTeams(Long userId) {
        List<MyTeamView> views = new ArrayList<>();
        for (Team team : teamDao.findByMember(userId)) {
            Hackathon hackathon = hackathonService.findHackathon(team.getHackathonId());
            views.add(new MyTeamView(team.getId(), team.getName(), team.getStatus(), hackathon.getId(),
                    hackathon.getTitle(), team.isLeader(userId)));
        }
        return views;
    }

    public TeamView getTeam(Long viewerId, Long teamId) {
        return buildTeamView(findTeam(teamId), viewerId);
    }

    // ------------------------------------------------------------------ leader and member actions

    @Transactional
    public TeamView createTeam(Long userId, Long hackathonId, TeamForm form) {
        User user = findUser(userId);
        TeamRules.requireStudent(user);
        Hackathon hackathon = hackathonService.findHackathon(hackathonId);
        TeamRules.requireRegistrationOpen(hackathon);
        if (teamMemberDao.existsByHackathonAndUser(hackathonId, userId)) {
            throw new AlreadyInTeamException("You are already in a team for this hackathon");
        }
        String name = form.name().trim();
        if (teamDao.existsByName(hackathonId, name)) {
            throw new ConflictException("A team called '" + name + "' already exists in this hackathon");
        }

        Team team = new Team(hackathonId, userId, name, TextUtils.trimToNull(form.description()),
                SkillUtils.normalize(form.lookingFor()));
        if (hackathon.getMaxTeamSize() == 1) {
            team.setStatus(TeamStatus.FULL);   // a one-person team is full straight away
        }
        try {
            teamDao.insert(team);
            teamMemberDao.insert(new TeamMember(team.getId(), hackathonId, userId));
        } catch (DuplicateKeyException e) {
            throw duplicateToBusinessError(e, name);
        }
        // The leader is in a team now, so their open invites/applications elsewhere in this hackathon are void,
        // and they no longer belong on the "looking for a team" list.
        requestService.cancelPendingRequestsOfStudent(user, hackathonId, "created their own team");
        seekerDao.deleteByHackathonAndUser(hackathonId, userId);

        activityLog.record(user.getName() + " created team " + name + " (" + hackathon.getTitle() + ")");
        log.info("Team created: id={}, name={}, hackathonId={}, leaderId={}", team.getId(), name, hackathonId, userId);
        return buildTeamView(team, userId);
    }

    public TeamView updateTeam(Long userId, Long teamId, TeamForm form) {
        Team team = findTeam(teamId);
        requireLeader(team, userId, "edit the team");
        TeamRules.requireNotLocked(team);

        String name = form.name().trim();
        if (!name.equals(team.getName()) && teamDao.existsByName(team.getHackathonId(), name)) {
            throw new ConflictException("A team called '" + name + "' already exists in this hackathon");
        }
        team.setName(name);
        team.setDescription(TextUtils.trimToNull(form.description()));
        team.setLookingFor(SkillUtils.normalize(form.lookingFor()));
        try {
            teamDao.update(team);
        } catch (DuplicateKeyException e) {
            throw duplicateToBusinessError(e, name);
        }
        log.info("Team updated: id={}", teamId);
        return buildTeamView(team, userId);
    }

    @Transactional
    public void deleteTeam(Long userId, Long teamId) {
        Team team = lockTeam(teamId);
        requireLeader(team, userId, "delete the team");
        TeamRules.requireNotLocked(team);
        String leaderName = findUser(userId).getName();
        deleteAndTellMembers(team, leaderName, false);
        activityLog.record(leaderName + " deleted team " + team.getName());
    }

    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        Team team = lockTeam(teamId);
        if (team.isLeader(userId)) {
            throw new BusinessRuleException("The leader cannot leave. Delete the team instead.");
        }
        TeamRules.requireNotLocked(team);
        if (!teamMemberDao.deleteByTeamAndUser(teamId, userId)) {
            throw new BusinessRuleException("You are not a member of this team");
        }
        reopenIfFull(team);

        String name = findUser(userId).getName();
        notificationService.notify(team.getLeaderId(), name + " left your team " + team.getName());
        activityLog.record(name + " left " + team.getName());
        log.info("Member left: userId={}, teamId={}", userId, teamId);
    }

    @Transactional
    public TeamView removeMember(Long leaderId, Long teamId, Long memberId) {
        Team team = lockTeam(teamId);
        requireLeader(team, leaderId, "remove members");
        if (team.isLeader(memberId)) {
            throw new BusinessRuleException("The leader cannot be removed. Delete the team instead.");
        }
        TeamRules.requireNotLocked(team);
        if (!teamMemberDao.deleteByTeamAndUser(teamId, memberId)) {
            throw new NotFoundException("That person is not a member of this team");
        }
        reopenIfFull(team);

        User member = findUser(memberId);
        notificationService.notify(memberId, "You were removed from team " + team.getName());
        activityLog.record(findUser(leaderId).getName() + " removed " + member.getName() + " from " + team.getName());
        log.info("Member removed: userId={}, teamId={}", memberId, teamId);
        return buildTeamView(team, leaderId);
    }

    // ------------------------------------------------------------------ admin

    public AdminTeamsView adminOverview() {
        List<Team> teams = teamDao.findAll();
        Map<Long, Integer> memberCounts = teamMemberDao.countMembersPerTeam();
        Map<Long, Hackathon> hackathons = new HashMap<>();
        Map<Long, Integer> teamsPerHackathon = new HashMap<>();

        List<AdminTeamsView.Row> rows = new ArrayList<>();
        for (Team team : teams) {
            Long hackathonId = team.getHackathonId();
            if (!hackathons.containsKey(hackathonId)) {
                hackathons.put(hackathonId, hackathonService.findHackathon(hackathonId));
            }
            Hackathon hackathon = hackathons.get(hackathonId);

            // Count teams per hackathon with a HashMap: read the count so far (0 if none) and add one.
            teamsPerHackathon.put(hackathonId, teamsPerHackathon.getOrDefault(hackathonId, 0) + 1);

            rows.add(new AdminTeamsView.Row(team.getId(), team.getName(), hackathon.getTitle(),
                    findUser(team.getLeaderId()).getName(), memberCounts.getOrDefault(team.getId(), 0),
                    hackathon.getMaxTeamSize(), team.getStatus(), team.getCreatedAt()));
        }

        List<AdminTeamsView.HackathonCount> counts = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : teamsPerHackathon.entrySet()) {
            counts.add(new AdminTeamsView.HackathonCount(hackathons.get(entry.getKey()).getTitle(), entry.getValue()));
        }
        // A HashMap has no order, so sort the counts: most teams first.
        counts.sort(Comparator.comparing(AdminTeamsView.HackathonCount::teamCount, Comparator.reverseOrder()));

        return new AdminTeamsView(rows, counts, activityLog.recent());
    }

    // The admin may delete any team, whatever its status.
    @Transactional
    public void adminDeleteTeam(Long teamId) {
        Team team = lockTeam(teamId);
        deleteAndTellMembers(team, "the admin", true);
        activityLog.record("Admin deleted team " + team.getName());
    }

    // ------------------------------------------------------------------ helpers

    private TeamView buildTeamView(Team team, Long viewerId) {
        Hackathon hackathon = hackathonService.findHackathon(team.getHackathonId());
        LocalDate today = LocalDate.now();
        User viewer = findUser(viewerId);

        List<MemberView> members = new ArrayList<>();
        boolean viewerIsMember = false;
        for (TeamMember m : teamMemberDao.findByTeam(team.getId())) {
            User u = findUser(m.getUserId());
            members.add(new MemberView(u.getId(), u.getName(), u.getUserCode(), u.getBranch(), u.getStudyYear(),
                    u.getSkills(), team.isLeader(u.getId()), u.hasPicture()));
            if (u.getId().equals(viewerId)) {
                viewerIsMember = true;
            }
        }

        TeamViewerRole role = TeamViewerRole.VISITOR;
        if (team.isLeader(viewerId)) {
            role = TeamViewerRole.LEADER;
        } else if (viewerIsMember) {
            role = TeamViewerRole.MEMBER;
        }

        String inviteBlockedReason = null;
        String applyBlockedReason = null;
        String submitBlockedReason = null;
        List<RequestView> pending = new ArrayList<>();
        TeamRegistrationView registration = null;
        if (role == TeamViewerRole.LEADER) {
            inviteBlockedReason = newMemberBlockedReason(team, hackathon, today);
            submitBlockedReason = registrationService.submitBlockedReason(team, hackathon, members.size());
            pending = requestService.pendingForTeam(team.getId(), viewerId);
        } else if (role == TeamViewerRole.VISITOR) {
            applyBlockedReason = applyBlockedReason(viewer, team, hackathon, today);
        }
        if (role != TeamViewerRole.VISITOR) {
            registration = registrationService.registrationForTeam(team.getId());
        }

        int matchPercent = SkillUtils.matchPercent(team.getLookingFor(), SkillUtils.toSet(viewer.getSkills()));
        return new TeamView(team.getId(), team.getName(), team.getDescription(), team.getLookingFor(), team.getStatus(),
                hackathon.getId(), hackathon.getTitle(), hackathon.getMinTeamSize(), hackathon.getMaxTeamSize(),
                DateUtils.describeDeadline(hackathon.getRegistrationDeadline(), today),
                members, role, matchPercent, TeamRules.lockedReason(team), inviteBlockedReason, applyBlockedReason,
                submitBlockedReason, pending, registration);
    }

    // Why nobody new can join right now (locked, deadline, full), or null if they can.
    private String newMemberBlockedReason(Team team, Hackathon hackathon, LocalDate today) {
        String locked = TeamRules.lockedReason(team);
        if (locked != null) {
            return locked;
        }
        if (!hackathon.isRegistrationOpenOn(today)) {
            return "Registration closed on " + hackathon.getRegistrationDeadline();
        }
        if (team.getStatus() == TeamStatus.FULL) {
            return "The team is full";
        }
        return null;
    }

    private String applyBlockedReason(User viewer, Team team, Hackathon hackathon, LocalDate today) {
        if (!viewer.isStudent()) {
            return "Only students can apply";
        }
        Optional<Team> myTeam = teamDao.findTeamOfUserInHackathon(hackathon.getId(), viewer.getId());
        if (myTeam.isPresent()) {
            return "You are already in team " + myTeam.get().getName() + " for this hackathon";
        }
        String blocked = newMemberBlockedReason(team, hackathon, today);
        if (blocked != null) {
            return blocked;
        }
        if (joinRequestDao.existsPending(team.getId(), viewer.getId())) {
            return "You already have a pending request with this team (see My Requests)";
        }
        return null;
    }

    // Someone left a FULL team: there is a free seat again.
    private void reopenIfFull(Team team) {
        if (team.getStatus() == TeamStatus.FULL) {
            teamDao.updateStatus(team.getId(), TeamStatus.OPEN);
            team.setStatus(TeamStatus.OPEN);
        }
    }

    // tellLeader: false when the leader deleted the team themselves (they already know).
    private void deleteAndTellMembers(Team team, String deletedBy, boolean tellLeader) {
        List<TeamMember> members = teamMemberDao.findByTeam(team.getId());
        teamDao.deleteById(team.getId());   // members, requests and registrations go too (ON DELETE CASCADE)
        for (TeamMember m : members) {
            if (tellLeader || !team.isLeader(m.getUserId())) {
                notificationService.notify(m.getUserId(), "Team " + team.getName() + " was deleted by " + deletedBy);
            }
        }
        log.info("Team deleted: id={}, name={}, by {}", team.getId(), team.getName(), deletedBy);
    }

    private void requireLeader(Team team, Long userId, String action) {
        if (!team.isLeader(userId)) {
            throw new ForbiddenActionException("Only the team leader can " + action);
        }
    }

    // Two UNIQUE rules can fire on a team insert: the team name, or the leader already being in a team.
    private RuntimeException duplicateToBusinessError(DuplicateKeyException e, String name) {
        if (e.getMessage() != null && e.getMessage().contains("uq_member_one_team_per_hackathon")) {
            return new AlreadyInTeamException("You are already in a team for this hackathon");
        }
        return new ConflictException("A team called '" + name + "' already exists in this hackathon");
    }

    // SELECT ... FOR UPDATE: only meaningful inside a @Transactional method (the lock lasts until commit).
    private Team lockTeam(Long teamId) {
        return teamDao.findByIdForUpdate(teamId).orElseThrow(() -> new NotFoundException("Team", teamId));
    }

    private Team findTeam(Long teamId) {
        return teamDao.findById(teamId).orElseThrow(() -> new NotFoundException("Team", teamId));
    }

    private User findUser(Long userId) {
        return userDao.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }
}
