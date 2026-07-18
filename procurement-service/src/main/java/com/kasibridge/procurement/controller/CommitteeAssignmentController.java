package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.AssignCommitteeMemberRequest;
import com.kasibridge.procurement.dto.CommitteeAssignmentResponse;
import com.kasibridge.procurement.service.CommitteeAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenders/{tenderId}/committee")
@RequiredArgsConstructor
@Slf4j
public class CommitteeAssignmentController {
    private final CommitteeAssignmentService service;

    @PostMapping
    public ResponseEntity<CommitteeAssignmentResponse> assignCommitteeMember(
            @PathVariable("tenderId") Long tenderId,
            @Valid @RequestBody AssignCommitteeMemberRequest request) {

        log.info("POST /api/v1/tenders/{}/committee - assigning userId={} role={}",
                tenderId,
                request.getUserId(),
                request.getCommitteeRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(service.assignCommitteeMember(tenderId, request));
    }

    @GetMapping
    public ResponseEntity<List<CommitteeAssignmentResponse>> getCommitteeAssignments(@PathVariable("tenderId") Long tenderId) {

        log.info(
                "GET /api/v1/tenders/{}/committee - fetching committee assignments",
                tenderId
        );

        return ResponseEntity.ok(service.getCommitteeAssignments(tenderId));
    }
}
