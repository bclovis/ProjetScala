//frontend/src/App.jsx
import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import Dashboard from "./pages/Dashboard";
import MarketDashboard from "./components/MarketData"; // Assurez-vous que le fichier existe et exporte le composant MarketDashboard

const App = () => (
    <Routes>
        <Route path="/" element={<Navigate to="/login" />} /> {/* Redirection vers la page de connexion */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/market-data" element={<MarketDashboard />} />
    </Routes>
);

export default App;