from __future__ import annotations

from enum import Enum
from typing import Any

from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
)


class RiskLevel(
    str,
    Enum,
):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class RecommendedAction(
    str,
    Enum,
):
    ALLOW_WITH_MONITORING = (
        "ALLOW_WITH_MONITORING"
    )

    REQUIRE_REVIEW = (
        "REQUIRE_REVIEW"
    )

    HOLD_PAYMENT_AND_REVIEW = (
        "HOLD_PAYMENT_AND_REVIEW"
    )

    BLOCK_AND_ESCALATE = (
        "BLOCK_AND_ESCALATE"
    )


class InvoiceFraudPredictionRequest(
    BaseModel,
):
    model_config = ConfigDict(
        extra="forbid",
    )

    invoice_amount: float = Field(
        ge=0,
        examples=[
            20346.26,
        ],
    )

    submission_hour: int = Field(
        ge=0,
        le=23,
        examples=[
            23,
        ],
    )

    supplier_invoice_count_30d: float = Field(
        ge=0,
        examples=[
            18,
        ],
    )

    supplier_avg_amount_90d: float = Field(
        ge=0,
        examples=[
            4252.16,
        ],
    )

    invoice_amount_zscore: float = Field(
        examples=[
            3.4,
        ],
    )

    supplier_age_days: int = Field(
        ge=0,
        examples=[
            61,
        ],
    )

    avg_invoice_amount: float = Field(
        ge=0,
        examples=[
            5590.25,
        ],
    )

    annual_budget: float = Field(
        ge=0,
        examples=[
            50000000,
        ],
    )

    ocr_total_extracted: float | None = Field(
        default=None,
        ge=0,
        examples=[
            20346.26,
        ],
    )

    invoice_month: int = Field(
        ge=1,
        le=12,
        examples=[
            10,
        ],
    )

    invoice_day_of_week: int = Field(
        ge=0,
        le=6,
        examples=[
            5,
        ],
    )

    invoice_day_of_month: int = Field(
        ge=1,
        le=31,
        examples=[
            24,
        ],
    )

    invoice_is_weekend: int = Field(
        ge=0,
        le=1,
        examples=[
            1,
        ],
    )

    has_invoice_image: int = Field(
        ge=0,
        le=1,
        examples=[
            1,
        ],
    )

    has_ocr_result: int = Field(
        ge=0,
        le=1,
        examples=[
            1,
        ],
    )

    ocr_amount_difference: float | None = Field(
        default=None,
        examples=[
            0,
        ],
    )

    ocr_amount_difference_abs: float | None = Field(
        default=None,
        ge=0,
        examples=[
            0,
        ],
    )

    ocr_amount_difference_ratio: float | None = Field(
        default=None,
        ge=0,
        examples=[
            0,
        ],
    )

    duplicate_invoice_flag: int = Field(
        ge=0,
        le=1,
        examples=[
            0,
        ],
    )

    split_invoice_flag: int = Field(
        ge=0,
        le=1,
        examples=[
            1,
        ],
    )

    late_night_submission_flag: int = Field(
        ge=0,
        le=1,
        examples=[
            1,
        ],
    )

    supplier_risk_score: float = Field(
        ge=0,
        le=1,
        examples=[
            0.82,
        ],
    )

    blacklisted_flag: int = Field(
        ge=0,
        le=1,
        examples=[
            0,
        ],
    )

    image_tamper_flag: int | None = Field(
        default=None,
        ge=0,
        le=1,
        examples=[
            0,
        ],
    )

    payment_terms: str = Field(
        min_length=1,
        examples=[
            "NET30",
        ],
    )

    invoice_type: str = Field(
        min_length=1,
        examples=[
            "SERVICES",
        ],
    )

    supplier_country: str = Field(
        min_length=2,
        max_length=3,
        examples=[
            "ZA",
        ],
    )

    region: str = Field(
        min_length=1,
        examples=[
            "Gauteng",
        ],
    )


class InvoiceFraudPredictionResponse(
    BaseModel,
):
    classification: str

    risk_level: RiskLevel

    risk_probability: float = Field(
        ge=0,
        le=1,
    )

    model_name: str

    model_version: str

    experiment: str

    detection_source: str

    recommended_action: RecommendedAction

    threshold: float = Field(
        ge=0,
        le=1,
    )

    model_sha256: str


class ModelInformationResponse(
    BaseModel,
):
    model_name: str

    model_version: str

    experiment: str

    target: str

    model_sha256: str

    feature_count: int = Field(
        ge=1,
    )

    test_metrics: dict[str, Any]


class HealthResponse(
    BaseModel,
):
    status: str

    model_loaded: bool

    model_checksum_verified: bool