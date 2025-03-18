// frontend/src/components/Row3.jsx
import React from "react";
import PropTypes from "prop-types";
import { useNavigate } from "react-router-dom";
import DashboardBox from "@/components/DashboardBox";
import AccountSummaryChart from "@/components/AccountSummaryChart";
import PortfolioChart from "@/components/portfolio/PortfolioChart.jsx";
import PortfolioAssets from "@/components/portfolio/PortfolioAssets.jsx";
import TransactionHistory from "@/components/actifs/TransactionHistory.jsx";
import AnimatedButton from "./AnimatedButton.jsx";

const Row3 = ({ performanceData, selectedPortfolio, token, accountSummary }) => {
    const navigate = useNavigate();

    const handleAddAsset = () => {
        navigate("/marketpage", { state: { portfolioId: selectedPortfolio } });
    };

    const handleSellAsset = () => {
        navigate("/sell-asset", { state: { portfolioId: selectedPortfolio } });
    };

    return (
        <>
            <DashboardBox gridArea="v">
                {/* Utilisation du résumé réel pour le graphique */}
                <AccountSummaryChart summaryData={accountSummary} />
            </DashboardBox>
            <DashboardBox gridArea="d">
                {performanceData ? (
                    <PortfolioChart chartData={performanceData} />
                ) : (
                    <p>Chargement des données...</p>
                )}
            </DashboardBox>
            <DashboardBox gridArea="e">
                {selectedPortfolio && (
                    <PortfolioAssets portfolioId={selectedPortfolio} token={token}/>
                )}
                <AnimatedButton onClick={handleAddAsset}>
                    Buy
                </AnimatedButton>
                <AnimatedButton onClick={handleSellAsset}>
                    Sell
                </AnimatedButton>
            </DashboardBox>
            <DashboardBox gridArea="t">
                <TransactionHistory portfolioId={selectedPortfolio} token={token}/>
            </DashboardBox>
        </>
    );
};

Row3.propTypes = {
    performanceData: PropTypes.object,
    selectedPortfolio: PropTypes.any,
    token: PropTypes.string,
    accountSummary: PropTypes.shape({
        crypto: PropTypes.number.isRequired,
        action: PropTypes.number.isRequired,
        devise: PropTypes.number.isRequired,
    }).isRequired,
};

export default Row3;