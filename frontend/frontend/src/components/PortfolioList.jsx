import React from "react";
import PropTypes from "prop-types";

const PortfolioList = ({ portfolios, onSelectPortfolio, selectedPortfolioId }) => {
    const handleChange = (e) => {
        // Convertir la valeur (string) en nombre
        onSelectPortfolio(Number(e.target.value));
    };

    return (
        <div className="portfolio-list p-4 bg-white rounded shadow mb-4">
            <h2 className="text-xl font-bold mb-2">Mes Portefeuilles</h2>
            {portfolios.length === 0 ? (
                <p>Aucun portefeuille trouvé.</p>
            ) : (
                <select
                    value={selectedPortfolioId || ""}
                    onChange={handleChange}
                    className="p-2 rounded border"
                >
                    {portfolios.map((portfolio) => (
                        <option key={portfolio.id} value={portfolio.id}>
                            {portfolio.name}
                        </option>
                    ))}
                </select>
            )}
        </div>
    );
};

PortfolioList.propTypes = {
    portfolios: PropTypes.array.isRequired,
    onSelectPortfolio: PropTypes.func.isRequired,
    selectedPortfolioId: PropTypes.number,
};

export default PortfolioList;