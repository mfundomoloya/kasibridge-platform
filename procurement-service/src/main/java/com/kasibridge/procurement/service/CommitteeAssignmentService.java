package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AssignCommitteeMemberRequest;
import com.kasibridge.procurement.dto.CommitteeAssignmentResponse;
import com.kasibridge.procurement.dto.CurrentUserCommitteeAssignmentResponse;
import com.kasibridge.procurement.entity.TenderCommitteeAssignment;

import java.util.List;

public interface CommitteeAssignmentService {

    CommitteeAssignmentResponse
    assignCommitteeMember(
            Long tenderId,
            AssignCommitteeMemberRequest request
    );

    List<CommitteeAssignmentResponse>
    getCommitteeAssignments(
            Long tenderId
    );

    /*
     * Returns the authenticated user's active
     * assignment records without tender enrichment.
     *
     * This preserves the existing endpoint contract.
     */
    List<CommitteeAssignmentResponse>
    getCurrentUserAssignments();

    /*
     * Returns the authenticated user's active
     * assignments for one committee role without
     * tender enrichment.
     *
     * This preserves the existing endpoint contract.
     */
    List<CommitteeAssignmentResponse>
    getCurrentUserAssignmentsByRole(
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    );

    /*
     * Returns an enriched dashboard queue for the
     * authenticated user.
     *
     * Each response combines the user's active
     * committee assignment with the associated
     * tender summary.
     *
     * When committeeRole is null, all active
     * assignments belonging to the authenticated
     * user are returned.
     */
    List<CurrentUserCommitteeAssignmentResponse>
    getCurrentUserAssignmentQueue(
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    );
}