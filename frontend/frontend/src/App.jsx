import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import Dashboard from "./pages/Dashboard";
import MarketDashboard from "./components/MarketData";
import AddAssetPage from "./pages/AddAssetPage";
import CreatePortfolioPage from "./pages/CreatePortfolioPage";


const App = () => (
    <Routes>
        <Route path="/" element={<Navigate to="/login" />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/market-data" element={<MarketDashboard />} />
        <Route path="/add-asset" element={<AddAssetPage />} />
        <Route path="/create-portfolio" element={<CreatePortfolioPage />} />
    </Routes>
);

export default App;