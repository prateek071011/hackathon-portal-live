package com.htf.portal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.htf.portal.dao.JoinRequestDao;
import com.htf.portal.dao.TeamDao;
import com.htf.portal.dao.TeamMemberDao;
import com.htf.portal.dao.TeamSeekerDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.exception.AlreadyInTeamException;
import com.htf.portal.exception.DeadlinePassedException;
import com.htf.portal.exception.DuplicateKeyException;
import com.htf.portal.exception.TeamFullException;
import com.htf.portal.model.Hackathon;
import com.htf.portal.model.JoinRequest;
import com.htf.portal.model.RequestStatus;
import com.htf.portal.model.RequestType;
import com.htf.portal.model.Role;
import com.htf.portal.model.Team;
import com.htf.portal.model.TeamMember;
import com.htf.portal.model.TeamStatus;
import com.htf.portal.model.User;

/**
 * Tests the accept-request rules from section 5.3 with Mockito mocks instead of a database.
 * Setting: team 10 (leader 100) in hackathon 1 with max 3 members; student 200 applied (request 5).
 * LENIENT: the shared set-up stubs more than some tests need; that is fine here.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestServiceTest {

    private static final Long HACKATHON_ID = 1L;
    private static final Long TEAM_ID = 10L;
    private static final Long LEADER_ID = 100L;
    private static final Long STUDENT_ID = 200L;
    private static final Long REQUEST_ID = 5L;

    @Mock
    private JoinRequestDao joinRequestDao;
    @Mock
    private TeamDao teamDao;
    @Mock
    private TeamMemberDao teamMemberDao;
    @Mock
    private TeamSeekerDao seekerDao;
    @Mock
    private UserDao userDao;
    @Mock
    private HackathonService hackathonService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ActivityLog activityLog;

    private RequestService requestService;
    private Team team;

    @BeforeEach
    void setUp() {
        requestService = new RequestService(joinRequestDao, teamDao, teamMemberDao, seekerDao, userDao,
                hackathonService, notificationService, activityLog);

        team = new Team(HACKATHON_ID, LEADER_ID, "Byte Me", null, "java, react");
        team.setId(TEAM_ID);
        when(teamDao.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(teamDao.findByIdForUpdate(TEAM_ID)).thenReturn(Optional.of(team));

        JoinRequest application = new JoinRequest(TEAM_ID, HACKATHON_ID, STUDENT_ID, RequestType.APPLICATION);
        application.setId(REQUEST_ID);
        when(joinRequestDao.findById(REQUEST_ID)).thenReturn(Optional.of(application));

        when(hackathonService.findHackathon(HACKATHON_ID)).thenReturn(hackathonWithDeadlineIn(10));
        when(userDao.findById(LEADER_ID)).thenReturn(Optional.of(user(LEADER_ID, "Asha")));
        when(userDao.findById(STUDENT_ID)).thenReturn(Optional.of(user(STUDENT_ID, "Ravi")));
    }

    private static Hackathon hackathonWithDeadlineIn(int days) {
        LocalDate deadline = LocalDate.now().plusDays(days);
        Hackathon h = new Hackathon("CodeSprint", "Club", null, deadline.plusDays(5), deadline.plusDays(6), deadline, 2, 3);
        h.setId(HACKATHON_ID);
        return h;
    }

    private static User user(Long id, String name) {
        User u = new User(name, name.toLowerCase() + "@example.com", "hash", Role.STUDENT, 1L);
        u.setId(id);
        return u;
    }

    @Test
    void cannotJoinTwoTeamsInOneHackathon() {
        when(teamMemberDao.countByTeam(TEAM_ID)).thenReturn(1L);
        when(teamMemberDao.existsByHackathonAndUser(HACKATHON_ID, STUDENT_ID)).thenReturn(true);

        assertThrows(AlreadyInTeamException.class, () -> requestService.accept(LEADER_ID, REQUEST_ID));
        verify(teamMemberDao, never()).insert(any());
        verify(joinRequestDao, never()).updateStatus(anyLong(), any());
    }

    @Test
    void duplicateKeyFromTheDatabaseBecomesAlreadyInTeam() {
        // Our check said "not in a team", but another team accepted the student a moment earlier:
        // the UNIQUE(hackathon_id, user_id) constraint refuses the insert.
        when(teamMemberDao.countByTeam(TEAM_ID)).thenReturn(1L);
        when(teamMemberDao.existsByHackathonAndUser(HACKATHON_ID, STUDENT_ID)).thenReturn(false);
        when(teamMemberDao.insert(any(TeamMember.class))).thenThrow(new DuplicateKeyException("Duplicate entry", null));

        assertThrows(AlreadyInTeamException.class, () -> requestService.accept(LEADER_ID, REQUEST_ID));
        verify(joinRequestDao, never()).updateStatus(anyLong(), any());
    }

    @Test
    void acceptingIntoAFullTeamFails() {
        when(teamMemberDao.countByTeam(TEAM_ID)).thenReturn(3L);   // already at max (3)

        assertThrows(TeamFullException.class, () -> requestService.accept(LEADER_ID, REQUEST_ID));
        verify(teamMemberDao, never()).insert(any());
    }

    @Test
    void teamThatBecomesFullCancelsItsOtherPendingRequests() {
        when(teamMemberDao.countByTeam(TEAM_ID)).thenReturn(2L);   // this accept fills the last seat
        when(teamMemberDao.existsByHackathonAndUser(HACKATHON_ID, STUDENT_ID)).thenReturn(false);
        JoinRequest otherApplication = new JoinRequest(TEAM_ID, HACKATHON_ID, 300L, RequestType.APPLICATION);
        otherApplication.setId(6L);
        when(joinRequestDao.findPendingByTeam(TEAM_ID)).thenReturn(List.of(otherApplication));
        when(joinRequestDao.findPendingByStudentInHackathon(STUDENT_ID, HACKATHON_ID)).thenReturn(List.of());

        String message = requestService.accept(LEADER_ID, REQUEST_ID);

        assertEquals("Ravi joined your team", message);
        verify(teamMemberDao).insert(any(TeamMember.class));
        verify(joinRequestDao).updateStatus(REQUEST_ID, RequestStatus.ACCEPTED);
        verify(teamDao).updateStatus(TEAM_ID, TeamStatus.FULL);
        verify(joinRequestDao).updateStatus(6L, RequestStatus.CANCELLED);
        verify(seekerDao).deleteByHackathonAndUser(HACKATHON_ID, STUDENT_ID);   // off the "looking for a team" list
    }

    @Test
    void deadlineBlocksAccepting() {
        when(hackathonService.findHackathon(HACKATHON_ID)).thenReturn(hackathonWithDeadlineIn(-1));

        assertThrows(DeadlinePassedException.class, () -> requestService.accept(LEADER_ID, REQUEST_ID));
        verify(teamMemberDao, never()).insert(any());
    }

    @Test
    void deadlineBlocksApplying() {
        when(hackathonService.findHackathon(HACKATHON_ID)).thenReturn(hackathonWithDeadlineIn(-1));

        assertThrows(DeadlinePassedException.class, () -> requestService.apply(STUDENT_ID, TEAM_ID));
        verify(joinRequestDao, never()).insert(any());
    }

    @Test
    void inviteSkipsCodesThatCannotBeInvited() {
        when(teamMemberDao.countByTeam(TEAM_ID)).thenReturn(1L);
        User free = user(201L, "Meena");
        User taken = user(202L, "Kiran");
        when(userDao.findByUserCode("HTF-FREE22")).thenReturn(Optional.of(free));
        when(userDao.findByUserCode("HTF-TAKEN2")).thenReturn(Optional.of(taken));
        when(userDao.findByUserCode("HTF-NOBODY")).thenReturn(Optional.empty());
        when(teamMemberDao.existsByHackathonAndUser(HACKATHON_ID, 202L)).thenReturn(true);

        var result = requestService.invite(LEADER_ID, TEAM_ID, "htf-free22, HTF-TAKEN2 HTF-NOBODY");

        assertEquals(List.of("Meena (HTF-FREE22)"), result.sent());
        assertEquals(2, result.skipped().size());
        verify(joinRequestDao).insert(any(JoinRequest.class));   // exactly one invite saved
    }
}
