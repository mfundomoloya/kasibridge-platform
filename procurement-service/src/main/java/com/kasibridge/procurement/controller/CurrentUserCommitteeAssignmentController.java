package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.CommitteeAssignmentResponse;
import com.kasibridge.procurement.dto.CurrentUserCommitteeAssignmentResponse;
import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import com.kasibridge.procurement.service.CommitteeAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(
        "/api/v1/committee-assignments"
)
@RequiredArgsConstructor
@Slf4j
public class CurrentUserCommitteeAssignmentController {

    private final CommitteeAssignmentService
            committeeAssignmentService;

    /*
     * Preserves the existing compact assignment
     * response for compatibility.
     *
     * Examples:
     *
     * GET /api/v1/committee-assignments/my
     *
     * GET /api/v1/committee-assignments/my
     *     ?committeeRole=EVALUATOR
     */
    @GetMapping("/my")
    public ResponseEntity<
            List<CommitteeAssignmentResponse>
            >
    getCurrentUserAssignments(
            @RequestParam(
                    name = "committeeRole",
                    required = false
            )
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    ) {
        if (committeeRole == null) {
            log.debug(
                    "GET /api/v1/"
                            + "committee-assignments/my"
            );

            return ResponseEntity.ok(
                    committeeAssignmentService
                            .getCurrentUserAssignments()
            );
        }

        log.debug(
                "GET /api/v1/"
                        + "committee-assignments/my "
                        + "- committeeRole={}",
                committeeRole
        );

        return ResponseEntity.ok(
                committeeAssignmentService
                        .getCurrentUserAssignmentsByRole(
                                committeeRole
                        )
        );
    }

    /*
     * Returns an enriched dashboard queue containing
     * both assignment information and tender details.
     *
     * Examples:
     *
     * GET /api/v1/committee-assignments/my/queue
     *
     * GET /api/v1/committee-assignments/my/queue
     *     ?committeeRole=EVALUATOR
     *
     * GET /api/v1/committee-assignments/my/queue
     *     ?committeeRole=ADJUDICATOR
     */
    @GetMapping("/my/queue")
    public ResponseEntity<
            List<CurrentUserCommitteeAssignmentResponse>
            >
    getCurrentUserAssignmentQueue(
            @RequestParam(
                    name = "committeeRole",
                    required = false
            )
            TenderCommitteeAssignment.CommitteeRole
                    committeeRole
    ) {
        log.debug(
                "GET /api/v1/"
                        + "committee-assignments/my/queue "
                        + "- committeeRole={}",
                committeeRole
        );

        return ResponseEntity.ok(
                committeeAssignmentService
                        .getCurrentUserAssignmentQueue(
                                committeeRole
                        )
        );
    }
}