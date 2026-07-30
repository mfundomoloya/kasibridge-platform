package com.kasibridge.procurement.controller;


import com.kasibridge.procurement.dto.CreateTenderRequest;
import com.kasibridge.procurement.dto.SpecificationIntegrityResponse;
import com.kasibridge.procurement.dto.TenderResponse;
import com.kasibridge.procurement.service.TenderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenders")
@RequiredArgsConstructor
@Slf4j
public class TenderController {

    private final TenderService service;

    @PostMapping
    public ResponseEntity<TenderResponse> createTender(
            @Valid @RequestBody CreateTenderRequest request
    ) {
        log.info("POST /api/v1/tenders - creating tender");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTender(request));
    }

    @GetMapping
    public ResponseEntity<Page<TenderResponse>> getTenders(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("GET /api/v1/tenders - fetching tenders");
        return ResponseEntity.ok(service.getTenders(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenderResponse> getTenderById(
            @PathVariable("id") Long id
    ) {
        log.info("GET /api/v1/tenders/{} - fetching tender", id);
        return ResponseEntity.ok(service.getTenderById(id));
    }

    @GetMapping("/reference/{tenderReference}")
    public ResponseEntity<TenderResponse> getTenderByReference(
            @PathVariable("tenderReference") String tenderReference
    ) {
        log.info("GET /api/v1/tenders/reference/{} - fetching tender", tenderReference);
        return ResponseEntity.ok(service.getTenderByReference(tenderReference));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<TenderResponse> publishTender(
            @PathVariable("id") Long id
    ) {
        log.info("PATCH /api/v1/tenders/{}/publish - publishing tender", id);
        return ResponseEntity.ok(service.publishTender(id));
    }

    @PatchMapping("/{id}/close-bidding")
    public ResponseEntity<TenderResponse> closeBidding(@PathVariable("id") Long id){
        log.info("PATCH /api/v1/tenders/{}/close-bidding - closing bidding", id);

        return ResponseEntity.ok(service.closeBidding(id));
    }

    @PatchMapping("/{id}/start-evaluation")
    public ResponseEntity<TenderResponse> startEvaluation(@PathVariable("id") Long id){
        log.info("PATCH /api/v1/tenders/{}/start-evalution - starting evaluation", id);

        return ResponseEntity.ok(service.startEvaluation(id));
    }

    @PatchMapping("/{id}/start-adjudication")
    public ResponseEntity<TenderResponse> startAdjudication(@PathVariable("id") Long id){
        log.info("PATCH /api/v1/tenders/{}/start-adjudication - starting adjudication", id);

        return ResponseEntity.ok(service.startAdjudication(id));
    }

    @GetMapping("/{id}/specification/verify")
    public ResponseEntity<SpecificationIntegrityResponse> verifySpecificationIntegrity(@PathVariable("id") Long id){
        log.info("GET /api/v1/tenders/{}/specification/verify - verifying specification integrity", id);

        return ResponseEntity.ok(service.verifySpecificationIntegrity(id));
    };
}
