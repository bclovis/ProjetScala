import React, { useEffect, useState } from "react";
import CoinsTable from "../components/CoinsTable";
import { useNavigate } from "react-router-dom";

const MarketPage = () => {
  const [selectedPortfolio, setSelectedPortfolio] = useState(null);
  const [walletBalance, setWalletBalance] = useState(0);
  const [token, setToken] = useState(localStorage.getItem("token"));

  const fetchWalletBalance = () => {
    fetch("http://localhost:8080/api/wallet-balance", {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      },
    })
    .then(res => res.json())
    .then(data => setWalletBalance(parseFloat(data.walletBalance)))
    .catch(err => console.error(err));
  };

  useEffect(() => {
    fetchWalletBalance();
    
    // Charger selectedPortfolio depuis localStorage au lieu de l'écraser
    const savedPortfolio = localStorage.getItem("selectedPortfolio");
    if (savedPortfolio) {
        setSelectedPortfolio(parseInt(savedPortfolio));
    } else {
        fetchPortfolios();  // Charger les portefeuilles si rien n'est sauvegardé
    }
}, [token]);

const fetchPortfolios = () => {
    fetch("http://localhost:8080/api/portfolios", {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`,
        },
    })
    .then(res => res.json())
    .then(data => {
        if (data.length > 0 && !selectedPortfolio) {
            setSelectedPortfolio(data[0].id);
            localStorage.setItem("selectedPortfolio", data[0].id);  // Sauvegarder la sélection
        }
    })
    .catch(err => console.error(err));
};


  return (
    <div className="market-page">
      {selectedPortfolio && token && (
        <CoinsTable
          portfolioId={selectedPortfolio}
          token={token}
          availableBalance={walletBalance}
          walletBalance={walletBalance}
        />
      )}
    </div>
  );
};

export default MarketPage;
