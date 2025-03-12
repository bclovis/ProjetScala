// frontend/src/pages/LoginPage.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import '@fortawesome/fontawesome-free/css/all.min.css';
import '../styles/LoginPage.css'; // Assurez-vous que ce fichier contient les styles inspirés du GitHub fourni

const LoginPage = () => {
    const [isActive, setIsActive] = useState(false); // false = mode Sign In, true = mode Sign Up
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem("token");
        if (token) {
            navigate("/dashboard"); // ✅ Redirige vers /dashboard s'il est déjà connecté
        }
    }, []);

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

    const handleSignUp = async (e) => {
        e.preventDefault();
        setError("");
        // Pour l'instant, nous simulons la réussite de l'inscription en utilisant uniquement l'email et le mot de passe
        console.log("Inscription effectuée avec", email, password);
        // Une fois l'inscription simulée, on passe en mode connexion
        setIsActive(false);
    };

    return (
        <div className="login-page">
            <div className={`container ${isActive ? "active" : ""}`} id="container">
                {/* Formulaire d'inscription */}
                <div className="form-container sign-up">
                    <form onSubmit={handleSignUp}>
                        <h1>Create Account</h1>
                        <div className="social-icons">
                            <a href="#" className="icon"><i className="fa-brands fa-google-plus-g"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-facebook-f"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-github"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-linkedin-in"></i></a>
                        </div>
                        <span>or use your email for registration</span>
                        {/* Champ "Name" supprimé */}
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                        <input
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                        <button type="submit">Sign Up</button>
                    </form>
                </div>

                {/* Formulaire de connexion */}
                <div className="form-container sign-in">
                    <form onSubmit={handleLogin}>
                        <h1>Sign In</h1>
                        <div className="social-icons">
                            <a href="#" className="icon"><i className="fa-brands fa-google-plus-g"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-facebook-f"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-github"></i></a>
                            <a href="#" className="icon"><i className="fa-brands fa-linkedin-in"></i></a>
                        </div>
                        <span>or use your email password</span>
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                        <input
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                        <a href="/">Forget Your Password?</a>
                        <button type="submit">Sign In</button>
                    </form>
                </div>

                {/* Zone de basculement */}
                <div className="toggle-container">
                    <div className="toggle">
                        <div className="toggle-panel toggle-left">
                            <h1>Welcome Back!</h1>
                            <p>Enter your email and password to sign in</p>
                            <button className="hidden" onClick={() => setIsActive(false)}>Sign In</button>
                        </div>
                        <div className="toggle-panel toggle-right">
                            <h1>Hello, Friend!</h1>
                            <p>Register with your email and password to get started</p>
                            <button className="nav-button" onClick={() => navigate('/register')}>Nouveau</button>
                        </div>
                    </div>
                </div>

                {error && <p className="error-message">{error}</p>}
            </div>
        </div>
    );
};

export default LoginPage;
