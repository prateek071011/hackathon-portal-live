package com.htf.portal.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.htf.portal.dao.CollegeDao;
import com.htf.portal.dao.RegistrationDao;
import com.htf.portal.dao.TeamDao;
import com.htf.portal.dao.TeamMemberDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.dto.RegistrationDetail;
import com.htf.portal.dto.RegistrationRow;
import com.htf.portal.dto.TeamRegistrationView;
import com.htf.portal.exception.BusinessRuleException;
import com.htf.portal.exception.ForbiddenActionException;
import com.htf.portal.exception.NotFoundException;
import com.htf.portal.io.CsvWriter;
import com.htf.portal.io.SnapshotStorage;
import com.htf.portal.model.College;
import com.htf.portal.model.Hackathon;
import com.htf.portal.model.Registration;
import com.htf.portal.model.RegistrationSnapshot;
import com.htf.portal.model.RegistrationStatus;
import com.htf.portal.model.Team;
import com.htf.portal.model.TeamMember;
import com.htf.portal.model.TeamStatus;
import com.htf.portal.model.User;

/**
 * Registration through the college (section 5.4): the leader submits the team, the leader's college approves
 * (team REGISTERED, registration code, snapshot file) or rejects it with a reason (team can try again).
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private static final String CSV_HEADER = "registrationCode,team,hackathon,leader,leaderEmail,members,approvedAt";

    private final RegistrationDao registrationDao;
    private final TeamDao teamDao;
    private final TeamMemberDao teamMemberDao;
    private final UserDao userDao;
    private final CollegeDao collegeDao;
    private final HackathonService hackathonService;
    private final RequestService requestService;
    private final NotificationService notificationService;
    private final SnapshotStorage snapshotStorage;
    private final ActivityLog activityLog;

    public RegistrationService(RegistrationDao registrationDao, TeamDao teamDao, TeamMemberDao teamMemberDao,
                               UserDao userDao, CollegeDao collegeDao, HackathonService hackathonService,
                               RequestService requestService, NotificationService notificationService,
                               SnapshotStorage snapshotStorage, ActivityLog activityLog) {
        this.registrationDao = registrationDao;
        this.teamDao = teamDao;
        this.teamMemberDao = teamMemberDao;
        this.userDao = userDao;
        this.collegeDao = collegeDao;
        this.hackathonService = hackathonService;
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.snapshotStorage = snapshotStorage;
        this.activityLog = activityLog;
    }

    // ------------------------------------------------------------------ student side

    /** Why the leader cannot submit right now, or null if they can. The team page shows it next to the button. */
    public String submitBlockedReason(Team team, Hackathon hackathon, int memberCount) {
        if (team.getStatus() == TeamStatus.SUBMITTED) {
            return "Already submitted. Waiting for your college to review it";
        }
        if (team.getStatus() == TeamStatus.REGISTERED) {
            return "Already registered";
        }
        if (!hackathon.isRegistrationOpenOn(LocalDate.now())) {
            return "Registration closed on " + hackathon.getRegistrationDeadline();
        }
        if (memberCount < hackathon.getMinTeamSize()) {
            return "Need " + hackathon.getMinTeamSize() + " members — you have " + memberCount;
        }
        if (memberCount > hackathon.getMaxTeamSize()) {
            return "Too many members: the maximum is " + hackathon.getMaxTeamSize();
        }
        return null;
    }

    /** Submit: team -> SUBMITTED (roster locked), pending requests cancelled, a PENDING row for the leader's college. */
    @Transactional
    public String submit(Long leaderId, Long teamId) {
        Team team = teamDao.findByIdForUpdate(teamId).orElseThrow(() -> new NotFoundException("Team", teamId));
        if (!team.isLeader(leaderId)) {
            throw new ForbiddenActionException("Only the team leader can submit the team");
        }
        Hackathon hackathon = hackathonService.findHackathon(team.getHackathonId());
        TeamRules.requireRegistrationOpen(hackathon);
        String blocked = submitBlockedReason(team, hackathon, (int) teamMemberDao.countByTeam(teamId));
        if (blocked != null) {
            throw new BusinessRuleException(blocked);
        }
        User leader = findUser(leaderId);
        if (leader.getCollegeId() == null) {
            throw new BusinessRuleException("Your account has no college to submit to");
        }
        College college = findCollege(leader.getCollegeId());

        teamDao.updateStatus(teamId, TeamStatus.SUBMITTED);
        team.setStatus(TeamStatus.SUBMITTED);
        requestService.cancelPendingRequestsOfTeam(team, "was submitted to the college");
        registrationDao.insert(new Registration(teamId, college.getId()));

        for (User member : membersOf(team)) {
            if (!member.getId().equals(leaderId)) {
                notificationService.notify(member.getId(), leader.getName() + " submitted team " + team.getName()
                        + " to " + college.getName() + " for approval");
            }
        }
        activityLog.record(leader.getName() + " submitted " + team.getName() + " to " + college.getName());
        log.info("Team submitted: teamId={}, collegeId={}", teamId, college.getId());
        return "Team submitted to " + college.getName() + " for approval";
    }

    /** The team's latest registration for its own page, with the snapshot read back from disk once approved. */
    public TeamRegistrationView registrationForTeam(Long teamId) {
        Registration reg = registrationDao.findLatestByTeam(teamId).orElse(null);
        if (reg == null) {
            return null;
        }
        TeamRegistrationView.Snapshot snapshot = null;
        if (reg.getStatus() == RegistrationStatus.APPROVED) {
            snapshot = readSnapshot(reg.getRegistrationCode());
        }
        return new TeamRegistrationView(reg.getId(), reg.getStatus(), findCollege(reg.getCollegeId()).getName(),
                reg.getRegistrationCode(), reg.getRejectReason(), reg.getCreatedAt(), reg.getDecidedAt(), snapshot);
    }

    // ------------------------------------------------------------------ college side

    public List<RegistrationRow> listForCollege(Long reviewerId, RegistrationStatus status) {
        User reviewer = findUser(reviewerId);
        List<RegistrationRow> rows = new ArrayList<>();
        for (Registration reg : registrationDao.findByCollegeAndStatus(reviewer.getCollegeId(), status)) {
            rows.add(toRow(reg));
        }
        return rows;
    }

    public RegistrationDetail detailForCollege(Long reviewerId, Long registrationId) {
        Registration reg = registrationDao.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration", registrationId));
        requireMyCollege(findUser(reviewerId), reg);

        Team team = findTeam(reg.getTeamId());
        Hackathon hackathon = hackathonService.findHackathon(team.getHackathonId());
        List<RegistrationDetail.Member> members = new ArrayList<>();
        for (User u : membersOf(team)) {
            String collegeName = u.getCollegeId() == null ? null : findCollege(u.getCollegeId()).getName();
            members.add(new RegistrationDetail.Member(u.getName(), u.getEmail(), u.getUserCode(), u.getRollNumber(),
                    u.getBranch(), u.getStudyYear(), collegeName, team.isLeader(u.getId())));
        }
        return new RegistrationDetail(toRow(reg), team.getDescription(), hackathon.getStartDate(),
                hackathon.getEndDate(), hackathon.getMinTeamSize(), hackathon.getMaxTeamSize(), members);
    }

    /**
     * Approve: one transaction. Registration -> APPROVED with a new code, team -> REGISTERED, snapshot file written,
     * members notified. The file is written BEFORE the method ends: if writing fails, the IOException becomes an
     * unchecked exception and the database changes roll back, so we never have a REGISTERED team without its file.
     */
    @Transactional
    public String approve(Long reviewerId, Long registrationId) {
        Registration reg = lockPendingRegistration(reviewerId, registrationId);
        Team team = teamDao.findByIdForUpdate(reg.getTeamId()).orElseThrow(() -> new NotFoundException("Team", reg.getTeamId()));
        Hackathon hackathon = hackathonService.findHackathon(team.getHackathonId());
        College college = findCollege(reg.getCollegeId());

        LocalDateTime now = LocalDateTime.now();
        String code = registrationCode(now.getYear(), reg.getId());
        reg.setStatus(RegistrationStatus.APPROVED);
        reg.setRegistrationCode(code);
        reg.setDecidedAt(now);
        registrationDao.update(reg);
        teamDao.updateStatus(team.getId(), TeamStatus.REGISTERED);

        List<User> members = membersOf(team);
        List<String> memberNames = new ArrayList<>();
        for (User member : members) {
            memberNames.add(member.getName());
        }
        RegistrationSnapshot snapshot = new RegistrationSnapshot(code, team.getName(), hackathon.getTitle(),
                college.getName(), memberNames, now, findUser(team.getLeaderId()).getEmail());
        try {
            snapshotStorage.save(snapshot);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write snapshot " + code, e);
        }

        for (User member : members) {
            notificationService.notify(member.getId(), "Team " + team.getName() + " is registered for "
                    + hackathon.getTitle() + "! Registration code: " + code);
        }
        activityLog.record(college.getName() + " approved " + team.getName() + " (" + code + ")");
        log.info("Registration approved: id={}, teamId={}, code={}", reg.getId(), team.getId(), code);
        return "Team " + team.getName() + " approved. Registration code " + code;
    }

    /** Reject with a reason: the team goes back to OPEN (or FULL if it is at the maximum size) and may submit again. */
    @Transactional
    public String reject(Long reviewerId, Long registrationId, String reason) {
        Registration reg = lockPendingRegistration(reviewerId, registrationId);
        Team team = teamDao.findByIdForUpdate(reg.getTeamId()).orElseThrow(() -> new NotFoundException("Team", reg.getTeamId()));
        Hackathon hackathon = hackathonService.findHackathon(team.getHackathonId());
        College college = findCollege(reg.getCollegeId());

        reg.setStatus(RegistrationStatus.REJECTED);
        reg.setRejectReason(reason.trim());
        reg.setDecidedAt(LocalDateTime.now());
        registrationDao.update(reg);

        long memberCount = teamMemberDao.countByTeam(team.getId());
        TeamStatus newStatus = memberCount >= hackathon.getMaxTeamSize() ? TeamStatus.FULL : TeamStatus.OPEN;
        teamDao.updateStatus(team.getId(), newStatus);

        for (User member : membersOf(team)) {
            notificationService.notify(member.getId(), college.getName() + " did not approve team " + team.getName()
                    + ". Reason: " + reason.trim());
        }
        activityLog.record(college.getName() + " rejected " + team.getName());
        log.info("Registration rejected: id={}, teamId={}, team back to {}", reg.getId(), team.getId(), newStatus);
        return "Team " + team.getName() + " rejected. The leader has been told why.";
    }

    /** "Download CSV" of the college's registered teams, written straight to the HTTP response. */
    public void exportApprovedCsv(Long reviewerId, Writer target) throws IOException {
        User reviewer = findUser(reviewerId);
        List<List<String>> rows = new ArrayList<>();
        for (Registration reg : registrationDao.findByCollegeAndStatus(reviewer.getCollegeId(), RegistrationStatus.APPROVED)) {
            Team team = findTeam(reg.getTeamId());
            User leader = findUser(team.getLeaderId());
            List<String> names = new ArrayList<>();
            for (User member : membersOf(team)) {
                names.add(member.getName());
            }
            rows.add(List.of(reg.getRegistrationCode(), team.getName(),
                    hackathonService.findHackathon(team.getHackathonId()).getTitle(), leader.getName(), leader.getEmail(),
                    String.join("; ", names), reg.getDecidedAt().toString()));
        }
        new CsvWriter().write(CSV_HEADER, rows, target);
        log.info("CSV export: {} registered teams for collegeId={}", rows.size(), reviewer.getCollegeId());
    }

    // ------------------------------------------------------------------ helpers

    // "REG-2026-000123": the year of approval and the registration's id padded to 6 digits (ids are unique).
    static String registrationCode(int year, long registrationId) {
        return "REG-" + year + "-" + String.format("%06d", registrationId);
    }

    // Locks the row, then checks it belongs to the reviewer's college and is still waiting for a decision.
    private Registration lockPendingRegistration(Long reviewerId, Long registrationId) {
        Registration reg = registrationDao.findByIdForUpdate(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration", registrationId));
        requireMyCollege(findUser(reviewerId), reg);
        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessRuleException("This team was already " + reg.getStatus().name().toLowerCase(Locale.ROOT));
        }
        return reg;
    }

    private void requireMyCollege(User reviewer, Registration reg) {
        if (reviewer.getCollegeId() == null || !reviewer.getCollegeId().equals(reg.getCollegeId())) {
            throw new ForbiddenActionException("This team was submitted to another college");
        }
    }

    private TeamRegistrationView.Snapshot readSnapshot(String code) {
        try {
            RegistrationSnapshot s = snapshotStorage.read(code);
            return new TeamRegistrationView.Snapshot(SnapshotStorage.fileNameFor(code), s.getRegistrationCode(),
                    s.getTeamName(), s.getHackathonTitle(), s.getCollegeName(), s.getMemberNames(), s.getApprovedAt(),
                    s.getLeaderEmail());
        } catch (IOException e) {
            // The registration itself is still valid; the page just shows no snapshot.
            log.warn("Could not read snapshot {}: {}", code, e.getMessage());
            return null;
        }
    }

    private RegistrationRow toRow(Registration reg) {
        Team team = findTeam(reg.getTeamId());
        return new RegistrationRow(reg.getId(), team.getId(), team.getName(),
                hackathonService.findHackathon(team.getHackathonId()).getTitle(), findUser(team.getLeaderId()).getName(),
                (int) teamMemberDao.countByTeam(team.getId()), reg.getStatus(), reg.getRegistrationCode(),
                reg.getRejectReason(), reg.getCreatedAt(), reg.getDecidedAt());
    }

    private List<User> membersOf(Team team) {
        List<User> users = new ArrayList<>();
        for (TeamMember m : teamMemberDao.findByTeam(team.getId())) {
            users.add(findUser(m.getUserId()));
        }
        return users;
    }

    private Team findTeam(Long id) {
        return teamDao.findById(id).orElseThrow(() -> new NotFoundException("Team", id));
    }

    private User findUser(Long id) {
        return userDao.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    private College findCollege(Long id) {
        return collegeDao.findById(id).orElseThrow(() -> new NotFoundException("College", id));
    }
}
