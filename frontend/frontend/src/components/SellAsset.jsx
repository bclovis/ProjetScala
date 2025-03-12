import React, { useState } from "react";
import PropTypes from "prop-types";

const SellAsset = ({ portfolioId, token, onAssetSold }) => {
    const [assetType, setAssetType] = useState("");
    const [symbol, setSymbol] = useState("");
    const [quantity, setQuantity] = useState("");
    const [error, setError] = useState("");

    const handleSubmit = async (e) => {
        e.preventDefault();
        const sellData = {
            asset_type: assetType,
            symbol,
            quantity: parseFloat(quantity)
        };

        try {
            const response = await fetch(`http://localhost:8080/api/portfolios/${portfolioId}/sell`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify(sellData)
            });

            if (!response.ok) {
                setError("Erreur lors de la vente de l'actif");
            } else {
                // Réinitialise le formulaire et informe le parent
                setAssetType("");
                setSymbol("");
                setQuantity("");
                setError("");
                onAssetSold && onAssetSold();
            }
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="sell-asset-form p-4 bg-white rounded shadow mb-4">
            <h3 className="text-lg font-bold mb-2">Vendre un actif</h3>
            <div className="mb-2">
                <input
                    type="text"
                    placeholder="Type d'actif (crypto, stock, forex)"
                    value={assetType}
                    onChange={(e) => setAssetType(e.target.value)}
                    className="border p-2 rounded w-full"
                    required
                />
            </div>
            <div className="mb-2">
                <input
                    type="text"
                    placeholder="Symbole (ex: BTC-EUR)"
                    value={symbol}
                    onChange={(e) => setSymbol(e.target.value)}
                    className="border p-2 rounded w-full"
                    required
                />
            </div>
            <div className="mb-2">
                <input
                    type="number"
                    placeholder="Quantité à vendre"
                    value={quantity}
                    onChange={(e) => setQuantity(e.target.value)}
                    className="border p-2 rounded w-full"
                    required
                />
            </div>
            <button type="submit" className="bg-red-500 text-white p-2 rounded w-full">
                Vendre
            </button>
            {error && <p className="text-red-500 mt-2">{error}</p>}
        </form>
    );
};

SellAsset.propTypes = {
    portfolioId: PropTypes.number.isRequired,
    token: PropTypes.string.isRequired,
    onAssetSold: PropTypes.func
};

export default SellAsset;