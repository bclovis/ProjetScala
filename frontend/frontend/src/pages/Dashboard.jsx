import React, { useEffect, useState } from "react";
import { Box, useMediaQuery } from "@mui/material";
import { useNavigate } from "react-router-dom";
import Row1 from "@/components/Row1";
import Row2 from "@/components/Row2";
import Row3 from "@/components/Row3";

import "@/styles/MarketData.css"

const gridTemplateLargeScreens = `
  "c c c"
  "f l v"
  "t d e"
  "t d e"
`;

const gridTemplateSmallScreens = `
  "c"
  "f"
  "l"
  "x"
  "v"
  "d"
  "e"
  "t"
`;

const Dashboard = () => {
    const isAboveMediumScreens = useMediaQuery("(min-width: 1200px)");
    const navigate = useNavigate();
    const token = localStorage.getItem("token");

    // Vos états pour stocker les données
    const [portfolios, setPortfolios] = useState([]);
    const [performanceData, setPerformanceData] = useState(null);
    const [globalBalance, setGlobalBalance] = useState(0);
    const [walletBalance, setWalletBalance] = useState(0);
    const [notifications, setNotifications] = useState([
        "Nouveau listing: SOL",
        "Attention: Mise à jour de sécurité disponible",
    ]);
    const [selectedPortfolio, setSelectedPortfolio] = useState(null);

    // Conserver vos fonctions fetch
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
            .catch((err) => console.error(err));
    };

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
                // Assurez-vous que les données soient au bon format
                // Ici, nous attendons un objet { crypto, action, devise }
                // Vous pouvez ajuster selon votre API
            })
            .catch((err) => console.error("Erreur lors du chargement du résumé du compte :", err));
    };

    // useEffect pour charger les données au montage
    useEffect(() => {
        fetchPortfolios();
        fetchGlobalBalance();
        fetchWalletBalance();
        fetchAccountSummary();
    }, [navigate, token]);

    // useEffect pour charger les performances du portefeuille sélectionné
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

    const gridStyle = isAboveMediumScreens
        ? {
            gridTemplateColumns: "repeat(3, minmax(370px, 1fr))",
            gridTemplateRows: "repeat(4, auto)",
            gridTemplateAreas: gridTemplateLargeScreens,
            gap: "40px",
            padding: "40px",
        }
        : {
            gridAutoColumns: "1fr",
            gridAutoRows: "auto",
            gridTemplateAreas: gridTemplateSmallScreens,
            gap: "20px",
            padding: "20px",
        };

    return (
        <Box width="100%" height="100%" display="grid" sx={gridStyle}>
            <Row1 performanceData={performanceData} />
            <Row2
                walletBalance={walletBalance}
                globalBalance={globalBalance}
                notifications={notifications}
                portfolios={portfolios}
                onSelectPortfolio={setSelectedPortfolio}
                selectedPortfolio={selectedPortfolio}
            />
            <Row3
                selectedPortfolio={selectedPortfolio}
                token={token}
                performanceData={performanceData}
            />
        </Box>
    );
};

export default Dashboard;