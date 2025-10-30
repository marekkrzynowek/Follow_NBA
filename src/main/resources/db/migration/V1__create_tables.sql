-- Create teams table
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    nba_team_id INTEGER UNIQUE NOT NULL,
    team_name VARCHAR(100) UNIQUE NOT NULL,
    abbreviation VARCHAR(3) UNIQUE NOT NULL,
    division VARCHAR(50) NOT NULL,
    conference VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create games table
CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    nba_game_id BIGINT UNIQUE NOT NULL,
    game_date DATE NOT NULL,
    home_team_id BIGINT NOT NULL REFERENCES teams(id),
    away_team_id BIGINT NOT NULL REFERENCES teams(id),
    home_score INTEGER NOT NULL,
    away_score INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for games table
CREATE INDEX idx_game_date ON games(game_date);
CREATE INDEX idx_home_team ON games(home_team_id);
CREATE INDEX idx_away_team ON games(away_team_id);

-- Create standings_snapshots table
CREATE TABLE standings_snapshots (
    id BIGSERIAL PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    wins INTEGER NOT NULL,
    losses INTEGER NOT NULL,
    win_pct DECIMAL(5,3) NOT NULL,
    games_back DECIMAL(4,1),
    division_rank INTEGER,
    conference_rank INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(snapshot_date, team_id)
);

-- Create indexes for standings_snapshots table
CREATE INDEX idx_snapshot_date ON standings_snapshots(snapshot_date);
CREATE INDEX idx_team_date ON standings_snapshots(team_id, snapshot_date);
