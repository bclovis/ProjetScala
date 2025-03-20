//frontend/src/pages/SellAssetPage.jsx
import React from "react";
import { useLocation } from "react-router-dom";
import SellAsset from "../components/actifs/SellAsset.jsx";

const SellAssetPage = () => {
    const location = useLocation();
    // Récuperer le portfolioId passé via l'état de navigation
    const portfolioId = location.state?.portfolioId || 1;
    const token = localStorage.getItem("token");

    const handleAssetSold = () => {
        alert("Actif vendu avec succès !");
    };

    return (
        <div className="login-page">
            <div className="container">
                <div className="form-container" style={{ padding: "40px" }}>
                    <h1 className="mb-4">Vendre un Actif</h1>
                    <SellAsset portfolioId={portfolioId} token={token} onAssetSold={handleAssetSold} />
                </div>
            </div>
        </div>
    );
};

export default SellAssetPage;