import React from "react";
import CoinsTable from "../components/CoinsTable";
import MarketDashboard from "../components/MarketData";
import Header from "../components/Header";

const MarketPage = () => {
  return (
    <div className="market-page">
            
      {/* Affichage du tableau des cryptos */}
      <CoinsTable />
      
    </div>
  );
};

export default MarketPage;
