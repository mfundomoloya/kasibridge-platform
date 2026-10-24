from __future__ import annotations

import json
import time
import warnings
from pathlib import Path
from typing import Any

import joblib
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.ensemble import (
    HistGradientBoostingClassifier,
    RandomForestClassifier,
)
from sklearn.exceptions import ConvergenceWarning
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    ConfusionMatrixDisplay,
    accuracy_score,
    average_precision_score,
    balanced_accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.naive_bayes import GaussianNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import (
    OrdinalEncoder,
    StandardScaler,
)
from sklearn.svm import LinearSVC
from sklearn.tree import DecisionTreeClassifier


RANDOM_STATE = 42

PROJECT_ROOT = Path(__file__).resolve().parents[2]

DATASET_FILE = (
        PROJECT_ROOT
        / "ml"
        / "data"
        / "processed"
        / "procurement_fraud_training_dataset.csv"
)

ARTIFACTS_DIR = (
        PROJECT_ROOT
        / "ml"
        / "artifacts"
)

METRICS_FILE = (
        ARTIFACTS_DIR
        / "model_metrics.csv"
)

BEST_MODEL_FILE = (
        ARTIFACTS_DIR
        / "best_model.joblib"
)

MODEL_METADATA_FILE = (
        ARTIFACTS_DIR
        / "model_metadata.json"
)

FEATURE_SCHEMA_FILE = (
        ARTIFACTS_DIR
        / "feature_schema.json"
)

TARGET = "is_fraud"

LEAKAGE_SAFE_NUMERIC_FEATURES = [
    "invoice_amount",
    "submission_hour",
    "supplier_invoice_count_30d",
    "supplier_avg_amount_90d",
    "invoice_amount_zscore",
    "supplier_age_days",
    "avg_invoice_amount",
    "annual_budget",
    "ocr_total_extracted",
    "invoice_month",
    "invoice_day_of_week",
    "invoice_day_of_month",
    "invoice_is_weekend",
    "has_invoice_image",
    "has_ocr_result",
    "ocr_amount_difference",
    "ocr_amount_difference_abs",
    "ocr_amount_difference_ratio",
]

LEAKAGE_SAFE_CATEGORICAL_FEATURES = [
    "payment_terms",
    "invoice_type",
    "supplier_country",
    "region",
]

HYBRID_NUMERIC_FEATURES = (
        LEAKAGE_SAFE_NUMERIC_FEATURES
        + [
            "duplicate_invoice_flag",
            "split_invoice_flag",
            "late_night_submission_flag",
            "supplier_risk_score",
            "blacklisted_flag",
            "image_tamper_flag",
        ]
)

HYBRID_CATEGORICAL_FEATURES = (
    LEAKAGE_SAFE_CATEGORICAL_FEATURES
)

FORBIDDEN_MODEL_FEATURES = {
    "invoice_id",
    "supplier_id",
    "department_id",
    "invoice_date",
    "currency",
    "image_path",
    "is_fraud",
    "fraud_type",
    "fraud_tags",
    "explanations",
    "split",
}

# A common stratified sample keeps KNN and SVM practical
# while ensuring all seven classifiers see identical rows.
MAX_TRAIN_ROWS = 60_000
MAX_VALIDATION_ROWS = 15_000
MAX_TEST_ROWS = 30_000


def stratified_sample(
        frame: pd.DataFrame,
        maximum_rows: int,
) -> pd.DataFrame:
    if len(frame) <= maximum_rows:
        return frame.copy()

    fraud_rows = frame[
        frame[TARGET] == 1
        ]

    legitimate_rows = frame[
        frame[TARGET] == 0
        ]

    fraud_fraction = (
            len(fraud_rows)
            / len(frame)
    )

    fraud_sample_size = round(
        maximum_rows
        * fraud_fraction
    )

    legitimate_sample_size = (
            maximum_rows
            - fraud_sample_size
    )

    fraud_sample = fraud_rows.sample(
        n=fraud_sample_size,
        random_state=RANDOM_STATE,
    )

    legitimate_sample = (
        legitimate_rows.sample(
            n=legitimate_sample_size,
            random_state=RANDOM_STATE,
        )
    )

    return (
        pd.concat(
            [
                fraud_sample,
                legitimate_sample,
            ]
        )
        .sample(
            frac=1,
            random_state=RANDOM_STATE,
        )
        .reset_index(drop=True)
    )


