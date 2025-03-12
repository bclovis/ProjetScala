import { useState } from "react";
import { useNavigate } from "react-router-dom";
import '../styles/LoginPage.css'; // ✅ Réutilisation du CSS de LoginPage
import logo from '../assets/Logo.png';

const RegisterPage = () => {
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const navigate = useNavigate();

    const handleRegister = async (e) => {
        e.preventDefault();
        setError("");
        setSuccess("");

        try {
            const response = await fetch("http://localhost:8080/api/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, email, password }),
            });

            const data = await response.json();
            if (!response.ok) throw new Error(data.message || "Échec de l'inscription");

            setSuccess("Compte créé avec succès ! Redirection...");
            setTimeout(() => navigate("/login"), 2000); // ✅ Redirige après 2 secondes
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <div>
            <nav className="navbar">
                <img src={logo} className="logo" alt="Logo"/>
                <div className="nav-links">
                    <button className="nav-button" onClick={() => navigate('/login')}>Connexion</button>
                    <button className="nav-button" onClick={() => navigate('/register')}>Nouveau</button>
                </div>
            </nav>

            <div className="wrapper">
                <h1>Créer un compte</h1>
                {error && <p className="error-message">{error}</p>}
                {success && <p className="success-message">{success}</p>}
                <form onSubmit={handleRegister}>
                    <div className="input-box">
                        <input
                            type="text"
                            placeholder="Nom d'utilisateur"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                    </div>
                    <div className="input-box">
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>
                    <div className="input-box">
                        <input
                            type="password"
                            placeholder="Mot de passe"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>
                    <button type="submit" className="button">S'inscrire</button>
                </form>

                <div className="register-link">
                    <p>
                        Déjà un compte ? <a href="/login">Connexion</a>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;
