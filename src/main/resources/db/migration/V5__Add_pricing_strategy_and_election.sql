-- Persist the elected pricing strategy and the election mode that governed each session
ALTER TABLE parking_session
    ADD COLUMN pricing_strategy VARCHAR(20) NULL,
    ADD COLUMN pricing_election VARCHAR(10) NULL;
