import React from "react";
import { Pie } from "react-chartjs-2";
import PropTypes from "prop-types";
import {
    Chart as ChartJS,
    ArcElement,
    Tooltip,
    Legend,
} from "chart.js";

// Enregistrement des éléments nécessaires
ChartJS.register(ArcElement, Tooltip, Legend);
const AccountSummaryChart = ({ summaryData }) => {
    const data = {
        labels: ["Crypto", "Actions", "Devises"],
        datasets: [
            {
                data: [summaryData.crypto, summaryData.action, summaryData.devise],
                backgroundColor: [
                    "rgba(75,192,192,0.6)",
                    "rgba(255,206,86,0.6)",
                    "rgba(153,102,255,0.6)"
                ],
            },
        ],
    };

    return (
        <div className="account-summary-chart">
            <Pie
                data={data}
                width={300}
                height={300}
                options={{ maintainAspectRatio: false }}
            />        </div>
    );
};

AccountSummaryChart.propTypes = {
    summaryData: PropTypes.shape({
        crypto: PropTypes.number.isRequired,
        action: PropTypes.number.isRequired,
        devise: PropTypes.number.isRequired,
    }).isRequired,
};

export default AccountSummaryChart;