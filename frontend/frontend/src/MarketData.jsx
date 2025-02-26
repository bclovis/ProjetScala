import React, { useState, useEffect } from 'react';
import { Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js';
import './MarketData.css';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

function MarketDashboard() {
    const [marketData, setMarketData] = useState({ stocks: [], crypto: {}, forex: {} });
    const [time, setTime] = useState([]);
    const [selectedAsset, setSelectedAsset] = useState(null); // Nouvel état pour l'élément sélectionné

    useEffect(() => {
        const socket = new WebSocket('ws://localhost:8080/market-data');

        socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log('Données reçues :', data);

            setMarketData((prevData) => ({
                stocks: data.stocks.map((stock) => ({
                    ...stock,
                    prices: [...(prevData.stocks.find((s) => s.symbol === stock.symbol)?.prices || []), stock.price],
                })),
                crypto: Object.entries(data.crypto).reduce((acc, [symbol, cryptoData]) => {
                    acc[symbol] = {
                        ...cryptoData,
                        prices: [...(prevData.crypto[symbol]?.prices || []), cryptoData.price],
                    };
                    return acc;
                }, {}),
                forex: Object.entries(data.forex).reduce((acc, [pair, forexData]) => {
                    acc[pair] = {
                        ...forexData,
                        prices: [...(prevData.forex[pair]?.prices || []), forexData.price],
                    };
                    return acc;
                }, {}),
            }));
        };

        // Interval pour ajouter un point toutes les 10 secondes
        const intervalId = setInterval(() => {
            const newTime = Date.now();

            setTime((prevTime) => {
                const updatedTime = [...prevTime, newTime];
                return updatedTime;
            });

            setMarketData((prevData) => ({
                ...prevData,
                crypto: Object.entries(prevData.crypto).reduce((acc, [symbol, cryptoData]) => {
                    acc[symbol] = {
                        ...cryptoData,
                        prices: [...cryptoData.prices, cryptoData.price], // Fixer le dernier prix même sans nouvelle donnée
                    };
                    return acc;
                }, {}),
                forex: Object.entries(prevData.forex).reduce((acc, [pair, forexData]) => {
                    acc[pair] = {
                        ...forexData,
                        prices: [...forexData.prices, forexData.price],
                    };
                    return acc;
                }, {}),
            }));
        }, 10000); // Met à jour toutes les 10 secondes

        return () => {
            socket.close();
            clearInterval(intervalId); // Nettoyer l'intervalle
        };
    }, []);


    // Fonction pour générer les données du graphique
    const generateChartData = (assetData, label) => ({
        labels: time.map((timestamp) => new Date(timestamp).toLocaleTimeString()), // Convertir les timestamps en heure lisible
        datasets: [{
            label: label,
            data: assetData.prices,
            borderColor: 'rgba(75,192,192,1)',
            fill: false,
        }],
    });

    // Sélectionner l'actif sur lequel on clique
    const handleAssetClick = (asset) => {
        setSelectedAsset(asset);
    };

    return (
        <div>
            <h1>Performance des Portefeuilles en Temps Réel</h1>

            <h2>Cryptomonnaies</h2>
            <table>
                <thead>
                <tr>
                    <th>Actif</th>
                    <th>Prix (en dollar)</th>
                    <th>Variation (%)</th>
                </tr>
                </thead>
                <tbody>
                {Object.entries(marketData.crypto).map(([symbol, data]) => (
                    <tr key={symbol} onClick={() => handleAssetClick(data)}>
                        <td>{symbol}</td>
                        <td>{data.price.toFixed(2)}</td>
                        <td style={{ color: data.change > 0 ? 'green' : 'red' }}>
                            {data.change.toFixed(2)}%
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            <h2>Devises</h2>
            <table>
                <thead>
                <tr>
                    <th>Devises</th>
                    <th>Taux de Change (en dollar)</th>
                    <th>Variation (%)</th>
                </tr>
                </thead>
                <tbody>
                {Object.entries(marketData.forex).map(([pair, data]) => (
                    <tr key={pair} onClick={() => handleAssetClick(data)}>
                        <td>{pair}</td>
                        <td>{data.price.toFixed(4)}</td>
                        <td style={{ color: data.change > 0 ? 'green' : 'red' }}>
                            {data.change.toFixed(2)}%
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            {selectedAsset && (
                <div>
                    <h2>Graphique pour {selectedAsset.symbol}</h2>
                    <Line data={generateChartData(selectedAsset, `${selectedAsset.symbol} (${selectedAsset.assetType})`)} />
                </div>
            )}
        </div>
    );
}

export default MarketDashboard;
