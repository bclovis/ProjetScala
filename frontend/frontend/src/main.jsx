import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import MarketDashboard from "./MarketData.jsx";

createRoot(document.getElementById("root")).render(
    <StrictMode>
        <BrowserRouter>
            <MarketDashboard />
        </BrowserRouter>
    </StrictMode>
);