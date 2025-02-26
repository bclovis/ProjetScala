import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import '../styles/Dashboard.css'; // Assurez-vous que le fichier CSS spécifique au Dashboard est importé

const Dashboard = () => {
    const [portfolios, setPortfolios] = useState([]);
    const [error, setError] = useState("");
    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem("token");
        if (!token) {
            navigate("/login"); // Si pas de token, rediriger vers la page de connexion
        } else {
            // Récupérer les portefeuilles de l'utilisateur à partir de l'API
            fetch("http://localhost:8080/portfolios", {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`, // Passer le token dans le header
                },
            })
                .then((res) => res.json())
                .then((data) => {
                    setPortfolios(data); // Si la requête réussit, mettre à jour les portefeuilles
                })
                .catch((err) => setError(err.message)); // Si erreur, afficher l'erreur
        }
    }, [navigate]);

    return (
        <div className="dashboard-page">
            <div className="dashboard-wrapper">
                <h1>Your Dashboard</h1>
                {error && <p>{error}</p>}
                <ul>
                    {portfolios.map((portfolio) => (
                        <li key={portfolio.id}>{portfolio.name}</li> // Afficher le nom du portefeuille
                    ))}
                </ul>
                <CreatePortfolio /> {/* Ajouter un formulaire pour créer un portefeuille */}
            </div>
        </div>
    );
};

// Formulaire pour créer un nouveau portefeuille
const CreatePortfolio = () => {
    const [name, setName] = useState("");
    const [error, setError] = useState("");

    const handleCreatePortfolio = async (e) => {
        e.preventDefault();

        const token = localStorage.getItem("token");
        if (!token) {
            setError("User is not authenticated");
            return;
        }

        const response = await fetch("http://localhost:8080/portfolios", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            },
            body: JSON.stringify({ name }),
        });

        if (!response.ok) {
            setError("Failed to create portfolio");
        } else {
            // Rafraîchir la liste des portefeuilles ou rediriger vers le tableau de bord
            setName(""); // Réinitialiser le champ du formulaire
        }
    };

    return (
        <form onSubmit={handleCreatePortfolio}>
            <input
                type="text"
                placeholder="Portfolio Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
            />
            <button type="submit">Create Portfolio</button>
            {error && <p>{error}</p>}
        </form>
    );
};

export default Dashboard;