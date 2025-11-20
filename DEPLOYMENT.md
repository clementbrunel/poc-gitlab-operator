# Guide de Déploiement sur VM

Ce guide explique comment déployer l'application GitLab Deployment Manager sur une machine virtuelle (VM) Linux.

## Prérequis sur la VM

### Logiciels requis
- **Java 17** ou supérieur
- **Systemd** (pour gérer l'application comme un service)
- Connexion réseau vers :
  - Instance GitLab
  - Serveur SMTP
  - Port 8080 accessible (ou port personnalisé)

### Installation de Java 17

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jre

# RHEL/CentOS/Rocky
sudo dnf install java-17-openjdk

# Vérifier l'installation
java -version
```

## Méthode 1 : Déploiement Manuel

### 1. Créer un utilisateur dédié

```bash
sudo useradd -m -s /bin/bash deployment-manager
sudo su - deployment-manager
```

### 2. Créer la structure de dossiers

```bash
mkdir -p ~/app
mkdir -p ~/logs
```

### 3. Transférer le JAR

```bash
# Depuis votre machine locale
scp target/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar user@vm-host:~/app/

# Ou compiler directement sur la VM
git clone <repository-url>
cd poc-gitlab-operator
mvn clean package
cp target/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar ~/app/
```

### 4. Créer le fichier de configuration

Créez le fichier `~/app/application-prod.yml` :

```yaml
spring:
  application:
    name: GitLab Deployment Manager

server:
  port: 8080

logging:
  level:
    root: INFO
  file:
    name: /home/deployment-manager/logs/application.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 5. Créer le fichier de variables d'environnement

Créez le fichier `~/app/.env` :

```bash
# GitLab
GITLAB_URL=https://gitlab.entreprise.com
GITLAB_TOKEN=votre_token_gitlab

# Admin
ADMIN_USERNAME=admin
ADMIN_PASSWORD=votre_mot_de_passe_securise

# SMTP
SMTP_HOST=smtp.entreprise.com
SMTP_PORT=587
SMTP_USERNAME=user@entreprise.com
SMTP_PASSWORD=password_smtp
SMTP_FROM=noreply@deployment-manager.com

# Déploiement
DEPLOYMENT_EMAIL=equipe-deploy@entreprise.com

# Serveur
SERVER_PORT=8080
```

Protégez le fichier :
```bash
chmod 600 ~/app/.env
```

### 6. Créer le script de démarrage

Créez le fichier `~/app/start.sh` :

```bash
#!/bin/bash

# Charger les variables d'environnement
set -a
source /home/deployment-manager/app/.env
set +a

# Lancer l'application
java -jar /home/deployment-manager/app/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar \
  --spring.config.additional-location=/home/deployment-manager/app/application-prod.yml \
  >> /home/deployment-manager/logs/application.log 2>&1 &

# Sauvegarder le PID
echo $! > /home/deployment-manager/app/app.pid

echo "Application démarrée (PID: $(cat /home/deployment-manager/app/app.pid))"
```

Rendre le script exécutable :
```bash
chmod +x ~/app/start.sh
```

### 7. Créer le script d'arrêt

Créez le fichier `~/app/stop.sh` :

```bash
#!/bin/bash

if [ -f /home/deployment-manager/app/app.pid ]; then
    PID=$(cat /home/deployment-manager/app/app.pid)
    kill $PID
    rm /home/deployment-manager/app/app.pid
    echo "Application arrêtée (PID: $PID)"
else
    echo "Fichier PID non trouvé"
fi
```

Rendre le script exécutable :
```bash
chmod +x ~/app/stop.sh
```

### 8. Démarrer l'application

```bash
~/app/start.sh
```

Vérifier que l'application fonctionne :
```bash
curl http://localhost:8080
tail -f ~/logs/application.log
```

## Méthode 2 : Service Systemd (Recommandé)

### 1. Créer le fichier de service

En tant que root, créez `/etc/systemd/system/deployment-manager.service` :

```ini
[Unit]
Description=GitLab Deployment Manager
After=network.target

[Service]
Type=simple
User=deployment-manager
WorkingDirectory=/home/deployment-manager/app
EnvironmentFile=/home/deployment-manager/app/.env
ExecStart=/usr/bin/java -jar /home/deployment-manager/app/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar --spring.config.additional-location=/home/deployment-manager/app/application-prod.yml
Restart=on-failure
RestartSec=10
StandardOutput=append:/home/deployment-manager/logs/application.log
StandardError=append:/home/deployment-manager/logs/application.log

[Install]
WantedBy=multi-user.target
```

### 2. Activer et démarrer le service

```bash
# Recharger systemd
sudo systemctl daemon-reload

# Activer le service au démarrage
sudo systemctl enable deployment-manager

# Démarrer le service
sudo systemctl start deployment-manager

# Vérifier le statut
sudo systemctl status deployment-manager
```

### 3. Commandes utiles

```bash
# Démarrer
sudo systemctl start deployment-manager

# Arrêter
sudo systemctl stop deployment-manager

# Redémarrer
sudo systemctl restart deployment-manager

# Voir les logs
sudo journalctl -u deployment-manager -f

# Voir le statut
sudo systemctl status deployment-manager
```

## Configuration du Pare-feu

Si vous utilisez `firewalld` :

```bash
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

Si vous utilisez `ufw` :

```bash
sudo ufw allow 8080/tcp
sudo ufw reload
```

## Configuration Nginx (Reverse Proxy)

Pour exposer l'application via Nginx :

### 1. Installer Nginx

```bash
sudo apt install nginx  # Ubuntu/Debian
sudo dnf install nginx  # RHEL/CentOS
```

### 2. Créer la configuration

Créez `/etc/nginx/sites-available/deployment-manager` :

```nginx
server {
    listen 80;
    server_name deployment.entreprise.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. Activer la configuration

```bash
sudo ln -s /etc/nginx/sites-available/deployment-manager /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## Mise à jour de l'application

### 1. Arrêter l'application

```bash
sudo systemctl stop deployment-manager
```

### 2. Sauvegarder l'ancienne version

```bash
cd /home/deployment-manager/app
mv gitlab-deployment-manager-1.0.0-SNAPSHOT.jar gitlab-deployment-manager-1.0.0-SNAPSHOT.jar.backup
```

### 3. Transférer la nouvelle version

```bash
scp target/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar user@vm-host:/home/deployment-manager/app/
```

### 4. Redémarrer l'application

```bash
sudo systemctl start deployment-manager
sudo systemctl status deployment-manager
```

## Monitoring

### Vérifier l'état de l'application

```bash
# Via systemd
sudo systemctl status deployment-manager

# Via curl
curl http://localhost:8080

# Logs en temps réel
tail -f /home/deployment-manager/logs/application.log
```

### Vérifier l'utilisation des ressources

```bash
# CPU et mémoire
ps aux | grep java

# Détails avec top
top -p $(pgrep -f deployment-manager)
```

## Sauvegarde

### Éléments à sauvegarder

- `/home/deployment-manager/app/.env` - Variables d'environnement
- `/home/deployment-manager/app/application-prod.yml` - Configuration
- `src/main/resources/applications.yaml` - Liste des applications

### Script de sauvegarde

```bash
#!/bin/bash
BACKUP_DIR="/backup/deployment-manager/$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR
cp /home/deployment-manager/app/.env $BACKUP_DIR/
cp /home/deployment-manager/app/application-prod.yml $BACKUP_DIR/
echo "Sauvegarde effectuée dans $BACKUP_DIR"
```

## Dépannage

### L'application ne démarre pas

1. Vérifier les logs :
```bash
sudo journalctl -u deployment-manager -n 50
tail -f /home/deployment-manager/logs/application.log
```

2. Vérifier les variables d'environnement :
```bash
sudo systemctl show deployment-manager --property=Environment
```

3. Vérifier que Java est installé :
```bash
java -version
```

### Problème de connexion à GitLab

1. Tester la connectivité :
```bash
curl -H "PRIVATE-TOKEN: your-token" https://gitlab.entreprise.com/api/v4/version
```

2. Vérifier le token GitLab dans `.env`

### Problème d'envoi d'emails

1. Tester la connexion SMTP :
```bash
telnet smtp.entreprise.com 587
```

2. Vérifier les credentials SMTP dans `.env`

## Sécurité

- Ne jamais committer le fichier `.env`
- Protéger le fichier `.env` avec `chmod 600`
- Utiliser un mot de passe fort pour l'admin
- Utiliser HTTPS avec un certificat SSL (via Nginx + Let's Encrypt)
- Restreindre l'accès à la VM via firewall
- Utiliser un token GitLab avec les permissions minimales

## Performance

Pour une VM avec des ressources limitées, ajustez les paramètres JVM dans le fichier service :

```ini
ExecStart=/usr/bin/java -Xmx512m -Xms256m -jar /home/deployment-manager/app/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar
```

Paramètres recommandés :
- **Minimum** : 512MB RAM, 1 CPU
- **Recommandé** : 1GB RAM, 2 CPU
