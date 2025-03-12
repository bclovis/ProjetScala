//frontend/src/components/CreatePortfolio.jsx
import React, { useState } from "react";
import PropTypes from "prop-types";

const CreatePortfolio = ({ onPortfolioCreated }) => {
    const [name, setName] = useState("");
    const [error, setError] = useState("");

    const handleCreatePortfolio = async (e) => {
        e.preventDefault();
        const token = localStorage.getItem("token");
        if (!token) {
            setError("User is not authenticated");
            return;
        }
        try {
            const response = await fetch("http://localhost:8080/api/portfolios", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({ name })
            });
            if (!response.ok) {
                setError("Failed to create portfolio");
            } else {
                setName("");
                onPortfolioCreated(); // Pour rafraîchir la liste des portefeuilles
            }
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <form onSubmit={handleCreatePortfolio} className="create-portfolio-form flex items-center gap-2 p-4 bg-white rounded shadow">
            <input
                type="text"
                placeholder="Nom du portefeuille"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="border p-2 rounded flex-1"
            />
            <button type="submit" className="bg-blue-500 text-white p-2 rounded">
                Créer un portefeuille
            </button>
            {error && <p className="text-red-500 mt-2">{error}</p>}
        </form>
    );
};

CreatePortfolio.propTypes = {
    onPortfolioCreated: PropTypes.func.isRequired,
};

export default CreatePortfolio;