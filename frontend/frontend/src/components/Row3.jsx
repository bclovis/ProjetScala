import React from "react";
import PropTypes from "prop-types";
import { useNavigate } from "react-router-dom";
import DashboardBox from "@/components/DashboardBox";
import AccountSummaryChart from "@/components/AccountSummaryChart";
import PortfolioChart from "@/components/portfolio/PortfolioChart.jsx";
import PortfolioAssets from "@/components/portfolio/PortfolioAssets.jsx";
import TransactionHistory from "@/components/actifs/TransactionHistory.jsx";

const Row3 = ({ performanceData, selectedPortfolio, token }) => {

    const navigate = useNavigate();

    const handleAddAsset = () => {
        navigate("/add-asset", { state: { portfolioId: selectedPortfolio } });
    };


    return (
        <>
            <DashboardBox gridArea="v">
                <AccountSummaryChart summaryData={{ crypto: 0, action: 0, devise: 0 }} />
            </DashboardBox>
            <DashboardBox gridArea="d">
                {performanceData ? (
                    <PortfolioChart chartData={performanceData} />
                ) : (
                    <p>Chargement des données...</p>
                )}
            </DashboardBox>
            <DashboardBox gridArea="e">
                {selectedPortfolio && <PortfolioAssets portfolioId={selectedPortfolio} token={token}/>}
                <button
                    onClick={handleAddAsset}
                    className="text-xl font-bold px-2 py-1 bg-green-500 text-white rounded"
                >
                    +
                </button>
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
};


export default Row3;