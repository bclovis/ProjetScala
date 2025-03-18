# Étape 1 : Build
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Étape 2 : Serveur web avec Nginx
FROM nginx:alpine
# Copier le dossier de build généré (généralement "dist") dans le dossier d'accueil de Nginx
COPY --from=builder /app/dist /usr/share/nginx/html
# Copier la configuration Nginx personnalisée depuis /frontend/config/default.conf vers /etc/nginx/conf.d/default.conf
COPY config/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]