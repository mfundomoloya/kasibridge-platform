package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenderCommitteeAssignmentRepository
        extends JpaRepository<
        TenderCommitteeAssignment,
        Long
        > {

    /*
     * Existing tender-level lookup used by the
     * committee assignment service.
     */
    List<TenderCommitteeAssignment>
    findByTenderIdAndActiveTrue(
            Long tenderId
    );

    /*
     * Returns all active committee assignments
     * belonging to one user. The newest assignment
     * appears first in the user's dashboard queue.
     */
    List<TenderCommitteeAssignment>
    findByUserIdAndActiveTrueOrderByAssignedAtDesc(
            Long userId
    );

    /*
     * Returns active assignments for one user and
     * committee role. This supports role-specific
     * Evaluator and Adjudicator queues.
     */
    List<TenderCommitteeAssignment>
    findByUserIdAndCommitteeRoleAndActiveTrueOrderByAssignedAtDesc(
            Long userId,
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    );

    /*
     * Existing segregation-of-duties check used to
     * prevent one user from holding multiple active
     * committee roles on the same tender.
     */
    boolean
    existsByTenderIdAndUserIdAndActiveTrue(
            Long tenderId,
            Long userId
    );

    /*
     * Existing role-specific authorization check.
     * This can verify that a user is assigned to a
     * tender in the required committee role.
     */
    boolean
    existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
            Long tenderId,
            Long userId,
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    );

    /*
     * Existing role-based tender lookup used by the
     * procurement notification service to find active
     * Adjudicators for anomaly notifications.
     */
    List<TenderCommitteeAssignment>
    findByTenderIdAndCommitteeRoleAndActiveTrue(
            Long tenderId,
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    );
}