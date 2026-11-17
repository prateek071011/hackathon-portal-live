package com.htf.portal.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.htf.portal.dao.CollegeDao;
import com.htf.portal.dao.JoinRequestDao;
import com.htf.portal.dao.TeamDao;
import com.htf.portal.dao.TeamMemberDao;
import com.htf.portal.dao.TeamSeekerDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.dto.SeekerView;
import com.htf.portal.exception.AlreadyInTeamException;
import com.htf.portal.exception.ConflictException;
import com.htf.portal.exception.DuplicateKeyException;
import com.htf.portal.exception.ForbiddenActionException;
import com.htf.portal.exception.NotFoundException;
import com.htf.portal.model.Hackathon;
import com.htf.portal.model.JoinRequest;
import com.htf.portal.model.Team;
import com.htf.portal.model.TeamSeeker;
import com.htf.portal.model.User;
import com.htf.portal.util.SkillUtils;

/**
 * "I'm looking for a team": a student without a team puts themselves on a hackathon's list (opt-in),
 * and team leaders see that list sorted by how well each student matches what the team is looking for.
 * Joining or creating a team takes the student off the list again (see RequestService.accept, TeamService.createTeam).
 */
@Service
public class SeekerService {

    private static final Logger log = LoggerFactory.getLogger(SeekerService.class);
    private static final String ALREADY_LISTED = "You are already on the list for this hackathon";

    // Best match first; for equal match, alphabetical by name.
    private static final Comparator<SeekerView> BEST_MATCH_FIRST =
            Comparator.comparing(SeekerView::matchPercent, Comparator.reverseOrder())
                    .thenComparing(SeekerView::name);

    private final TeamSeekerDao seekerDao;
    private final TeamDao teamDao;
    private final TeamMemberDao teamMemberDao;
    private final JoinRequestDao joinRequestDao;
    private final UserDao userDao;
    private final CollegeDao collegeDao;
    private final HackathonService hackathonService;

    public SeekerService(TeamSeekerDao seekerDao, TeamDao teamDao, TeamMemberDao teamMemberDao,
                         JoinRequestDao joinRequestDao, UserDao userDao, CollegeDao collegeDao,
                         HackathonService hackathonService) {
        this.seekerDao = seekerDao;
        this.teamDao = teamDao;
        this.teamMemberDao = teamMemberDao;
        this.joinRequestDao = joinRequestDao;
        this.userDao = userDao;
        this.collegeDao = collegeDao;
        this.hackathonService = hackathonService;
    }

    public String startLooking(Long userId, Long hackathonId) {
        User user = findUser(userId);
        TeamRules.requireStudent(user);
        Hackathon hackathon = hackathonService.findHackathon(hackathonId);
        TeamRules.requireRegistrationOpen(hackathon);
        if (teamMemberDao.existsByHackathonAndUser(hackathonId, userId)) {
            throw new AlreadyInTeamException("You are already in a team for this hackathon");
        }
        if (seekerDao.exists(hackathonId, userId)) {
            throw new ConflictException(ALREADY_LISTED);
        }
        try {
            seekerDao.insert(new TeamSeeker(hackathonId, userId));
        } catch (DuplicateKeyException e) {
            // Two clicks at the same moment: UNIQUE(hackathon_id, user_id) lets only one row in.
            throw new ConflictException(ALREADY_LISTED);
        }
        log.info("Looking for a team: userId={}, hackathonId={}", userId, hackathonId);
        return "You are on the list. Team leaders in " + hackathon.getTitle() + " can now find and invite you.";
    }

    // Leaving the list twice is harmless: either way the student is no longer on it, so no error.
    public String stopLooking(Long userId, Long hackathonId) {
        hackathonService.findHackathon(hackathonId);
        seekerDao.deleteByHackathonAndUser(hackathonId, userId);
        log.info("Stopped looking for a team: userId={}, hackathonId={}", userId, hackathonId);
        return "You are no longer on the list";
    }

    /** The "Find teammates" list for one team. Only its leader may see it. */
    public List<SeekerView> seekersForTeam(Long leaderId, Long teamId) {
        Team team = teamDao.findById(teamId).orElseThrow(() -> new NotFoundException("Team", teamId));
        if (!team.isLeader(leaderId)) {
            throw new ForbiddenActionException("Only the team leader can see who is looking for a team");
        }

        // Students this team already has a pending invite or application with.
        Set<Long> pendingStudentIds = new HashSet<>();
        for (JoinRequest r : joinRequestDao.findPendingByTeam(teamId)) {
            pendingStudentIds.add(r.getStudentId());
        }
        Map<Long, String> collegeNames = new HashMap<>();

        List<SeekerView> views = new ArrayList<>();
        for (TeamSeeker seeker : seekerDao.findAvailableByHackathon(team.getHackathonId())) {
            User student = findUser(seeker.getUserId());
            int match = SkillUtils.matchPercent(team.getLookingFor(), SkillUtils.toSet(student.getSkills()));
            views.add(new SeekerView(student.getId(), student.getName(), student.getUserCode(),
                    collegeName(student.getCollegeId(), collegeNames), student.getBranch(), student.getStudyYear(),
                    student.getSkills(), match, pendingStudentIds.contains(student.getId())));
        }
        views.sort(BEST_MATCH_FIRST);
        return views;
    }

    // Looks each college up once, then remembers it in the HashMap.
    private String collegeName(Long collegeId, Map<Long, String> cache) {
        if (!cache.containsKey(collegeId)) {
            cache.put(collegeId, collegeDao.findById(collegeId).orElseThrow(() -> new NotFoundException("College", collegeId)).getName());
        }
        return cache.get(collegeId);
    }

    private User findUser(Long userId) {
        return userDao.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }
}
