FROM openjdk:11-jre-slim

WORKDIR /app

# Copie des fichiers du projet
COPY project ./project
COPY src ./src
COPY build.sbt ./

# Mise à jour et installation de curl et unzip, téléchargement et extraction de sbt
RUN apt-get update && apt-get install -y curl unzip && \
    curl -L -o sbt.zip https://github.com/sbt/sbt/releases/download/v1.5.5/sbt-1.5.5.zip && \
    unzip sbt.zip -d /usr/local && \
    rm sbt.zip

# Ajout de sbt dans le PATH
ENV PATH="/usr/local/sbt/bin:${PATH}"

# Compilation du projet
RUN sbt compile

EXPOSE 8080

CMD ["sbt", "run"]