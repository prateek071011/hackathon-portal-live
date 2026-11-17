package com.htf.portal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.htf.portal.dao.JoinRequestDao;
import com.htf.portal.dao.TeamDao;
import com.htf.portal.dao.TeamMemberDao;
import com.htf.portal.dao.TeamSeekerDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.dto.SeekerView;
import com.htf.portal.exception.AlreadyInTeamException;
import com.htf.portal.exception.ForbiddenActionException;
import com.htf.portal.model.College;
import com.htf.portal.model.Hackathon;
import com.htf.portal.model.JoinRequest;
import com.htf.portal.model.RequestType;
import com.htf.portal.model.Role;
import com.htf.portal.model.Team;
import com.htf.portal.model.TeamSeeker;
import com.htf.portal.model.User;

/**
 * Tests the "looking for a team" list: who may join it, who may see it, and the best-match-first order.
 * Setting: team 10 (leader 100) in hackathon 1 is looking for "python, figma".
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeekerServiceTest {

    @Mock
    private TeamSeekerDao seekerDao;
    @Mock
    private TeamDao teamDao;
    @Mock
    private TeamMemberDao teamMemberDao;
    @Mock
    private JoinRequestDao joinRequestDao;
    @Mock
    private UserDao userDao;
    @Mock
    private CollegeDao collegeDao;
    @Mock
    private HackathonService hackathonService;

    private SeekerService seekerService;

    @BeforeEach
    void setUp() {
        seekerService = new SeekerService(seekerDao, teamDao, teamMemberDao, joinRequestDao, userDao, collegeDao,
                hackathonService);
        LocalDate deadline = LocalDate.now().plusDays(10);
        Hackathon hackathon = new Hackathon("CodeSprint", "Club", null, deadline.plusDays(5), deadline.plusDays(6), deadline, 2, 4);
        hackathon.setId(1L);
        when(hackathonService.findHackathon(1L)).thenReturn(hackathon);

        Team team = new Team(1L, 100L, "Quick Build", null, "python, figma");
        team.setId(10L);
        when(teamDao.findById(10L)).thenReturn(Optional.of(team));
        when(collegeDao.findById(1L)).thenReturn(Optional.of(new College(1L, "Greenfield", null)));

        when(userDao.findById(200L)).thenReturn(Optional.of(student(200L, "Meena", "figma, react")));       // 50%
        when(userDao.findById(201L)).thenReturn(Optional.of(student(201L, "Dev", "python, figma, cad")));   // 100%
        when(userDao.findById(202L)).thenReturn(Optional.of(student(202L, "Arun", "java")));                // 0%
    }

    private static User student(Long id, String name, String skills) {
        User u = new User(name, name.toLowerCase() + "@example.com", "hash", Role.STUDENT, 1L);
        u.setId(id);
        u.setUserCode("HTF-" + name.toUpperCase().substring(0, 3) + "234");
        u.setSkills(skills);
        return u;
    }

    @Test
    void aStudentAlreadyInATeamCannotJoinTheList() {
        when(teamMemberDao.existsByHackathonAndUser(1L, 200L)).thenReturn(true);

        assertThrows(AlreadyInTeamException.class, () -> seekerService.startLooking(200L, 1L));
        verify(seekerDao, never()).insert(any());
    }

    @Test
    void onlyTheLeaderSeesTheList() {
        assertThrows(ForbiddenActionException.class, () -> seekerService.seekersForTeam(200L, 10L));
    }

    @Test
    void listIsSortedByMatchAndMarksPendingInvites() {
        when(seekerDao.findAvailableByHackathon(1L)).thenReturn(List.of(
                new TeamSeeker(1L, 200L), new TeamSeeker(1L, 201L), new TeamSeeker(1L, 202L)));
        JoinRequest invite = new JoinRequest(10L, 1L, 200L, RequestType.INVITE);
        when(joinRequestDao.findPendingByTeam(10L)).thenReturn(List.of(invite));

        List<SeekerView> list = seekerService.seekersForTeam(100L, 10L);

        assertEquals(List.of("Dev", "Meena", "Arun"), List.of(list.get(0).name(), list.get(1).name(), list.get(2).name()));
        assertEquals(100, list.get(0).matchPercent());
        assertEquals(50, list.get(1).matchPercent());
        assertTrue(list.get(1).invitePending());   // Meena already has an invite from this team
    }
}
