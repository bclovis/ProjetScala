import React from "react";
import { Line } from "react-chartjs-2";
import { Box, Typography } from "@mui/material";
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from "chart.js";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

const CoinInfo = ({ coin }) => {
    if (!coin) return null;

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
        <Box sx={{ padding: 3, backgroundColor: "#121212", color: "white" }}>
            <Typography variant="h5" gutterBottom>{coin.longName} ({coin.symbol})</Typography>
            <Typography variant="h6">Price: ${coin.prices[coin.prices.length - 1]?.price.toFixed(2)}</Typography>
            <Typography>Market Cap Change: <span style={{ color: coin.change < 0 ? "red" : "lightgreen" }}>{coin.change.toFixed(2)}%</span></Typography>
            <Box sx={{ marginTop: 2 }}>
                <Line data={priceData} />
            </Box>
        </Box>
    );
};

export default CoinInfo;
