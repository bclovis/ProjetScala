-- Table des utilisateurs
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     username VARCHAR(50) UNIQUE NOT NULL,
                                     email VARCHAR(100) UNIQUE NOT NULL,
                                     password_hash TEXT NOT NULL,
                                     is_verified BOOLEAN DEFAULT FALSE,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des portefeuilles des utilisateurs
CREATE TABLE IF NOT EXISTS portfolios (
                                          id SERIAL PRIMARY KEY,
                                          user_id INT REFERENCES users(id) ON DELETE CASCADE,
                                          name VARCHAR(50) NOT NULL,
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des actifs détenus par un portefeuille
CREATE TABLE IF NOT EXISTS portfolio_assets (
                                                id SERIAL PRIMARY KEY,
                                                portfolio_id INT REFERENCES portfolios(id) ON DELETE CASCADE,
                                                asset_type VARCHAR(10) CHECK (asset_type IN ('crypto', 'stock', 'forex')),
                                                symbol VARCHAR(10) NOT NULL,
                                                quantity DECIMAL(20, 8) NOT NULL,
                                                avg_buy_price DECIMAL(20, 8) NOT NULL,
                                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des transactions (achat, vente, dépôt, retrait)
CREATE TABLE IF NOT EXISTS transactions (
                                            id SERIAL PRIMARY KEY,
                                            portfolio_id INT REFERENCES portfolios(id) ON DELETE CASCADE,
                                            asset_type VARCHAR(10) CHECK (asset_type IN ('crypto', 'stock', 'forex')),
                                            symbol VARCHAR(10) NOT NULL,
                                            amount DECIMAL(20, 8) NOT NULL,
                                            price DECIMAL(20, 8) NOT NULL,
                                            tx_type VARCHAR(10) CHECK (tx_type IN ('buy', 'sell', 'deposit', 'withdraw')),
                                            status VARCHAR(10) DEFAULT 'pending',
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des prix en temps réel et historique
CREATE TABLE market_data (
                             time         TIMESTAMP WITH TIME ZONE,
                             symbol       VARCHAR(10),
                             asset_type   VARCHAR(10),
                             price_usd    NUMERIC(20, 8),
                             portfolio_id INTEGER,
                             PRIMARY KEY (time, symbol),
                             FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE
);

-- Index pour accélérer les recherches de prix
CREATE INDEX IF NOT EXISTS idx_market_data_symbol_time ON market_data (symbol, time DESC);

-- Table des indicateurs financiers calculés (Sharpe Ratio, volatilité, etc.)
CREATE TABLE IF NOT EXISTS financial_indicators (
                                                    id SERIAL PRIMARY KEY,
                                                    portfolio_id INT REFERENCES portfolios(id) ON DELETE CASCADE,
                                                    symbol VARCHAR(10) NOT NULL,
                                                    indicator_name VARCHAR(50) NOT NULL,
                                                    value DECIMAL(20, 8) NOT NULL,
                                                    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des simulations d’investissement
CREATE TABLE IF NOT EXISTS investment_simulations (
                                                      id SERIAL PRIMARY KEY,
                                                      portfolio_id INT REFERENCES portfolios(id) ON DELETE CASCADE,
                                                      scenario_name VARCHAR(50) NOT NULL,
                                                      symbol VARCHAR(10) NOT NULL,
                                                      simulated_buy_price DECIMAL(20, 8) NOT NULL,
                                                      simulated_sell_price DECIMAL(20, 8) NOT NULL,
                                                      potential_profit DECIMAL(20, 8),
                                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des notifications et alertes utilisateur
CREATE TABLE IF NOT EXISTS notifications (
                                             id SERIAL PRIMARY KEY,
                                             user_id INT REFERENCES users(id) ON DELETE CASCADE,
                                             message TEXT NOT NULL,
                                             is_read BOOLEAN DEFAULT FALSE,
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table pour le portefeuille utilisateur (wallet)
CREATE TABLE IF NOT EXISTS user_accounts (
                                             id SERIAL PRIMARY KEY,
                                             user_id INT REFERENCES users(id) ON DELETE CASCADE,
                                             balance DECIMAL(20, 8) NOT NULL DEFAULT 0,
                                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/*
-- Insertion de données d'exemple pour AAPL (stock)
INSERT INTO market_data (time, asset_type, symbol, price_usd, volume_24h, market_cap)
VALUES
    ('2025-03-05 21:00:00+01', 'stock', 'AAPL', 230.50, NULL, NULL),
    ('2025-03-05 21:05:00+01', 'stock', 'AAPL', 231.00, NULL, NULL),
    ('2025-03-05 21:10:00+01', 'stock', 'AAPL', 230.75, NULL, NULL),
    ('2025-03-05 21:15:00+01', 'stock', 'AAPL', 232.00, NULL, NULL);

-- Insertion de données d'exemple pour BTC-USD (crypto)
INSERT INTO market_data (time, asset_type, symbol, price_usd, volume_24h, market_cap)
VALUES
    ('2025-03-05 21:00:00+01', 'crypto', 'BTC-USD', 30000.00, NULL, NULL),
    ('2025-03-05 21:05:00+01', 'crypto', 'BTC-USD', 30050.00, NULL, NULL),
    ('2025-03-05 21:10:00+01', 'crypto', 'BTC-USD', 29950.00, NULL, NULL),
    ('2025-03-05 21:15:00+01', 'crypto', 'BTC-USD', 30020.00, NULL, NULL);

-- Insertion de données d'exemple pour SEKEUR=X (forex)
INSERT INTO market_data (time, asset_type, symbol, price_usd, volume_24h, market_cap)
VALUES
    ('2025-03-05 21:00:00+01', 'forex', 'SEKEUR=X', 1.10, NULL, NULL),
    ('2025-03-05 21:05:00+01', 'forex', 'SEKEUR=X', 1.11, NULL, NULL),
    ('2025-03-05 21:10:00+01', 'forex', 'SEKEUR=X', 1.09, NULL, NULL);
*/