def build_preprocessor(
        numeric_features: list[str],
        categorical_features: list[str],
) -> ColumnTransformer:
    numeric_pipeline = Pipeline(
        steps=[
            (
                "imputer",
                SimpleImputer(
                    strategy="median"
                ),
            ),
            (
                "scaler",
                StandardScaler(),
            ),
        ]
    )

    categorical_pipeline = Pipeline(
        steps=[
            (
                "imputer",
                SimpleImputer(
                    strategy="most_frequent"
                ),
            ),
            (
                "encoder",
                OrdinalEncoder(
                    handle_unknown="use_encoded_value",
                    unknown_value=-1,
                ),
            ),
        ]
    )

    return ColumnTransformer(
        transformers=[
            (
                "numeric",
                numeric_pipeline,
                numeric_features,
            ),
            (
                "categorical",
                categorical_pipeline,
                categorical_features,
            ),
        ],
        remainder="drop",
        verbose_feature_names_out=False,
    )


def build_models() -> dict[str, Any]:
    return {
        "Logistic Regression":
            LogisticRegression(
                max_iter=1_000,
                class_weight="balanced",
                random_state=RANDOM_STATE,
            ),

        "Decision Tree":
            DecisionTreeClassifier(
                max_depth=14,
                min_samples_leaf=10,
                class_weight="balanced",
                random_state=RANDOM_STATE,
            ),

        "Random Forest":
            RandomForestClassifier(
                n_estimators=250,
                max_depth=20,
                min_samples_leaf=4,
                class_weight="balanced_subsample",
                n_jobs=-1,
                random_state=RANDOM_STATE,
            ),

        "Support Vector Machine":
            LinearSVC(
                class_weight="balanced",
                random_state=RANDOM_STATE,
                dual="auto",
                max_iter=10_000,
            ),

        "K-Nearest Neighbours":
            KNeighborsClassifier(
                n_neighbors=7,
                weights="distance",
                n_jobs=-1,
            ),

        "Gaussian Naive Bayes":
            GaussianNB(),

        "Gradient Boosting":
            HistGradientBoostingClassifier(
                max_iter=250,
                learning_rate=0.08,
                max_leaf_nodes=31,
                l2_regularization=0.1,
                class_weight="balanced",
                random_state=RANDOM_STATE,
            ),
    }


def get_risk_scores(
        pipeline: Pipeline,
        features: pd.DataFrame,
) -> np.ndarray:
    if hasattr(
            pipeline,
            "predict_proba",
    ):
        probabilities = (
            pipeline.predict_proba(
                features
            )
        )

        return probabilities[:, 1]

    if hasattr(
            pipeline,
            "decision_function",
    ):
        decisions = (
            pipeline.decision_function(
                features
            )
        )

        decisions = np.asarray(
            decisions,
            dtype=float,
        )

        # Convert margin scores to a monotonic
        # probability-like range for presentation.
        return 1.0 / (
                1.0
                + np.exp(
            -np.clip(
                decisions,
                -30,
                30,
            )
        )
        )

    return pipeline.predict(
        features
    ).astype(float)


def calculate_false_positive_rate(
        targets: pd.Series,
        predictions: np.ndarray,
) -> float:
    matrix = confusion_matrix(
        targets,
        predictions,
        labels=[0, 1],
    )

    true_negative = matrix[0, 0]
    false_positive = matrix[0, 1]

    denominator = (
            true_negative
            + false_positive
    )

    if denominator == 0:
        return 0.0

    return float(
        false_positive
        / denominator
    )


