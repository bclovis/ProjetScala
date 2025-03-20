import React, { useEffect, useState } from "react";
import { Pie } from "react-chartjs-2";
import PropTypes from "prop-types";
import {
    Chart as ChartJS,
    ArcElement,
    Tooltip,
    Legend,
} from "chart.js";

// Enregistrement des éléments nécessaires pour Chart.js
ChartJS.register(ArcElement, Tooltip, Legend);

const AccountSummaryChart = ({ summaryData }) => {
    // État local pour stocker les données du graphique et forcer la mise à jour
    const [chartData, setChartData] = useState(null);

    useEffect(() => {
        if (summaryData && summaryData.crypto !== undefined) {
            setChartData({
                labels: ["Crypto", "Actions", "Devises"],
                datasets: [
                    {
                        data: [
                            summaryData.crypto || 0,
                            summaryData.action || 0,
                            summaryData.devise || 0,
                        ],
                        backgroundColor: [
                            "rgba(75,192,192,0.6)",
                            "rgba(255,206,86,0.6)",
                            "rgba(153,102,255,0.6)",
                        ],
                    },
                ],
            });
        }
    }, [summaryData]); // Met à jour le graphique dès que summaryData change

    return (
        <div className="account-summary-chart">
            {chartData ? (
                <Pie
                    data={chartData}
                    width={300}
                    height={300}
                    options={{ maintainAspectRatio: false }}
                />
            ) : (
                <p>Chargement du graphique...</p>
            )}
        </div>
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
