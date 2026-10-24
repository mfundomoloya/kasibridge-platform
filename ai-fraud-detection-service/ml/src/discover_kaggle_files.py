from __future__ import annotations

import json
import re
import subprocess
import time
from pathlib import Path


DATASET = (
    "tokelomashile2/"
    "procurement-invoice-fraud-dataset"
)

PROJECT_ROOT = Path(__file__).resolve().parents[2]

DATA_DIR = (
        PROJECT_ROOT
        / "ml"
        / "data"
)

OUTPUT_FILE = (
        DATA_DIR
        / "dataset_file_listing.txt"
)

STATE_FILE = (
        DATA_DIR
        / "dataset_discovery_state.json"
)

ERROR_FILE = (
        DATA_DIR
        / "dataset_discovery_error.txt"
)

STRUCTURED_EXTENSIONS = (
    ".csv",
    ".json",
    ".jsonl",
    ".parquet",
    ".xlsx",
    ".xls",
    ".feather",
)

NEXT_TOKEN_PATTERN = re.compile(
    r"^Next Page Token = (.+)$",
    re.MULTILINE,
)

MAX_RETRIES = 5


def load_existing_paths() -> set[str]:
    if not OUTPUT_FILE.exists():
        return set()

    return {
        line.strip()
        for line in OUTPUT_FILE.read_text(
            encoding="utf-8"
        ).splitlines()
        if line.strip()
    }


def load_state() -> dict[str, object]:
    if not STATE_FILE.exists():
        return {
            "page_number": 1,
            "page_token": None,
            "completed": False,
        }

    return json.loads(
        STATE_FILE.read_text(
            encoding="utf-8"
        )
    )


def save_paths(
        paths: set[str],
) -> None:
    OUTPUT_FILE.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    content = "\n".join(
        sorted(paths)
    )

    if content:
        content += "\n"

    OUTPUT_FILE.write_text(
        content,
        encoding="utf-8",
    )


def save_state(
        page_number: int,
        page_token: str | None,
        completed: bool,
) -> None:
    STATE_FILE.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    STATE_FILE.write_text(
        json.dumps(
            {
                "page_number": page_number,
                "page_token": page_token,
                "completed": completed,
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
        )


def run_kaggle_page(
        page_token: str | None,
) -> str:
    command = [
        "kaggle",
        "datasets",
        "files",
        DATASET,
        "--page-size",
        "1000",
    ]

    if page_token:
        command.extend(
            [
                "--page-token",
                page_token,
            ]
        )

    last_error = ""

    for attempt in range(
            1,
            MAX_RETRIES + 1,
    ):
        result = subprocess.run(
            command,
            capture_output=True,
            text=True,
        )

        if result.returncode == 0:
            return result.stdout

        last_error = (
                result.stderr.strip()
                or result.stdout.strip()
                or (
                    "Kaggle CLI exited with "
                    f"status {result.returncode}."
                )
        )

        print(
            f"  Attempt {attempt}/"
            f"{MAX_RETRIES} failed:"
        )
        print(
            f"  {last_error}"
        )

        if attempt < MAX_RETRIES:
            delay_seconds = min(
                2 ** attempt,
                30,
                )

            print(
                "  Retrying after "
                f"{delay_seconds} seconds..."
            )

            time.sleep(
                delay_seconds
            )

    raise RuntimeError(
        last_error
    )


def extract_next_token(
        output: str,
) -> str | None:
    match = NEXT_TOKEN_PATTERN.search(
        output
    )

    if not match:
        return None

    token = match.group(1).strip()

    return token or None


def extract_structured_paths(
        output: str,
) -> list[str]:
    paths: list[str] = []

    for line in output.splitlines():
        stripped = line.strip()

        if not stripped:
            continue

        if stripped.startswith(
                (
                        "Next Page Token",
                        "name ",
                        "---",
                )
        ):
            continue

        lower_line = stripped.lower()

        if "/images/" in lower_line:
            continue

        if not any(
                extension in lower_line
                for extension in STRUCTURED_EXTENSIONS
        ):
            continue

        match = re.match(
            r"^(.*?)\s{2,}\d+\s+"
            r"\d{4}-\d{2}-\d{2}",
            stripped,
        )

        if match:
            paths.append(
                match.group(1).strip()
            )

    return paths


def main() -> None:
    DATA_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    state = load_state()
    structured_paths = load_existing_paths()

    page_number = int(
        state.get(
            "page_number",
            1,
        )
    )

    raw_token = state.get(
        "page_token"
    )

    page_token = (
        str(raw_token)
        if raw_token
        else None
    )

    completed = bool(
        state.get(
            "completed",
            False,
        )
    )

    if completed:
        print(
            "Dataset discovery was already "
            "completed."
        )
        print(
            f"Structured files: "
            f"{len(structured_paths)}"
        )
        print(
            f"Listing: {OUTPUT_FILE}"
        )
        return

    if page_token:
        print(
            "Resuming dataset discovery "
            f"from page {page_number}."
        )
    else:
        print(
            "Starting dataset discovery "
            "from the first page."
        )

    seen_tokens: set[str] = set()

    try:
        while True:
            print(
                f"Scanning dataset page "
                f"{page_number}..."
            )

            output = run_kaggle_page(
                page_token
            )

            discovered_paths = (
                extract_structured_paths(
                    output
                )
            )

            previous_count = len(
                structured_paths
            )

            structured_paths.update(
                discovered_paths
            )

            save_paths(
                structured_paths
            )

            new_count = (
                    len(structured_paths)
                    - previous_count
            )

            if new_count > 0:
                print(
                    f"  Found {new_count} new "
                    "structured file(s)."
                )

                for path in discovered_paths:
                    print(
                        f"  + {path}"
                    )

            next_token = (
                extract_next_token(
                    output
                )
            )

            if not next_token:
                save_state(
                    page_number=page_number,
                    page_token=None,
                    completed=True,
                )

                ERROR_FILE.unlink(
                    missing_ok=True
                )

                print()
                print(
                    "Dataset discovery completed."
                )
                print(
                    f"Pages scanned: "
                    f"{page_number}"
                )
                print(
                    "Structured files found: "
                    f"{len(structured_paths)}"
                )
                print(
                    f"Listing: {OUTPUT_FILE}"
                )

            if (
                    next_token == page_token
                    or next_token in seen_tokens
            ):
                raise RuntimeError(
                    "Kaggle returned a repeated "
                    "page token."
                )

            seen_tokens.add(
                next_token
            )

            page_number += 1
            page_token = next_token

            save_state(
                page_number=page_number,
                page_token=page_token,
                completed=False,
            )

            time.sleep(0.25)

    except KeyboardInterrupt:
        print()
        print(
            "Discovery interrupted. Progress "
            "has been preserved."
        )
        print(
            "Run the script again to resume."
        )

    except Exception as exception:
        error_message = (
            f"Page: {page_number}\n"
            f"Error: {exception}\n"
        )

        ERROR_FILE.write_text(
            error_message,
            encoding="utf-8",
        )

        save_paths(
            structured_paths
        )

        save_state(
            page_number=page_number,
            page_token=page_token,
            completed=False,
        )

        print()
        print(
            "Dataset discovery stopped, but "
            "progress was preserved."
        )
        print(
            f"Structured files currently "
            f"found: {len(structured_paths)}"
        )
        print(
            f"Listing: {OUTPUT_FILE}"
        )
        print(
            f"Error details: {ERROR_FILE}"
        )

        raise


if __name__ == "__main__":
    main()