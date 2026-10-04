package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AssignCommitteeMemberRequest;
import com.kasibridge.procurement.dto.CommitteeAssignmentResponse;

import java.util.List;

public interface CommitteeAssignmentService {
    CommitteeAssignmentResponse assignCommitteeMember(
            Long tenderId,
            AssignCommitteeMemberRequest request
    );

    List<CommitteeAssignmentResponse> getCommitteeAssignments(Long tenderId);
}
