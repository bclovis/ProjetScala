import React from "react";
import PropTypes from "prop-types";
import DashboardBox from "@/components/DashboardBox";
import Overview from "@/components/Overview";
import PortfolioList from "@/components/portfolio/PortfolioList";

const handleCreatePortfolio = () => {
    navigate("/create-portfolio");
};
const Row2 = ({ walletBalance, globalBalance, notifications, portfolios, onSelectPortfolio, selectedPortfolio }) => {
    return (
        <>
            <DashboardBox gridArea="f">
                <Overview walletBalance={walletBalance} globalBalance={globalBalance} notifications={notifications} />
            </DashboardBox>
            <DashboardBox gridArea="l">

                <PortfolioList
                    portfolios={portfolios}
                    onSelectPortfolio={onSelectPortfolio}
                    selectedPortfolioId={selectedPortfolio}
                />
                <button
                    onClick={handleCreatePortfolio}
                    className="text-xl font-bold px-2 py-1 bg-green-500 text-white rounded"
                >
                    +
                </button>
            </DashboardBox>
        </>
    );
};

Row2.propTypes = {
    walletBalance: PropTypes.number.isRequired,
    globalBalance: PropTypes.number.isRequired,
    notifications: PropTypes.array.isRequired,
    portfolios: PropTypes.array.isRequired,
    onSelectPortfolio: PropTypes.func.isRequired,
    selectedPortfolio: PropTypes.any,
};

export default Row2;