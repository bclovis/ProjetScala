import React, { useState, useEffect } from 'react';
import { Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js';
import { useLocation } from 'react-router-dom';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

function MarketGraphPage() {
    const location = useLocation();
    const { assetData, time } = location.state || { assetData: null, time: [] };

    const generateChartData = (assetData, label) => ({
        labels: time.map((timestamp) => new Date(timestamp).toLocaleTimeString()), // Convertir les timestamps en heure lisible
        datasets: [{
            label: label,
            data: assetData.prices,
            borderColor: 'rgba(75,192,192,1)',
            fill: false,
        }],
    });

    if (!assetData) {
        return <div>Aucune donnée sélectionnée.</div>;
    }

    return (
        <div>
            <h1>Graphique pour {assetData.symbol}</h1>
            <Line data={generateChartData(assetData, `${assetData.symbol} (${assetData.assetType})`)} />
        </div>
    );
}

export default MarketGraphPage;
