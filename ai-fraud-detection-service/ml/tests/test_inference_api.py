from __future__ import annotations

import json
import sys
from pathlib import Path

import pandas as pd
import pytest
from fastapi.testclient import TestClient


ML_ROOT = Path(__file__).resolve().parents[1]
SRC_DIR = ML_ROOT / "src"
ARTIFACTS_DIR = ML_ROOT / "artifacts"
DATASET_FILE = (
        ML_ROOT
        / "data"
        / "processed"
        / "procurement_fraud_training_dataset.csv"
)

sys.path.insert(
    0,
    str(SRC_DIR),
)

from inference_api import app  # noqa: E402


@pytest.fixture(scope="module")
def client() -> TestClient:
    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture(scope="module")
def test_requests() -> dict[str, dict]:
    metadata = json.loads(
        (
                ARTIFACTS_DIR
                / "model_metadata.json"
        ).read_text(
            encoding="utf-8"
        )
    )

    features = metadata["features"]

    dataset = pd.read_csv(
        DATASET_FILE,
        low_memory=False,
    )

    test_data = dataset[
        dataset["split"] == "test"
        ]

    fraud_row = test_data[
        test_data["is_fraud"] == 1
        ].iloc[0]

    legitimate_row = test_data[
        test_data["is_fraud"] == 0
        ].iloc[0]

    def payload(row: pd.Series) -> dict:
        result = {}

        for feature in features:
            value = row[feature]

            if pd.isna(value):
                result[feature] = None
            elif hasattr(value, "item"):
                result[feature] = (
                    value.item()
                )
            else:
                result[feature] = value

        return result

    return {
        "fraud": payload(fraud_row),
        "legitimate": payload(
            legitimate_row
        ),
    }


def test_health_reports_verified_model(
        client: TestClient,
) -> None:
    response = client.get(
        "/health"
    )

    assert response.status_code == 200

    assert response.json() == {
        "status": "UP",
        "model_loaded": True,
        "model_checksum_verified": True,
    }


def test_model_information(
        client: TestClient,
) -> None:
    response = client.get(
        "/model"
    )

    assert response.status_code == 200

    body = response.json()

    assert body["model_name"] == (
        "Random Forest"
    )

    assert body["model_version"] == (
        "procurement-fraud-v1"
    )

    assert body["feature_count"] == 28

    assert body["model_sha256"] == (
        "1a9dc135791a80c183b707de7686e84"
        "a87d6e3c15e7c6db0d1ed17ee1a7"
        "fe4de"
    )


def test_fraud_invoice_is_high_risk(
        client: TestClient,
        test_requests: dict[str, dict],
) -> None:
    response = client.post(
        "/predict",
        json=test_requests["fraud"],
    )

    assert response.status_code == 200

    body = response.json()

    assert body["classification"] == (
        "HIGH_RISK"
    )

    assert body["risk_level"] in {
        "HIGH",
        "CRITICAL",
    }

    assert (
            body["risk_probability"]
            >= 0.75
    )

    assert body[
               "recommended_action"
           ] in {
               "HOLD_PAYMENT_AND_REVIEW",
               "BLOCK_AND_ESCALATE",
           }


def test_legitimate_invoice_is_low_risk(
        client: TestClient,
        test_requests: dict[str, dict],
) -> None:
    response = client.post(
        "/predict",
        json=test_requests[
            "legitimate"
        ],
    )

    assert response.status_code == 200

    body = response.json()

    assert body["classification"] == (
        "LEGITIMATE"
    )

    assert body["risk_level"] == "LOW"

    assert (
            body["risk_probability"]
            < 0.50
    )

    assert body[
               "recommended_action"
           ] == "ALLOW_WITH_MONITORING"


def test_invalid_submission_hour_is_rejected(
        client: TestClient,
        test_requests: dict[str, dict],
) -> None:
    request = dict(
        test_requests["fraud"]
    )

    request["submission_hour"] = 31

    response = client.post(
        "/predict",
        json=request,
    )

    assert response.status_code == 422

    details = response.json()[
        "detail"
    ]

    assert any(
        detail["loc"]
        == [
            "body",
            "submission_hour",
        ]
        for detail in details
    )