//frontend/src/components/Header.jsx
import React, { useState } from "react";
import { Link } from "react-router-dom";
import PixIcon from "@mui/icons-material/Pix";
import { Box, Typography, useTheme } from "@mui/material";
import FlexBetween from "./FlexBetween";

const Header = () => {
    const { palette } = useTheme();
    const [selected, setSelected] = useState("dashboard");

    return (
        <FlexBetween mb="0.25rem" p="0.5rem 0rem" color={palette.grey[300]}>
            {/* Left side */}
            <FlexBetween gap="0.75rem">
                <PixIcon sx={{ fontSize: "28px" }} />
                <Typography variant="h4" fontSize="16px">
                    Finanseer
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
                        to="/predictions"
                        onClick={() => setSelected("predictions")}
                        style={{
                            color: selected === "predictions" ? "inherit" : palette.grey[700],
                            textDecoration: "none",
                            fontWeight: selected === "predictions" ? 600 : 400,
                        }}
                    >
                        Predictions
                    </Link>
                </Box>
            </FlexBetween>
        </FlexBetween>
    );
};

export default Header;