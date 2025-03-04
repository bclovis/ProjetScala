import { useEffect, useState } from "react";
import PropTypes from "prop-types";

const PortfolioAssets = ({ portfolioId, token }) => {
    const [assets, setAssets] = useState([]);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!portfolioId) return;
        fetch(`http://localhost:8080/api/portfolios/${portfolioId}/assets`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            },
        })
            .then((res) => res.json())
            .then((data) => setAssets(data))
            .catch((err) => setError(err.message));
    }, [portfolioId, token]);

    return (
        <div className="portfolio-assets">
            <h3>Actifs du portefeuille #{portfolioId}</h3>
            {error && <p className="error">{error}</p>}
            {assets.length === 0 ? (
                <p>Aucun actif trouvé.</p>
            ) : (
                <ul>
                    {assets.map((asset) => (
                        <li key={asset.id}>
                            {asset.symbol} - {asset.quantity} ({asset.assetType}) - Prix moyen : {asset.avgBuyPrice}
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

PortfolioAssets.propTypes = {
    portfolioId: PropTypes.number.isRequired,
    token: PropTypes.string.isRequired,
};

export default PortfolioAssets;