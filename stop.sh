#!/bin/bash

# Script d'arrêt de GitLab Deployment Manager

# Force UTF-8 encoding
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

echo "==================================="
echo "GitLab Deployment Manager - STOP"
echo "==================================="

if [ ! -f app.pid ]; then
    echo "❌ Fichier app.pid non trouvé"
    echo "L'application ne semble pas être en cours d'exécution"
    exit 1
fi

APP_PID=$(cat app.pid)

if ! ps -p $APP_PID > /dev/null 2>&1; then
    echo "❌ Processus $APP_PID n'existe pas"
    rm app.pid
    exit 1
fi

echo "🛑 Arrêt de l'application (PID: $APP_PID)..."
kill $APP_PID

# Attendre que le processus se termine
for i in {1..10}; do
    if ! ps -p $APP_PID > /dev/null 2>&1; then
        break
    fi
    sleep 1
done

# Vérifier si le processus est toujours actif
if ps -p $APP_PID > /dev/null 2>&1; then
    echo "⚠️  Le processus ne s'est pas arrêté, force l'arrêt..."
    kill -9 $APP_PID
fi

rm app.pid
echo "✓ Application arrêtée"
echo "==================================="
