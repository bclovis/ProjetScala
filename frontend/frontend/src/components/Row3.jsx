import React, { useState } from "react";
import PropTypes from "prop-types";
import { useNavigate } from "react-router-dom";
import {
    Dialog,
    Box,
    Button,
    Slider,
    MenuItem,
    Select,
    Typography,
    Snackbar,
    Alert
} from "@mui/material";
import DashboardBox from "@/components/DashboardBox";
import AccountSummaryChart from "@/components/AccountSummaryChart";
import PortfolioChart from "@/components/portfolio/PortfolioChart.jsx";
import PortfolioAssets from "@/components/portfolio/PortfolioAssets.jsx";
import TransactionHistory from "@/components/actifs/TransactionHistory.jsx";
import AnimatedButton from "./AnimatedButton.jsx";

const Row3 = ({ performanceData, selectedPortfolio, token, accountSummary }) => {
    const navigate = useNavigate();

    // États pour la gestion de la modale et de l'alerte
    const [openSellModal, setOpenSellModal] = useState(false);
    const [assets, setAssets] = useState([]);
    const [selectedAsset, setSelectedAsset] = useState(null);
    const [sellQuantity, setSellQuantity] = useState(0);
    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState("");
    const [snackbarSeverity, setSnackbarSeverity] = useState("success");

    // Ouvrir la modale et charger les actifs disponibles
    const handleOpenSellModal = () => {
        fetch(`http://localhost:8080/api/portfolios/${selectedPortfolio}/assets`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            },
        })
            .then((res) => res.json())
            .then((data) => {
                setAssets(data);
                if (data.length > 0) setSelectedAsset(data[0]);
                setSellQuantity(0);
                setOpenSellModal(true);
            })
            .catch((err) => console.error("Erreur lors de la récupération des actifs :", err));
    };

    // Fermer la modale
    const handleCloseSellModal = () => {
        setOpenSellModal(false);
        setSelectedAsset(null);
    };

    // Vendre un actif
    const handleSell = () => {
        if (!selectedAsset || sellQuantity <= 0) return;

        const sellData = {
            asset_type: selectedAsset.assetType,
            symbol: selectedAsset.symbol,
            quantity: parseFloat(sellQuantity),
        };

        fetch(`http://localhost:8080/api/portfolios/${selectedPortfolio}/sell`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            },
            body: JSON.stringify(sellData),
        })
            .then((response) => {
                if (!response.ok) {
                    return response.text().then(text => { throw new Error(text); });
                }
                return response.json();
            })
            .then(() => {
                setSnackbarMessage("Vente réussie !");
                setSnackbarSeverity("success");
                setSnackbarOpen(true);

                setTimeout(() => {
                    window.location.reload();
                }, 1500);

                handleCloseSellModal();
            })
            .catch((err) => {
                console.error("Erreur :", err);
                setSnackbarMessage("Erreur lors de la vente.");
                setSnackbarSeverity("error");
                setSnackbarOpen(true);
            });
    };

    const handleAddAsset = () => {
        navigate("/marketpage", { state: { portfolioId: selectedPortfolio } });
    };

    return (
        <>
            {/* Modale de vente */}
            <Dialog open={openSellModal} onClose={handleCloseSellModal} maxWidth="sm" fullWidth>
                <Box sx={{ padding: 4, backgroundColor: "#1e1e1e", color: "white" }}>
                    <Typography variant="h6" gutterBottom>Vendre un actif</Typography>

                    {/* Sélection de l'actif à vendre */}
                    <Select
                        fullWidth
                        value={selectedAsset ? selectedAsset.symbol : ""}
                        onChange={(e) => {
                            const asset = assets.find((a) => a.symbol === e.target.value);
                            setSelectedAsset(asset);
                            setSellQuantity(0);
                        }}
                        sx={{
                            marginBottom: 2,
                            backgroundColor: "#1e1e1e",
                            color: "white",
                            borderRadius: "6px",
                            "& .MuiSelect-select": { color: "white" },
                        }}
                        MenuProps={{
                            PaperProps: {
                                sx: {
                                    backgroundColor: "#1e1e1e",
                                    boxShadow: "none",
                                    border: "none",
                                },
                            },
                        }}
                    >
                        {assets.map((asset) => (
                            <MenuItem
                                key={asset.symbol}
                                value={asset.symbol}
                                sx={{
                                    backgroundColor: "#1e1e1e",
                                    color: "white",
                                    ":hover": { backgroundColor: "#333", color: "white" },
                                    "&.Mui-selected": { backgroundColor: "#34d399", color: "black" },
                                    "&.Mui-selected:hover": { backgroundColor: "#2aa378", color: "black" },
                                }}
                            >
                                {asset.symbol} ({asset.assetType}) - {asset.quantity} dispo
                            </MenuItem>
                        ))}
                    </Select>


                    {/* Slider pour sélectionner la quantité à vendre */}
                    {selectedAsset && (
                        <>
                            <Typography>Quantité: {sellQuantity.toFixed(4)}</Typography>
                            <Slider
                                value={sellQuantity}
                                onChange={(e, val) => setSellQuantity(val)}
                                min={0}
                                max={selectedAsset.quantity}
                                step={0.0001}
                                valueLabelDisplay="auto"
                            />
                            <Typography variant="body2">
                                Montant total: {(sellQuantity * selectedAsset.avgBuyPrice).toFixed(2)} €
                            </Typography>

                            <Button
                                fullWidth
                                variant="contained"
                                sx={{ marginTop: 2, backgroundColor: "red", color: "white" }}
                                onClick={handleSell}
                                disabled={sellQuantity <= 0}
                            >
                                Confirmer la vente
                            </Button>
                        </>
                    )}
                </Box>
            </Dialog>

            {/* Snackbar pour afficher les messages */}
            <Snackbar open={snackbarOpen} autoHideDuration={2000} onClose={() => setSnackbarOpen(false)}>
                <Alert onClose={() => setSnackbarOpen(false)} severity={snackbarSeverity} sx={{ width: "100%" }}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>

            {/* DashboardBox contenant les différentes sections */}
            <DashboardBox gridArea="v">
                <AccountSummaryChart summaryData={accountSummary} />
            </DashboardBox>
            <DashboardBox gridArea="d">
                {performanceData ? (
                    <PortfolioChart chartData={performanceData} />
                ) : (
                    <p>Chargement des données...</p>
                )}
            </DashboardBox>
            <DashboardBox gridArea="e">
                {selectedPortfolio && <PortfolioAssets portfolioId={selectedPortfolio} token={token} />}
                
                {/* Bouton Buy, redirige vers info marché */}
                <AnimatedButton onClick={handleAddAsset}>Buy</AnimatedButton>

                {/* Bouton Sell pour ouvrir la modale */}
                <AnimatedButton onClick={handleOpenSellModal}>Sell</AnimatedButton>
            </DashboardBox>
            <DashboardBox gridArea="t">
                <TransactionHistory portfolioId={selectedPortfolio} token={token} />
            </DashboardBox>
        </>
    );
};

// Définition des types des props
Row3.propTypes = {
    performanceData: PropTypes.object,
    selectedPortfolio: PropTypes.any,
    token: PropTypes.string,
    accountSummary: PropTypes.shape({
        crypto: PropTypes.number.isRequired,
        action: PropTypes.number.isRequired,
        devise: PropTypes.number.isRequired,
    }).isRequired,
};

export default Row3;
