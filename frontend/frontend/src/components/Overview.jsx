//frontend/src/components/Overview.jsx
import React from "react";
import PropTypes from "prop-types";
import { useNavigate } from "react-router-dom";

const Overview = ({ walletBalance, globalBalance, notifications }) => {
    const navigate = useNavigate();

    return (
        <div className="overview bg-white p-4 rounded shadow mb-4">
            <div className="flex justify-between items-center">
                <div>
                    <p className="text-xl font-bold">Fonds Déposés</p>
                    <h2 className="text-2xl">{walletBalance ? walletBalance.toFixed(2) : "0.00"} €</h2>
                </div>
                <button
                    className="bg-blue-500 text-white py-2 px-4 rounded"
                    onClick={() => navigate("/deposit")}
                >
                    Approvisionner
                </button>
            </div>
            <div className="mt-4">
                <p className="text-xl font-bold">Valeur des Investissements</p>
                <h2 className="text-2xl">{globalBalance ? globalBalance.toFixed(2) : "0.00"} €</h2>
            </div>
        </div>
    );
};

Overview.propTypes = {
    walletBalance: PropTypes.number.isRequired,
    globalBalance: PropTypes.number.isRequired,
    notifications: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default Overview;