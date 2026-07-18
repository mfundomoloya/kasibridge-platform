package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AssignCommitteeMemberRequest;
import com.kasibridge.procurement.dto.CommitteeAssignmentResponse;
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

    @Override
    @Transactional
    public CommitteeAssignmentResponse assignCommitteeMember(Long tenderId, AssignCommitteeMemberRequest request) {
        log.info("Assigning userId={} to tenderId={} as role={}",
                request.getUserId(),
                tenderId,
                request.getCommitteeRole()
        );

        validateTenderExists(tenderId);

        enforceSegregationOfDuties(tenderId, request.getUserId());


        TenderCommitteeAssignment assignment = TenderCommitteeAssignment.builder()
                .tenderId(tenderId)
                .userId(request.getUserId())
                .committeeRole(request.getCommitteeRole())
                .assignedByUserId(request.getAssignedByUserId())
                .active(true)
                .reason(request.getReason())
                .build();

        TenderCommitteeAssignment saved = assignmentRepository.save(assignment);

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
        return List.of();
    }

    private void validateTenderExists(Long tenderId) {
        if(!tenderRepository.existsById(tenderId)) {
            throw new TenderNotFoundException("Tender not found with ID: " + tenderId);
        }
    }

    private void enforceSegregationOfDuties(Long tenderId, Long userId) {
        boolean alreadyAssigned = assignmentRepository.existsByTenderIdAndUserIdAndActiveTrue(tenderId, userId);
        if (alreadyAssigned) {
            throw new SegregationOfDutiesException(
                    "User cannot hold multiple committee roles on the same tender."
            );
        }
    }
}
