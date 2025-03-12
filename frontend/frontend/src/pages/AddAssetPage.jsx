import React from "react";
import AddAsset from "../components/AddAsset";
import Header from "../components/Header";
import "../styles/LoginPage.css"; // Réutilise le style du Login

const AddAssetPage = () => {
    const token = localStorage.getItem("token");
    // Pour cet exemple, nous utilisons un portfolioId fixe (à adapter selon votre logique)
    const portfolioId = 19;

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