CREATE TABLE provider_event (
    guid                    UUID                     NOT NULL,
    version                 BIGINT                   NOT NULL DEFAULT 0,
    created_by              VARCHAR(255)             NOT NULL,
    created_timestamp       TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated_by         VARCHAR(255)             NOT NULL,
    last_updated_timestamp  TIMESTAMP WITH TIME ZONE NOT NULL,
    event_type              VARCHAR(100)             NOT NULL,
    event_source            VARCHAR(100)             NOT NULL,
    correlation_id          VARCHAR(255),
    trace_id                VARCHAR(255),
    payload                 JSONB                    NOT NULL,
    PRIMARY KEY (guid)
);

CREATE INDEX idx_provider_event_event_type       ON provider_event (event_type);
CREATE INDEX idx_provider_event_correlation_id   ON provider_event (correlation_id);
CREATE INDEX idx_provider_event_created_timestamp ON provider_event (created_timestamp DESC);
