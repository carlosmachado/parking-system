-- Drop FK constraints before altering PKs
ALTER TABLE spot DROP FOREIGN KEY fk_spot_sector;
ALTER TABLE vehicle_event DROP FOREIGN KEY fk_vehicle_sector;
ALTER TABLE vehicle_event DROP FOREIGN KEY fk_vehicle_spot;
ALTER TABLE daily_revenue DROP FOREIGN KEY fk_daily_revenue_sector;

-- sector: BIGINT AUTO_INCREMENT → BINARY(16)
ALTER TABLE sector MODIFY id BIGINT NOT NULL;
ALTER TABLE sector DROP PRIMARY KEY;
ALTER TABLE sector ADD COLUMN id_new BINARY(16) NOT NULL FIRST;
ALTER TABLE sector DROP COLUMN id;
ALTER TABLE sector RENAME COLUMN id_new TO id;
ALTER TABLE sector ADD PRIMARY KEY (id);

-- spot: BIGINT → BINARY(16), add external_id for simulator reference
ALTER TABLE spot DROP PRIMARY KEY;
ALTER TABLE spot ADD COLUMN id_new BINARY(16) NOT NULL FIRST;
ALTER TABLE spot ADD COLUMN external_id BIGINT NULL AFTER id_new;
ALTER TABLE spot DROP COLUMN id;
ALTER TABLE spot RENAME COLUMN id_new TO id;
ALTER TABLE spot ADD PRIMARY KEY (id);

-- vehicle_event: id BIGINT → BINARY(16); spot_id BIGINT → BINARY(16)
ALTER TABLE vehicle_event MODIFY id BIGINT NOT NULL;
ALTER TABLE vehicle_event DROP PRIMARY KEY;
ALTER TABLE vehicle_event ADD COLUMN id_new BINARY(16) NOT NULL FIRST;
ALTER TABLE vehicle_event DROP COLUMN id;
ALTER TABLE vehicle_event RENAME COLUMN id_new TO id;
ALTER TABLE vehicle_event ADD PRIMARY KEY (id);
ALTER TABLE vehicle_event MODIFY COLUMN spot_id BINARY(16) NULL;

-- daily_revenue: BIGINT → BINARY(16)
ALTER TABLE daily_revenue MODIFY id BIGINT NOT NULL;
ALTER TABLE daily_revenue DROP PRIMARY KEY;
ALTER TABLE daily_revenue ADD COLUMN id_new BINARY(16) NOT NULL FIRST;
ALTER TABLE daily_revenue DROP COLUMN id;
ALTER TABLE daily_revenue RENAME COLUMN id_new TO id;
ALTER TABLE daily_revenue ADD PRIMARY KEY (id);

-- Recreate FK constraints
ALTER TABLE spot ADD CONSTRAINT fk_spot_sector FOREIGN KEY (sector_code) REFERENCES sector (code);
ALTER TABLE vehicle_event ADD CONSTRAINT fk_vehicle_sector FOREIGN KEY (sector_code) REFERENCES sector (code);
ALTER TABLE vehicle_event ADD CONSTRAINT fk_vehicle_spot FOREIGN KEY (spot_id) REFERENCES spot (id);
ALTER TABLE daily_revenue ADD CONSTRAINT fk_daily_revenue_sector FOREIGN KEY (sector_code) REFERENCES sector (code);

CREATE INDEX idx_spot_external_id ON spot (external_id);
