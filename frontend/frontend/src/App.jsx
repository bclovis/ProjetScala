//frontend/src/pages/App.jsx
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { ThemeProvider, CssBaseline } from "@mui/material";
import { createTheme } from "@mui/material/styles";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import Dashboard from "./pages/Dashboard";
import MarketDashboard from "./components/MarketData";
import AddAssetPage from "./pages/AddAssetPage";
import SellAssetPage from "./pages/SellAssetPage"; // Importez la page de vente
import CreatePortfolioPage from "./pages/CreatePortfolioPage";
import DepositPage from "./pages/DepositPage";
import ProtectedRoute from "./components/ProtectedRoute";
import Header from "./components/Header";

// Définissez votre thème personnalisé
const theme = createTheme({
    palette: {
        background: {
            default: "#1f2026",
            light: "#2d2d34",
        },
        primary: {
            main: "#12efc8",
        },
        grey: {
            700: "#6b6d74",
            300: "#f0f0f3",
        },
        // Ajoutez d'autres couleurs selon vos besoins
    },
    text: {
        primary: "#ffffff",
    },
    typography: {
        fontFamily: "Inter, sans-serif",
        h2: {
            fontSize: "32px",
            fontWeight: 800,
            color: "#ffffff", // par exemple
        },
        h6: {
            fontSize: "14px",
            fontWeight: 600,
            color: "#ffffff", // par exemple
        },
        h5: {
            fontSize: "12px",
            fontWeight: 400,
            color: "#ffffff",
        },
    },
});

const token = localStorage.getItem("token");
const App = () => (
    <ThemeProvider theme={theme}>
        <CssBaseline />
        <Router>
            {token && <Header />}
            <Routes>
                <Route path="/" element={<Navigate to="/login" />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route
                    path="/dashboard"
                    element={
                        <ProtectedRoute>
                            <Dashboard />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/market-data"
                    element={
                        <ProtectedRoute>
                            <MarketDashboard />
                        </ProtectedRoute>
                    }
                />
                <Route path="/add-asset" element={<AddAssetPage />} />
                <Route path="/sell-asset" element={<SellAssetPage />} />
                <Route path="/create-portfolio" element={<CreatePortfolioPage />} />
                <Route path="/deposit" element={<DepositPage />} />
            </Routes>
        </Router>
    </ThemeProvider>
);

export default App;