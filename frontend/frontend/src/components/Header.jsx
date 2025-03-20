// frontend/src/components/Header.jsx
import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import PixIcon from "@mui/icons-material/Pix";
import { Box, Typography, useTheme, Button } from "@mui/material";
import FlexBetween from "./FlexBetween";

const Header = () => {
    const { palette } = useTheme();
    const [selected, setSelected] = useState("dashboard");
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const navigate = useNavigate();

    // Vérifier si l'utilisateur est connecté
    useEffect(() => {
        const token = localStorage.getItem("token");
        setIsAuthenticated(!!token); // Met à jour l'état en fonction de la présence du token
    }, []);

    // Déconnexion de l'utilisateur
    const handleLogout = () => {
        localStorage.removeItem("token");
        setIsAuthenticated(false);
        navigate("/login");
    };

    return (
        <FlexBetween mb="0.25rem" p="0.5rem 0rem" color={palette.grey[300]}>
            {/* Left side */}
            <FlexBetween gap="0.75rem">
                <PixIcon sx={{ fontSize: "28px" }} />
                <Typography variant="h4" fontSize="16px">
                    FinTech
                </Typography>
            </FlexBetween>

            {/* Right side */}
            <FlexBetween gap="2rem">
                <Box sx={{ "&:hover": { color: palette.primary[100] } }}>
                    <Link
                        to="/"
                        onClick={() => setSelected("dashboard")}
                        style={{
                            color: selected === "dashboard" ? "inherit" : palette.grey[700],
                            textDecoration: "none",
                            fontWeight: selected === "dashboard" ? 600 : 400,
                        }}
                    >
                        Dashboard
                    </Link>
                </Box>
                <Box sx={{ "&:hover": { color: palette.primary[100] } }}>
                    <Link
                        to="/marketpage"
                        onClick={() => setSelected("marketpage")}
                        style={{
                            color: selected === "marketpage" ? "inherit" : palette.grey[700],
                            textDecoration: "none",
                            fontWeight: selected === "marketpage" ? 600 : 400,
                        }}
                    >
                        Info Marché
                    </Link>
                </Box>


                {/* Bouton Connexion / Déconnexion */}
                {isAuthenticated ? (
                    <Button
                        onClick={handleLogout}
                        variant="contained"
                        color="secondary"
                        sx={{ fontSize: "14px", padding: "6px 12px" }}
                    >
                        Se déconnecter
                    </Button>
                ) : (
                    <Button
                        onClick={() => navigate("/login")}
                        variant="contained"
                        color="primary"
                        sx={{ fontSize: "14px", padding: "6px 12px" }}
                    >
                        Se connecter
                    </Button>
                )}
            </FlexBetween>
        </FlexBetween>
    );
};

export default Header;
