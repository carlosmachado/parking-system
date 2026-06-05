-- Rename tables to match updated domain model names
RENAME TABLE vehicle_event TO parking_session;
RENAME TABLE spot          TO parking_spot;

-- Add operating-hours and duration-limit columns to sector
ALTER TABLE sector
    ADD COLUMN open_hour              TIME NOT NULL DEFAULT '00:00:00',
    ADD COLUMN close_hour             TIME NOT NULL DEFAULT '23:59:00',
    ADD COLUMN duration_limit_minutes INT  NOT NULL DEFAULT 1440;
