package com.htf.portal.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import com.htf.portal.dto.TeamForm;
import com.htf.portal.exception.AlreadyInTeamException;
import com.htf.portal.exception.BusinessRuleException;
import com.htf.portal.exception.DeadlinePassedException;
import com.htf.portal.model.Hackathon;
import com.htf.portal.model.Role;
import com.htf.portal.model.Team;
import com.htf.portal.model.TeamStatus;
import com.htf.portal.model.User;

/**
 * Tests team rules from section 5.2: one team per hackathon, the deadline, leaving a FULL team,
 * and the locked roster after submission.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamServiceTest {

    @Mock
    private TeamDao teamDao;
    @Mock
    private TeamMemberDao teamMemberDao;
    @Mock
    private JoinRequestDao joinRequestDao;
    @Mock
    private TeamSeekerDao seekerDao;
    @Mock
    private UserDao userDao;
    @Mock
    private HackathonService hackathonService;
    @Mock
    private RequestService requestService;
    @Mock
    private RegistrationService registrationService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ActivityLog activityLog;

    private TeamService teamService;
    private final TeamForm form = new TeamForm("Byte Me", "We build things", "java, react");

    @BeforeEach
    void setUp() {
        teamService = new TeamService(teamDao, teamMemberDao, joinRequestDao, seekerDao, userDao, hackathonService,
                requestService, registrationService, notificationService, activityLog);
        User student = new User("Asha", "asha@example.com", "hash", Role.STUDENT, 1L);
        student.setId(100L);
        when(userDao.findById(100L)).thenReturn(Optional.of(student));
        User member = new User("Ravi", "ravi@example.com", "hash", Role.STUDENT, 1L);
        member.setId(200L);
        when(userDao.findById(200L)).thenReturn(Optional.of(member));
    }

    private static Hackathon hackathonWithDeadlineIn(int days) {
        LocalDate deadline = LocalDate.now().plusDays(days);
        Hackathon h = new Hackathon("CodeSprint", "Club", null, deadline.plusDays(5), deadline.plusDays(6), deadline, 2, 3);
        h.setId(1L);
        return h;
    }

    private Team teamWithStatus(TeamStatus status) {
        Team team = new Team(1L, 100L, "Byte Me", null, null);
        team.setId(10L);
        team.setStatus(status);
        when(teamDao.findByIdForUpdate(10L)).thenReturn(Optional.of(team));
        return team;
    }

    @Test
    void cannotCreateASecondTeamInTheSameHackathon() {
        when(hackathonService.findHackathon(1L)).thenReturn(hackathonWithDeadlineIn(5));
        when(teamMemberDao.existsByHackathonAndUser(1L, 100L)).thenReturn(true);

        assertThrows(AlreadyInTeamException.class, () -> teamService.createTeam(100L, 1L, form));
        verify(teamDao, never()).insert(any());
    }

    @Test
    void cannotCreateATeamAfterTheDeadline() {
        when(hackathonService.findHackathon(1L)).thenReturn(hackathonWithDeadlineIn(-2));

        assertThrows(DeadlinePassedException.class, () -> teamService.createTeam(100L, 1L, form));
        verify(teamDao, never()).insert(any());
    }

    @Test
    void leavingAFullTeamReopensIt() {
        teamWithStatus(TeamStatus.FULL);
        when(teamMemberDao.deleteByTeamAndUser(10L, 200L)).thenReturn(true);

        teamService.leaveTeam(200L, 10L);

        verify(teamDao).updateStatus(10L, TeamStatus.OPEN);
    }

    @Test
    void nobodyCanLeaveASubmittedTeam() {
        teamWithStatus(TeamStatus.SUBMITTED);

        assertThrows(BusinessRuleException.class, () -> teamService.leaveTeam(200L, 10L));
        verify(teamMemberDao, never()).deleteByTeamAndUser(10L, 200L);
    }
}
