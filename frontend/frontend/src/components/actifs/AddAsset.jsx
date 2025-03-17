import React, { useState } from "react";
import PropTypes from "prop-types";

const AddAsset = ({ portfolioId, token, onAssetAdded }) => {
    const [assetType, setAssetType] = useState("");
    const [symbol, setSymbol] = useState("");
    const [quantity, setQuantity] = useState("");
    const [error, setError] = useState("");

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Normalisation des valeurs saisies
        const normalizedAssetType = assetType.trim().toLowerCase();
        const normalizedSymbol = symbol.trim();
        const parsedQuantity = parseFloat(quantity);

        // Validation du type d'actif
        const validAssetTypes = ["crypto", "stock", "forex"];
        if (!validAssetTypes.includes(normalizedAssetType)) {
            setError("Type d'actif invalide. Utilisez 'crypto', 'stock' ou 'forex'.");
            return;
        }

        // Validation de la longueur du symbole (VARCHAR(10))
        if (normalizedSymbol.length > 10) {
            setError("Le symbole ne doit pas dépasser 10 caractères.");
            return;
        }

        // Validation de la quantité
        if (isNaN(parsedQuantity) || parsedQuantity <= 0) {
            setError("Quantité invalide.");
            return;
        }

        const assetData = {
            asset_type: normalizedAssetType,
            symbol: normalizedSymbol,
            quantity: parsedQuantity
        };

        console.log("[DEBUG] Envoi de assetData: ", assetData);

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
                const responseBody = await response.text();
                console.error("[ERROR] Réponse de l'API:", responseBody);
                setError("Erreur lors de l'ajout de l'actif");
            } else {
                // Réinitialiser le formulaire et informer le parent
                setAssetType("");
                setSymbol("");
                setQuantity("");
                setError("");
                onAssetAdded && onAssetAdded();
            }
        } catch (err) {
            console.error("[ERROR] Exception dans handleSubmit: ", err);
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
    onAssetAdded: PropTypes.func,
};

export default AddAsset;