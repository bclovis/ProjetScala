import React, { useState, useEffect } from 'react';
import { Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js';
import '../styles/MarketData.css';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

function MarketDashboard() {
    const [marketData, setMarketData] = useState({ stocks: [], crypto: {}, forex: {} });
    const [selectedAsset, setSelectedAsset] = useState(null);

    useEffect(() => {
        const socket = new WebSocket('ws://localhost:8080/market-data');

        socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log('Données reçues :', data);

            setMarketData(data);
        };

        return () => socket.close();
    }, []);

    const generateChartData = (assetData, label) => ({
        labels: assetData.prices.map(point => new Date(point.timestamp).toLocaleTimeString()),
        datasets: [{
            label: label,
            data: assetData.prices.map(point => point.price),
            borderColor: 'rgba(75,192,192,1)',
            fill: false,
            pointRadius: 0,
            pointHoverRadius: 0,
        }],
    });

    const handleAssetClick = (asset) => {
        setSelectedAsset(asset);
    };

    return (
        <div>
            <h1>Performance des Portefeuilles en Temps Réel</h1>

            <div className="asset-category">
                <h2>Devises</h2>
                <div className="asset-grid">
                    {Object.entries(marketData.forex).map(([symbol, data]) => (
                        <div className="asset-card" key={symbol} onClick={() => handleAssetClick(data)}>
                            <h3>{data.longName}</h3> {/* Affichage du longName */}
                            <p>Change (EUR): {data.prices[data.prices.length - 1]?.price.toFixed(2)}</p>
                            <p className={`variation ${data.change > 0 ? 'positive' : 'negative'}`}>
                                {data.change ? data.change.toFixed(2) : "N/A"}%
                            </p>
                        </div>
                    ))}
                </div>
            </div>

            <div className="asset-category">
                <h2>Cryptomonnaies</h2>
                <div className="asset-grid">
                    {Object.entries(marketData.crypto).map(([symbol, data]) => (
                        <div className="asset-card" key={symbol} onClick={() => handleAssetClick(data)}>
                            <h3>{data.longName}</h3> {/* Affichage du longName */}
                            <p>Prix (EUR): {data.prices[data.prices.length - 1]?.price.toFixed(2)}</p>
                            <p className={`variation ${data.change > 0 ? 'positive' : 'negative'}`}>
                                {data.change ? data.change.toFixed(2) : "N/A"}%
                            </p>
                        </div>
                    ))}
                </div>
            </div>

            {selectedAsset && (
                <div className="chart-container">
                    <h2>Graphique pour {selectedAsset.longName}</h2> {/* Affichage du longName dans le titre du graphique */}
                    <Line data={generateChartData(selectedAsset, `${selectedAsset.symbol} (${selectedAsset.assetType})`)} />
                </div>
            )}
        </div>
    );
}

export default MarketDashboard;
