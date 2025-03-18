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
        labels: coin.prices.map(point => new Date(point.timestamp).toLocaleDateString()),
        datasets: [
            {
                label: "Price",
                data: coin.prices.map(point => point.price),
                borderColor: "#00ff99",
                backgroundColor: "rgba(0,255,153,0.2)",
                fill: true,
            },
        ],
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
        </Box>
    );
};

export default CoinInfo;
