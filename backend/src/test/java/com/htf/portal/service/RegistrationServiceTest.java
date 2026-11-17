package com.htf.portal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import com.htf.portal.dao.CollegeDao;
import com.htf.portal.dao.RegistrationDao;
import com.htf.portal.dao.TeamDao;
import com.htf.portal.dao.TeamMemberDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.exception.BusinessRuleException;
import com.htf.portal.exception.ForbiddenActionException;
import com.htf.portal.io.SnapshotStorage;
import com.htf.portal.model.College;
import com.htf.portal.model.Hackathon;
import com.htf.portal.model.Registration;
import com.htf.portal.model.Role;
import com.htf.portal.model.Team;
import com.htf.portal.model.TeamMember;
import com.htf.portal.model.TeamStatus;
import com.htf.portal.model.User;

/**
 * Tests section 5.4: the submit rules and the college decision rules.
 * Setting: team 10 (leader 100, college 1) in a hackathon that needs 3 to 4 members.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationServiceTest {

    @Mock
    private RegistrationDao registrationDao;
    @Mock
    private TeamDao teamDao;
    @Mock
    private TeamMemberDao teamMemberDao;
    @Mock
    private UserDao userDao;
    @Mock
    private CollegeDao collegeDao;
    @Mock
    private HackathonService hackathonService;
    @Mock
    private RequestService requestService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SnapshotStorage snapshotStorage;
    @Mock
    private ActivityLog activityLog;

    private RegistrationService registrationService;
    private Team team;
    private Hackathon hackathon;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(registrationDao, teamDao, teamMemberDao, userDao, collegeDao,
                hackathonService, requestService, notificationService, snapshotStorage, activityLog);

        LocalDate deadline = LocalDate.now().plusDays(10);
        hackathon = new Hackathon("CodeSprint", "Club", null, deadline.plusDays(5), deadline.plusDays(6), deadline, 3, 4);
        hackathon.setId(1L);
        when(hackathonService.findHackathon(1L)).thenReturn(hackathon);

        team = new Team(1L, 100L, "Byte Me", null, null);
        team.setId(10L);
        when(teamDao.findById(10L)).thenReturn(Optional.of(team));
        when(teamDao.findByIdForUpdate(10L)).thenReturn(Optional.of(team));

        when(userDao.findById(100L)).thenReturn(Optional.of(user(100L, Role.STUDENT, 1L)));
        when(userDao.findById(500L)).thenReturn(Optional.of(user(500L, Role.COLLEGE, 1L)));   // reviewer of college 1
        when(userDao.findById(600L)).thenReturn(Optional.of(user(600L, Role.COLLEGE, 2L)));   // reviewer of college 2
        when(collegeDao.findById(1L)).thenReturn(Optional.of(new College(1L, "Greenfield", null)));
        when(teamMemberDao.findByTeam(10L)).thenReturn(List.of(new TeamMember(10L, 1L, 100L)));
    }

    private static User user(Long id, Role role, Long collegeId) {
        User u = new User("User " + id, "u" + id + "@example.com", "hash", role, collegeId);
        u.setId(id);
        return u;
    }

    private Registration pendingRegistration() {
        Registration reg = new Registration(10L, 1L);
        reg.setId(3L);
        when(registrationDao.findByIdForUpdate(3L)).thenReturn(Optional.of(reg));
        return reg;
    }

    @Test
    void submitNeedsTheMinimumNumberOfMembers() {
        when(teamMemberDao.countByTeam(10L)).thenReturn(2L);

        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> registrationService.submit(100L, 10L));
        assertEquals("Need 3 members — you have 2", e.getMessage());
        verify(registrationDao, never()).insert(any());
    }

    @Test
    void submitLocksTheTeamAndCreatesAPendingRegistration() {
        when(teamMemberDao.countByTeam(10L)).thenReturn(3L);

        registrationService.submit(100L, 10L);

        verify(teamDao).updateStatus(10L, TeamStatus.SUBMITTED);
        verify(requestService).cancelPendingRequestsOfTeam(team, "was submitted to the college");
        verify(registrationDao).insert(any(Registration.class));
    }

    @Test
    void anotherCollegeCannotApprove() {
        pendingRegistration();

        assertThrows(ForbiddenActionException.class, () -> registrationService.approve(600L, 3L));
        verify(teamDao, never()).updateStatus(any(), any());
    }

    @Test
    void rejectSendsTheTeamBackToOpen() {
        pendingRegistration();
        team.setStatus(TeamStatus.SUBMITTED);
        when(teamMemberDao.countByTeam(10L)).thenReturn(3L);   // 3 of max 4: not full

        registrationService.reject(500L, 3L, "Roll numbers missing");

        verify(teamDao).updateStatus(10L, TeamStatus.OPEN);
    }

    @Test
    void registrationCodeHasYearAndPaddedId() {
        assertEquals("REG-2026-000123", RegistrationService.registrationCode(2026, 123));
        assertNull(registrationService.submitBlockedReason(team, hackathon, 4));
    }
}
