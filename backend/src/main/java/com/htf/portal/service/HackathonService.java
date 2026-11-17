package com.htf.portal.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.htf.portal.dao.HackathonDao;
import com.htf.portal.dto.HackathonForm;
import com.htf.portal.dto.HackathonView;
import com.htf.portal.exception.BusinessRuleException;
import com.htf.portal.exception.NotFoundException;
import com.htf.portal.model.Hackathon;
import com.htf.portal.util.TextUtils;

/**
 * Hackathon rules: students list and view them, the admin creates, edits and deletes them.
 * Once a hackathon has teams, its team sizes are fixed and it cannot be deleted.
 */
@Service
public class HackathonService {

    private static final Logger log = LoggerFactory.getLogger(HackathonService.class);

    private final HackathonDao hackathonDao;

    public HackathonService(HackathonDao hackathonDao) {
        this.hackathonDao = hackathonDao;
    }

    /** All hackathons: open ones first (closing soonest at the top), then the rest (most recent first). */
    public List<HackathonView> listAll() {
        LocalDate today = LocalDate.now();
        Map<Long, Integer> teamCounts = hackathonDao.countTeamsPerHackathon();

        List<HackathonView> open = new ArrayList<>();
        List<HackathonView> others = new ArrayList<>();
        // The DAO returns them sorted by deadline, oldest first.
        for (Hackathon h : hackathonDao.findAll()) {
            HackathonView view = HackathonView.from(h, today, teamCounts.getOrDefault(h.getId(), 0));
            if (h.isRegistrationOpenOn(today)) {
                open.add(view);
            } else {
                others.add(0, view);   // adding at the front reverses them: most recent first
            }
        }
        open.addAll(others);
        return open;
    }

    public HackathonView getView(Long id) {
        Hackathon h = findHackathon(id);
        return HackathonView.from(h, LocalDate.now(), (int) hackathonDao.countTeams(id));
    }

    // Other services (teams, requests) use this to load a hackathon or fail with 404.
    public Hackathon findHackathon(Long id) {
        return hackathonDao.findById(id).orElseThrow(() -> new NotFoundException("Hackathon", id));
    }

    public HackathonView create(HackathonForm form) {
        Hackathon h = fromForm(form);
        checkRules(h);
        hackathonDao.insert(h);
        log.info("Hackathon created: id={}, title={}", h.getId(), h.getTitle());
        return HackathonView.from(h, LocalDate.now(), 0);
    }

    public HackathonView update(Long id, HackathonForm form) {
        Hackathon existing = findHackathon(id);
        Hackathon changed = fromForm(form);
        changed.setId(id);
        checkRules(changed);

        long teamCount = hackathonDao.countTeams(id);
        boolean sizesChanged = existing.getMinTeamSize() != changed.getMinTeamSize()
                || existing.getMaxTeamSize() != changed.getMaxTeamSize();
        if (teamCount > 0 && sizesChanged) {
            throw new BusinessRuleException("Team sizes cannot change once teams exist (" + teamsText(teamCount) + ")");
        }

        hackathonDao.update(changed);
        log.info("Hackathon updated: id={}", id);
        return HackathonView.from(changed, LocalDate.now(), (int) teamCount);
    }

    public void delete(Long id) {
        Hackathon h = findHackathon(id);
        long teamCount = hackathonDao.countTeams(id);
        if (teamCount > 0) {
            throw new BusinessRuleException("Delete its " + teamsText(teamCount) + " first");
        }
        hackathonDao.deleteById(id);
        log.info("Hackathon deleted: id={}, title={}", id, h.getTitle());
    }

    // "1 team", "3 teams"
    private static String teamsText(long count) {
        return count == 1 ? "1 team" : count + " teams";
    }

    private void checkRules(Hackathon h) {
        String problem = h.checkRules();
        if (problem != null) {
            throw new BusinessRuleException(problem);
        }
    }

    private static Hackathon fromForm(HackathonForm form) {
        return new Hackathon(form.title().trim(), form.organizer().trim(), TextUtils.trimToNull(form.description()),
                form.startDate(), form.endDate(), form.registrationDeadline(),
                form.minTeamSize(), form.maxTeamSize());
    }
}
