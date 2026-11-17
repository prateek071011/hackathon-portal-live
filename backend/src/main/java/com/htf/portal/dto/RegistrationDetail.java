package com.htf.portal.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything a college reviewer needs to decide on one submitted team: the summary, the event,
 * and each member's details (so the reviewer can check roll numbers and colleges).
 */
public record RegistrationDetail(RegistrationRow summary, String teamDescription, LocalDate eventStart,
                                 LocalDate eventEnd, int minTeamSize, int maxTeamSize, List<Member> members) {

    public record Member(String name, String email, String userCode, String rollNumber, String branch,
                         Integer year, String collegeName, boolean leader) {
    }
}
