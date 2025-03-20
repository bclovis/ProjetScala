import React from "react";
import DashboardBox from "@/components/DashboardBox";
import MarketDashboard from "@/components/MarketData";
import PropTypes from "prop-types";

const Row1 = ({ performanceData }) => {
    return (
        <>
            <DashboardBox gridArea="c">
                <MarketDashboard marketData={performanceData} />
            </DashboardBox>
        </>
    );
};

Row1.propTypes = {
    performanceData: PropTypes.object,
}

export default Row1;