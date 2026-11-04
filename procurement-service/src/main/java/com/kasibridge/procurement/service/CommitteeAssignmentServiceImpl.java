package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AssignCommitteeMemberRequest;
import com.kasibridge.procurement.dto.CommitteeAssignmentResponse;
import com.kasibridge.procurement.dto.CurrentUserCommitteeAssignmentResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import com.kasibridge.procurement.exception.SegregationOfDutiesException;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.repository.TenderCommitteeAssignmentRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitteeAssignmentServiceImpl
        implements CommitteeAssignmentService {

    private final TenderCommitteeAssignmentRepository
            assignmentRepository;

    private final TenderRepository
            tenderRepository;

    private final ProcurementAuditService
            auditService;

    private final CurrentUserService
            currentUserService;

    @Override
    @Transactional
    public CommitteeAssignmentResponse
    assignCommitteeMember(
            Long tenderId,
            AssignCommitteeMemberRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Committee assignment request "
                            + "is required."
            );
        }

        validateTenderExists(
                tenderId
        );

        validateAssignmentRequest(
                request
        );

        log.info(
                "Assigning userId={} to tenderId={} "
                        + "as role={}",
                request.getUserId(),
                tenderId,
                request.getCommitteeRole()
        );

        enforceSegregationOfDuties(
                tenderId,
                request.getUserId(),
                request.getAssignedByUserId()
        );

        TenderCommitteeAssignment assignment =
                TenderCommitteeAssignment.builder()
                        .tenderId(
                                tenderId
                        )
                        .userId(
                                request.getUserId()
                        )
                        .committeeRole(
                                request.getCommitteeRole()
                        )
                        .assignedByUserId(
                                request.getAssignedByUserId()
                        )
                        .active(
                                true
                        )
                        .reason(
                                normalizeReason(
                                        request.getReason()
                                )
                        )
                        .build();

        TenderCommitteeAssignment saved =
                assignmentRepository.save(
                        assignment
                );

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType
                        .COMMITTEE_MEMBER_ASSIGNED,
                tenderId,
                null,
                request.getAssignedByUserId(),
                "Committee member assigned",
                "Assigned userId="
                        + request.getUserId()
                        + " role="
                        + request.getCommitteeRole()
        );

        log.info(
                "Committee assignment created: "
                        + "id={} tenderId={} "
                        + "userId={} role={}",
                saved.getId(),
                saved.getTenderId(),
                saved.getUserId(),
                saved.getCommitteeRole()
        );

        return CommitteeAssignmentResponse.from(
                saved
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommitteeAssignmentResponse>
    getCommitteeAssignments(
            Long tenderId
    ) {
        validateTenderExists(
                tenderId
        );

        return assignmentRepository
                .findByTenderIdAndActiveTrue(
                        tenderId
                )
                .stream()
                .map(
                        CommitteeAssignmentResponse::from
                )
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommitteeAssignmentResponse>
    getCurrentUserAssignments() {
        Long currentUserId =
                requireCurrentUserId();

        log.debug(
                "Fetching active committee "
                        + "assignments for userId={}",
                currentUserId
        );

        return assignmentRepository
                .findByUserIdAndActiveTrueOrderByAssignedAtDesc(
                        currentUserId
                )
                .stream()
                .map(
                        CommitteeAssignmentResponse::from
                )
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommitteeAssignmentResponse>
    getCurrentUserAssignmentsByRole(
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    ) {
        requireCommitteeRole(
                committeeRole
        );

        Long currentUserId =
                requireCurrentUserId();

        log.debug(
                "Fetching active committee "
                        + "assignments for "
                        + "userId={} role={}",
                currentUserId,
                committeeRole
        );

        return assignmentRepository
                .findByUserIdAndCommitteeRoleAndActiveTrueOrderByAssignedAtDesc(
                        currentUserId,
                        committeeRole
                )
                .stream()
                .map(
                        CommitteeAssignmentResponse::from
                )
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrentUserCommitteeAssignmentResponse>
    getCurrentUserAssignmentQueue(
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    ) {
        Long currentUserId =
                requireCurrentUserId();

        List<TenderCommitteeAssignment> assignments =
                loadCurrentUserAssignments(
                        currentUserId,
                        committeeRole
                );

        if (assignments.isEmpty()) {
            log.debug(
                    "No active committee assignments "
                            + "found for userId={} role={}",
                    currentUserId,
                    committeeRole
            );

            return Collections.emptyList();
        }

        Set<Long> tenderIds =
                assignments.stream()
                        .map(
                                TenderCommitteeAssignment
                                        ::getTenderId
                        )
                        .collect(
                                Collectors.toCollection(
                                        LinkedHashSet::new
                                )
                        );

        validateAssignmentTenderIds(
                tenderIds
        );

        Map<Long, Tender> tendersById =
                tenderRepository
                        .findAllById(
                                tenderIds
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Tender::getId,
                                        Function.identity()
                                )
                        );

        validateEveryAssignedTenderExists(
                tenderIds,
                tendersById
        );

        List<CurrentUserCommitteeAssignmentResponse>
                queue =
                assignments.stream()
                        .map(
                                assignment ->
                                        mapAssignmentQueueItem(
                                                assignment,
                                                tendersById
                                        )
                        )
                        .toList();

        log.debug(
                "Loaded {} enriched committee "
                        + "assignment records for "
                        + "userId={} role={}",
                queue.size(),
                currentUserId,
                committeeRole
        );

        return queue;
    }

    private List<TenderCommitteeAssignment>
    loadCurrentUserAssignments(
            Long currentUserId,
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    ) {
        if (committeeRole == null) {
            return assignmentRepository
                    .findByUserIdAndActiveTrueOrderByAssignedAtDesc(
                            currentUserId
                    );
        }

        return assignmentRepository
                .findByUserIdAndCommitteeRoleAndActiveTrueOrderByAssignedAtDesc(
                        currentUserId,
                        committeeRole
                );
    }

    private CurrentUserCommitteeAssignmentResponse
    mapAssignmentQueueItem(
            TenderCommitteeAssignment assignment,
            Map<Long, Tender> tendersById
    ) {
        Tender tender =
                tendersById.get(
                        assignment.getTenderId()
                );

        if (tender == null) {
            throw new TenderNotFoundException(
                    "Tender not found for committee "
                            + "assignment ID: "
                            + assignment.getId()
                            + ", tender ID: "
                            + assignment.getTenderId()
            );
        }

        return CurrentUserCommitteeAssignmentResponse
                .from(
                        assignment,
                        tender
                );
    }

    private void validateEveryAssignedTenderExists(
            Set<Long> tenderIds,
            Map<Long, Tender> tendersById
    ) {
        for (Long tenderId : tenderIds) {
            if (!tendersById.containsKey(tenderId)) {
                throw new TenderNotFoundException(
                        "Tender not found with ID: "
                                + tenderId
                                + " for an active "
                                + "committee assignment."
                );
            }
        }
    }

    private void validateAssignmentTenderIds(
            Set<Long> tenderIds
    ) {
        if (
                tenderIds.contains(null)
                        || tenderIds.stream()
                        .anyMatch(
                                tenderId ->
                                        tenderId <= 0
                        )
        ) {
            throw new IllegalStateException(
                    "An active committee assignment "
                            + "contains an invalid "
                            + "tender ID."
            );
        }
    }

    private void validateTenderExists(
            Long tenderId
    ) {
        if (
                tenderId == null
                        || tenderId <= 0
        ) {
            throw new IllegalArgumentException(
                    "A valid tender ID is required."
            );
        }

        if (
                !tenderRepository.existsById(
                        tenderId
                )
        ) {
            throw new TenderNotFoundException(
                    "Tender not found with ID: "
                            + tenderId
            );
        }
    }

    private void validateAssignmentRequest(
            AssignCommitteeMemberRequest request
    ) {
        if (
                request.getUserId() == null
                        || request.getUserId() <= 0
        ) {
            throw new IllegalArgumentException(
                    "A valid committee member "
                            + "user ID is required."
            );
        }

        requireCommitteeRole(
                request.getCommitteeRole()
        );

        if (
                request.getAssignedByUserId() == null
                        || request.getAssignedByUserId() <= 0
        ) {
            throw new IllegalArgumentException(
                    "A valid assigning user ID "
                            + "is required."
            );
        }
    }

    private void requireCommitteeRole(
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    ) {
        if (committeeRole == null) {
            throw new IllegalArgumentException(
                    "Committee role is required."
            );
        }
    }

    private void enforceSegregationOfDuties(
            Long tenderId,
            Long userId,
            Long actorUserId
    ) {
        boolean alreadyAssigned =
                assignmentRepository
                        .existsByTenderIdAndUserIdAndActiveTrue(
                                tenderId,
                                userId
                        );

        if (!alreadyAssigned) {
            return;
        }

        auditService.recordFailure(
                ProcurementAuditEvent.AuditEventType
                        .COMMITTEE_ASSIGNMENT_REJECTED_SOD,
                tenderId,
                null,
                actorUserId,
                "Committee assignment rejected "
                        + "due to segregation of duties",
                "UserId="
                        + userId
                        + " already has an active "
                        + "committee role on tenderId="
                        + tenderId
        );

        throw new SegregationOfDutiesException(
                "User cannot hold multiple "
                        + "committee roles on the "
                        + "same tender."
        );
    }

    private Long requireCurrentUserId() {
        Long currentUserId =
                currentUserService
                        .getCurrentUserId();

        if (
                currentUserId == null
                        || currentUserId <= 0
        ) {
            throw new IllegalStateException(
                    "An authenticated user ID "
                            + "is required."
            );
        }

        return currentUserId;
    }

    private String normalizeReason(
            String reason
    ) {
        if (reason == null) {
            return null;
        }

        String normalizedReason =
                reason.trim();

        if (normalizedReason.isEmpty()) {
            return null;
        }

        return normalizedReason;
    }
}