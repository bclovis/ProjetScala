import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

const Header = () => {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        // Vérifier si un token JWT est stocké
        const token = localStorage.getItem("token");
        setIsLoggedIn(!!token);
    }, []);

    const handleLogout = () => {
        localStorage.removeItem("token"); // Supprimer le token
        setIsLoggedIn(false);
        navigate("/login"); // Rediriger vers la page de connexion
    };

    return (
        <header className="header">
            <div className="logo">Mon Dashboard</div>
            <div className="nav-links">
                <a href="/buy-crypto">Buy Crypto</a>
                <a href="/buy-action">Buy Action</a>
                <a href="/buy-devise">Buy Devise</a>
                <a href="/wallet">Wallet</a>
            </div>
            {isLoggedIn ? (
                <button className="nav-button" onClick={handleLogout}>Déconnexion</button>
            ) : (
                <button className="nav-button" onClick={() => navigate('/login')}>Connexion</button>
            )}
        </header>
    );
};

export default Header;
