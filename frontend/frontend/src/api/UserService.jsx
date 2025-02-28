const API_URL = "http://localhost:8080/users";

export async function fetchUser(userId) {
    try {
        const response = await fetch(`${API_URL}/${userId}`);
        if (!response.ok) {
            throw new Error(`Erreur HTTP: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error("Erreur lors de la récupération de l'utilisateur:", error);
        return null;
    }
}

export async function registerUser(user) {
    try {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(user),
        });
        if (!response.ok) {
            throw new Error(`Erreur HTTP: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error("Erreur lors de l'inscription:", error);
        return null;
    }
}

export async function updateUser(userId, user) {
    try {
        const response = await fetch(`${API_URL}/${userId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(user),
        });
        if (!response.ok) {
            throw new Error(`Erreur HTTP: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error("Erreur lors de la mise à jour de l'utilisateur:", error);
        return null;
    }
}
