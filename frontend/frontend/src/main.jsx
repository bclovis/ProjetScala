//frontend/src/main.jsx
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import "./index.css"
import "./theme.ts"
import "./expanded-theme.ts"

createRoot(document.getElementById("root")).render(
    <StrictMode>
            <App />
    </StrictMode>
);