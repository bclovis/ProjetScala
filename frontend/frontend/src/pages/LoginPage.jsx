// frontend/src/pages/LoginPage.jsx
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "@fortawesome/fontawesome-free/css/all.min.css";
import "@/styles/LoginPage.css"; // Assurez-vous que ce fichier existe

const LoginPage = () => {
    const [isActive, setIsActive] = useState(false);
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    // Vérifier si l'utilisateur est déjà connecté
    useEffect(() => {
        const token = localStorage.getItem("token");
        if (token) {
            navigate("/dashboard"); // Redirige si connecté
        }
    }, [navigate]);

    // Connexion utilisateur
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

            
            localStorage.setItem("token", data.token); // Stocke le JWT
            navigate("/dashboard");
        } catch (err) {
            setError(err.message);
        }
    };

    // Inscription utilisateur
    const handleSignUp = async (e) => {
        e.preventDefault();
        setError("");

        try {
            const response = await fetch("http://localhost:8080/api/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, email, password }),
            });

            const data = await response.json();
            if (!response.ok) throw new Error(data.message || "Échec de l'inscription");

            localStorage.setItem("token", data.token); // Stocke le JWT
            navigate("/dashboard");
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <div className="login-page">
            <div className={`container ${isActive ? "active" : ""}`} id="container">
                {/* Formulaire d'inscription */}
                <div className="form-container sign-up">
                    <form onSubmit={handleSignUp}>
                        <h1>Créer un compte</h1>
                        <div className="social-icons">
                            <a href="#" className="icon"><i className="fa-brands fa-google-plus-g"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-facebook-f"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-github"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-linkedin-in"></i></a>
                        </div>
                        <span>Ou utilisez votre e-mail pour vous inscrire</span>
                        <input
                            type="text"
                            placeholder="Nom d'utilisateur"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                        <input
                            type="password"
                            placeholder="Mot de passe"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                        <button type="submit">S'inscrire</button>
                    </form>
                </div>

                {/* Formulaire de connexion */}
                <div className="form-container sign-in">
                    <form onSubmit={handleLogin}>
                        <h1>Connexion</h1>
                        <div className="social-icons">
                            <a href="#" className="icon"><i className="fa-brands fa-google-plus-g"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-facebook-f"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-github"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-linkedin-in"></i></a>
                        </div>
                        <span>Ou utilisez votre e-mail et mot de passe</span>
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                        <input
                            type="password"
                            placeholder="Mot de passe"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                        <a href="/">Mot de passe oublié ?</a>
                        <button type="submit">Se connecter</button>
                    </form>
                </div>

                {/* Zone de basculement */}
                <div className="toggle-container">
                    <div className="toggle">
                        <div className="toggle-panel toggle-left">
                            <h1>Bienvenue !</h1>
                            <p>Entrez vos identifiants pour vous connecter</p>
                            <button onClick={() => setIsActive(false)}>Connexion</button>
                        </div>
                        <div className="toggle-panel toggle-right">
                            <h1>Rejoignez-nous !</h1>
                            <p>Inscrivez-vous en quelques clics</p>
                            <button onClick={() => setIsActive(true)}>Inscription</button>
                        </div>
                    </div>
                </div>

                {error && <p className="error-message">{error}</p>}
            </div>
        </div>
    );
};

export default LoginPage;
