//frontend/src/pages/CreatePortfolioPage.jsx
import React from "react";
import CreatePortfolio from "../components/portfolio/CreatePortfolio.jsx";

const CreatePortfolioPage = () => {
    const handlePortfolioCreated = () => {
        alert("Portefeuille créé avec succès !");
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