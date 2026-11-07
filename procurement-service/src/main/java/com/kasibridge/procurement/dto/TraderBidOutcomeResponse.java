package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.Tender;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TraderBidOutcomeResponse {

    private Long bidId;

    private String bidReference;

    private Bid.BidStatus bidStatus;

    private String bidderAlias;

    private BigDecimal priceAmount;

    private LocalDateTime submittedAt;

    private Long tenderId;

    private String tenderReference;

    private String tenderTitle;

    private Tender.TenderStatus tenderStatus;

    private boolean compliancePassed;

    private String complianceFailureReason;

    private TraderBidOutcome outcome;

    private LocalDateTime awardedAt;

    private String publishedOutcomeReason;

    public enum TraderBidOutcome {
        PENDING,
        COMPLIANCE_FAILED,
        WITHDRAWN,
        AWARDED,
        UNSUCCESSFUL,
        CANCELLED
    }

    public static TraderBidOutcomeResponse from(
            Bid bid,
            Tender tender,
            BidComplianceResponse compliance
    ) {
        boolean compliancePassed =
                compliance != null
                        && compliance.isPassed();

        String complianceFailureReason =
                compliance == null
                        ? null
                        : compliance
                        .getFailureReason();

        TraderBidOutcome outcome =
                determineOutcome(
                        bid,
                        tender
                );

        return TraderBidOutcomeResponse
                .builder()
                .bidId(
                        bid.getId()
                )
                .bidReference(
                        bid.getBidReference()
                )
                .bidStatus(
                        bid.getStatus()
                )
                .bidderAlias(
                        bid.getBidderAlias()
                )
                .priceAmount(
                        bid.getPriceAmount()
                )
                .submittedAt(
                        bid.getSubmittedAt()
                )
                .tenderId(
                        tender.getId()
                )
                .tenderReference(
                        tender.getTenderReference()
                )
                .tenderTitle(
                        tender.getTitle()
                )
                .tenderStatus(
                        tender.getStatus()
                )
                .compliancePassed(
                        compliancePassed
                )
                .complianceFailureReason(
                        complianceFailureReason
                )
                .outcome(
                        outcome
                )
                .awardedAt(
                        tender.getAwardedAt()
                )
                .publishedOutcomeReason(
                        determinePublishedOutcomeReason(
                                bid,
                                tender,
                                outcome
                        )
                )
                .build();
    }

    private static TraderBidOutcome
    determineOutcome(
            Bid bid,
            Tender tender
    ) {
        if (
                tender.getStatus()
                        == Tender.TenderStatus.CANCELLED
        ) {
            return TraderBidOutcome.CANCELLED;
        }

        if (
                bid.getStatus()
                        == Bid.BidStatus.WITHDRAWN
        ) {
            return TraderBidOutcome.WITHDRAWN;
        }

        if (
                bid.getStatus()
                        == Bid.BidStatus
                        .COMPLIANCE_FAILED
        ) {
            return TraderBidOutcome
                    .COMPLIANCE_FAILED;
        }

        if (
                tender.getStatus()
                        == Tender.TenderStatus.AWARDED
        ) {
            if (
                    tender.getAwardedBidId()
                            != null
                            && tender
                            .getAwardedBidId()
                            .equals(
                                    bid.getId()
                            )
            ) {
                return TraderBidOutcome.AWARDED;
            }

            return TraderBidOutcome
                    .UNSUCCESSFUL;
        }

        return TraderBidOutcome.PENDING;
    }

    private static String
    determinePublishedOutcomeReason(
            Bid bid,
            Tender tender,
            TraderBidOutcome outcome
    ) {
        return switch (outcome) {
            case AWARDED ->
                    tender.getAwardReason();

            case UNSUCCESSFUL ->
                    "The tender outcome has been "
                            + "finalised. This bid "
                            + "was not selected for "
                            + "the award.";

            case CANCELLED ->
                    "The tender was cancelled before "
                            + "an award was completed.";

            case WITHDRAWN ->
                    "This bid was withdrawn before "
                            + "the tender outcome was "
                            + "finalised.";

            case COMPLIANCE_FAILED ->
                    "This bid did not pass the "
                            + "mandatory compliance "
                            + "assessment.";

            case PENDING ->
                    "The tender outcome has not yet "
                            + "been finalised.";
        };
    }
}