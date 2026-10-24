from __future__ import annotations

import json
from pathlib import Path

import joblib
import pandas as pd
from sklearn.metrics import (
    average_precision_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)


PROJECT_ROOT = Path(__file__).resolve().parents[2]

DATASET_FILE = (
        PROJECT_ROOT
        / "ml"
        / "data"
        / "processed"
        / "procurement_fraud_training_dataset.csv"
)

MODEL_FILE = (
        PROJECT_ROOT
        / "ml"
        / "artifacts"
        / "best_model.joblib"
)

METADATA_FILE = (
        PROJECT_ROOT
        / "ml"
        / "artifacts"
        / "model_metadata.json"
)

RANDOM_STATE = 42
TEST_SAMPLE_SIZE = 30_000


def stratified_sample(
        dataframe: pd.DataFrame,
        sample_size: int,
) -> pd.DataFrame:
    if len(dataframe) <= sample_size:
        return dataframe.copy()

    fraud_rows = dataframe[
        dataframe["is_fraud"] == 1
        ]

    legitimate_rows = dataframe[
        dataframe["is_fraud"] == 0
        ]

    fraud_sample_size = round(
        sample_size
        * len(fraud_rows)
        / len(dataframe)
    )

    legitimate_sample_size = (
            sample_size
            - fraud_sample_size
    )

    return (
        pd.concat(
            [
                fraud_rows.sample(
                    n=fraud_sample_size,
                    random_state=RANDOM_STATE,
                ),
                legitimate_rows.sample(
                    n=legitimate_sample_size,
                    random_state=RANDOM_STATE,
                ),
            ]
        )
        .sample(
            frac=1,
            random_state=RANDOM_STATE,
        )
        .reset_index(drop=True)
    )


def main() -> None:
    if not MODEL_FILE.exists():
        raise SystemExit(
            f"Model not found: {MODEL_FILE}"
        )

    metadata = json.loads(
        METADATA_FILE.read_text(
            encoding="utf-8"
        )
    )

    feature_names = metadata["features"]

    dataset = pd.read_csv(
        DATASET_FILE,
        low_memory=False,
    )

    test_data = stratified_sample(
        dataset[
            dataset["split"] == "test"
            ],
        TEST_SAMPLE_SIZE,
    )

    x_test = test_data[
        feature_names
    ]

    y_test = test_data[
        "is_fraud"
    ].astype(int)

    model = joblib.load(
        MODEL_FILE
    )

    predictions = model.predict(
        x_test
    )

    probabilities = model.predict_proba(
        x_test
    )[:, 1]

    print("Saved model validation")
    print("=" * 72)

    print(
        f"Model: {metadata['model_name']}"
    )

    print(
        f"Experiment: {metadata['experiment']}"
    )

    print(
        f"Rows: {len(x_test):,}"
    )

    print()
    print(
        classification_report(
            y_test,
            predictions,
            target_names=[
                "LEGITIMATE",
                "HIGH_RISK",
            ],
            digits=4,
        )
    )

    print("Confusion matrix:")

    print(
        confusion_matrix(
            y_test,
            predictions,
            labels=[0, 1],
        )
    )

    print()
    print(
        "Precision:",
        f"{precision_score(y_test, predictions):.6f}",
    )

    print(
        "Recall:",
        f"{recall_score(y_test, predictions):.6f}",
    )

    print(
        "F1:",
        f"{f1_score(y_test, predictions):.6f}",
    )

    print(
        "ROC-AUC:",
        f"{roc_auc_score(y_test, probabilities):.6f}",
    )

    print(
        "Average Precision:",
        f"{average_precision_score(y_test, probabilities):.6f}",
    )

    print()
    print("Example predictions:")

    examples = test_data[
        [
            "invoice_id",
            "invoice_amount",
            "supplier_country",
            "is_fraud",
        ]
    ].copy()

    examples["prediction"] = predictions
    examples["risk_probability"] = probabilities

    print(
        examples.head(10)
        .to_string(index=False)
    )


if __name__ == "__main__":
    main()