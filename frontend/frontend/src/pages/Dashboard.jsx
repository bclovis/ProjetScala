//frontend/src/pages/Dashboard.jsx
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import PropTypes from "prop-types";
import PortfolioChart from "../components/PortfolioChart";
import PortfolioAssets from "../components/PortfolioAssets";
import "../styles/Dashboard.css";

const Dashboard = () => {
    const [portfolios, setPortfolios] = useState([]);
    const [error, setError] = useState("");
    const [selectedPortfolio, setSelectedPortfolio] = useState(null);
    const navigate = useNavigate();
    const token = localStorage.getItem("token");

    const fetchPortfolios = () => {
        if (!token) {
            navigate("/login");
            return;
        }
        fetch("http://localhost:8080/api/portfolios", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            },
        })
            .then((res) => res.json())
            .then((data) => setPortfolios(data))
            .catch((err) => setError(err.message));
    };

    useEffect(() => {
        fetchPortfolios();
    }, [navigate, token]);

    // Données fictives pour la courbe de performance (à remplacer par des données réelles)
    const sampleChartData = {
        labels: ["Jan", "Feb", "Mar", "Apr", "May", "Jun"],
        data: [100, 150, 130, 170, 160, 180],
    };

    return (
        <div className="dashboard-page">
            <div className="dashboard-wrapper">
                <h1>Your Dashboard</h1>
                {error && <p className="error">{error}</p>}

                {/* Section Portefeuilles */}
                <div className="portfolios-section">
                    <h2>Mes portefeuilles</h2>
                    <ul>
                        {portfolios.map((portfolio) => (
                            <li key={portfolio.id}>
                                {portfolio.name}
                                <button
                                    onClick={() => setSelectedPortfolio(portfolio.id)}
                                    className="view-assets-btn"
                                >
                                    Afficher les actifs
                                </button>
                            </li>
                        ))}
                    </ul>
                </div>

                {/* Formulaire de création de portefeuille */}
                <CreatePortfolio onPortfolioCreated={fetchPortfolios} />

                {/* Affichage graphique de performance */}
                <div className="portfolio-chart">
                    <PortfolioChart chartData={sampleChartData} />
                </div>

                {/* Affichage des actifs si un portefeuille est sélectionné */}
                {selectedPortfolio && (
                    <PortfolioAssets portfolioId={selectedPortfolio} token={token} />
                )}
            </div>
        </div>
    );
};

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
        const response = await fetch("http://localhost:8080/api/portfolios", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            },
            body: JSON.stringify({ name }),
        });
        if (!response.ok) {
            setError("Failed to create portfolio");
        } else {
            setName("");
            onPortfolioCreated();
        }
    };

    return (
        <form onSubmit={handleCreatePortfolio} className="create-portfolio-form">
            <input
                type="text"
                placeholder="Nom du portefeuille"
                value={name}
                onChange={(e) => setName(e.target.value)}
            />
            <button type="submit" className="button">Créer un portefeuille</button>
            {error && <p className="error">{error}</p>}
        </form>
    );
};

CreatePortfolio.propTypes = {
    onPortfolioCreated: PropTypes.func.isRequired,
};

export default Dashboard;