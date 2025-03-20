FinTech

FinTech est une application de gestion de portefeuille financier en temps réel. Elle permet aux utilisateurs de suivre leurs investissements, d’effectuer des analyses de performance et d’interagir avec des données de marché en direct via une interface moderne et intuitive.

Table des Matières
	•	Présentation
	•	Objectifs Techniques
	•	Architecture du Projet
	•	Technologies Utilisées
	•	Structure du Projet
	•	Installation et Déploiement
	•	Backend
	•	Frontend
	•	Base de Données
	•	Utilisation

Présentation

Ce projet est une application de gestion de portefeuille financier en temps réel qui offre aux utilisateurs une interface moderne pour suivre leurs investissements et analyser la performance de leurs actifs. L’application intègre des flux de données en direct via des APIs financières (comme Finnhub.io) et utilise une architecture distribuée pour gérer efficacement ces données.

Objectifs Techniques
	•	Backend en Scala avec Akka
	•	Mise en place d’un système distribué pour la gestion des données de portefeuille.
	•	Traitement des flux de données en temps réel avec Akka Streams.
	•	Communication inter-services via Akka Actors.
	•	Connexion à une API financière (Finnhub.io).
	•	Stockage des données dans une base de données PostgreSQL.
	•	Frontend Moderne (React avec TailwindCSS)
	•	Création d’une interface utilisateur dynamique et intuitive.
	•	Implémentation d’une authentification sécurisée (JWT, OAuth).
	•	Affichage de données en temps réel via WebSockets ou API polling.
	•	Conception responsive adaptée à tous les appareils.
	•	Sécurité et Optimisation
	•	Sécurisation des APIs avec JWT et OAuth 2.0.
	•	Déploiement via Docker pour assurer une gestion efficace des environnements.

Architecture du Projet

L’architecture se divise en deux parties principales :
	•	Backend : Développé en Scala avec Akka, il gère le traitement des données, la communication entre services et l’accès aux données via PostgreSQL.
	•	Frontend : Construit en React.js, il offre une interface utilisateur moderne et responsive, intégrant des composants UI personnalisés et la gestion des données en temps réel.

Technologies Utilisées
	•	Backend:
	•	Langage : Scala
	•	Framework : Akka (Akka Streams, Akka Actors)
	•	Base de données : PostgreSQL
	•	API : YahooFinance
	•	Conteneurisation : Docker
	•	Frontend:
	•	Bibliothèque : React.js
	•	Style : TailwindCSS, Material-UI
	•	Routage et gestion des états : React Router
	•	Communication : WebSockets et API REST
	•	Sécurité:
	•	Authentification : JWT, OAuth 2.0
	•	Sécurisation des communications via HTTPS

Structure du Projet

├── backend/                  # Code source du backend en Scala
│   ├── build.sbt
│   ├── Dockerfile
├── project/                  # Configuration SBT
│   ├── build.properties
│   └── plugins.sbt
├── src/                      # Code source Scala
│   ├── main/
│   │   ├── scala/
│   │   │   └── com/portfolio/...
│   │   ├── resources/
│   │       ├── application.conf
│   │       └── logback.xml
│   ├── test/
│       └── scala/
│           └── com/portfolio/
├── frontend/                 # Code source du frontend React
│   ├── src/
│   │   ├── components/       # Composants React (ex. AddAsset, SellAsset, DashboardBox, etc.)
│   │   ├── pages/            # Pages de l'application (ex. LoginPage, Dashboard, MarketPage, etc.)
│   │   ├── services/
│   │   ├── assets/
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   ├── theme.ts
│   │   └── expanded-theme.ts
│   ├── public/
│   ├── package.json
│   ├── tailwind.config.js
│   └── Dockerfile
└── database/                 # Scripts SQL pour la création et configuration de la base de données
    └── schema.sql            # Schéma de la base de données

Installation et Déploiement

Prérequis
	•	Backend : Java, Scala, SBT, Docker 
	•	Frontend : Node.js, npm ou yarn, Docker
	•	Base de données : PostgreSQL

Backend
	1.	Installation des dépendances :
	•	Assurez-vous d’avoir Java et SBT installés.
	•	Dans le dossier backend, compilez le projet :



	•	Avec Docker :

docker-compose up --build

Puis aller sur le localhost:3000

Utilisation
	•	Authentification : Les utilisateurs peuvent s’inscrire et se connecter via les pages dédiées (/register et /login). Un token JWT est généré pour chaque session.
	•	Gestion de Portefeuille : Création, consultation et gestion des portefeuilles. Possibilité d’ajouter ou de vendre des actifs et de consulter l’historique des transactions.
	•	Données de Marché : Affichage en temps réel des données de marché via WebSockets et API REST.
	•	Interface Utilisateur : Navigation intuitive entre les différents modules (dashboard, marché, dépôt, etc.) grâce à une interface responsive.
