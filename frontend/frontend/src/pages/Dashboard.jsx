import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../components/Header";
import Overview from "../components/Overview";
import PortfolioChart from "../components/PortfolioChart";
import PortfolioAssets from "../components/PortfolioAssets";
import CreatePortfolio from "../components/CreatePortfolio";
import PortfolioList from "../components/PortfolioList";
import AddAsset from "../components/AddAsset";
import "../styles/Dashboard.css";

const Dashboard = () => {
    const [portfolios, setPortfolios] = useState([]);
    const [performanceData, setPerformanceData] = useState(null);
    const [globalBalance, setGlobalBalance] = useState(0);
    const [accountSummary, setAccountSummary] = useState("Répartition: 60% Crypto, 30% Actions, 10% Devises");
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

    // Nouvelle fonction pour récupérer le solde global réel depuis le backend
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
                // On suppose que la réponse est de la forme { "globalBalance": "valeur" }
                setGlobalBalance(parseFloat(data.globalBalance));
            })
            .catch((err) => console.error("Erreur lors du chargement du solde global :", err));
    };

    useEffect(() => {
        fetchPortfolios();
        // Remplacer l'appel statique par l'appel à l'endpoint pour le solde global
        fetchGlobalBalance();
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

    return (
        <div className="dashboard-page">
            <Header />
            <div className="dashboard-wrapper container mx-auto p-4">
                <Overview
                    globalBalance={globalBalance}
                    accountSummary={accountSummary}
                    notifications={notifications}
                />
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
                            {selectedPortfolio && (
                                <PortfolioAssets portfolioId={selectedPortfolio} token={token} />
                            )}
                        </div>
                    </div>
                </div>
                <div className="create-portfolio-container mt-4">
                    <CreatePortfolio onPortfolioCreated={fetchPortfolios} />
                </div>
                {/* Intégration du composant pour ajouter un actif */}
                <div className="add-asset-container mt-4">
                    {selectedPortfolio && (
                        <AddAsset
                            portfolioId={selectedPortfolio}
                            token={token}
                            onAssetAdded={() => {
                                console.log("Actif ajouté !");
                                // Optionnel : rafraîchir la liste des actifs si besoin
                            }}
                        />
                    )}
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
    );
};

export default Dashboard;