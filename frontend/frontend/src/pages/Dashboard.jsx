import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../components/Header";
import Overview from "../components/Overview";
import PortfolioChart from "../components/PortfolioChart";
import PortfolioAssets from "../components/PortfolioAssets";
import CreatePortfolio from "../components/CreatePortfolio";
import MarketData from "../components/MarketData"; // Importer le composant de MarketData
import "../styles/Dashboard.css";

const Dashboard = () => {
    const [portfolios, setPortfolios] = useState([]);
    const [performanceData, setPerformanceData] = useState(null);
    const [globalBalance, setGlobalBalance] = useState(0);
    const [accountSummary, setAccountSummary] = useState("");
    const [notifications, setNotifications] = useState([]);
    const [error, setError] = useState("");
    const [selectedPortfolio, setSelectedPortfolio] = useState(null);
    const [marketData, setMarketData] = useState({ stocks: [], crypto: {}, forex: {} }); // État pour les données du marché en temps réel
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

    // Connexion WebSocket pour récupérer les données de marché en temps réel
    useEffect(() => {
        const socket = new WebSocket('ws://localhost:8080/market-data');

        socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log('Données de marché reçues :', data);
            setMarketData(data);
        };

        return () => socket.close();
    }, []);

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
            <MarketData marketData={marketData} />

            <div className="dashboard-wrapper">
                <Overview
                    globalBalance={globalBalance}
                    accountSummary={accountSummary}
                    notifications={notifications}
                />

                <div className="portfolio-content">
                    <div className="portfolio-chart-container">
                        {performanceData ? (
                            <PortfolioChart chartData={performanceData} />
                        ) : (
                            <p>Chargement des données...</p>
                        )}
                    </div>
                    <div className="portfolio-assets-container">
                        {selectedPortfolio && (
                            <PortfolioAssets portfolioId={selectedPortfolio} token={token} />
                        )}
                    </div>
                </div>

                <div className="create-portfolio-container">
                    <CreatePortfolio onPortfolioCreated={fetchPortfolios} />
                </div>

                <div className="market-data-button-container">
                    <button
                        onClick={handleGoToMarketData}
                        className="market-data-button"
                    >
                        Voir les Données de Marché
                    </button>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
