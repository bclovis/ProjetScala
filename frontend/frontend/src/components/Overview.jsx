//frontend/src/components/Overview.jsx
import React from "react";
import PropTypes from "prop-types";


const Overview = ({ globalBalance, accountSummary, notifications }) => {
    return (
        <div className="overview bg-white p-4 rounded shadow mb-4">
            <div className="flex justify-between">
                <div>
                    <h2 className="text-xl font-bold">Solde Global</h2>
                    <p className="text-2xl">${globalBalance ? globalBalance.toFixed(2) : "0.00"}</p>
                </div>
                <div>
                    <h2 className="text-xl font-bold">Résumé du Compte</h2>
                    <p>{accountSummary || "Aucun résumé disponible"}</p>
                </div>
            </div>
            {notifications && notifications.length > 0 && (
                <div className="mt-4">
                    <h3 className="font-bold">Notifications</h3>
                    <ul>
                        {notifications.map((note, idx) => (
                            <li key={idx}>{note}</li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
};

Overview.propTypes = {
    globalBalance: PropTypes.number,
    accountSummary: PropTypes.string,
    notifications: PropTypes.arrayOf(PropTypes.string)
};

export default Overview;