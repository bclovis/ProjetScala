import { useState } from "react";
import { registerUser } from "../api/UserService";

function UserForm() {
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");

    const handleSubmit = async (e) => {
        e.preventDefault();
        const user = {
            username,
            email,
            passwordHash: password,
            isVerified: false,
        };
        const result = await registerUser(user);
        if (result) {
            setMessage("Utilisateur créé avec succès !");
        } else {
            setMessage("Erreur lors de la création de l'utilisateur.");
        }
    };

    return (
        <div>
            <h2>Créer un utilisateur</h2>
            <form onSubmit={handleSubmit}>
                <input type="text" placeholder="Nom d'utilisateur" value={username} onChange={(e) => setUsername(e.target.value)} required />
                <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                <input type="password" placeholder="Mot de passe" value={password} onChange={(e) => setPassword(e.target.value)} required />
                <button type="submit">S inscrire</button>
            </form>
            {message && <p>{message}</p>}
        </div>
    );
}

export default UserForm;
