FROM openjdk:11-jre-slim

WORKDIR /app

# Installation de sbt et des dépendances nécessaires
RUN apt-get update && apt-get install -y curl unzip && \
    curl -L -o sbt.zip https://github.com/sbt/sbt/releases/download/v1.5.5/sbt-1.5.5.zip && \
    unzip sbt.zip -d /usr/local && \
    rm sbt.zip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Ajout de sbt dans le PATH
ENV PATH="/usr/local/sbt/bin:${PATH}"

# Nous ne copions pas les fichiers sources ici, ils seront montés via volumes
# Nous pouvons copier uniquement les fichiers de configuration de build
COPY build.sbt ./
COPY project ./project

# Téléchargement des dépendances à l'avance
RUN sbt update

EXPOSE 8080

CMD ["sbt", "~run"]