def evaluate_model(
        model_name: str,
        pipeline: Pipeline,
        x_train: pd.DataFrame,
        y_train: pd.Series,
        x_validation: pd.DataFrame,
        y_validation: pd.Series,
        experiment_name: str,
) -> tuple[dict[str, Any], Pipeline]:
    print()
    print("=" * 78)
    print(
        f"{experiment_name}: "
        f"{model_name}"
    )
    print("=" * 78)

    training_started = (
        time.perf_counter()
    )

    pipeline.fit(
        x_train,
        y_train,
    )

    training_seconds = (
            time.perf_counter()
            - training_started
    )

    inference_started = (
        time.perf_counter()
    )

    predictions = pipeline.predict(
        x_validation
    )

    risk_scores = get_risk_scores(
        pipeline,
        x_validation,
    )

    inference_seconds = (
            time.perf_counter()
            - inference_started
    )

    metrics = {
        "experiment":
            experiment_name,
        "model":
            model_name,
        "accuracy":
            accuracy_score(
                y_validation,
                predictions,
            ),
        "balanced_accuracy":
            balanced_accuracy_score(
                y_validation,
                predictions,
            ),
        "precision":
            precision_score(
                y_validation,
                predictions,
                zero_division=0,
            ),
        "recall":
            recall_score(
                y_validation,
                predictions,
                zero_division=0,
            ),
        "f1":
            f1_score(
                y_validation,
                predictions,
                zero_division=0,
            ),
        "roc_auc":
            roc_auc_score(
                y_validation,
                risk_scores,
            ),
        "average_precision":
            average_precision_score(
                y_validation,
                risk_scores,
            ),
        "false_positive_rate":
            calculate_false_positive_rate(
                y_validation,
                predictions,
            ),
        "training_seconds":
            training_seconds,
        "inference_seconds":
            inference_seconds,
        "training_rows":
            len(x_train),
        "validation_rows":
            len(x_validation),
    }

    print(
        classification_report(
            y_validation,
            predictions,
            target_names=[
                "LEGITIMATE",
                "HIGH_RISK",
            ],
            zero_division=0,
        )
    )

    print(
        "Metrics:",
        json.dumps(
            metrics,
            indent=2,
        ),
    )

    matrix = confusion_matrix(
        y_validation,
        predictions,
        labels=[0, 1],
    )

    display = ConfusionMatrixDisplay(
        confusion_matrix=matrix,
        display_labels=[
            "Legitimate",
            "High risk",
        ],
    )

    display.plot(
        cmap="Blues",
        values_format=",d",
    )

    safe_name = (
        model_name
        .lower()
        .replace(" ", "_")
        .replace("-", "_")
    )

    safe_experiment = (
        experiment_name
        .lower()
        .replace(" ", "_")
        .replace("-", "_")
    )

    plt.title(
        f"{model_name} "
        f"({experiment_name})"
    )

    plt.tight_layout()

    plt.savefig(
        ARTIFACTS_DIR
        / (
            "confusion_matrix_"
            f"{safe_experiment}_"
            f"{safe_name}.png"
        ),
        dpi=160,
        )

    plt.close()

    return metrics, pipeline


def run_experiment(
        dataset: pd.DataFrame,
        experiment_name: str,
        numeric_features: list[str],
        categorical_features: list[str],
) -> tuple[
    list[dict[str, Any]],
    dict[str, Pipeline],
]:
    selected_features = (
            numeric_features
            + categorical_features
    )

    forbidden_used = (
            set(selected_features)
            & FORBIDDEN_MODEL_FEATURES
    )

    if forbidden_used:
        raise ValueError(
            "Forbidden features selected: "
            f"{sorted(forbidden_used)}"
        )

    missing_features = (
            set(selected_features)
            - set(dataset.columns)
    )

    if missing_features:
        raise ValueError(
            "Missing dataset features: "
            f"{sorted(missing_features)}"
        )

    train_frame = stratified_sample(
        dataset[
            dataset["split"] == "train"
            ],
        MAX_TRAIN_ROWS,
    )

    validation_frame = (
        stratified_sample(
            dataset[
                dataset["split"] == "val"
                ],
            MAX_VALIDATION_ROWS,
        )
    )

    x_train = train_frame[
        selected_features
    ]

    y_train = train_frame[
        TARGET
    ].astype(int)

    x_validation = validation_frame[
        selected_features
    ]

    y_validation = validation_frame[
        TARGET
    ].astype(int)

    metrics: list[dict[str, Any]] = []
    pipelines: dict[str, Pipeline] = {}

    for model_name, model in (
            build_models().items()
    ):
        preprocessor = build_preprocessor(
            numeric_features,
            categorical_features,
        )

        pipeline = Pipeline(
            steps=[
                (
                    "preprocessor",
                    preprocessor,
                ),
                (
                    "classifier",
                    model,
                ),
            ]
        )

        model_metrics, fitted_pipeline = (
            evaluate_model(
                model_name=model_name,
                pipeline=pipeline,
                x_train=x_train,
                y_train=y_train,
                x_validation=x_validation,
                y_validation=y_validation,
                experiment_name=experiment_name,
            )
        )

        metrics.append(
            model_metrics
        )

        pipelines[model_name] = (
            fitted_pipeline
        )

    return metrics, pipelines


