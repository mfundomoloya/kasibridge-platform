from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pandas as pd


PROJECT_ROOT = Path(__file__).resolve().parents[2]

RAW_DATA_DIR = (
        PROJECT_ROOT
        / "ml"
        / "data"
        / "raw"
)

PROCESSED_DATA_DIR = (
        PROJECT_ROOT
        / "ml"
        / "data"
        / "processed"
)

OUTPUT_FILE = (
        PROCESSED_DATA_DIR
        / "procurement_fraud_training_dataset.csv"
)

REPORT_FILE = (
        PROCESSED_DATA_DIR
        / "procurement_fraud_dataset_report.json"
)


def find_table(
        filename: str,
) -> Path:
    matches = list(
        RAW_DATA_DIR.rglob(filename)
    )

    if len(matches) != 1:
        raise RuntimeError(
            f"Expected exactly one {filename}, "
            f"but found {len(matches)}."
        )

    return matches[0]


def read_parquet(
        filename: str,
) -> pd.DataFrame:
    path = find_table(filename)

    print(
        f"Loading {filename}: {path}"
    )

    return pd.read_parquet(path)


def validate_unique_key(
        dataframe: pd.DataFrame,
        table_name: str,
        key: str,
) -> None:
    if key not in dataframe.columns:
        raise ValueError(
            f"{table_name} does not contain "
            f"the required key {key}."
        )

    duplicate_count = (
        dataframe[key]
        .duplicated()
        .sum()
    )

    if duplicate_count > 0:
        raise ValueError(
            f"{table_name} contains "
            f"{duplicate_count:,} duplicate "
            f"{key} values."
        )


def validate_required_columns(
        dataframe: pd.DataFrame,
        table_name: str,
        required_columns: set[str],
) -> None:
    missing_columns = (
            required_columns
            - set(dataframe.columns)
    )

    if missing_columns:
        raise ValueError(
            f"{table_name} is missing columns: "
            f"{sorted(missing_columns)}"
        )


def add_date_features(
        dataframe: pd.DataFrame,
) -> pd.DataFrame:
    result = dataframe.copy()

    result["invoice_date"] = pd.to_datetime(
        result["invoice_date"],
        errors="raise",
    )

    result["invoice_month"] = (
        result["invoice_date"]
        .dt.month
        .astype("int16")
    )

    result["invoice_day_of_week"] = (
        result["invoice_date"]
        .dt.dayofweek
        .astype("int16")
    )

    result["invoice_day_of_month"] = (
        result["invoice_date"]
        .dt.day
        .astype("int16")
    )

    result["invoice_is_weekend"] = (
        result["invoice_day_of_week"]
        .isin([5, 6])
        .astype("int8")
    )

    return result


def add_image_features(
        dataframe: pd.DataFrame,
) -> pd.DataFrame:
    result = dataframe.copy()

    result["has_invoice_image"] = (
        result["image_path"]
        .notna()
        .astype("int8")
    )

    result["has_ocr_result"] = (
        result["ocr_total_extracted"]
        .notna()
        .astype("int8")
    )

    result["ocr_amount_difference"] = (
            result["ocr_total_extracted"]
            - result["invoice_amount"]
    )

    result["ocr_amount_difference_abs"] = (
        result["ocr_amount_difference"]
        .abs()
    )

    denominator = (
        result["invoice_amount"]
        .abs()
        .replace(0, np.nan)
    )

    result["ocr_amount_difference_ratio"] = (
            result["ocr_amount_difference_abs"]
            / denominator
    )

    result[
        "ocr_amount_difference_ratio"
    ] = (
        result[
            "ocr_amount_difference_ratio"
        ]
        .replace(
            [np.inf, -np.inf],
            np.nan,
        )
    )

    return result


def build_consistency_report(
        labels: pd.DataFrame,
        dataset: pd.DataFrame,
) -> dict[str, object]:
    positive_label_count = int(
        labels["is_fraud"].sum()
    )

    non_none_type_count = int(
        labels["fraud_type"]
        .ne("NONE")
        .sum()
    )

    positive_with_none_type = int(
        (
                labels["is_fraud"].eq(1)
                & labels["fraud_type"].eq("NONE")
        ).sum()
    )

    negative_with_fraud_type = int(
        (
                labels["is_fraud"].eq(0)
                & labels["fraud_type"].ne("NONE")
        ).sum()
    )

    split_distribution = (
        dataset["split"]
        .value_counts()
        .sort_index()
        .to_dict()
    )

    label_by_split = (
        dataset
        .groupby(
            "split",
            observed=True,
        )["is_fraud"]
        .agg(
            rows="size",
            fraud_count="sum",
            fraud_rate="mean",
        )
        .reset_index()
        .to_dict(
            orient="records"
        )
    )

    fraud_type_distribution = (
        labels["fraud_type"]
        .value_counts()
        .to_dict()
    )

    return {
        "rows": int(len(dataset)),
        "columns": int(
            len(dataset.columns)
        ),
        "positive_label_count":
            positive_label_count,
        "negative_label_count":
            int(
                len(labels)
                - positive_label_count
            ),
        "fraud_rate":
            float(
                labels["is_fraud"]
                .mean()
            ),
        "non_none_fraud_type_count":
            non_none_type_count,
        "positive_label_with_none_type":
            positive_with_none_type,
        "negative_label_with_fraud_type":
            negative_with_fraud_type,
        "split_distribution":
            split_distribution,
        "label_by_split":
            label_by_split,
        "fraud_type_distribution":
            fraud_type_distribution,
    }


