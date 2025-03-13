//frontend/src/pages/SellAssetPage.jsx
import React from "react";
import { useLocation } from "react-router-dom";
import SellAsset from "../components/actifs/SellAsset.jsx";
import Header from "../components/Header";
//import "../styles/LoginPage.css"; // Vous pouvez réutiliser ce style ou adapter

const SellAssetPage = () => {
    const location = useLocation();
    // On récupère le portfolioId passé via l'état de navigation, sinon on définit une valeur par défaut
    const portfolioId = location.state?.portfolioId || 1;
    const token = localStorage.getItem("token");

    const handleAssetSold = () => {
        alert("Actif vendu avec succès !");
        // Ici, vous pourriez actualiser le portefeuille ou rediriger l'utilisateur
    };

    return (
        <div className="login-page">
            <div className="container">
                <Header />
                <div className="form-container" style={{ padding: "40px" }}>
                    <h1 className="mb-4">Vendre un Actif</h1>
                    <SellAsset portfolioId={portfolioId} token={token} onAssetSold={handleAssetSold} />
                </div>
            </div>
        </div>
    );
};

export default SellAssetPage;