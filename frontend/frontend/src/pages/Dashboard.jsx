//frontend/src/pages/Dashboard.jsx
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../components/Header";
import Overview from "../components/Overview";
import PortfolioChart from "../components/PortfolioChart";
import PortfolioAssets from "../components/PortfolioAssets";
import CreatePortfolio from "../components/CreatePortfolio";
import "../styles/Dashboard.css";

const Dashboard = () => {
    const [portfolios, setPortfolios] = useState([]);
    const [performanceData, setPerformanceData] = useState(null);
    const [globalBalance, setGlobalBalance] = useState(0);
    const [accountSummary, setAccountSummary] = useState("");
    const [notifications, setNotifications] = useState([]);
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

    // Pour l'exemple, simuler des données globales et des notifications
    const fetchOverviewData = () => {
        setGlobalBalance(12500.75);
        setAccountSummary("Répartition: 60% Crypto, 30% Actions, 10% Devises");
        setNotifications([
            "Nouveau listing: SOL",
            "Attention: Mise à jour de sécurité disponible",
        ]);
    };

    useEffect(() => {
        fetchPortfolios();
        fetchOverviewData();
    }, [navigate, token]);

    // Sélection automatique du premier portefeuille s'il existe
    useEffect(() => {
        if (portfolios.length > 0 && selectedPortfolio === null) {
            setSelectedPortfolio(portfolios[0].id);
        }
    }, [portfolios, selectedPortfolio]);

    // Récupérer les données de performance pour le portefeuille sélectionné
    useEffect(() => {
        if (selectedPortfolio) {
            fetch(`http://localhost:8080/api/portfolios/${selectedPortfolio}/performance`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`,
                },
            })
                .then((res) => res.json())
                .then((data) => {
                    console.log("Données de performance reçues :", data);
                    setPerformanceData(data);
                })
                .catch((err) => console.error(err));
        }
    }, [selectedPortfolio, token]);

    // Fonction de redirection vers la page MarketData
    const handleGoToMarketData = () => {
        navigate("/market-data");
    };

    return (
        <div className="dashboard-page">
            <Header />
            <div className="container mx-auto p-4">
                <Overview
                    globalBalance={globalBalance}
                    accountSummary={accountSummary}
                    notifications={notifications}
                />
                <div className="flex flex-col md:flex-row gap-4">
                    <div className="w-full md:w-1/2">
                        <h2 className="text-lg font-bold mb-2">Performance</h2>
                        {performanceData ? (
                            <PortfolioChart chartData={performanceData} />
                        ) : (
                            <p>Chargement des données...</p>
                        )}
                    </div>
                    <div className="w-full md:w-1/2">
                        <h2 className="text-lg font-bold mb-2">Actifs du Portefeuille</h2>
                        {selectedPortfolio && (
                            <PortfolioAssets portfolioId={selectedPortfolio} token={token} />
                        )}
                    </div>
                </div>
                <div className="mt-4">
                    <CreatePortfolio onPortfolioCreated={fetchPortfolios} />
                </div>
                <div className="mt-4">
                    {/* Bouton de redirection vers MarketData */}
                    <button
                        onClick={handleGoToMarketData}
                        className="bg-green-500 text-white p-2 rounded"
                    >
                        Voir les Données de Marché
                    </button>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;