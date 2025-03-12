import React, { useState, useEffect } from 'react';
import { Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js';
import '../styles/MarketData.css';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

function MarketDashboard() {
    const [marketData, setMarketData] = useState({ stocks: [], crypto: {}, forex: {} });
    const [selectedAsset, setSelectedAsset] = useState(null);
    const [selectedCategory, setSelectedCategory] = useState('crypto'); // Catégorie par défaut (crypto)
    const [isGraphVisible, setIsGraphVisible] = useState(false); // Nouveau state pour basculer entre la carte et le graphique

    useEffect(() => {
        const socket = new WebSocket('ws://localhost:8080/market-data');

        socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log('Données reçues :', data);

            setMarketData(data);
        };

        //return () => socket.close();
        return;
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

    // Options pour masquer la légende et les valeurs des abscisses
    const chartOptions = {
        responsive: true,
        plugins: {
            legend: {
                display: false,  // Masquer la légende
            },
        },
        scales: {
            x: {
                display: false,  // Masquer les valeurs sur l'axe des abscisses
            },
        },
    };

    const handleAssetClick = (asset) => {
        if (selectedAsset === asset) {
            // Si l'actif est déjà sélectionné, alterne l'affichage entre la carte et le graphique
            setIsGraphVisible(!isGraphVisible);
        } else {
            // Sinon, sélectionne un nouvel actif et affiche son graphique
            setSelectedAsset(asset);
            setIsGraphVisible(true);
        }
    };

    const handleCategoryChange = (category) => {
        setSelectedCategory(category);
        setSelectedAsset(null); // Réinitialiser la sélection d'actif lors du changement de catégorie
        setIsGraphVisible(false); // Masquer le graphique si on change de catégorie
    };

    // Fonction pour obtenir les données de la catégorie sélectionnée
    const getCategoryData = () => {
        if (selectedCategory === 'crypto') {
            return marketData.crypto;
        } else if (selectedCategory === 'forex') {
            return marketData.forex;
        } else {
            return marketData.stocks;
        }
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

            {/* Conteneur des cartes */}
            <div className="cards-container">
                <div className="scroll-container">
                    {Object.entries(getCategoryData()).map(([symbol, data]) => (
                        <div className="asset-card" key={symbol} onClick={() => handleAssetClick(data)}>
                            {isGraphVisible && selectedAsset === data ? (
                                <div className="chart-container" style={{ width: '200px', height: '150px' }}>
                                    <Line
                                        data={generateChartData(data, `${data.symbol} (${data.assetType})`)}
                                        options={chartOptions} // Appliquer les options pour masquer la légende et les valeurs d'abscisse
                                        width={200}  // Largeur du graphique
                                        height={150} // Hauteur du graphique
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
