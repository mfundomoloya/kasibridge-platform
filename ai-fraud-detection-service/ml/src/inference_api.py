from __future__ import annotations

import os
from contextlib import (
    asynccontextmanager,
)
from pathlib import Path

from fastapi import (
    FastAPI,
    HTTPException,
    Request,
)

from fastapi.responses import (
    RedirectResponse,
)


from inference_service import (
    InvoiceFraudInferenceService,
)

from model_contract import (
    HealthResponse,
    InvoiceFraudPredictionRequest,
    InvoiceFraudPredictionResponse,
    ModelInformationResponse,
)


PROJECT_ROOT = Path(__file__).resolve().parents[2]

DEFAULT_ARTIFACTS_DIR = (
        PROJECT_ROOT
        / "ml"
        / "artifacts"
)


def artifact_path(
        environment_name: str,
        default_name: str,
) -> Path:
    configured_value = os.getenv(
        environment_name
    )

    if configured_value:
        return Path(
            configured_value
        ).expanduser().resolve()

    return (
            DEFAULT_ARTIFACTS_DIR
            / default_name
    )


def create_service(
) -> InvoiceFraudInferenceService:
    return InvoiceFraudInferenceService(
        model_path=artifact_path(
            "KASIBRIDGE_ML_MODEL_PATH",
            "best_model.joblib",
        ),
        metadata_path=artifact_path(
            "KASIBRIDGE_ML_METADATA_PATH",
            "model_metadata.json",
        ),
        checksum_path=artifact_path(
            "KASIBRIDGE_ML_CHECKSUM_PATH",
            "best_model.sha256",
        ),
    )


@asynccontextmanager
async def lifespan(
        app: FastAPI,
):
    service = create_service()
    service.load()

    app.state.inference_service = (
        service
    )

    yield

    app.state.inference_service = None


app = FastAPI(
    title=(
        "KasiBridge Procurement "
        "Fraud ML Inference API"
    ),
    version="1.0.0",
    description=(
        "Internal inference service for "
        "the validated procurement invoice "
        "fraud classifier."
    ),
    lifespan=lifespan,
)

@app.get(
    "/",
    include_in_schema=False,
)
def root() -> RedirectResponse:
    return RedirectResponse(
        url="/docs",
    )


def get_service(
        request: Request,
) -> InvoiceFraudInferenceService:
    service = getattr(
        request.app.state,
        "inference_service",
        None,
    )

    if service is None:
        raise HTTPException(
            status_code=503,
            detail=(
                "Fraud inference model "
                "is unavailable."
            ),
        )

    return service


@app.get(
    "/health",
    response_model=HealthResponse,
)
def health(
        request: Request,
) -> HealthResponse:
    service = get_service(
        request
    )

    return HealthResponse(
        status="UP",
        model_loaded=(
            service.is_loaded()
        ),
        model_checksum_verified=(
            service.checksum_verified
        ),
    )


@app.get(
    "/model",
    response_model=ModelInformationResponse,
)
def model_information(
        request: Request,
) -> ModelInformationResponse:
    service = get_service(
        request
    )

    return (
        service.model_information()
    )


@app.post(
    "/predict",
    response_model=(
            InvoiceFraudPredictionResponse
    ),
)
def predict(
        prediction_request:
        InvoiceFraudPredictionRequest,
        request: Request,
) -> (
        InvoiceFraudPredictionResponse
):
    service = get_service(
        request
    )

    try:
        return service.predict(
            prediction_request
        )

    except ValueError as exception:
        raise HTTPException(
            status_code=400,
            detail=str(exception),
        ) from exception

    except Exception as exception:
        raise HTTPException(
            status_code=500,
            detail=(
                "Fraud prediction failed."
            ),
        ) from exception