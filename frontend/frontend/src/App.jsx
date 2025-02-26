import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import Dashboard from "./pages/Dashboard";

const App = () => (
    <Routes>
        <Route path="/" element={<Navigate to="/login" />} />  {/* Redirection automatique */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<Dashboard />} />
    </Routes>
);

export default App;