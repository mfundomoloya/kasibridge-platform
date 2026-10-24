from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

import joblib
import pandas as pd

from model_contract import (
    InvoiceFraudPredictionRequest,
    InvoiceFraudPredictionResponse,
    ModelInformationResponse,
    RecommendedAction,
    RiskLevel,
)


class ModelIntegrityError(
    RuntimeError
):
    pass


class InvoiceFraudInferenceService:
    def __init__(
            self,
            model_path: Path,
            metadata_path: Path,
            checksum_path: Path,
    ) -> None:
        self.model_path = model_path
        self.metadata_path = metadata_path
        self.checksum_path = checksum_path

        self.model: Any | None = None
        self.metadata: dict[
            str,
            Any
        ] = {}

        self.model_sha256 = ""

        self.checksum_verified = False

    def load(self) -> None:
        self._validate_files()

        self.model_sha256 = (
            self._calculate_sha256(
                self.model_path
            )
        )

        expected_checksum = (
            self._read_expected_checksum()
        )

        if (
                self.model_sha256
                != expected_checksum
        ):
            raise ModelIntegrityError(
                "Model checksum verification "
                "failed. Expected "
                f"{expected_checksum}, found "
                f"{self.model_sha256}."
            )

        self.checksum_verified = True

        self.metadata = json.loads(
            self.metadata_path.read_text(
                encoding="utf-8"
            )
        )

        # joblib.load uses pickle-based
        # deserialization. Only load this
        # internally generated and checksum-
        # verified model artifact.

        self.model = joblib.load(
            self.model_path
        )

        self._validate_loaded_model()

    def _validate_files(self) -> None:
        required_files = [
            self.model_path,
            self.metadata_path,
            self.checksum_path,
        ]

        missing_files = [
            str(path)
            for path in required_files
            if not path.exists()
        ]

        if missing_files:
            raise FileNotFoundError(
                "Required model files are "
                "missing: "
                + ", ".join(
                    missing_files
                )
            )

    def _calculate_sha256(
            self,
            path: Path,
    ) -> str:
        digest = hashlib.sha256()

        with path.open("rb") as file:
            for block in iter(
                    lambda: file.read(
                        1024 * 1024
                    ),
                    b"",
            ):
                digest.update(block)

        return digest.hexdigest()

    def _read_expected_checksum(
            self,
    ) -> str:
        content = (
            self.checksum_path
            .read_text(
                encoding="utf-8"
            )
            .strip()
        )

        if not content:
            raise ModelIntegrityError(
                "Model checksum file is empty."
            )

        return content.split()[0]

    def _validate_loaded_model(
            self,
    ) -> None:
        if self.model is None:
            raise RuntimeError(
                "Model did not load."
            )

        required_metadata = {
            "model_name",
            "model_version",
            "experiment",
            "target",
            "features",
            "test_metrics",
        }

        missing_metadata = (
                required_metadata
                - set(self.metadata)
        )

        if missing_metadata:
            raise ModelIntegrityError(
                "Model metadata is missing: "
                f"{sorted(missing_metadata)}"
            )

        feature_count = len(
            self.metadata["features"]
        )

        if feature_count != 28:
            raise ModelIntegrityError(
                "Expected 28 model features, "
                f"found {feature_count}."
            )

        if not hasattr(
                self.model,
                "predict"
        ):
            raise ModelIntegrityError(
                "Loaded model does not expose "
                "predict()."
            )

        if not hasattr(
                self.model,
                "predict_proba"
        ):
            raise ModelIntegrityError(
                "Loaded model does not expose "
                "predict_proba()."
            )

    def is_loaded(self) -> bool:
        return (
                self.model is not None
                and self.checksum_verified
        )

    def predict(
            self,
            request:
            InvoiceFraudPredictionRequest,
    ) -> (
            InvoiceFraudPredictionResponse
    ):
        if not self.is_loaded():
            raise RuntimeError(
                "Fraud model is not loaded."
            )

        feature_names: list[str] = (
            self.metadata["features"]
        )

        request_data = (
            request.model_dump()
        )

        missing_features = [
            feature
            for feature in feature_names
            if feature not in request_data
        ]

        if missing_features:
            raise ValueError(
                "Prediction request is missing "
                f"features: {missing_features}"
            )

        feature_frame = pd.DataFrame(
            [
                {
                    feature:
                        request_data[feature]
                    for feature
                    in feature_names
                }
            ]
        )

        prediction = int(
            self.model.predict(
                feature_frame
            )[0]
        )

        risk_probability = float(
            self.model.predict_proba(
                feature_frame
            )[0][1]
        )

        risk_level = (
            self._risk_level(
                risk_probability
            )
        )

        return (
            InvoiceFraudPredictionResponse(
                classification=(
                    "HIGH_RISK"
                    if prediction == 1
                    else "LEGITIMATE"
                ),
                risk_level=risk_level,
                risk_probability=round(
                    risk_probability,
                    6,
                ),
                model_name=(
                    self.metadata[
                        "model_name"
                    ]
                ),
                model_version=(
                    self.metadata[
                        "model_version"
                    ]
                ),
                experiment=(
                    self.metadata[
                        "experiment"
                    ]
                ),
                detection_source=(
                    "SUPERVISED_MODEL"
                ),
                recommended_action=(
                    self._recommended_action(
                        risk_level
                    )
                ),
                threshold=0.5,
                model_sha256=(
                    self.model_sha256
                ),
            )
        )

    def model_information(
            self,
    ) -> ModelInformationResponse:
        if not self.is_loaded():
            raise RuntimeError(
                "Fraud model is not loaded."
            )

        return ModelInformationResponse(
            model_name=(
                self.metadata["model_name"]
            ),
            model_version=(
                self.metadata[
                    "model_version"
                ]
            ),
            experiment=(
                self.metadata["experiment"]
            ),
            target=(
                self.metadata["target"]
            ),
            model_sha256=(
                self.model_sha256
            ),
            feature_count=len(
                self.metadata["features"]
            ),
            test_metrics=(
                self.metadata["test_metrics"]
            ),
        )

    def _risk_level(
            self,
            probability: float,
    ) -> RiskLevel:
        if probability >= 0.90:
            return RiskLevel.CRITICAL

        if probability >= 0.75:
            return RiskLevel.HIGH

        if probability >= 0.50:
            return RiskLevel.MEDIUM

        return RiskLevel.LOW

    def _recommended_action(
            self,
            risk_level: RiskLevel,
    ) -> RecommendedAction:
        if (
                risk_level
                == RiskLevel.CRITICAL
        ):
            return (
                RecommendedAction
                .BLOCK_AND_ESCALATE
            )

        if risk_level == RiskLevel.HIGH:
            return (
                RecommendedAction
                .HOLD_PAYMENT_AND_REVIEW
            )

        if (
                risk_level
                == RiskLevel.MEDIUM
        ):
            return (
                RecommendedAction
                .REQUIRE_REVIEW
            )

        return (
            RecommendedAction
            .ALLOW_WITH_MONITORING
        )
