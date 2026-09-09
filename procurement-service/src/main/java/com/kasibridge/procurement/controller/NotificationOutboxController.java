package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.MarkNotificationFailedRequest;
import com.kasibridge.procurement.dto.NotificationDispatchResponse;
import com.kasibridge.procurement.dto.NotificationOutboxResponse;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.service.NotificationDispatcherService;
import com.kasibridge.procurement.service.NotificationOutboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/outbox")
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxController {
    private final NotificationOutboxService service;
    private final NotificationDispatcherService dispatcherService;

    @GetMapping
    public ResponseEntity<Page<NotificationOutboxResponse>> getNotifications(@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                                                                 Pageable pageable
    ) {
        return ResponseEntity.ok(service.getNotifications(pageable));
    }

    @GetMapping("/in-app")
    public ResponseEntity<Page<NotificationOutboxResponse>> getInAppNotifications(
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        log.info(
                "GET /api/v1/notifications/outbox/in-app"
        );

        return ResponseEntity.ok(
                service.getInAppNotifications(pageable)
        );
    }

    @GetMapping("/in-app/unread")
    public ResponseEntity<Page<NotificationOutboxResponse>> getUnreadInAppNotifications(
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        log.info(
                "GET /api/v1/notifications/outbox/in-app/unread"
        );

        return ResponseEntity.ok(
                service.getUnreadInAppNotifications(pageable)
        );
    }

    @GetMapping("/in-app/read")
    public ResponseEntity<Page<NotificationOutboxResponse>> getReadInAppNotifications(
            @PageableDefault(
                    sort = "readAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        log.info(
                "GET /api/v1/notifications/outbox/in-app/read"
        );

        return ResponseEntity.ok(
                service.getReadInAppNotifications(pageable)
        );
    }

    @PatchMapping("/in-app/{id}/read")
    public ResponseEntity<NotificationOutboxResponse> markInAppNotificationRead(@PathVariable("id") Long id) {
        log.info(
                "PATCH /api/v1/notifications/outbox/in-app/{}/read",
                id
        );
        return ResponseEntity.ok(
                service.markInAppNotificationRead(id)
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<NotificationOutboxResponse>> getNotificationsByStatus(@PathVariable("status") NotificationOutbox.NotificationStatus status,
                                                                                     @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                                                                     Pageable pageable
    ) {
        return ResponseEntity.ok(service.getNotificationsByStatus(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationOutboxResponse> getNotificationById(@PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(service.getNotificationById(id));
    }

    @PatchMapping("/{id}/mark-sent")
    public ResponseEntity<NotificationOutboxResponse> markSent(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.markSent(id));
    }

    @PatchMapping("/{id}/mark-failed")
    public ResponseEntity<NotificationOutboxResponse> markFailed(@PathVariable("id") Long id,
                                                                 @Valid @RequestBody MarkNotificationFailedRequest request
    ) {
        return ResponseEntity.ok(service.markFailed(id, request));
    }

    @PostMapping("/dispatch-pending")
    public ResponseEntity<NotificationDispatchResponse> dispatchPending() {
        log.info(
                "POST /api/v1/notifications/outbox/dispatch-pending"
        );

        return ResponseEntity.ok(
                dispatcherService.dispatchPending()
        );
    }

    @PostMapping("/retry-failed")
    public ResponseEntity<NotificationDispatchResponse> retryFailed() {
        log.info(
                "POST /api/v1/notifications/outbox/retry-failed"
        );

        return ResponseEntity.ok(
                dispatcherService.retryFailed()
        );
    }

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<NotificationOutboxResponse> dispatchOne(
            @PathVariable("id") Long id
    ) {
        log.info(
                "POST /api/v1/notifications/outbox/{}/dispatch",
                id
        );

        return ResponseEntity.ok(
                dispatcherService.dispatchOne(id)
        );
    }
}
