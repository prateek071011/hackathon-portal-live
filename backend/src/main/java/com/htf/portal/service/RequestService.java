package com.htf.portal.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.htf.portal.dao.JoinRequestDao;
import com.htf.portal.dao.TeamDao;
import com.htf.portal.dao.TeamMemberDao;
import com.htf.portal.dao.TeamSeekerDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.dto.InviteResult;
import com.htf.portal.dto.MyRequestsView;
import com.htf.portal.dto.RequestView;
import com.htf.portal.exception.AlreadyInTeamException;
import com.htf.portal.exception.BusinessRuleException;
import com.htf.portal.exception.ConflictException;
import com.htf.portal.exception.DuplicateKeyException;
import com.htf.portal.exception.ForbiddenActionException;
import com.htf.portal.exception.NotFoundException;
import com.htf.portal.exception.TeamFullException;
import com.htf.portal.model.Hackathon;
import com.htf.portal.model.JoinRequest;
import com.htf.portal.model.RequestStatus;
import com.htf.portal.model.RequestType;
import com.htf.portal.model.Team;
import com.htf.portal.model.TeamMember;
import com.htf.portal.model.TeamStatus;
import com.htf.portal.model.User;

/**
 * Invites and applications (section 5.3): sending, accepting, rejecting and cancelling them.
 * Accepting is the most important method in the project: one locked transaction, see accept().
 */