def test_best_model(
        dataset: pd.DataFrame,
        best_pipeline: Pipeline,
        best_metrics: dict[str, Any],
        selected_features: list[str],
) -> dict[str, Any]:
    test_frame = stratified_sample(
        dataset[
            dataset["split"] == "test"
            ],
        MAX_TEST_ROWS,
    )

    x_test = test_frame[
        selected_features
    ]

    y_test = test_frame[
        TARGET
    ].astype(int)

    predictions = (
        best_pipeline.predict(
            x_test
        )
    )

    risk_scores = get_risk_scores(
        best_pipeline,
        x_test,
    )

    return {
        "model":
            best_metrics["model"],
        "experiment":
            best_metrics["experiment"],
        "test_rows":
            len(x_test),
        "accuracy":
            accuracy_score(
                y_test,
                predictions,
            ),
        "balanced_accuracy":
            balanced_accuracy_score(
                y_test,
                predictions,
            ),
        "precision":
            precision_score(
                y_test,
                predictions,
                zero_division=0,
            ),
        "recall":
            recall_score(
                y_test,
                predictions,
                zero_division=0,
            ),
        "f1":
            f1_score(
                y_test,
                predictions,
                zero_division=0,
            ),
        "roc_auc":
            roc_auc_score(
                y_test,
                risk_scores,
            ),
        "average_precision":
            average_precision_score(
                y_test,
                risk_scores,
            ),
        "false_positive_rate":
            calculate_false_positive_rate(
                y_test,
                predictions,
            ),
    }


