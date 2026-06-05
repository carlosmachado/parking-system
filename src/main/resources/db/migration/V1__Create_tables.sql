CREATE TABLE IF NOT EXISTS sector (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(10)    NOT NULL UNIQUE,
    base_price DECIMAL(10, 2) NOT NULL,
    max_capacity INT          NOT NULL
);

CREATE TABLE IF NOT EXISTS spot (
    id          BIGINT         NOT NULL PRIMARY KEY,
    sector_code VARCHAR(10)    NOT NULL,
    lat         DOUBLE         NOT NULL,
    lng         DOUBLE         NOT NULL,
    occupied    BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_spot_sector FOREIGN KEY (sector_code) REFERENCES sector (code)
);

CREATE INDEX idx_spot_sector_code ON spot (sector_code);
CREATE INDEX idx_spot_occupied ON spot (occupied);

CREATE TABLE IF NOT EXISTS vehicle_event (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_plate  VARCHAR(20)    NOT NULL,
    sector_code    VARCHAR(10),
    spot_id        BIGINT,
    entry_time     DATETIME       NOT NULL,
    parked_time    DATETIME,
    exit_time      DATETIME,
    amount_charged DECIMAL(10, 2),
    status         VARCHAR(20)    NOT NULL,
    created_at     DATETIME       NOT NULL,
    updated_at     DATETIME       NOT NULL,
    CONSTRAINT fk_vehicle_sector FOREIGN KEY (sector_code) REFERENCES sector (code),
    CONSTRAINT fk_vehicle_spot   FOREIGN KEY (spot_id)     REFERENCES spot (id)
);

CREATE INDEX idx_vehicle_event_license_plate ON vehicle_event (license_plate);
CREATE INDEX idx_vehicle_event_status ON vehicle_event (status);
CREATE INDEX idx_vehicle_event_exit_time ON vehicle_event (exit_time);

CREATE TABLE IF NOT EXISTS daily_revenue (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sector_code  VARCHAR(10)    NOT NULL,
    date         DATE           NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    created_at   DATETIME       NOT NULL,
    updated_at   DATETIME       NOT NULL,
    CONSTRAINT fk_daily_revenue_sector FOREIGN KEY (sector_code) REFERENCES sector (code),
    UNIQUE INDEX idx_daily_revenue_sector_date (sector_code, date)
);
