package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AssignCommitteeMemberRequest;
import com.kasibridge.procurement.dto.CommitteeAssignmentResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import com.kasibridge.procurement.exception.SegregationOfDutiesException;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.repository.TenderCommitteeAssignmentRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitteeAssignmentServiceImpl implements CommitteeAssignmentService {

    private final TenderCommitteeAssignmentRepository assignmentRepository;
    private final TenderRepository tenderRepository;
    private final ProcurementAuditService auditService;

    @Override
    @Transactional
    public CommitteeAssignmentResponse assignCommitteeMember(Long tenderId, AssignCommitteeMemberRequest request) {
        log.info("Assigning userId={} to tenderId={} as role={}",
                request.getUserId(),
                tenderId,
                request.getCommitteeRole()
        );

        validateTenderExists(tenderId);

        enforceSegregationOfDuties(tenderId, request.getUserId(), request.getAssignedByUserId());


        TenderCommitteeAssignment assignment = TenderCommitteeAssignment.builder()
                .tenderId(tenderId)
                .userId(request.getUserId())
                .committeeRole(request.getCommitteeRole())
                .assignedByUserId(request.getAssignedByUserId())
                .active(true)
                .reason(request.getReason())
                .build();

        TenderCommitteeAssignment saved = assignmentRepository.save(assignment);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.COMMITTEE_MEMBER_ASSIGNED,
                tenderId,
                null,
                request.getAssignedByUserId(),
                "Committee member assigned",
                "Assigned userId=" + request.getUserId() + " role=" + request.getCommitteeRole()
        );

        log.info(
                "Committee assignment created: id={} tenderId={} userId={} role={}",
                saved.getId(),
                saved.getTenderId(),
                saved.getUserId(),
                saved.getCommitteeRole()
        );

        return CommitteeAssignmentResponse.from(saved);

    }

    @Override
    public List<CommitteeAssignmentResponse> getCommitteeAssignments(Long tenderId) {

        validateTenderExists(tenderId);

        return assignmentRepository.findByTenderIdAndActiveTrue(tenderId)
                .stream()
                .map(CommitteeAssignmentResponse::from)
                .toList();
    }

    private void validateTenderExists(Long tenderId) {
        if(!tenderRepository.existsById(tenderId)) {
            throw new TenderNotFoundException("Tender not found with ID: " + tenderId);
        }
    }

    private void enforceSegregationOfDuties(Long tenderId, Long userId, Long actorUserId) {
        boolean alreadyAssigned = assignmentRepository.existsByTenderIdAndUserIdAndActiveTrue(tenderId, userId);
        if (alreadyAssigned) {
            auditService.recordFailure(
                    ProcurementAuditEvent.AuditEventType.COMMITTEE_ASSIGNMENT_REJECTED_SOD,
                    tenderId,
                    null,
                    actorUserId,
                    "Committee assignment rejected due to a segregation of duties",
                    "UserId=" + userId + " already has an active committee role on tenderId=" + tenderId
            );
            throw new SegregationOfDutiesException(
                    "User cannot hold multiple committee roles on the same tender."
            );
        }
    }
}
