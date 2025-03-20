# Étape 1 : Build avec Node.js
FROM node:18-alpine AS builder
WORKDIR /app

# Copier uniquement package.json pour éviter de re-télécharger les dépendances à chaque modification
COPY package*.json ./
RUN npm install

# Copier le reste du projet
COPY . .

# Définir une variable d'environnement pour distinguer dev/prod
ARG NODE_ENV=production
ENV NODE_ENV=${NODE_ENV}

# Si on est en production, on génère un build
RUN if [ "$NODE_ENV" = "production" ]; then npm run build; fi

# Étape 2 : Exécuter le serveur en mode développement
FROM node:18-alpine AS development
WORKDIR /app
COPY --from=builder /app .
EXPOSE 3000
CMD ["npm", "run", "dev"]
