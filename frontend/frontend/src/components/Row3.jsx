import React from "react";
import DashboardBox from "@/components/DashboardBox";
import AccountSummaryChart from "@/components/AccountSummaryChart";
import PortfolioChart from "@/components/portfolio/PortfolioChart.jsx";
import PortfolioAssets from "@/components/portfolio/PortfolioAssets.jsx";
import TransactionHistory from "@/components/actifs/TransactionHistory.jsx";

const Row3 = ({ performanceData, selectedPortfolio, token }) => {
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
                {selectedPortfolio && <PortfolioAssets portfolioId={selectedPortfolio} token={token} />}
            </DashboardBox>
            <DashboardBox gridArea="t">
                <TransactionHistory portfolioId={selectedPortfolio} token={token} />
            </DashboardBox>
        </>
    );
};

export default Row3;