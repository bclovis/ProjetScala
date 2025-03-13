//frontend/src/components/PortfolioList.jsx
import React from "react";
import PropTypes from "prop-types";

const PortfolioList = ({ portfolios, onSelectPortfolio, selectedPortfolioId }) => {
    return (
        <div className="portfolio-list p-4 bg-white rounded shadow mb-4">
            <h2 className="text-xl font-bold mb-2">Mes Portefeuilles</h2>
            {portfolios.length === 0 ? (
                <p>Aucun portefeuille trouvé.</p>
            ) : (
                <ul>
                    {portfolios.map((portfolio) => (
                        <li
                            key={portfolio.id}
                            className={`cursor-pointer p-2 rounded mb-1 ${
                                selectedPortfolioId === portfolio.id ? "bg-blue-100" : "bg-gray-100"
                            }`}
                            onClick={() => onSelectPortfolio(portfolio.id)}
                        >
                            {portfolio.name}
                        </li>
                    ))}
                </ul>
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