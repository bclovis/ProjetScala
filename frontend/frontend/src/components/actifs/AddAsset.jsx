//frontend/src/components/AddAsset.jsx.jsx
import React, { useState } from "react";
import PropTypes from "prop-types";

const AddAsset = ({ portfolioId, token, onAssetAdded }) => {
    const [assetType, setAssetType] = useState("");
    const [symbol, setSymbol] = useState("");
    const [quantity, setQuantity] = useState("");
    const [error, setError] = useState("");

    const handleSubmit = async (e) => {
        e.preventDefault();
        const assetData = {
            asset_type: assetType,
            symbol,
            quantity: parseFloat(quantity)
            // Le prix payé sera récupéré automatiquement côté backend
        };

        try {
            const response = await fetch(`http://localhost:8080/api/portfolios/${portfolioId}/assets`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify(assetData)
            });

            if (!response.ok) {
                setError("Erreur lors de l'ajout de l'actif");
            } else {
                // Réinitialise le formulaire et informe le parent
                setAssetType("");
                setSymbol("");
                setQuantity("");
                setError("");
                onAssetAdded && onAssetAdded();
            }
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="add-asset-form p-4 bg-white rounded shadow mb-4">
            <h3 className="text-lg font-bold mb-2">Ajouter un actif</h3>
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
                    placeholder="Quantité"
                    value={quantity}
                    onChange={(e) => setQuantity(e.target.value)}
                    className="border p-2 rounded w-full"
                    required
                />
            </div>
            <button type="submit" className="bg-blue-500 text-white p-2 rounded w-full">
                Ajouter l'actif
            </button>
            {error && <p className="text-red-500 mt-2">{error}</p>}
        </form>
    );
};

AddAsset.propTypes = {
    portfolioId: PropTypes.number.isRequired,
    token: PropTypes.string.isRequired,
    onAssetAdded: PropTypes.func
};

export default AddAsset;