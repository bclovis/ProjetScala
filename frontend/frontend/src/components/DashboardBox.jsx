import React from "react";
import { Box } from "@mui/material";
import BoxHeader from "./BoxHeader";

const DashboardBox = ({ children, title, subtitle, sideText, sx, gridArea, icon }) => {
    return (
        <Box
            sx={{
                backgroundColor: "rgba(253,253,253,0.86)", // Utilisez une couleur définie dans votre thème
                p: 2,
                borderRadius: 2,
                boxShadow: 3,
                gridArea, // Pour le positionnement dans la grille
                ...sx,
            }}
        >
            {(title || subtitle || sideText) && (
                <BoxHeader icon={icon} title={title} subtitle={subtitle} sideText={sideText} />
            )}
            {children}
        </Box>
    );
};

export default DashboardBox;