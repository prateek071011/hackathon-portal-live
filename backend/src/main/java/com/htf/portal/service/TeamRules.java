package com.htf.portal.service;

import java.time.LocalDate;

import com.htf.portal.exception.BusinessRuleException;
import com.htf.portal.exception.DeadlinePassedException;
import com.htf.portal.exception.ForbiddenActionException;
import com.htf.portal.exception.TeamFullException;
import com.htf.portal.model.Hackathon;
import com.htf.portal.model.Team;
import com.htf.portal.model.TeamStatus;
import com.htf.portal.model.User;

/**
 * Small checks that both TeamService and RequestService need. Each one throws our exception if the rule is broken.
 * The class has no "public": it is package-private, so only classes in the service package can use it.
 */
final class TeamRules {

    private TeamRules() {
    }

    static void requireStudent(User user) {
        if (!user.isStudent()) {
            throw new ForbiddenActionException("Only students can do this");
        }
    }

    static void requireRegistrationOpen(Hackathon hackathon) {
        if (!hackathon.isRegistrationOpenOn(LocalDate.now())) {
            throw new DeadlinePassedException(hackathon.getRegistrationDeadline());
        }
    }

    /** Why the roster cannot change any more, or null if it still can. */
    static String lockedReason(Team team) {
        if (team.getStatus() == TeamStatus.SUBMITTED) {
            return "The team has been submitted to the college, so the roster is locked";
        }
        if (team.getStatus() == TeamStatus.REGISTERED) {
            return "The team is registered, so the roster is locked";
        }
        return null;
    }

    static void requireNotLocked(Team team) {
        String reason = lockedReason(team);
        if (reason != null) {
            throw new BusinessRuleException(reason);
        }
    }

    // New members (invite, apply, accept) need a team that is OPEN: not locked and not full.
    static void requireOpenForNewMembers(Team team) {
        requireNotLocked(team);
        if (team.getStatus() == TeamStatus.FULL) {
            throw new TeamFullException(team.getName());
        }
    }
}
