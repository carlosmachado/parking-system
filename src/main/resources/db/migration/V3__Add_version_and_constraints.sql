-- Optimistic locking version column on all aggregates
ALTER TABLE sector        ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE spot          ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE vehicle_event ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE daily_revenue ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- New unique constraint: spot location within a sector is unique
ALTER TABLE spot ADD CONSTRAINT uq_spot_sector_location UNIQUE (sector_code, lat, lng);