def main() -> None:
    warnings.filterwarnings(
        "ignore",
        category=ConvergenceWarning,
    )

    if not DATASET_FILE.exists():
        raise SystemExit(
            f"Dataset not found: "
            f"{DATASET_FILE}"
        )

    ARTIFACTS_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    print(
        f"Loading dataset: "
        f"{DATASET_FILE}"
    )

    dataset = pd.read_csv(
        DATASET_FILE,
        low_memory=False,
    )

    print(
        f"Rows: {len(dataset):,}"
    )

    print(
        f"Columns: "
        f"{len(dataset.columns)}"
    )

    all_metrics: list[
        dict[str, Any]
    ] = []

    fitted_models: dict[
        tuple[str, str],
        Pipeline
    ] = {}

    safe_metrics, safe_models = (
        run_experiment(
            dataset=dataset,
            experiment_name=
            "Leakage-safe behavioural",
            numeric_features=
            LEAKAGE_SAFE_NUMERIC_FEATURES,
            categorical_features=
            LEAKAGE_SAFE_CATEGORICAL_FEATURES,
        )
    )

    all_metrics.extend(
        safe_metrics
    )

    for model_name, pipeline in (
            safe_models.items()
    ):
        fitted_models[
            (
                "Leakage-safe behavioural",
                model_name,
            )
        ] = pipeline

    hybrid_metrics, hybrid_models = (
        run_experiment(
            dataset=dataset,
            experiment_name=
            "Hybrid full-signal",
            numeric_features=
            HYBRID_NUMERIC_FEATURES,
            categorical_features=
            HYBRID_CATEGORICAL_FEATURES,
        )
    )

    all_metrics.extend(
        hybrid_metrics
    )

    for model_name, pipeline in (
            hybrid_models.items()
    ):
        fitted_models[
            (
                "Hybrid full-signal",
                model_name,
            )
        ] = pipeline

    metrics_frame = pd.DataFrame(
        all_metrics
    ).sort_values(
        by=[
            "average_precision",
            "recall",
            "f1",
        ],
        ascending=False,
    )

    metrics_frame.to_csv(
        METRICS_FILE,
        index=False,
    )

    eligible_metrics = (
        metrics_frame[
            metrics_frame["recall"]
            >= 0.80
            ]
    )

    if eligible_metrics.empty:
        best_row = (
            metrics_frame.iloc[0]
        )
    else:
        best_row = (
            eligible_metrics
            .sort_values(
                by=[
                    "average_precision",
                    "false_positive_rate",
                ],
                ascending=[
                    False,
                    True,
                ],
            )
            .iloc[0]
        )

    best_key = (
        str(best_row["experiment"]),
        str(best_row["model"]),
    )

    best_pipeline = (
        fitted_models[best_key]
    )

    if (
            best_key[0]
            == "Hybrid full-signal"
    ):
        best_features = (
                HYBRID_NUMERIC_FEATURES
                + HYBRID_CATEGORICAL_FEATURES
        )
    else:
        best_features = (
                LEAKAGE_SAFE_NUMERIC_FEATURES
                + LEAKAGE_SAFE_CATEGORICAL_FEATURES
        )

    test_metrics = test_best_model(
        dataset=dataset,
        best_pipeline=best_pipeline,
        best_metrics=best_row.to_dict(),
        selected_features=best_features,
    )

    joblib.dump(
        best_pipeline,
        BEST_MODEL_FILE,
    )

    metadata = {
        "model_name":
            best_key[1],
        "experiment":
            best_key[0],
        "model_version":
            "procurement-fraud-v1",
        "target":
            TARGET,
        "selection_rule":
            (
                "Highest validation Average Precision "
                "with recall >= 0.80; "
                "otherwise highest PR-AUC."
            ),
        "validation_metrics":
            {
                key: (
                    float(value)
                    if isinstance(
                        value,
                        (
                            np.floating,
                            float,
                        ),
                    )
                    else value
                )
                for key, value
                in best_row.to_dict().items()
            },
        "test_metrics":
            test_metrics,
        "features":
            best_features,
        "random_state":
            RANDOM_STATE,
        "training_sample_limit":
            MAX_TRAIN_ROWS,
        "validation_sample_limit":
            MAX_VALIDATION_ROWS,
        "test_sample_limit":
            MAX_TEST_ROWS,
    }

    MODEL_METADATA_FILE.write_text(
        json.dumps(
            metadata,
            indent=2,
            default=str,
        )
        + "\n",
        encoding="utf-8",
        )

    FEATURE_SCHEMA_FILE.write_text(
        json.dumps(
            {
                "numeric_features":
                    (
                        HYBRID_NUMERIC_FEATURES
                        if best_key[0]
                           == "Hybrid full-signal"
                        else LEAKAGE_SAFE_NUMERIC_FEATURES
                    ),
                "categorical_features":
                    (
                        HYBRID_CATEGORICAL_FEATURES
                        if best_key[0]
                           == "Hybrid full-signal"
                        else LEAKAGE_SAFE_CATEGORICAL_FEATURES
                    ),
                "target": TARGET,
                "excluded_label_fields": [
                    "fraud_type",
                    "fraud_tags",
                    "explanations",
                ],
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
        )

    print()
    print("=" * 78)
    print("MODEL COMPARISON COMPLETE")
    print("=" * 78)

    print(
        metrics_frame[
            [
                "experiment",
                "model",
                "precision",
                "recall",
                "f1",
                "roc_auc",
                "average_precision",
                "balanced_accuracy",
                "false_positive_rate",
                "training_seconds",
            ]
        ].to_string(
            index=False
        )
    )

    print()
    print(
        f"Best model: {best_key[1]}"
    )

    print(
        f"Experiment: {best_key[0]}"
    )

    print(
        f"Metrics: {METRICS_FILE}"
    )

    print(
        f"Model: {BEST_MODEL_FILE}"
    )

    print(
        f"Metadata: "
        f"{MODEL_METADATA_FILE}"
    )

    print(
        f"Feature schema: "
        f"{FEATURE_SCHEMA_FILE}"
    )

    print()
    print(
        "Important: comparison training uses "
        "fixed stratified samples so KNN and "
        "SVM remain practical. Sample limits "
        "are recorded in model metadata."
    )


if __name__ == "__main__":
    main()