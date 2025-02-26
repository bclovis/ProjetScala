import { useState } from "react";
import { useNavigate } from "react-router-dom";
import '../styles/LoginPage.css'; // Assurez-vous que le fichier CSS est importé
import '../styles/index.css';

const LoginPage = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");

        try {
            const response = await fetch("http://localhost:8080/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password }),
            });

            const data = await response.json();
            if (!response.ok) throw new Error(data.message || "Échec de connexion");

            localStorage.setItem("token", data.token);
            navigate("/dashboard"); // Redirige vers le tableau de bord après connexion
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <div className="wrapper">  {/* La classe wrapper va maintenant centrer tout le contenu */}
            <h1>Connexion</h1>
            {error && <p className="error-message">{error}</p>} {/* Affichage des erreurs */}
            <form onSubmit={handleLogin} className="form-container">
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
                    Pas encore de compte ? <a href="/register">S'inscrire</a>
                </p>
            </div>
        </div>
    );
};

export default LoginPage;