@Service
public class RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    private final JoinRequestDao joinRequestDao;
    private final TeamDao teamDao;
    private final TeamMemberDao teamMemberDao;
    private final TeamSeekerDao seekerDao;
    private final UserDao userDao;
    private final HackathonService hackathonService;
    private final NotificationService notificationService;
    private final ActivityLog activityLog;

    public RequestService(JoinRequestDao joinRequestDao, TeamDao teamDao, TeamMemberDao teamMemberDao,
                          TeamSeekerDao seekerDao, UserDao userDao, HackathonService hackathonService,
                          NotificationService notificationService, ActivityLog activityLog) {
        this.joinRequestDao = joinRequestDao;
        this.teamDao = teamDao;
        this.teamMemberDao = teamMemberDao;
        this.seekerDao = seekerDao;
        this.userDao = userDao;
        this.hackathonService = hackathonService;
        this.notificationService = notificationService;
        this.activityLog = activityLog;
    }

    // ------------------------------------------------------------------ sending

    /** The leader invites one or more students by user code. Codes that cannot be invited are skipped with a reason. */
    @Transactional
    public InviteResult invite(Long leaderId, Long teamId, String codesText) {
        Team team = findTeam(teamId);
        if (!team.isLeader(leaderId)) {
            throw new ForbiddenActionException("Only the team leader can invite people");
        }
        Hackathon hackathon = hackathonService.findHackathon(team.getHackathonId());
        TeamRules.requireRegistrationOpen(hackathon);
        TeamRules.requireOpenForNewMembers(team);

        List<String> codes = parseCodes(codesText);
        if (codes.isEmpty()) {
            throw new BusinessRuleException("Type at least one user code");
        }

        List<String> skipped = new ArrayList<>();
        Map<String, User> studentByCode = new HashMap<>();
        // Walk the list and drop every code we cannot invite. Iterator.remove() is the safe way to delete
        // from a list while looping over it (removing inside a for-each loop throws ConcurrentModificationException).
        Iterator<String> iterator = codes.iterator();
        while (iterator.hasNext()) {
            String code = iterator.next();
            String problem = null;
            Optional<User> found = userDao.findByUserCode(code);
            if (found.isEmpty() || !found.get().isStudent()) {
                problem = "no student has this code";
            } else if (found.get().getId().equals(leaderId)) {
                problem = "that is your own code";
            } else if (teamMemberDao.existsByHackathonAndUser(hackathon.getId(), found.get().getId())) {
                problem = found.get().getName() + " is already in a team for this hackathon";
            } else if (joinRequestDao.existsPending(teamId, found.get().getId())) {
                problem = found.get().getName() + " already has a pending request with your team";
            }

            if (problem != null) {
                skipped.add(code + ": " + problem);
                iterator.remove();
            } else {
                studentByCode.put(code, found.get());
            }
        }

        User leader = findUser(leaderId);
        List<String> sent = new ArrayList<>();
        for (String code : codes) {   // only the codes that survived the loop above
            User student = studentByCode.get(code);
            joinRequestDao.insert(new JoinRequest(teamId, hackathon.getId(), student.getId(), RequestType.INVITE));
            notificationService.notify(student.getId(), leader.getName() + " invited you to join team "
                    + team.getName() + " for " + hackathon.getTitle());
            sent.add(student.getName() + " (" + code + ")");
        }
        log.info("Invites from team {}: {} sent, {} skipped", teamId, sent.size(), skipped.size());
        if (!skipped.isEmpty()) {
            log.warn("Invites skipped for team {}: {}", teamId, skipped);
        }
        return new InviteResult(sent, skipped);
    }

    /** A student applies to join an OPEN team. */
    @Transactional
    public String apply(Long studentId, Long teamId) {
        User student = findUser(studentId);
        TeamRules.requireStudent(student);
        Team team = findTeam(teamId);
        Hackathon hackathon = hackathonService.findHackathon(team.getHackathonId());
        TeamRules.requireRegistrationOpen(hackathon);
        if (teamMemberDao.existsByHackathonAndUser(hackathon.getId(), studentId)) {
            throw new AlreadyInTeamException("You are already in a team for this hackathon");
        }
        TeamRules.requireOpenForNewMembers(team);
        if (joinRequestDao.existsPending(teamId, studentId)) {
            throw new ConflictException("You already have a pending request with this team");
        }

        joinRequestDao.insert(new JoinRequest(teamId, hackathon.getId(), studentId, RequestType.APPLICATION));
        notificationService.notify(team.getLeaderId(), student.getName() + " applied to join your team " + team.getName());
        log.info("Application: student {} -> team {}", studentId, teamId);
        return "Application sent to " + team.getName();
    }

    // ------------------------------------------------------------------ answering

    /*
     * ACCEPTING A REQUEST: ONE TRANSACTION
     * ------------------------------------
     * Everything below commits together, or nothing does:
     *   1. Lock the team row (SELECT ... FOR UPDATE). A second accept for the same team waits here
     *      until we finish, so two people can never squeeze into the last free seat.
     *   2. Re-check with fresh data: the request is still PENDING, the deadline has not passed, the team is OPEN
     *      and not full, and the student is not already in a team for this hackathon.
     *   3. Insert the team member.
     *   4. Mark this request ACCEPTED.
     *   5. Cancel the student's other pending requests for this hackathon, and take them off the
     *      "looking for a team" list (they have a team now).
     *   6. If the team is now full: set it to FULL and cancel the team's remaining pending requests.
     *   7. Insert notifications for the people affected.
     * ROLLBACK: every failed check throws one of our unchecked exceptions. When it leaves this method, Spring rolls
     * back steps 3-7, so no half-done accept is ever saved. A DuplicateKeyException from UNIQUE(hackathon_id, user_id)
     * (another team accepted the same student a moment earlier) becomes AlreadyInTeamException and also rolls back.
     * READ_COMMITTED: once we hold the lock, every SELECT sees the newest committed rows (not an older snapshot),
     * so the member count we check is really up to date.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String accept(Long userId, Long requestId) {
        JoinRequest request = findRequest(requestId);

        // Step 1: lock the team row.
        Team team = teamDao.findByIdForUpdate(request.getTeamId())
                .orElseThrow(() -> new NotFoundException("Team", request.getTeamId()));
        // Read the request again now that we hold the lock: someone may have answered it while we waited.
        JoinRequest fresh = findRequest(requestId);

        // Step 2: re-check everything.
        if (!fresh.isAnsweredBy(userId, team)) {
            throw new ForbiddenActionException("Only the person who received this request can accept it");
        }
        requirePending(fresh);
        Hackathon hackathon = hackathonService.findHackathon(team.getHackathonId());
        TeamRules.requireRegistrationOpen(hackathon);
        TeamRules.requireOpenForNewMembers(team);
        long memberCount = teamMemberDao.countByTeam(team.getId());
        if (memberCount >= hackathon.getMaxTeamSize()) {
            throw new TeamFullException(team.getName());
        }
        User student = findUser(fresh.getStudentId());
        if (teamMemberDao.existsByHackathonAndUser(hackathon.getId(), student.getId())) {
            throw new AlreadyInTeamException(student.getName() + " is already in a team for this hackathon");
        }

        // Step 3: add the member. The UNIQUE constraint is the last line of defence.
        try {
            teamMemberDao.insert(new TeamMember(team.getId(), hackathon.getId(), student.getId()));
        } catch (DuplicateKeyException e) {
            throw new AlreadyInTeamException(student.getName() + " is already in a team for this hackathon");
        }

        // Step 4.
        joinRequestDao.updateStatus(requestId, RequestStatus.ACCEPTED);

        // Step 5.
        cancelPendingRequestsOfStudent(student, hackathon.getId(), "joined team " + team.getName());
        seekerDao.deleteByHackathonAndUser(hackathon.getId(), student.getId());

        // Step 6.
        boolean nowFull = memberCount + 1 >= hackathon.getMaxTeamSize();
        if (nowFull) {
            teamDao.updateStatus(team.getId(), TeamStatus.FULL);
            cancelPendingRequestsOfTeam(team, "is now full");
        }

        // Step 7.
        String message;
        if (fresh.getType() == RequestType.INVITE) {
            notificationService.notify(team.getLeaderId(), student.getName() + " accepted your invite and joined " + team.getName());
            message = "You joined team " + team.getName();
        } else {
            notificationService.notify(student.getId(), "Your application to " + team.getName() + " was accepted. Welcome to the team!");
            message = student.getName() + " joined your team";
        }
        if (nowFull) {
            notificationService.notify(team.getLeaderId(), "Your team " + team.getName() + " is now full");
        }

        activityLog.record(student.getName() + " joined " + team.getName());
        log.info("Request {} accepted: student {} joined team {} ({} members{})", requestId, student.getId(), team.getId(),
                memberCount + 1, nowFull ? ", now FULL" : "");
        return message;
    }

    @Transactional
    public String reject(Long userId, Long requestId) {
        JoinRequest request = findRequest(requestId);
        Team team = findTeam(request.getTeamId());
        if (!request.isAnsweredBy(userId, team)) {
            throw new ForbiddenActionException("Only the person who received this request can reject it");
        }
        requirePending(request);
        joinRequestDao.updateStatus(requestId, RequestStatus.REJECTED);

        User student = findUser(request.getStudentId());
        if (request.getType() == RequestType.INVITE) {
            notificationService.notify(team.getLeaderId(), student.getName() + " declined your invite to " + team.getName());
        } else {
            notificationService.notify(student.getId(), "Your application to " + team.getName() + " was declined");
        }
        log.info("Request {} rejected by user {}", requestId, userId);
        return "Request rejected";
    }

    @Transactional
    public String cancel(Long userId, Long requestId) {
        JoinRequest request = findRequest(requestId);
        Team team = findTeam(request.getTeamId());
        if (!request.isSentBy(userId, team)) {
            throw new ForbiddenActionException("Only the person who sent this request can cancel it");
        }
        requirePending(request);
        joinRequestDao.updateStatus(requestId, RequestStatus.CANCELLED);
        log.info("Request {} cancelled by user {}", requestId, userId);
        return "Request cancelled";
    }

    // ------------------------------------------------------------------ clean-up used by several actions

    /** Cancels every pending request of one student in one hackathon, and tells each team leader why. */
    public void cancelPendingRequestsOfStudent(User student, Long hackathonId, String why) {
        for (JoinRequest pending : joinRequestDao.findPendingByStudentInHackathon(student.getId(), hackathonId)) {
            joinRequestDao.updateStatus(pending.getId(), RequestStatus.CANCELLED);
            Team otherTeam = findTeam(pending.getTeamId());
            notificationService.notify(otherTeam.getLeaderId(), student.getName() + " " + why
                    + ", so their request with " + otherTeam.getName() + " was cancelled");
        }
    }

    /** Cancels every pending request of one team, and tells each student why. */
    public void cancelPendingRequestsOfTeam(Team team, String why) {
        for (JoinRequest pending : joinRequestDao.findPendingByTeam(team.getId())) {
            joinRequestDao.updateStatus(pending.getId(), RequestStatus.CANCELLED);
            notificationService.notify(pending.getStudentId(), "Team " + team.getName() + " " + why
                    + ", so your request was cancelled");
        }
    }

    // ------------------------------------------------------------------ reading

    public MyRequestsView myRequests(Long userId) {
        // A user meets requests from two sides: as the student, and as the leader of a team. Merge both lists.
        List<JoinRequest> all = new ArrayList<>(joinRequestDao.findByStudent(userId));
        Set<Long> seen = new HashSet<>();
        for (JoinRequest r : all) {
            seen.add(r.getId());
        }
        for (JoinRequest r : joinRequestDao.findByLeader(userId)) {
            if (!seen.contains(r.getId())) {
                all.add(r);
            }
        }

        Lookup lookup = new Lookup();
        List<RequestView> incoming = new ArrayList<>();
        List<RequestView> outgoing = new ArrayList<>();
        for (JoinRequest r : all) {
            RequestView view = toView(r, userId, lookup);
            if (view.incoming()) {
                incoming.add(view);
            } else {
                outgoing.add(view);
            }
        }
        // Newest first.
        incoming.sort(Comparator.comparing(RequestView::createdAt).reversed());
        outgoing.sort(Comparator.comparing(RequestView::createdAt).reversed());
        return new MyRequestsView(incoming, outgoing);
    }

    /** Pending requests of one team, for the leader's team page. */
    public List<RequestView> pendingForTeam(Long teamId, Long viewerId) {
        Lookup lookup = new Lookup();
        List<RequestView> views = new ArrayList<>();
        for (JoinRequest r : joinRequestDao.findPendingByTeam(teamId)) {
            views.add(toView(r, viewerId, lookup));
        }
        return views;
    }

    private RequestView toView(JoinRequest r, Long viewerId, Lookup lookup) {
        Team team = lookup.team(r.getTeamId());
        Hackathon hackathon = lookup.hackathon(r.getHackathonId());
        User student = lookup.user(r.getStudentId());
        User leader = lookup.user(team.getLeaderId());
        return new RequestView(r.getId(), r.getType(), r.getStatus(), team.getId(), team.getName(),
                hackathon.getId(), hackathon.getTitle(), student.getId(), student.getName(), student.getUserCode(),
                leader.getName(), r.getCreatedAt(), r.isAnsweredBy(viewerId, team));
    }

    /**
     * Remembers teams, users and hackathons already loaded while building one list,
     * so each is read from the database only once. (An inner class: it can call RequestService's own methods.)
     */
    private class Lookup {
        private final Map<Long, Team> teams = new HashMap<>();
        private final Map<Long, User> users = new HashMap<>();
        private final Map<Long, Hackathon> hackathons = new HashMap<>();

        Team team(Long id) {
            if (!teams.containsKey(id)) {
                teams.put(id, findTeam(id));
            }
            return teams.get(id);
        }

        User user(Long id) {
            if (!users.containsKey(id)) {
                users.put(id, findUser(id));
            }
            return users.get(id);
        }

        Hackathon hackathon(Long id) {
            if (!hackathons.containsKey(id)) {
                hackathons.put(id, hackathonService.findHackathon(id));
            }
            return hackathons.get(id);
        }
    }

    // ------------------------------------------------------------------ small helpers

    /** "htf-abc123, HTF-XYZ789  htf-abc123" -> [HTF-ABC123, HTF-XYZ789] (upper case, no duplicates). */
    static List<String> parseCodes(String text) {
        List<String> codes = new ArrayList<>();
        for (String part : text.split("[,\\s]+")) {
            String code = part.trim().toUpperCase(Locale.ROOT);
            if (!code.isEmpty() && !codes.contains(code)) {
                codes.add(code);
            }
        }
        return codes;
    }

    private void requirePending(JoinRequest request) {
        if (!request.isPending()) {
            throw new BusinessRuleException("This request is already " + request.getStatus().name().toLowerCase(Locale.ROOT));
        }
    }

    private JoinRequest findRequest(Long id) {
        return joinRequestDao.findById(id).orElseThrow(() -> new NotFoundException("Request", id));
    }

    private Team findTeam(Long id) {
        return teamDao.findById(id).orElseThrow(() -> new NotFoundException("Team", id));
    }

    private User findUser(Long id) {
        return userDao.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }
}
