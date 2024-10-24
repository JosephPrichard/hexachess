BEGIN;
-- Create extensions.
CREATE EXTENSION pg_trgm;

-- Create tables.
CREATE TABLE users (
    id VARCHAR NOT NULL,
    username VARCHAR NOT NULL,
    country VARCHAR,
    elo NUMERIC NOT NULL,
    highestElo NUMERIC NOT NULL,
    wins INTEGER NOT NULL,
    losses INTEGER NOT NULL,
    joinedOn TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    password VARCHAR NOT NULL,
    salt VARCHAR NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT unique_username UNIQUE (username));

CREATE TABLE users_metadata (
    id NUMERIC,
    count INTEGER,
    PRIMARY KEY (id));

CREATE TABLE game_histories (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    whiteId VARCHAR NOT NULL,
    blackId VARCHAR NOT NULL,
    result INTEGER NOT NULL,
    data BYTEA NOT NULL,
    playedOn TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    winElo NUMERIC,
    loseElo NUMERIC);

-- Create indices.
CREATE INDEX idxTrgmUsername ON users USING GIST (username gist_trgm_ops);
CREATE INDEX idxUsername ON users(username);
CREATE INDEX idxElo ON users(elo);

CREATE INDEX idxWhiteId ON game_histories(whiteId, id);
CREATE INDEX idxBlackId ON game_histories(blackId, id);
CREATE INDEX idxBothIds ON game_histories(whiteId, blackId, id);

-- Create functions and procedures.
CREATE FUNCTION probabilityWins(IN elo1 NUMERIC, IN elo2 NUMERIC)
    RETURNS NUMERIC
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN 1.0 / (1.0 + POWER(10, (elo1 - elo2) / 400.0));
END $$;

CREATE PROCEDURE updateStatsUsingResult(
    IN winId VARCHAR, IN loseId VARCHAR,
    OUT winEloNext NUMERIC, OUT loseEloNext NUMERIC)
    LANGUAGE plpgsql
AS $$
DECLARE
    winElo NUMERIC;
    loseElo NUMERIC;
BEGIN
    SELECT elo INTO winElo FROM users WHERE id = winId;
    SELECT elo INTO loseElo FROM users WHERE id = loseId;

    winEloNext = winElo + (30 * (1 - probabilityWins(loseElo, winElo)));
    loseEloNext = loseElo + ((30 * probabilityWins(winElo, loseElo)) * -1);

    UPDATE users
    SET elo = winEloNext, wins = wins + 1, highestElo = GREATEST(highestElo, winEloNext)
    WHERE id = winId;

    UPDATE users
    SET elo = loseEloNext, losses = losses + 1
    WHERE id = loseId;
END $$;
END;

BEGIN; INSERT INTO users_metadata (id, count) VALUES (1, 0); END;