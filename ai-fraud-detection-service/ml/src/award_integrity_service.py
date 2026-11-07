from __future__ import annotations

from datetime import datetime, timezone

from model_contract import (
    AwardIntegrityAssessmentRequest,
    AwardIntegrityAssessmentResponse,
    AwardIntegrityRecommendation,
    AwardIntegrityRiskLevel,
    AwardIntegritySignal,
    AwardIntegritySignalSeverity,
)


RULE_VERSION = "award-integrity-rules-v1"


class AwardIntegrityService:
    def assess(
        self,
        request: AwardIntegrityAssessmentRequest,
    ) -> AwardIntegrityAssessmentResponse:
        signals: list[AwardIntegritySignal] = []
        risk_points = 0

        budget_utilisation = self._percentage(
            request.awarded_amount,
            request.budget_amount,
        )

        score_completion = self._percentage(
            request.submitted_score_count,
            request.expected_score_count,
        )

        risk_points += self._assess_bid_competition(
            request,
            signals,
        )

        risk_points += self._assess_budget_utilisation(
            budget_utilisation,
            signals,
        )

        evaluation_complete = self._evaluation_complete(
            request
        )

        risk_points += self._assess_evaluation_completion(
            evaluation_complete,
            score_completion,
            request,
            signals,
        )

        technical_threshold_passed = (
            request.average_technical_score
            >= request.minimum_technical_threshold
        )

        risk_points += self._assess_technical_threshold(
            technical_threshold_passed,
            request,
            signals,
        )

        top_ranked_bid_awarded = request.award_rank == 1

        risk_points += self._assess_award_rank(
            request.award_rank,
            signals,
        )

        segregation_passed = self._segregation_passed(
            request
        )

        risk_points += self._assess_segregation(
            segregation_passed,
            signals,
        )

        risk_points += self._assess_timeline(
            request,
            signals,
        )

        risk_score = min(
            risk_points / 100,
            1.0,
        )

        risk_level = self._risk_level(
            risk_score
        )

        recommendation = self._recommendation(
            risk_level,
            request.eligible_bid_count,
        )

        award_consistent = (
            request.eligible_bid_count > 0
            and evaluation_complete
            and technical_threshold_passed
            and top_ranked_bid_awarded
            and segregation_passed
            and request.awarded_amount
            <= request.budget_amount
        )

        return AwardIntegrityAssessmentResponse(
            tender_id=request.tender_id,
            assessment_type=(
                "DETERMINISTIC_AWARD_INTEGRITY"
            ),
            risk_score=round(
                risk_score,
                4,
            ),
            risk_level=risk_level,
            recommendation=recommendation,
            rule_version=RULE_VERSION,
            model_used=False,
            award_consistent_with_rules=(
                award_consistent
            ),
            signals=signals,
            assessed_at=datetime.now(
                timezone.utc
            ),
        )

    def _assess_bid_competition(
        self,
        request: AwardIntegrityAssessmentRequest,
        signals: list[AwardIntegritySignal],
    ) -> int:
        if request.eligible_bid_count == 0:
            signals.append(
                AwardIntegritySignal(
                    code="NO_ELIGIBLE_BIDS",
                    severity=(
                        AwardIntegritySignalSeverity
                        .CRITICAL
                    ),
                    message=(
                        "The tender was awarded "
                        "without an eligible bid."
                    ),
                    observed_value=0,
                    threshold=1,
                    risk_points=45,
                )
            )
            return 45

        if request.eligible_bid_count == 1:
            signals.append(
                AwardIntegritySignal(
                    code="SINGLE_ELIGIBLE_BID",
                    severity=(
                        AwardIntegritySignalSeverity
                        .MEDIUM
                    ),
                    message=(
                        "Only one eligible bid "
                        "remained for evaluation."
                    ),
                    observed_value=1,
                    threshold=2,
                    risk_points=25,
                )
            )
            return 25

        signals.append(
            AwardIntegritySignal(
                code="COMPETITIVE_BID_SET",
                severity=(
                    AwardIntegritySignalSeverity.INFO
                ),
                message=(
                    "More than one eligible bid "
                    "was available."
                ),
                observed_value=(
                    request.eligible_bid_count
                ),
                threshold=2,
                risk_points=0,
            )
        )
        return 0

    def _assess_budget_utilisation(
        self,
        budget_utilisation: float,
        signals: list[AwardIntegritySignal],
    ) -> int:
        observed_value = round(
            budget_utilisation,
            2,
        )

        if budget_utilisation > 100:
            signals.append(
                AwardIntegritySignal(
                    code="AWARD_EXCEEDS_BUDGET",
                    severity=(
                        AwardIntegritySignalSeverity
                        .CRITICAL
                    ),
                    message=(
                        "The awarded amount exceeds "
                        "the published budget."
                    ),
                    observed_value=observed_value,
                    threshold=100,
                    risk_points=40,
                )
            )
            return 40

        if budget_utilisation >= 95:
            signals.append(
                AwardIntegritySignal(
                    code=(
                        "VERY_HIGH_BUDGET_UTILISATION"
                    ),
                    severity=(
                        AwardIntegritySignalSeverity
                        .MEDIUM
                    ),
                    message=(
                        "The awarded amount uses at "
                        "least 95 percent of the "
                        "published budget."
                    ),
                    observed_value=observed_value,
                    threshold=95,
                    risk_points=15,
                )
            )
            return 15

        if budget_utilisation >= 90:
            signals.append(
                AwardIntegritySignal(
                    code="HIGH_BUDGET_UTILISATION",
                    severity=(
                        AwardIntegritySignalSeverity.LOW
                    ),
                    message=(
                        "The awarded amount uses at "
                        "least 90 percent of the "
                        "published budget."
                    ),
                    observed_value=observed_value,
                    threshold=90,
                    risk_points=7,
                )
            )
            return 7

        signals.append(
            AwardIntegritySignal(
                code=(
                    "BUDGET_UTILISATION_ACCEPTABLE"
                ),
                severity=(
                    AwardIntegritySignalSeverity.INFO
                ),
                message=(
                    "The awarded amount remains "
                    "below 90 percent of the "
                    "published budget."
                ),
                observed_value=observed_value,
                threshold=90,
                risk_points=0,
            )
        )
        return 0

    def _assess_evaluation_completion(
        self,
        evaluation_complete: bool,
        score_completion: float,
        request: AwardIntegrityAssessmentRequest,
        signals: list[AwardIntegritySignal],
    ) -> int:
        observed_value = round(
            score_completion,
            2,
        )

        if evaluation_complete:
            signals.append(
                AwardIntegritySignal(
                    code=(
                        "EVALUATION_MATRIX_COMPLETE"
                    ),
                    severity=(
                        AwardIntegritySignalSeverity.INFO
                    ),
                    message=(
                        "All expected evaluator "
                        "scores were submitted."
                    ),
                    observed_value=observed_value,
                    threshold=100,
                    risk_points=0,
                )
            )
            return 0

        signals.append(
            AwardIntegritySignal(
                code=(
                    "EVALUATION_MATRIX_INCOMPLETE"
                ),
                severity=(
                    AwardIntegritySignalSeverity.HIGH
                ),
                message=(
                    "The tender was awarded before "
                    "all expected evaluator scores "
                    "were submitted."
                ),
                observed_value=observed_value,
                threshold=100,
                risk_points=35,
            )
        )

        if request.expected_score_count == 0:
            signals.append(
                AwardIntegritySignal(
                    code="NO_EXPECTED_SCORES",
                    severity=(
                        AwardIntegritySignalSeverity.HIGH
                    ),
                    message=(
                        "The evaluation matrix did "
                        "not require any scores."
                    ),
                    observed_value=0,
                    threshold=1,
                    risk_points=0,
                )
            )

        return 35

    def _assess_technical_threshold(
        self,
        threshold_passed: bool,
        request: AwardIntegrityAssessmentRequest,
        signals: list[AwardIntegritySignal],
    ) -> int:
        if threshold_passed:
            signals.append(
                AwardIntegritySignal(
                    code=(
                        "TECHNICAL_THRESHOLD_PASSED"
                    ),
                    severity=(
                        AwardIntegritySignalSeverity.INFO
                    ),
                    message=(
                        "The awarded bid passed the "
                        "published technical "
                        "threshold."
                    ),
                    observed_value=(
                        request.average_technical_score
                    ),
                    threshold=(
                        request
                        .minimum_technical_threshold
                    ),
                    risk_points=0,
                )
            )
            return 0

        signals.append(
            AwardIntegritySignal(
                code="TECHNICAL_THRESHOLD_FAILED",
                severity=(
                    AwardIntegritySignalSeverity.CRITICAL
                ),
                message=(
                    "The awarded bid did not meet "
                    "the published technical "
                    "threshold."
                ),
                observed_value=(
                    request.average_technical_score
                ),
                threshold=(
                    request.minimum_technical_threshold
                ),
                risk_points=40,
            )
        )
        return 40

    def _assess_award_rank(
        self,
        award_rank: int,
        signals: list[AwardIntegritySignal],
    ) -> int:
        if award_rank == 1:
            signals.append(
                AwardIntegritySignal(
                    code="TOP_RANKED_BID_AWARDED",
                    severity=(
                        AwardIntegritySignalSeverity.INFO
                    ),
                    message=(
                        "The highest-ranked bid "
                        "was selected."
                    ),
                    observed_value=1,
                    threshold=1,
                    risk_points=0,
                )
            )
            return 0

        signals.append(
            AwardIntegritySignal(
                code=(
                    "NON_TOP_RANKED_BID_AWARDED"
                ),
                severity=(
                    AwardIntegritySignalSeverity.HIGH
                ),
                message=(
                    "The selected bid was not "
                    "ranked first."
                ),
                observed_value=award_rank,
                threshold=1,
                risk_points=35,
            )
        )
        return 35

    def _assess_segregation(
        self,
        segregation_passed: bool,
        signals: list[AwardIntegritySignal],
    ) -> int:
        if segregation_passed:
            signals.append(
                AwardIntegritySignal(
                    code=(
                        "SEGREGATION_OF_DUTIES_PASSED"
                    ),
                    severity=(
                        AwardIntegritySignalSeverity.INFO
                    ),
                    message=(
                        "Specification, evaluation "
                        "and adjudication used "
                        "different users."
                    ),
                    risk_points=0,
                )
            )
            return 0

        signals.append(
            AwardIntegritySignal(
                code=(
                    "SEGREGATION_OF_DUTIES_FAILED"
                ),
                severity=(
                    AwardIntegritySignalSeverity.CRITICAL
                ),
                message=(
                    "One user participated in "
                    "conflicting committee roles."
                ),
                risk_points=50,
            )
        )
        return 50

    def _assess_timeline(
        self,
        request: AwardIntegrityAssessmentRequest,
        signals: list[AwardIntegritySignal],
    ) -> int:
        published_at = request.published_at
        bidding_closed_at = (
            request.bidding_closed_at
        )
        awarded_at = request.awarded_at

        if not (
            published_at
            <= bidding_closed_at
            <= awarded_at
        ):
            signals.append(
                AwardIntegritySignal(
                    code="INVALID_WORKFLOW_TIMELINE",
                    severity=(
                        AwardIntegritySignalSeverity
                        .CRITICAL
                    ),
                    message=(
                        "The procurement timestamps "
                        "do not follow the required "
                        "publication, closure and "
                        "award order."
                    ),
                    observed_value=(
                        published_at.isoformat()
                        + " | "
                        + bidding_closed_at.isoformat()
                        + " | "
                        + awarded_at.isoformat()
                    ),
                    threshold=(
                        "published <= closed "
                        "<= awarded"
                    ),
                    risk_points=40,
                )
            )
            return 40

        signals.append(
            AwardIntegritySignal(
                code="WORKFLOW_TIMELINE_VALID",
                severity=(
                    AwardIntegritySignalSeverity.INFO
                ),
                message=(
                    "Publication, bidding closure "
                    "and award timestamps follow "
                    "the expected order."
                ),
                risk_points=0,
            )
        )
        return 0

    @staticmethod
    def _evaluation_complete(
        request: AwardIntegrityAssessmentRequest,
    ) -> bool:
        return (
            request.active_evaluator_count > 0
            and request.expected_score_count > 0
            and request.submitted_score_count
            == request.expected_score_count
        )

    @staticmethod
    def _segregation_passed(
        request: AwardIntegrityAssessmentRequest,
    ) -> bool:
        specification_officer_id = (
            request.specification_officer_user_id
        )

        adjudicator_id = (
            request.adjudicator_user_id
        )

        evaluator_ids = set(
            request.evaluator_user_ids
        )

        return (
            specification_officer_id
            != adjudicator_id
            and adjudicator_id
            not in evaluator_ids
            and specification_officer_id
            not in evaluator_ids
        )

    @staticmethod
    def _percentage(
        numerator: float | int,
        denominator: float | int,
    ) -> float:
        if denominator <= 0:
            return 0.0

        return (
            float(numerator)
            / float(denominator)
            * 100
        )

    @staticmethod
    def _risk_level(
        risk_score: float,
    ) -> AwardIntegrityRiskLevel:
        if risk_score >= 0.75:
            return (
                AwardIntegrityRiskLevel.CRITICAL
            )

        if risk_score >= 0.50:
            return AwardIntegrityRiskLevel.HIGH

        if risk_score >= 0.25:
            return AwardIntegrityRiskLevel.MEDIUM

        return AwardIntegrityRiskLevel.LOW

    @staticmethod
    def _recommendation(
        risk_level: AwardIntegrityRiskLevel,
        eligible_bid_count: int,
    ) -> AwardIntegrityRecommendation:
        if (
            risk_level
            == AwardIntegrityRiskLevel.CRITICAL
        ):
            return (
                AwardIntegrityRecommendation
                .ESCALATE_FOR_INVESTIGATION
            )

        if (
            risk_level
            == AwardIntegrityRiskLevel.HIGH
        ):
            return (
                AwardIntegrityRecommendation
                .REQUIRE_GOVERNANCE_REVIEW
            )

        if eligible_bid_count <= 1:
            return (
                AwardIntegrityRecommendation
                .REVIEW_COMPETITION
            )

        return (
            AwardIntegrityRecommendation
            .ACCEPT_WITH_MONITORING
        )
