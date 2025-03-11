import React from "react";
import CreatePortfolio from "../components/CreatePortfolio";
import Header from "../components/Header";
import "../styles/LoginPage.css"; // Réutilise le style du Login

const CreatePortfolioPage = () => {
    const handlePortfolioCreated = () => {
        alert("Portefeuille créé avec succès !");
        // Rediriger ou actualiser la page selon vos besoins
    };

    return (

        <div className="login-page">
            <div className="container">
                <div className="form-container" style={{ padding: "40px" }}>
                    <h1 className="mb-4">Créer un Portefeuille</h1>
                    <CreatePortfolio onPortfolioCreated={handlePortfolioCreated} />
                </div>
            </div>
        </div>
    );
};

export default CreatePortfolioPage;