import React from "react";
import DashboardBox from "@/components/DashboardBox";
import MarketDashboard from "@/components/MarketData";

const Row1 = ({ performanceData }) => {
    return (
        <>
            <DashboardBox gridArea="c">
                <MarketDashboard marketData={performanceData} />
            </DashboardBox>
            {/* Vous pouvez ajouter d'autres boxes ou composants si besoin */}
        </>
    );
};

export default Row1;