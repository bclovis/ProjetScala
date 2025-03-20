import React, { useState } from "react";
import PropTypes from "prop-types";
import { Dialog, Box, TextField, Snackbar, Alert } from "@mui/material";
import DashboardBox from "@/components/DashboardBox";
import Overview from "@/components/Overview";
import PortfolioList from "@/components/portfolio/PortfolioList";
import AnimatedButton from "./AnimatedButton.jsx";

const Row2 = ({ walletBalance, globalBalance, notifications, portfolios, onSelectPortfolio, selectedPortfolio }) => {
    // État pour la gestion de la modale et de l'alerte
    const [openModal, setOpenModal] = useState(false);
    const [portfolioName, setPortfolioName] = useState("");
    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState("");
    const [snackbarSeverity, setSnackbarSeverity] = useState("success");

    // Fonction pour ouvrir la modale
    const handleOpenModal = () => {
        setPortfolioName("");
        setOpenModal(true);
    };

    // Fonction pour fermer la modale
    const handleCloseModal = () => {
        setOpenModal(false);
    };

    // Fonction pour ajouter un portefeuille
    const handleCreatePortfolio = () => {
        if (!portfolioName.trim()) {
            setSnackbarMessage("Veuillez entrer un nom de portefeuille.");
            setSnackbarSeverity("error");
            setSnackbarOpen(true);
            return;
        }

        const newPortfolio = { name: portfolioName };

        fetch("http://localhost:8080/api/portfolios", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${localStorage.getItem("token")}`,
            },
            body: JSON.stringify(newPortfolio),
        })
            .then((response) => {
                if (!response.ok) {
                    return response.text().then(text => { throw new Error(text); });
                }
                return response.json();
            })
            .then(() => {
                setSnackbarMessage("Portefeuille ajouté avec succès !");
                setSnackbarSeverity("success");
                setSnackbarOpen(true);

                setTimeout(() => {
                    window.location.reload(); // Recharge la page après succès
                }, 1500);

                handleCloseModal(); // Fermer la modale après succès
            })
            .catch((err) => {
                console.error("Erreur :", err);
                setSnackbarMessage("Erreur lors de l'ajout du portefeuille.");
                setSnackbarSeverity("error");
                setSnackbarOpen(true);
            });
    };

    return (
        <>
            {/* Modale pour ajouter un portefeuille */}
            <Dialog open={openModal} onClose={handleCloseModal} maxWidth="sm" fullWidth>
                <Box sx={{ padding: 4, backgroundColor: "#1e1e1e", color: "white" }}>
                    <TextField
                        fullWidth
                        label="Nom du portefeuille"
                        variant="outlined"
                        value={portfolioName}
                        onChange={(e) => setPortfolioName(e.target.value)}
                        sx={{ marginBottom: 2, backgroundColor: "white", borderRadius: 1 }}
                    />

                    <AnimatedButton onClick={handleCreatePortfolio}>
                        Ajouter
                    </AnimatedButton>
                </Box>
            </Dialog>

            {/* Snackbar pour afficher les messages */}
            <Snackbar open={snackbarOpen} autoHideDuration={2000} onClose={() => setSnackbarOpen(false)}>
                <Alert onClose={() => setSnackbarOpen(false)} severity={snackbarSeverity} sx={{ width: "100%" }}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>

            {/* Contenu du Row2 */}
            <DashboardBox gridArea="f">
                <Overview
                    walletBalance={walletBalance}
                    globalBalance={globalBalance}
                    notifications={notifications}
                />
            </DashboardBox>
            <DashboardBox gridArea="l">
                <PortfolioList
                    portfolios={portfolios}
                    onSelectPortfolio={onSelectPortfolio}
                    selectedPortfolioId={selectedPortfolio}
                />
                <AnimatedButton onClick={handleOpenModal}>
                    ++++++
                </AnimatedButton>
            </DashboardBox>
        </>
    );
};

// Définition des types des props
Row2.propTypes = {
    walletBalance: PropTypes.number.isRequired,
    globalBalance: PropTypes.number.isRequired,
    notifications: PropTypes.array.isRequired,
    portfolios: PropTypes.array.isRequired,
    onSelectPortfolio: PropTypes.func.isRequired,
    selectedPortfolio: PropTypes.any,
};

export default Row2;
