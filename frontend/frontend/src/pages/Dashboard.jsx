// frontend/src/pages/Dashboard.jsx
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../components/Header";
import Overview from "../components/Overview";
import PortfolioChart from "../components/PortfolioChart";
import PortfolioAssets from "../components/PortfolioAssets";
import PortfolioList from "../components/PortfolioList";
import AddAsset from "../components/AddAsset";
import AccountSummaryChart from "../components/AccountSummaryChart";
import "../styles/Dashboard.css";

const Dashboard = () => {
    const [portfolios, setPortfolios] = useState([]);
    const [performanceData, setPerformanceData] = useState(null);
    const [globalBalance, setGlobalBalance] = useState(0);
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
                "Authorization": `Bearer ${token}`
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

    // Récupérer le solde global
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

    // Récupérer le résumé dynamique du compte
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
                    devise: parseFloat(data.devise)
                });
            })
            .catch((err) => console.error("Erreur lors du chargement du résumé du compte :", err));
    };

    useEffect(() => {
        fetchPortfolios();
        fetchGlobalBalance();
        fetchAccountSummary();
    }, [navigate, token]);

    useEffect(() => {
        if (selectedPortfolio) {
            fetch(`http://localhost:8080/api/portfolios/${selectedPortfolio}/performance`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
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
        navigate("/add-asset");
    };

    // Définition du grid template areas pour les grands écrans
    const gridTemplateLargeScreens = `
  "a b"
  "d b"
`;
    const gridStyle = {
        display: "grid",
        gridTemplateAreas: gridTemplateLargeScreens,
        gridTemplateColumns: "1fr 2fr",
        gap: "10px",
        padding: "10px"
    };

    // Inversion : Zone D affiche Actualités, Zone E affiche Forex Populaires
    const PopularNews = () => (
        <div style={{ padding: '10px' }}>
            <h3>Actualités</h3>
            <p>Infos du marché...</p>
        </div>
    );

    return (
        <div className="dashboard-page">
            <Header />
            <div style={gridStyle}>
                {/* Zone A : Overview & Account Summary Chart */}
                <div style={{ gridArea: "a", background: "#e0e0e0", padding: "10px" }}>
                    <Overview
                        globalBalance={globalBalance}
                        accountSummary={""}
                        notifications={notifications}
                    />
                    <AccountSummaryChart summaryData={accountSummaryData} />
                </div>
                {/* Zone B : Portfolio List & Performance Details */}
                <div style={{ gridArea: "b", background: "#d0d0d0", padding: "10px" }}>
                    {/* En-tête "Mes portefeuilles" avec bouton "+" */}
                    <div className="portfolio-header flex justify-between items-center mb-4">
                        <h2 className="text-xl font-bold">Mes portefeuilles</h2>
                        <button onClick={handleCreatePortfolio} className="text-xl font-bold px-2 py-1 bg-green-500 text-white rounded">+</button>
                    </div>
                    <PortfolioList
                        portfolios={portfolios}
                        onSelectPortfolio={setSelectedPortfolio}
                        selectedPortfolioId={selectedPortfolio}
                    />
                    <div className="portfolios-section mt-4">
                        <h2 className="text-xl font-bold mb-2">Détail du Portefeuille</h2>
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
                                    <h3 className="text-lg font-bold">Actifs du portefeuille</h3>
                                    <button onClick={handleAddAsset} className="text-xl font-bold px-2 py-1 bg-green-500 text-white rounded">+</button>
                                </div>
                                {selectedPortfolio && (
                                    <PortfolioAssets portfolioId={selectedPortfolio} token={token} />
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
                {/* Zone D : Actualités */}
                <div style={{ gridArea: "d", background: "#b0b0b0", padding: "10px" }}>
                    <PopularNews />
                </div>
            </div>
        </div>
    );
};

export default Dashboard;