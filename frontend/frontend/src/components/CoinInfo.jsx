import React, { useState, useEffect } from "react";
import { Line } from "react-chartjs-2";
import { Box, Typography, IconButton, Dialog, Slider, Button, Snackbar, Alert } from "@mui/material";
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from "chart.js";
import AddIcon from '@mui/icons-material/Add';
import { useNavigate } from "react-router-dom";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

const CoinInfo = ({ coin, portfolioId, token, availableBalance, walletBalance, onAssetAdded }) => {
    const [open, setOpen] = useState(false);
    const [quantity, setQuantity] = useState(0);
    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState("");
    const [snackbarSeverity, setSnackbarSeverity] = useState("success");
    const navigate = useNavigate();

    const currentPrice = coin?.prices[coin.prices.length - 1]?.price || 0;
    const maxAmount = availableBalance && currentPrice ? availableBalance / currentPrice : 0;

    const priceData = {
        labels: coin.prices.map(point => {
            const date = new Date(point.timestamp);
            return date.toLocaleString("fr-FR", { 
                day: "2-digit", month: "2-digit", year: "2-digit",
                hour: "2-digit", minute: "2-digit"
            });
        }),
        datasets: [
            {
                label: "Price",
                data: coin.prices.map(point => point.price),
                borderColor: "#00ff99",
                backgroundColor: "rgba(0,255,153,0.2)",
                fill: true,
                tension: 0.4, // Courbe douce
                pointRadius: 0, // Supprime les points
                pointHoverRadius: 0, // Supprime les points au survol
                borderWidth: 2, // Épaisseur de la ligne
            },
        ],
    };
        
    const handleSubmit = () => {
        if (!portfolioId || !token || currentPrice <= 0 || quantity <= 0 || quantity > maxAmount) {
            setSnackbarMessage("Erreur : Paramètres invalides.");
            setSnackbarSeverity("error");
            setSnackbarOpen(true);
            return;
        }

        const assetData = {
            asset_type: coin.assetType.toLowerCase(),
            symbol: coin.symbol,
            quantity: parseFloat(quantity),
            avg_buy_price: currentPrice
        };

        fetch(`http://localhost:8080/api/portfolios/${portfolioId}/assets`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify(assetData)
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => { throw new Error(text); });
                }
                return response.json();
            })
            .then(data => {
                console.log("Actif ajouté avec succès:", data);
                setSnackbarMessage("Actif ajouté avec succès !");
                setSnackbarSeverity("success");
                setSnackbarOpen(true);

                if (typeof onAssetAdded === 'function') {
                    onAssetAdded();
                }

                setTimeout(() => {
                    navigate("/dashboard"); // Redirige après succès
                }, 1500);

                setOpen(false);
                setQuantity(0);
            })
            .catch(err => {
                console.error("Erreur lors de l'ajout de l'actif:", err);
                setSnackbarMessage("Erreur lors de l'ajout de l'actif.");
                setSnackbarSeverity("error");
                setSnackbarOpen(true);
            });
    };

    const handleOpenModal = () => {
        setQuantity(0);
        setOpen(true);
    };

    return (
        <Box sx={{ padding: 3, backgroundColor: "#121212", color: "white", position: 'relative' }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                    <Typography variant="h5">{coin.longName} ({coin.symbol})</Typography>
                    <Typography variant="h6">Price: ${currentPrice.toFixed(2)}</Typography>
                    <Typography>
                        Market Cap Change: <span style={{ color: coin.change < 0 ? "red" : "lightgreen" }}>{coin.change.toFixed(2)}%</span>
                    </Typography>
                </div>
                <IconButton color="primary" onClick={handleOpenModal}>
                    <AddIcon />
                </IconButton>
            </div>

            <Box sx={{ marginTop: "20px" }}>
                <Line data={priceData} />
            </Box>

            <Dialog open={open} onClose={() => setOpen(false)}>
                <Box sx={{ padding: 4, width: 400, backgroundColor: "#1e1e1e", color: "white" }}>
                    <Typography variant="h6" gutterBottom>Ajouter {coin.symbol}</Typography>
                    <Typography variant="body2" gutterBottom>
                        Solde disponible: {availableBalance ? parseFloat(availableBalance).toFixed(2) : "0.00"} €
                    </Typography>
                    <Typography gutterBottom>Quantité: {quantity.toFixed(4)}</Typography>
                    <Slider
                        value={quantity}
                        onChange={(e, val) => setQuantity(val)}
                        aria-labelledby="quantity-slider"
                        valueLabelDisplay="auto"
                        min={0}
                        max={maxAmount}
                        step={0.0001}
                    />
                    <Typography variant="body2">
                        Montant total: {(quantity * currentPrice).toFixed(2)} €
                    </Typography>
                    <Button
                        variant="contained"
                        fullWidth
                        sx={{ marginTop: 2, backgroundColor: "#00ff99", color: "black" }}
                        onClick={handleSubmit}
                        disabled={quantity <= 0 || quantity >= maxAmount}
                    >
                        Ajouter au portefeuille
                    </Button>
                </Box>
            </Dialog>

            {/* Snackbar pour afficher les messages */}
            <Snackbar open={snackbarOpen} autoHideDuration={2000} onClose={() => setSnackbarOpen(false)}>
                <Alert onClose={() => setSnackbarOpen(false)} severity={snackbarSeverity} sx={{ width: "100%" }}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default CoinInfo;