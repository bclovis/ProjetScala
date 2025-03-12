import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import Dashboard from "./pages/Dashboard";
import MarketDashboard from "./components/MarketData";
import AddAssetPage from "./pages/AddAssetPage";
import SellAssetPage from "./pages/SellAssetPage"; // Importez la page de vente
import CreatePortfolioPage from "./pages/CreatePortfolioPage";
import DepositPage from "./pages/DepositPage";

const App = () => (
    <Routes>
            <Route path="/" element={<Navigate to="/login" />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/market-data" element={<MarketDashboard />} />
            <Route path="/add-asset" element={<AddAssetPage />} />
            <Route path="/sell-asset" element={<SellAssetPage />} />
            <Route path="/create-portfolio" element={<CreatePortfolioPage />} />
            <Route path="/deposit" element={<DepositPage />} />
    </Routes>
);

export default App;