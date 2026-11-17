package com.htf.portal.dao;

import java.util.List;

import com.htf.portal.model.JoinRequest;
import com.htf.portal.model.RequestStatus;

/**
 * Database access for the join_requests table (invites and applications).
 * The basic find/insert/update/delete come from GenericDao. Extra queries are listed here.
 */
public interface JoinRequestDao extends GenericDao<JoinRequest, Long> {

    boolean existsPending(Long teamId, Long studentId);

    List<JoinRequest> findPendingByTeam(Long teamId);

    List<JoinRequest> findPendingByStudentInHackathon(Long studentId, Long hackathonId);

    // Every request (any status) where this user is the student.
    List<JoinRequest> findByStudent(Long studentId);

    // Every request (any status) for teams this user leads.
    List<JoinRequest> findByLeader(Long leaderId);

    boolean updateStatus(Long requestId, RequestStatus status);
}
