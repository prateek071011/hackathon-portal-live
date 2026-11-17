package com.htf.portal.dto;

import java.time.LocalDateTime;

import com.htf.portal.model.RequestStatus;
import com.htf.portal.model.RequestType;

/**
 * A join request as the pages show it, with names instead of ids.
 * incoming = true when the person looking at it is the one who must accept or reject it.
 */
public record RequestView(Long id, RequestType type, RequestStatus status,
                          Long teamId, String teamName, Long hackathonId, String hackathonTitle,
                          Long studentId, String studentName, String studentUserCode, String leaderName,
                          LocalDateTime createdAt, boolean incoming) {
}
