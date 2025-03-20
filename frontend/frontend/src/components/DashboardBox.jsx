import React from "react";
import { Box } from "@mui/material";
import BoxHeader from "./BoxHeader";
import PropTypes from "prop-types";


const DashboardBox = ({ children, title, subtitle, sideText, sx, gridArea, icon }) => {
    return (
        <Box
            sx={{
                backgroundColor: "rgba(253,253,253,0.86)",
                p: 2,
                borderRadius: 2,
                boxShadow: 3,
                gridArea,
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

DashboardBox.propTypes = {
    children: PropTypes.node.isRequired,
    title: PropTypes.string,
    subtitle: PropTypes.string,
    sideText: PropTypes.string,
    sx: PropTypes.object,
    gridArea: PropTypes.string,
    icon: PropTypes.string,
}

export default DashboardBox;