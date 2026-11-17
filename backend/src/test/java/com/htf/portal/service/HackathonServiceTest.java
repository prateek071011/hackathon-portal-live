package com.htf.portal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.htf.portal.dao.HackathonDao;
import com.htf.portal.dto.HackathonForm;
import com.htf.portal.exception.BusinessRuleException;
import com.htf.portal.model.Hackathon;
import com.htf.portal.model.HackathonStatus;

/**
 * Unit tests for HackathonService rules and for how a hackathon's status follows the calendar.
 */
@ExtendWith(MockitoExtension.class)
class HackathonServiceTest {

    private static final LocalDate DEADLINE = LocalDate.of(2026, 12, 10);
    private static final LocalDate START = LocalDate.of(2026, 12, 20);
    private static final LocalDate END = LocalDate.of(2026, 12, 22);

    @Mock
    private HackathonDao hackathonDao;

    private HackathonService hackathonService;

    @BeforeEach
    void setUp() {
        hackathonService = new HackathonService(hackathonDao);
    }

    private HackathonForm form(int min, int max, LocalDate deadline) {
        return new HackathonForm("CodeSprint", "Tech Club", "", START, END, deadline, min, max);
    }

    private Hackathon stored(int min, int max) {
        Hackathon h = new Hackathon("CodeSprint", "Tech Club", null, START, END, DEADLINE, min, max);
        h.setId(7L);
        return h;
    }

    @Test
    void createRejectsMinBiggerThanMax() {
        BusinessRuleException e = assertThrows(BusinessRuleException.class,
                () -> hackathonService.create(form(4, 2, DEADLINE)));
        assertEquals("Minimum team size cannot be bigger than the maximum", e.getMessage());
        verify(hackathonDao, never()).insert(any());
    }

    @Test
    void createRejectsDeadlineOnOrAfterStart() {
        assertThrows(BusinessRuleException.class, () -> hackathonService.create(form(2, 4, START)));
        verify(hackathonDao, never()).insert(any());
    }

    @Test
    void teamSizesCannotChangeOnceTeamsExist() {
        when(hackathonDao.findById(7L)).thenReturn(Optional.of(stored(2, 4)));
        when(hackathonDao.countTeams(7L)).thenReturn(3L);

        assertThrows(BusinessRuleException.class, () -> hackathonService.update(7L, form(2, 5, DEADLINE)));
        verify(hackathonDao, never()).update(any());
    }

    @Test
    void hackathonWithTeamsCannotBeDeleted() {
        when(hackathonDao.findById(7L)).thenReturn(Optional.of(stored(2, 4)));
        when(hackathonDao.countTeams(7L)).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> hackathonService.delete(7L));
        verify(hackathonDao, never()).deleteById(anyLong());
    }

    @Test
    void statusFollowsTheCalendar() {
        Hackathon h = stored(2, 4);
        assertEquals(HackathonStatus.OPEN, h.statusOn(DEADLINE.minusDays(5)));
        assertEquals(HackathonStatus.OPEN, h.statusOn(DEADLINE));            // deadline day still open
        assertEquals(HackathonStatus.CLOSED, h.statusOn(DEADLINE.plusDays(1)));
        assertEquals(HackathonStatus.ONGOING, h.statusOn(START));
        assertEquals(HackathonStatus.ONGOING, h.statusOn(END));
        assertEquals(HackathonStatus.COMPLETED, h.statusOn(END.plusDays(1)));
    }
}
