-- Add Public Defender Service as a first-class provider office discriminator.
--
-- PROVIDER.FIRM_TYPE is unconstrained text, so no schema change is required there to store
-- 'Public Defender Service'.

ALTER TABLE PROVIDER_OFFICE_LINK
    DROP CONSTRAINT IF EXISTS provider_office_link_firm_type_check;

ALTER TABLE PROVIDER_OFFICE_LINK
    ADD CONSTRAINT CK_PROVIDER_OFFICE_LINK_FIRM_TYPE
        CHECK (FIRM_TYPE IN (
            'ProviderOfficeLinkEntity',
            'Advocate',
            'Chambers',
            'Legal Services Provider',
            'Public Defender Service'
        ));
