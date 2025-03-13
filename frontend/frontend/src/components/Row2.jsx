import React from "react";
import DashboardBox from "@/components/DashboardBox";
import Overview from "@/components/Overview";
import PortfolioList from "@/components/portfolio/PortfolioList";
import BoxHeader from "./BoxHeader.jsx";

const Row2 = ({ walletBalance, globalBalance, notifications, portfolios, onSelectPortfolio, selectedPortfolio }) => {
    return (
        <>
            <DashboardBox gridArea="f">
                <Overview walletBalance={walletBalance} globalBalance={globalBalance} notifications={notifications} />
            </DashboardBox>
            <DashboardBox gridArea="l">
                <BoxHeader
                    title={"Repartition of portfolio"}
                    subitle={"topline super"}
                    sideText={"+3%"}
                />
                <PortfolioList
                    portfolios={portfolios}
                    onSelectPortfolio={onSelectPortfolio}
                    selectedPortfolioId={selectedPortfolio}
                />
            </DashboardBox>
        </>
    );
};

export default Row2;