ALTER TABLE public.tenders
    ADD COLUMN required_business_type
        character varying(150);

ALTER TABLE public.tenders
    ADD COLUMN service_area
        character varying(150);

UPDATE public.tenders
SET required_business_type = 'UNSPECIFIED'
WHERE required_business_type IS NULL;

UPDATE public.tenders
SET service_area = 'UNSPECIFIED'
WHERE service_area IS NULL;

ALTER TABLE public.tenders
    ALTER COLUMN required_business_type
        SET NOT NULL;

ALTER TABLE public.tenders
    ALTER COLUMN service_area
        SET NOT NULL;

CREATE INDEX idx_tender_required_business_type
    ON public.tenders (
                       required_business_type
        );

CREATE INDEX idx_tender_service_area
    ON public.tenders (
                       service_area
        );

CREATE INDEX idx_tender_matching_criteria
    ON public.tenders (
                       status,
                       required_business_type,
                       service_area
        );
