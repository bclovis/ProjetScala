\c portfolio_db

-- Table des utilisateurs
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password_hash TEXT NOT NULL,
                       is_verified BOOLEAN DEFAULT FALSE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des portefeuilles des utilisateurs
CREATE TABLE portfolios (
                            id SERIAL PRIMARY KEY,
                            user_id INT REFERENCES users(id) ON DELETE CASCADE,
                            name VARCHAR(50) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des actifs détenus par un portefeuille
CREATE TABLE portfolio_assets (
                                  id SERIAL PRIMARY KEY,
                                  portfolio_id INT REFERENCES portfolios(id) ON DELETE CASCADE,
                                  asset_type VARCHAR(10) CHECK (asset_type IN ('crypto', 'stock', 'forex')),
                                  symbol VARCHAR(10) NOT NULL,
                                  quantity DECIMAL(20, 8) NOT NULL,
                                  avg_buy_price DECIMAL(20, 8) NOT NULL,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des transactions (achat, vente, dépôt, retrait)
CREATE TABLE transactions (
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

-- Table des prix en temps réel (Utilisation de TimescaleDB pour l'optimisation)
CREATE TABLE market_data (
                             time TIMESTAMPTZ NOT NULL,
                             asset_type VARCHAR(10) CHECK (asset_type IN ('crypto', 'stock', 'forex')),
                             symbol VARCHAR(10) NOT NULL,
                             price_usd DECIMAL(20, 8) NOT NULL,
                             volume_24h DECIMAL(20, 8),
                             market_cap DECIMAL(20, 8),
                             PRIMARY KEY (time, symbol)
) PARTITION BY RANGE (time);


-- Index pour accélérer les recherches de prix
CREATE INDEX ON market_data (symbol, time DESC);

-- Table des indicateurs financiers calculés (Sharpe Ratio, volatilité, etc.)
CREATE TABLE financial_indicators (
                                      id SERIAL PRIMARY KEY,
                                      portfolio_id INT REFERENCES portfolios(id) ON DELETE CASCADE,
                                      symbol VARCHAR(10) NOT NULL,
                                      indicator_name VARCHAR(50) NOT NULL,
                                      value DECIMAL(20, 8) NOT NULL,
                                      calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des simulations d’investissement
CREATE TABLE investment_simulations (
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
CREATE TABLE notifications (
                               id SERIAL PRIMARY KEY,
                               user_id INT REFERENCES users(id) ON DELETE CASCADE,
                               message TEXT NOT NULL,
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);