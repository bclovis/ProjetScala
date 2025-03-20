import React, { useState } from "react";
import PropTypes from "prop-types";
import { useNavigate } from "react-router-dom";
import AnimatedButton from "./AnimatedButton.jsx";
import {
    Dialog,
    Box,
    TextField,
    Snackbar,
    Alert
} from "@mui/material";

const Overview = ({ walletBalance, globalBalance, notifications }) => {
    const navigate = useNavigate();

    // États pour gérer le modal et l'alerte
    const [openModal, setOpenModal] = useState(false);
    const [depositAmount, setDepositAmount] = useState("");
    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState("");
    const [snackbarSeverity, setSnackbarSeverity] = useState("success");

    const token = localStorage.getItem("token");

    // Ouvrir la modale
    const handleOpenModal = () => {
        setDepositAmount("");
        setOpenModal(true);
    };

    // Fermer la modale
    const handleCloseModal = () => {
        setOpenModal(false);
    };

    // Effectuer le dépôt
    const handleDeposit = async () => {
        if (!depositAmount || parseFloat(depositAmount) <= 0) {
            setSnackbarMessage("Veuillez entrer un montant valide.");
            setSnackbarSeverity("error");
            setSnackbarOpen(true);
            return;
        }

        try {
            const response = await fetch("http://localhost:8080/api/deposit", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({ amount: parseFloat(depositAmount) })
            });

            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.message || "Erreur lors du dépôt");
            }

            setSnackbarMessage("Dépôt réussi !");
            setSnackbarSeverity("success");
            setSnackbarOpen(true);

            setTimeout(() => {
                window.location.reload();
            }, 1500);

            handleCloseModal();
        } catch (err) {
            setSnackbarMessage(err.message);
            setSnackbarSeverity("error");
            setSnackbarOpen(true);
        }
    };

    return (
        <div className="overview bg-white p-4 rounded shadow mb-4">
            <div className="flex justify-between items-center">
                <div>
                    <p className="text-xl font-bold">Fonds Déposés</p>
                    <h2 className="text-2xl">{walletBalance ? walletBalance.toFixed(2) : "0.00"} €</h2>
                </div>
                {/* Bouton pour ouvrir le modal */}
                <AnimatedButton onClick={handleOpenModal}>
                    Approvisionner
                </AnimatedButton>
            </div>
            <div className="mt-4">
                <p className="text-xl font-bold">Valeur des Investissements</p>
                <h2 className="text-2xl">{globalBalance ? globalBalance.toFixed(2) : "0.00"} €</h2>
            </div>

            {/* Modale pour le dépôt */}
            <Dialog open={openModal} onClose={handleCloseModal} maxWidth="sm" fullWidth>
                <Box sx={{ padding: 4, backgroundColor: "#1e1e1e", color: "white" }}>
                    <h2 className="text-xl font-bold mb-2">Approvisionner le Compte</h2>

                    <TextField
                        fullWidth
                        type="number"
                        label="Montant à déposer"
                        variant="outlined"
                        value={depositAmount}
                        onChange={(e) => setDepositAmount(e.target.value)}
                        sx={{ marginBottom: 2, backgroundColor: "white", borderRadius: 1 }}
                    />

                    <AnimatedButton onClick={handleDeposit}>
                        Confirmer le Dépôt
                    </AnimatedButton>
                </Box>
            </Dialog>

            {/* Snackbar pour afficher les messages */}
            <Snackbar open={snackbarOpen} autoHideDuration={2000} onClose={() => setSnackbarOpen(false)}>
                <Alert onClose={() => setSnackbarOpen(false)} severity={snackbarSeverity} sx={{ width: "100%" }}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </div>
    );
};

Overview.propTypes = {
    walletBalance: PropTypes.number.isRequired,
    globalBalance: PropTypes.number.isRequired,
    notifications: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default Overview;