def main() -> None:
    invoices = read_parquet(
        "invoices.parquet"
    )

    labels = read_parquet(
        "labels.parquet"
    )

    behavioural = read_parquet(
        "behavioural_features.parquet"
    )

    suppliers = read_parquet(
        "suppliers.parquet"
    )

    departments = read_parquet(
        "departments.parquet"
    )

    splits = read_parquet(
        "splits.parquet"
    )

    images = read_parquet(
        "images_metadata.parquet"
    )

    validate_unique_key(
        invoices,
        "invoices",
        "invoice_id",
    )

    validate_unique_key(
        labels,
        "labels",
        "invoice_id",
    )

    validate_unique_key(
        behavioural,
        "behavioural_features",
        "invoice_id",
    )

    validate_unique_key(
        splits,
        "splits",
        "invoice_id",
    )

    validate_unique_key(
        suppliers,
        "suppliers",
        "supplier_id",
    )

    validate_unique_key(
        departments,
        "departments",
        "department_id",
    )

    validate_unique_key(
        images,
        "images_metadata",
        "invoice_id",
    )

    validate_required_columns(
        labels,
        "labels",
        {
            "invoice_id",
            "is_fraud",
            "fraud_type",
            "fraud_tags",
            "explanations",
        },
    )

    dataset = (
        invoices
        .merge(
            labels,
            on="invoice_id",
            how="inner",
            validate="one_to_one",
        )
        .merge(
            behavioural,
            on="invoice_id",
            how="inner",
            validate="one_to_one",
        )
        .merge(
            splits,
            on="invoice_id",
            how="inner",
            validate="one_to_one",
        )
        .merge(
            suppliers,
            on="supplier_id",
            how="left",
            validate="many_to_one",
        )
        .merge(
            departments,
            on="department_id",
            how="left",
            validate="many_to_one",
        )
        .merge(
            images[
                [
                    "invoice_id",
                    "ocr_total_extracted",
                    "image_tamper_flag",
                ]
            ],
            on="invoice_id",
            how="left",
            validate="one_to_one",
        )
    )

    if len(dataset) != len(invoices):
        raise RuntimeError(
            "Merged dataset row count does "
            "not match the invoice table. "
            f"Expected {len(invoices):,}, "
            f"found {len(dataset):,}."
        )

    dataset = add_date_features(
        dataset
    )

    dataset = add_image_features(
        dataset
    )

    report = build_consistency_report(
        labels,
        dataset,
    )

    print()
    print("Label consistency")
    print("-" * 72)

    print(
        "is_fraud = 1:",
        f"{report['positive_label_count']:,}",
    )

    print(
        "fraud_type != NONE:",
        f"{report['non_none_fraud_type_count']:,}",
    )

    print(
        "is_fraud = 1 and fraud_type = NONE:",
        f"{report['positive_label_with_none_type']:,}",
    )

    print(
        "is_fraud = 0 and fraud_type != NONE:",
        f"{report['negative_label_with_fraud_type']:,}",
    )

    print()
    print("Label balance by split")
    print("-" * 72)

    for split_result in report[
        "label_by_split"
    ]:
        print(
            f"{split_result['split']}: "
            f"rows={split_result['rows']:,}, "
            f"fraud="
            f"{int(split_result['fraud_count']):,}, "
            f"rate="
            f"{split_result['fraud_rate']:.4f}"
        )

    if dataset[
        [
            "supplier_country",
            "supplier_age_days",
            "annual_budget",
        ]
    ].isna().any().any():
        raise RuntimeError(
            "Supplier or department joins "
            "produced unexpected missing values."
        )

    dataset = dataset.sort_values(
        "invoice_id"
    ).reset_index(
        drop=True
    )

    PROCESSED_DATA_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    dataset.to_csv(
        OUTPUT_FILE,
        index=False,
    )

    REPORT_FILE.write_text(
        json.dumps(
            report,
            indent=2,
            default=str,
        )
        + "\n",
        encoding="utf-8",
        )

    print()
    print("Prepared dataset")
    print("-" * 72)

    print(
        f"Rows: {len(dataset):,}"
    )

    print(
        f"Columns: {len(dataset.columns)}"
    )

    print(
        f"CSV: {OUTPUT_FILE}"
    )

    print(
        f"Report: {REPORT_FILE}"
    )

    print()
    print(
        "Important: fraud_type, fraud_tags "
        "and explanations are descriptive "
        "labels. They must not be model inputs."
    )


if __name__ == "__main__":
    main()