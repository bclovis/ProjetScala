//frontend/src/pages/AddAssetPage.jsx
import React from "react";
import { useLocation } from "react-router-dom";
import AddAsset from "../components/actifs/AddAsset.jsx";
import Header from "../components/Header";
//import "../styles/LoginPage.css"; // Réutilise le style du Login

const AddAssetPage = () => {


    const location = useLocation();
    // Récupère le portfolioId passé via l'état, avec une valeur par défaut si besoin
    const portfolioId = location.state?.portfolioId || 1;
    const token = localStorage.getItem("token");

    const handleAssetAdded = () => {
        alert("Actif ajouté avec succès !");
        // Rediriger ou actualiser la page selon vos besoins
    };

    return (
        <div className="login-page">
            <div className="container">
                <Header />
                <div className="form-container" style={{ padding: "40px" }}>
                    <h1 className="mb-4">Ajouter un Actif</h1>
                    <AddAsset portfolioId={portfolioId} token={token} onAssetAdded={handleAssetAdded} />
                </div>
            </div>
        </div>
    );
};

export default AddAssetPage;