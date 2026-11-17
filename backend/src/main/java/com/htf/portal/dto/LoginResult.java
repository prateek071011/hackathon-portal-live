package com.htf.portal.dto;

/**
 * What login and signup return: the JWT to send on later requests, and who logged in.
 */
public record LoginResult(String token, UserSummary user) {
}
