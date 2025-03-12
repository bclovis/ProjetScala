// frontend/src/pages/Dashboard.jsx
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Overview from "../components/Overview";
import PortfolioChart from "../components/PortfolioChart";
import PortfolioAssets from "../components/PortfolioAssets";
import PortfolioList from "../components/PortfolioList";
import AccountSummaryChart from "../components/AccountSummaryChart";
import TransactionHistory from "../components/TransactionHistory";
import MarketDashboard from "../components/MarketData"; // Import pour le dashboard des marchés

import "../styles/Dashboard.css";

const Dashboard = () => {
    const [portfolios, setPortfolios] = useState([]);
    const [performanceData, setPerformanceData] = useState(null);
    const [globalBalance, setGlobalBalance] = useState(0);
    const [walletBalance, setWalletBalance] = useState(0);
    const [accountSummaryData, setAccountSummaryData] = useState({ crypto: 0, action: 0, devise: 0 });
    const [notifications, setNotifications] = useState([
        "Nouveau listing: SOL",
        "Attention: Mise à jour de sécurité disponible",
    ]);

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
            .then((data) => {
                setPortfolios(data);
                if (data.length > 0 && !selectedPortfolio) {
                    setSelectedPortfolio(data[0].id);
                }
            })
            .catch((err) => setError(err.message));
    };

    // Récupérer le solde global des actifs
    const fetchGlobalBalance = () => {
        fetch("http://localhost:8080/api/global-balance", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            },
        })
            .then((res) => res.json())
            .then((data) => {
                setGlobalBalance(parseFloat(data.globalBalance));
            })
            .catch((err) => console.error("Erreur lors du chargement du solde global :", err));
    };

    // Récupérer le solde des fonds déposés (compte liquide)
    const fetchWalletBalance = () => {
        fetch("http://localhost:8080/api/wallet-balance", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            },
        })
            .then((res) => res.json())
            .then((data) => {
                setWalletBalance(parseFloat(data.walletBalance));
            })
            .catch((err) => console.error("Erreur lors du chargement du solde du wallet :", err));
    };

    // Récupérer le résumé dynamique du compte (pour le graphique en camembert)
    const fetchAccountSummary = () => {
        fetch("http://localhost:8080/api/account-summary", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            },
        })
            .then((res) => res.json())
            .then((data) => {
                setAccountSummaryData({
                    crypto: parseFloat(data.crypto),
                    action: parseFloat(data.action),
                    devise: parseFloat(data.devise),
                });
            })
            .catch((err) => console.error("Erreur lors du chargement du résumé du compte :", err));
    };

    useEffect(() => {
        fetchPortfolios();
        fetchGlobalBalance();
        fetchWalletBalance();
        fetchAccountSummary();
    }, [navigate, token]);

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

    const handleGoToMarketData = () => {
        navigate("/market-data");
    };

    // Bouton "+" pour créer un nouveau portefeuille redirigeant vers une nouvelle page
    const handleCreatePortfolio = () => {
        navigate("/create-portfolio");
    };

    // Bouton "+" pour ajouter un actif redirigeant vers une nouvelle page
    const handleAddAsset = () => {
        navigate("/add-asset", { state: { portfolioId: selectedPortfolio } });
    };

    const handleSellAsset = () => {
        navigate("/sell-asset", { state: { portfolioId: selectedPortfolio } });
    };

    // Définition du grid template areas pour les grands écrans
    const gridTemplateLargeScreens = `
        "c c"
        "a b"
        "d b"
    `;
    const gridStyle = {
        display: "grid",
        gridTemplateAreas: gridTemplateLargeScreens,
        gridTemplateColumns: "1fr 2fr",
        gap: "10px",
        padding: "10px",
    };

    // Section Historique des transactions
    const TransactionHistorySection = () => (
        <div style={{ padding: '10px' }}>
            <TransactionHistory portfolioId={selectedPortfolio} token={token} />
        </div>
    );

    return (
        <div className="dashboard-page">
            <div style={gridStyle}>
                {/* Zone A : Overview & Account Summary Chart */}
                <div style={{ gridArea: "a", background: "#e0e0e0", padding: "10px" }}>
                    <Overview
                        walletBalance={walletBalance}
                        globalBalance={globalBalance}
                        notifications={notifications}
                    />
                    <AccountSummaryChart summaryData={accountSummaryData} />
                </div>
                {/* Zone B : Portfolio List & Performance Details */}
                <div style={{ gridArea: "b", background: "#d0d0d0", padding: "10px" }}>
                    <div className="portfolio-header flex justify-between items-center mb-4">
                        <button onClick={handleCreatePortfolio} className="text-xl font-bold px-2 py-1 bg-green-500 text-white rounded">
                            +
                        </button>
                    </div>
                    <PortfolioList
                        portfolios={portfolios}
                        onSelectPortfolio={setSelectedPortfolio}
                        selectedPortfolioId={selectedPortfolio}
                    />
                    <div className="portfolios-section mt-4">
                        <div className="portfolio-content flex flex-col md:flex-row">
                            <div className="portfolio-chart-container flex-1 p-2">
                                {performanceData ? (
                                    <PortfolioChart chartData={performanceData} />
                                ) : (
                                    <p>Chargement des données...</p>
                                )}
                            </div>
                            <div className="portfolio-assets-container flex-1 p-2">
                                <div className="portfolio-assets-header flex justify-between items-center mb-2">
                                    <button onClick={handleAddAsset}
                                            className="text-xl font-bold px-2 py-1 bg-green-500 text-white rounded">
                                        +
                                    </button>
                                    <button onClick={handleSellAsset}
                                            className="text-xl font-bold px-2 py-1 bg-green-500 text-white rounded">

                                    </button>
                                </div>
                                {selectedPortfolio && (
                                    <PortfolioAssets portfolioId={selectedPortfolio} token={token}/>
                                )}
                            </div>
                        </div>
                        <div className="market-data-button-container mt-4">
                            <button
                                onClick={handleGoToMarketData}
                                className="market-data-button bg-blue-500 text-white py-2 px-4 rounded"
                            >
                                Voir les Données de Marché
                            </button>
                        </div>
                    </div>
                </div>
                {/* Zone C: Données de Marché */}
                <div style={{ gridArea: "c", background: "#c0c0c0", padding: "10px" }}>
                    <MarketDashboard marketData={performanceData} />
                </div>
                {/* Zone D : Actualités */}
                <div style={{ gridArea: "d", background: "#b0b0b0", padding: "10px" }}>
                    <TransactionHistorySection />
                </div>

            </div>
        </div>
    );
};

export default Dashboard;