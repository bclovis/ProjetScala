import React, { useState, useEffect } from 'react';
import { Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js';
import './MarketData.css';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

function MarketDashboard() {
    const [marketData, setMarketData] = useState({ stocks: [], crypto: {}, forex: {} });
    const [time, setTime] = useState([]);
    const [selectedAsset, setSelectedAsset] = useState(null);

    useEffect(() => {
        const socket = new WebSocket('ws://localhost:8080/market-data');

        socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log('Données reçues :', data);

            setMarketData(data);
            setTime(data.crypto?.[Object.keys(data.crypto)[0]]?.prices.map(point => point.timestamp) || []);
        };

        return () => socket.close();
    }, []);

    const generateChartData = (assetData, label) => ({
        labels: assetData.prices.map(point => new Date(point.timestamp - 3600000).toLocaleTimeString()),
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

            <h2>Devises</h2>
            <table>
                <thead>
                <tr>
                    <th>Actif</th>
                    <th>Change (en euro)</th>
                    <th>Variation (%)</th>
                </tr>
                </thead>
                <tbody>
                {Object.entries(marketData.forex).map(([symbol, data]) => (
                    <tr key={symbol} onClick={() => handleAssetClick(data)}>
                        <td>{symbol}</td>
                        <td>{data.prices[data.prices.length - 1]?.price.toFixed(2)}</td>
                        <td style={{ color: data.change > 0 ? 'green' : 'red' }}>
                            {data.change ? data.change.toFixed(2) : "N/A"}%
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            <h2>Cryptomonnaies</h2>
            <table>
                <thead>
                <tr>
                    <th>Actif</th>
                    <th>Prix (en euro)</th>
                    <th>Variation (%)</th>
                </tr>
                </thead>
                <tbody>
                {Object.entries(marketData.crypto).map(([symbol, data]) => (
                    <tr key={symbol} onClick={() => handleAssetClick(data)}>
                        <td>{symbol}</td>
                        <td>{data.prices[data.prices.length - 1]?.price.toFixed(2)}</td>
                        <td style={{ color: data.change > 0 ? 'green' : 'red' }}>
                            {data.change ? data.change.toFixed(2) : "N/A"}%
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            {selectedAsset && (
                <div>
                    <h2>Graphique pour {selectedAsset.symbol} (UTC)</h2>
                    <Line data={generateChartData(selectedAsset, `${selectedAsset.symbol} (${selectedAsset.assetType})`)} />
                </div>
            )}
        </div>
    );
}

export default MarketDashboard;
