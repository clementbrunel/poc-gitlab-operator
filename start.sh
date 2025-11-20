#!/bin/bash

# Script de démarrage de GitLab Deployment Manager

set -e

echo "==================================="
echo "GitLab Deployment Manager - START"
echo "==================================="

# Vérifier que Java est installé
if ! command -v java &> /dev/null; then
    echo "❌ Erreur: Java n'est pas installé"
    echo "Installez Java 17 ou supérieur"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
echo "✓ Java version: $JAVA_VERSION"

if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "⚠️  Avertissement: Java 17+ recommandé (version actuelle: $JAVA_VERSION)"
fi

# Vérifier que le JAR existe
JAR_FILE="target/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Erreur: $JAR_FILE n'existe pas"
    echo "Compilez d'abord l'application avec: mvn clean package"
    exit 1
fi

# Charger les variables d'environnement depuis .env si le fichier existe
if [ -f .env ]; then
    echo "✓ Chargement des variables depuis .env"
    set -a
    source .env
    set +a
else
    echo "⚠️  Fichier .env non trouvé - l'application démarrera en mode DÉMO"
    echo "   Copiez .env.example vers .env et configurez les variables"
fi

# Créer le dossier de logs s'il n'existe pas
mkdir -p logs

# Démarrer l'application
echo ""
echo "🚀 Démarrage de l'application..."
echo ""

java -jar "$JAR_FILE" > logs/application.log 2>&1 &
APP_PID=$!

# Sauvegarder le PID
echo $APP_PID > app.pid

echo "✓ Application démarrée (PID: $APP_PID)"
echo ""
echo "📝 Logs: tail -f logs/application.log"
echo "🌐 URL: http://localhost:${SERVER_PORT:-8080}"
echo "🛑 Arrêt: ./stop.sh"
echo ""
echo "==================================="
