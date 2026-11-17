package com.htf.portal.dto;

import java.util.List;

/**
 * The "My Requests" page: requests waiting for my answer (incoming) and requests I sent (outgoing).
 */
public record MyRequestsView(List<RequestView> incoming, List<RequestView> outgoing) {
}
