import { useState } from "react";
import { useNavigate } from "react-router-dom";
import '../styles/LoginPage.css';
import logo from '../assets/Logo.png';

const LoginPage = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");

        try {
            const response = await fetch("http://localhost:8080/api/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password }),
            });

            const data = await response.json();
            if (!response.ok) throw new Error(data.message || "Échec de connexion");

            localStorage.setItem("token", data.token);
            navigate("/dashboard");
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
                <h1>Connexion</h1>
                {error && <p className="error-message">{error}</p>}
                <form onSubmit={handleLogin}>
                    <div className="input-box">
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                    </div>
                    <div className="input-box">
                        <input
                            type="password"
                            placeholder="Mot de passe"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>
                    <button type="submit" className="button">Se connecter</button>
                </form>

                <div className="register-link">
                    <p>
                        Pas encore de compte ? <a href="/register">Nouveau</a>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;