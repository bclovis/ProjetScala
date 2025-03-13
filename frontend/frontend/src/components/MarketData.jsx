//frontend/src/components/MarketData.jsx
// frontend/src/components/MarketData.jsx
import React, { useState, useEffect, useRef } from 'react';
import { Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

function MarketDashboard() {
    const [marketData, setMarketData] = useState({ stocks: [], crypto: {}, forex: {} });
    const [selectedAsset, setSelectedAsset] = useState(null);
    const [selectedCategory, setSelectedCategory] = useState('crypto'); // Catégorie par défaut
    const [isGraphVisible, setIsGraphVisible] = useState(false);
    const socketRef = useRef(null);

    const connectWebSocket = () => {
        const socket = new WebSocket('ws://localhost:8080/market-data');
        socket.onopen = () => {
            console.log('WebSocket connecté');
        };
        socket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                console.log('Données reçues :', data);
                setMarketData(data);
            } catch (error) {
                console.error("Erreur de parsing JSON :", error);
            }
        };
        socket.onerror = (err) => {
            console.error("Erreur WebSocket :", err);
            socket.close();
        };
        socket.onclose = (e) => {
            console.log('WebSocket fermé. Nouvelle tentative dans 3 secondes.', e.reason);
            setTimeout(connectWebSocket, 3000);
        };
        socketRef.current = socket;
    };

    useEffect(() => {
        connectWebSocket();
        return () => {
            if (socketRef.current) {
                socketRef.current.close();
            }
        };
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

    const chartOptions = {
        responsive: true,
        plugins: {
            legend: { display: false },
        },
        scales: {
            x: { display: false },
        },
    };

    const handleAssetClick = (asset) => {
        if (selectedAsset === asset) {
            setIsGraphVisible(!isGraphVisible);
        } else {
            setSelectedAsset(asset);
            setIsGraphVisible(true);
        }
    };

    const handleCategoryChange = (category) => {
        setSelectedCategory(category);
        setSelectedAsset(null);
        setIsGraphVisible(false);
    };

    const getCategoryData = () => {
        if (selectedCategory === 'crypto') return marketData.crypto;
        if (selectedCategory === 'forex') return marketData.forex;
        return marketData.stocks;
    };

    return (
        <div className="market-dashboard">
            <div className="category-selector-container">
                <div className="category-selector">
                    <button
                        className={selectedCategory === 'stocks' ? 'selected' : ''}
                        onClick={() => handleCategoryChange('stocks')}
                    >
                        Actions
                    </button>
                    <button
                        className={selectedCategory === 'crypto' ? 'selected' : ''}
                        onClick={() => handleCategoryChange('crypto')}
                    >
                        Cryptomonnaies
                    </button>
                    <button
                        className={selectedCategory === 'forex' ? 'selected' : ''}
                        onClick={() => handleCategoryChange('forex')}
                    >
                        Devises
                    </button>
                </div>
            </div>

            <div className="cards-container">
                <div className="scroll-container">
                    {Object.entries(getCategoryData()).map(([symbol, data]) => (
                        <div className="asset-card" key={symbol} onClick={() => handleAssetClick(data)}>
                            {isGraphVisible && selectedAsset === data ? (
                                <div className="chart-container" style={{ width: '200px', height: '150px' }}>
                                    <Line
                                        data={generateChartData(data, `${data.symbol} (${data.assetType})`)}
                                        options={chartOptions}
                                        width={200}
                                        height={150}
                                    />
                                </div>
                            ) : (
                                <>
                                    <h3>{data.longName || symbol}</h3>
                                    <p>Prix (en Dollars): {data.prices[data.prices.length - 1]?.price.toFixed(2)}</p>
                                    <p className={`variation ${data.change > 0 ? 'positive' : 'negative'}`}>
                                        {data.change ? data.change.toFixed(2) : "N/A"}%
                                    </p>
                                </>
                            )}
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}

export default MarketDashboard;