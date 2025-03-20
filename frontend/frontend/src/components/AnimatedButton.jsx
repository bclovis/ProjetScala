// frontend/src/components/AnimatedButton.jsx
import React from "react";
import Button from "@mui/material/Button";
import PropTypes from "prop-types";

const AnimatedButton = ({ children, onClick, sx, ...props }) => {
    return (
        <Button
            variant="contained"
            onClick={onClick}
            sx={{
                position: "relative",
                top: 8,
                right: 8,
                px: 2,
                py: 1,
                fontSize: "0.875rem",
                backgroundColor: "transparent",
                color: "inherit",
                overflow: "hidden",
                // Styles du before
                "&:before": {
                    content: '""',
                    position: "absolute",
                    top: 0,
                    left: 0,
                    width: "66.666667%",
                    height: "66.666667%",
                    borderTop: "2px solid white",
                    borderLeft: "2px solid white",
                    transition: "width 0.3s, height 0.3s",
                },
                // Styles du after
                "&:after": {
                    content: '""',
                    position: "absolute",
                    bottom: 0,
                    right: 0,
                    width: "66.666667%",
                    height: "66.666667%",
                    borderBottom: "2px solid white",
                    borderRight: "2px solid white",
                    transition: "width 0.3s, height 0.3s",
                },
                // Effets au survol
                "&:hover:before": {
                    width: "100%",
                    height: "100%",
                },
                "&:hover:after": {
                    width: "100%",
                    height: "100%",
                },
            }}
            {...props}
        >
            {children}
        </Button>
    );
};

AnimatedButton.propTypes = {
    children: PropTypes.node.isRequired,
    onClick: PropTypes.func.isRequired,
    sx: PropTypes.object,
};

export default AnimatedButton;