//frontend/src/App.jsx
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
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

const App = () => (
    <Router>
        <Header />
    <Routes>
            <Route path="/" element={<Navigate to="/login" />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
        <Route path="/dashboard" element={
            <ProtectedRoute>
                <Dashboard />
            </ProtectedRoute>
        } />
        <Route path="/market-data" element={
            <ProtectedRoute>
                <MarketDashboard />
            </ProtectedRoute>
        } />
            <Route path="/add-asset" element={<AddAssetPage />} />
            <Route path="/sell-asset" element={<SellAssetPage />} />
            <Route path="/create-portfolio" element={<CreatePortfolioPage />} />
            <Route path="/deposit" element={<DepositPage />} />
    </Routes>
    </Router>
);

export default App;