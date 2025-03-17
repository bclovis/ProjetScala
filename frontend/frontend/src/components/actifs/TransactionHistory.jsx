//frontend/src/components/TransactionHistory.jsx
import React, { useEffect, useState } from "react";
import PropTypes from "prop-types";

const TransactionHistory = ({ portfolioId, token }) => {
    const [transactions, setTransactions] = useState([]);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!portfolioId) return;
        fetch(`http://localhost:8080/api/portfolios/${portfolioId}/transactions`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            }
        })
            .then((res) => res.json())
            .then((data) => setTransactions(data))
            .catch((err) => setError(err.message));
    }, [portfolioId, token]);

    return (
        <div className="transaction-history p-4 bg-white rounded shadow" style={{ maxHeight: "300px", overflowY: "auto" }}>
            <h3 className="text-xl font-bold mb-2">Historique des transactions</h3>
            {error && <p className="text-red-500">{error}</p>}
            {transactions.length === 0 ? (
                <ol>Aucune transaction enregistrée.</ol>
            ) : (
                <ul>
                    {transactions.map((tx) => (
                        <ol key={tx.id} className="mb-2 border-b pb-1">
                            <p>
                                <strong>{tx.txType.toUpperCase()}</strong> - {tx.symbol} : {tx.amount} à {tx.price} €
                            </p>
                            <small>{new Date(tx.createdAt).toLocaleString()}</small>
                        </ol>
                    ))}
                </ul>
            )}
        </div>
    );
};

TransactionHistory.propTypes = {
    portfolioId: PropTypes.number.isRequired,
    token: PropTypes.string.isRequired
};

export default TransactionHistory;