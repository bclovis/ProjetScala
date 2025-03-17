// frontend/src/components/PortfolioChart.jsx
import { Line } from "react-chartjs-2";
import PropTypes from "prop-types";
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend,
} from "chart.js";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

const PortfolioChart = ({ chartData }) => {
    const data = {
        labels: chartData.labels,
        datasets: [
            {
                label: "Performance",
                data: chartData.data,
                fill: false,
                borderColor: "rgba(75,192,192,1)",
                tension: 0.1,
            },
        ],
    };

    const options = {
        responsive: true,
        plugins: {
            legend: { position: "top" },
            title: { display: true, text: "Courbe de Performance du Portefeuille" },
        },
    };

    return (
        <div className="portfolio-chart">
            <Line data={data} options={options} />
        </div>
    );
};

PortfolioChart.propTypes = {
    chartData: PropTypes.shape({
        labels: PropTypes.arrayOf(PropTypes.string).isRequired,
        data: PropTypes.arrayOf(PropTypes.number).isRequired,
    }).isRequired,
};

export default PortfolioChart;