import React, { useEffect, useState } from "react";
import { Box, useMediaQuery } from "@mui/material";
import { useNavigate } from "react-router-dom";
import Row1 from "@/components/Row1";
import Row2 from "@/components/Row2";
import Row3 from "@/components/Row3";
import "@/styles/MarketData.css";

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

    // États pour stocker les données
    const [portfolios, setPortfolios] = useState([]);
    const [performanceData, setPerformanceData] = useState(null);
    const [globalBalance, setGlobalBalance] = useState(0);
    const [walletBalance, setWalletBalance] = useState(0);
    const [selectedPortfolio, setSelectedPortfolio] = useState(
        localStorage.getItem("selectedPortfolio")
            ? parseInt(localStorage.getItem("selectedPortfolio"))
            : null
    );
    const [accountSummary, setAccountSummary] = useState({
        crypto: 0,
        action: 0,
        devise: 0,
    });

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
                console.log("API portfolios response:", data); // 🔍 Vérifier ce que l'API retourne

                if (data && data.length > 0) {
                    setPortfolios(data);

                    // Si selectedPortfolio est null ou n'existe plus, on prend le premier dispo
                    if (!selectedPortfolio || !data.some(p => p.id === selectedPortfolio)) {
                        setSelectedPortfolio(data[0].id);
                        localStorage.setItem("selectedPortfolio", data[0].id);
                    }
                } else {
                    console.warn("⚠️ L'API a retourné un tableau vide pour portfolios !");
                    setPortfolios([]); // Empêche les erreurs de rendu
                }
            })
            .catch((err) => {
                console.error("Erreur lors de la récupération des portfolios:", err);
            });
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
            .then((data) => setGlobalBalance(parseFloat(data.globalBalance)))
            .catch((err) => console.error("Erreur solde global:", err));
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
            .then((data) => setWalletBalance(parseFloat(data.walletBalance)))
            .catch((err) => console.error("Erreur solde wallet:", err));
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
            .then((data) => setAccountSummary(data))
            .catch((err) => console.error("Erreur résumé du compte:", err));
    };

    // Chargement des données au montage
    useEffect(() => {
        fetchPortfolios();
        fetchGlobalBalance();
        fetchWalletBalance();
        fetchAccountSummary();

        // Mise à jour des portfolios toutes les 30 secondes
        const interval = setInterval(fetchPortfolios, 30000);
        return () => clearInterval(interval); // Nettoyer l'intervalle en cas de démontage
    }, [navigate, token]);

    // Mettre à jour le localStorage lorsque selectedPortfolio change
    useEffect(() => {
        if (selectedPortfolio !== null) {
            localStorage.setItem("selectedPortfolio", selectedPortfolio);
        }
    }, [selectedPortfolio]);

    // Chargement des performances du portefeuille sélectionné
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
                .then((data) => setPerformanceData(data))
                .catch((err) => console.error(err));
        }
    }, [selectedPortfolio, token]);

    console.log("🟢 selectedPortfolio:", selectedPortfolio);
    console.log("🟢 portfolios:", portfolios);

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
                notifications={["Nouveau listing: SOL", "Attention: Mise à jour de sécurité disponible"]}
                portfolios={portfolios}
                onSelectPortfolio={(id) => {
                    setSelectedPortfolio(id);
                    localStorage.setItem("selectedPortfolio", id);
                }}
                selectedPortfolio={selectedPortfolio}
            />
            <Row3
                selectedPortfolio={selectedPortfolio}
                token={token}
                performanceData={performanceData}
                accountSummary={accountSummary}
            />
        </Box>
    );
};

export default Dashboard;
