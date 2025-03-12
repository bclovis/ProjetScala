import React, { useState } from "react";
import Header from "../components/Header";
import "../styles/LoginPage.css"; // Réutilise le style du Login

const DepositPage = () => {
    const [amount, setAmount] = useState("");
    const [message, setMessage] = useState("");
    const token = localStorage.getItem("token");

    const handleDeposit = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch("http://localhost:8080/api/deposit", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({ amount: parseFloat(amount) })
            });
            const data = await response.json();
            if (!response.ok) {
                setMessage(data.message || "Erreur lors du dépôt");
            } else {
                setMessage("Dépôt réussi !");
                setAmount("");
            }
        } catch (err) {
            setMessage(err.message);
        }
    };

    return (
        <div className="login-page">
            <div className="container">
                <Header />
                <div className="form-container" style={{ padding: "40px" }}>
                    <h1 className="mb-4">Approvisionner le Compte</h1>
                    <form onSubmit={handleDeposit}>
                        <input
                            type="number"
                            placeholder="Montant à déposer"
                            value={amount}
                            onChange={(e) => setAmount(e.target.value)}
                            required
                        />
                        <button type="submit">Déposer</button>
                    </form>
                    {message && <p>{message}</p>}
                </div>
            </div>
        </div>
    );
};

export default DepositPage;