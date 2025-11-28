# GitLab Deployment Manager

Application Spring Boot permettant de gérer les déploiements en coopération avec une instance GitLab d'entreprise.

## Table des matières

- [Fonctionnalités](#fonctionnalités)
- [Démarrage rapide](#démarrage-rapide)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Installation et lancement](#installation-et-lancement)
- [Synchronisation GitLab](#synchronisation-gitlab)
- [Déploiement Docker](#déploiement-docker)
- [Déploiement sur VM](#déploiement-sur-vm)
- [Utilisation](#utilisation)
- [Sécurité](#sécurité)
- [Développement](#développement)

---

## Fonctionnalités

### Pour tous les utilisateurs (non authentifiés)
- **Visualisation des versions** : Consultation rapide des versions des applications sur la branche `develop`
- **Demande de déploiement** : Formulaire permettant de demander le déploiement d'applications par email à l'équipe de déploiement

### Pour l'administrateur
- **Gestion du Code Freeze** :
  - Activation/désactivation du code freeze global
  - Gel/dégel d'applications spécifiques
  - Protection de certaines applications pendant les périodes de test

---

## Démarrage rapide

### Mode Démo (sans configuration)

L'application peut démarrer en mode DÉMO sans variables d'environnement pour valider l'architecture :

```bash
# 1. Compiler l'application
mvn clean package

# 2. Lancer l'application
./start.sh
```

L'application démarrera sur **http://localhost:8080** avec :
- Identifiants admin par défaut : `admin` / `admin123`
- Navigation et interface fonctionnelles
- Fonctionnalités GitLab et Email simulées

### Mode Production (configuration complète)

1. **Configurer les variables d'environnement**

```bash
cp .env.example .env
# Éditer .env avec vos valeurs
```

2. **Compiler et lancer**

```bash
mvn clean package
./start.sh
```

3. **Accéder à l'application**
- Home : http://localhost:8080
- Versions : http://localhost:8080/versions
- Déploiement : http://localhost:8080/deployment
- Admin : http://localhost:8080/admin/login

---

## Architecture

### Technologies utilisées
- **Spring Boot 3.5.7** - Framework principal
- **Spring Security** - Authentification admin
- **Thymeleaf** - Templates HTML
- **Spring Mail** - Envoi d'emails
- **WebClient** - Appels API GitLab
- **Java 21** - Version Java

### Structure du projet
```
src/
├── main/
│   ├── java/com/deployment/gitlab/
│   │   ├── config/           # Configuration (GitLab, Security)
│   │   ├── controller/       # Contrôleurs web
│   │   ├── model/            # Modèles de données
│   │   ├── repository/       # Repositories (en mémoire)
│   │   ├── service/          # Services métier
│   │   └── DeploymentManagerApplication.java
│   └── resources/
│       ├── templates/        # Templates Thymeleaf
│       ├── static/           # CSS, JS
│       ├── application.yml   # Configuration Spring
│       └── applications.yaml # Liste des applications
└── test/                     # Tests unitaires
```

### Caractéristiques clés

- **Pas de base de données** : Toutes les données en mémoire
- **Admin unique** : Configuré via variables d'environnement
- **API GitLab v4** : Compatible GitLab 18.4.1-ee
- **Notifications email** : Envoi de demandes de déploiement par email
- **Code Freeze** : L'admin peut bloquer les déploiements pendant les tests

---

## Configuration

### Variables d'environnement requises

Créez un fichier `.env` ou définissez les variables suivantes :

```bash
# GitLab
GITLAB_URL=https://gitlab.entreprise.com
GITLAB_TOKEN=votre_token_gitlab

# Admin
ADMIN_USERNAME=admin
ADMIN_PASSWORD=votre_mot_de_passe_securise

# SMTP (Email) - SMTP_HOST obligatoire, auth optionnel
SMTP_HOST=smtp.entreprise.com
SMTP_PORT=587
SMTP_USERNAME=  # Optionnel si pas d'auth requise
SMTP_PASSWORD=  # Optionnel si pas d'auth requise
SMTP_AUTH=false
SMTP_STARTTLS=false
SMTP_FROM=noreply@deployment-manager.com

# Déploiement
DEPLOYMENT_EMAIL=equipe-deploy@entreprise.com

# Serveur (optionnel)
SERVER_PORT=8080

# Synchronisation GitLab (optionnel)
GITLAB_GROUPS=team/backend,team/frontend
GITLAB_SYNC_BRANCH=develop
```

### Configuration des applications

**Option 1 : Synchronisation automatique** (recommandé)

Configurez `GITLAB_GROUPS` dans `.env` pour synchroniser automatiquement les applications au démarrage :

```bash
GITLAB_GROUPS=team/backend,team/frontend
GITLAB_SYNC_BRANCH=develop
```

**Option 2 : Configuration manuelle**

Modifiez le fichier `src/main/resources/applications.yaml` :

```yaml
applications:
  - name: "Mon Application"
    description: "Description de l'application"
    gitlabProjectId: 123
    gitlabProjectPath: "namespace/project-name"
    branch: "develop"
    enabled: true
    freezable: true
```

**Paramètres :**
- `name` : Nom affiché de l'application
- `description` : Description courte
- `gitlabProjectId` : ID numérique du projet dans GitLab
- `gitlabProjectPath` : Chemin complet (namespace/project)
- `branch` : Branche à surveiller (généralement "develop")
- `enabled` : Si `false`, l'application n'apparaît pas dans l'interface
- `freezable` : Si `true`, peut être gelée pendant les code freeze

### Token GitLab

Créez un Personal Access Token dans GitLab avec les permissions :
- `read_api` : Lecture des projets et branches
- `read_repository` : Lecture des commits

---

## Installation et lancement

### Prérequis
- Java 21 ou supérieur
- Maven 3.6+
- Accès à une instance GitLab (18.4.1-ee)
- Serveur SMTP pour l'envoi d'emails

### Compilation

```bash
mvn clean package
```

### Lancement en local

```bash
# Avec Maven
mvn spring-boot:run

# Avec le JAR
java -jar target/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar

# Avec le script de démarrage
./start.sh
```

### Arrêt de l'application

```bash
./stop.sh
```

L'application sera accessible sur `http://localhost:8080`

---

## Synchronisation GitLab

### Fonctionnement

L'application peut synchroniser automatiquement la liste des applications depuis des groupes GitLab :

1. **Au démarrage**, si `GITLAB_GROUPS` est configuré, le script `sync-gitlab-apps.sh` s'exécute
2. Le script **récupère les projets** des groupes GitLab spécifiés via l'API
3. Il **génère** le fichier `applications.yaml` avec tous les projets trouvés
4. En cas d'échec, l'application utilise `applications.default.yaml` comme fallback

### Configuration automatique

Ajoutez dans votre fichier `.env` :

```bash
# Connexion GitLab (requis)
GITLAB_URL=https://gitlab.company.com
GITLAB_TOKEN=glpat-your-token

# Configuration de la synchronisation
GITLAB_GROUPS=team/backend,team/frontend
GITLAB_SYNC_BRANCH=develop
```

### Synchronisation manuelle

```bash
# Charger les variables d'environnement
source .env

# Synchroniser depuis des groupes spécifiques
./sync-gitlab-apps.sh "team/backend,team/frontend"

# Synchroniser tous les projets d'un groupe
./sync-gitlab-apps.sh "my-organization"

# Synchroniser avec une branche personnalisée
./sync-gitlab-apps.sh "team/*" main
```

### Format de GITLAB_GROUPS

La variable `GITLAB_GROUPS` accepte une liste de chemins de groupes GitLab séparés par des virgules :

```bash
# Groupe unique
GITLAB_GROUPS=team/backend

# Groupes multiples
GITLAB_GROUPS=team/backend,team/frontend,infra/tools

# Groupe niveau organisation
GITLAB_GROUPS=my-company

# Groupes imbriqués
GITLAB_GROUPS=company/engineering/backend,company/engineering/frontend
```

### Trouver les chemins de groupes

1. Allez sur votre instance GitLab
2. Naviguez vers **Groups**
3. Sélectionnez votre groupe
4. L'URL affiche le chemin du groupe : `https://gitlab.com/<group-path>`
5. Utilisez ce chemin dans `GITLAB_GROUPS`

### Prérequis pour la synchronisation

Le script de synchronisation nécessite :
- `curl` - Pour les appels API
- `jq` - Pour le parsing JSON

Installation sur Ubuntu/Debian :
```bash
sudo apt install curl jq
```

Installation sur RHEL/CentOS :
```bash
sudo dnf install curl jq
```

### Mode anonyme pour le débogage

Pour masquer les informations sensibles lors du débogage :

```bash
export ANONYMOUS_MODE=true
./sync-gitlab-apps.sh "your-groups" develop 2>&1 | tee output.log
```

---

## Déploiement Docker

### Option 1 : Docker Compose (recommandé)

1. **Configurer les variables d'environnement**

Créez un fichier `.env` à la racine du projet :

```bash
# GitLab Configuration
GITLAB_URL=https://your-gitlab-instance.com
GITLAB_TOKEN=your-personal-access-token

# Admin Credentials
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your-secure-password

# Email Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@example.com
SMTP_PASSWORD=your-email-password
DEPLOYMENT_EMAIL=deployment-team@example.com
```

2. **Lancer l'application**

```bash
docker-compose up -d
```

3. **Voir les logs**

```bash
docker-compose logs -f
```

4. **Arrêter l'application**

```bash
docker-compose down
```

### Option 2 : Docker seul

1. **Build l'image**

```bash
docker build -t gitlab-deployment-manager:latest .
```

2. **Lancer le container**

```bash
docker run -d \
  --name gitlab-deployment-manager \
  -p 8080:8080 \
  -e GITLAB_URL=https://your-gitlab-instance.com \
  -e GITLAB_TOKEN=your-token \
  -e ADMIN_USERNAME=admin \
  -e ADMIN_PASSWORD=your-password \
  -v $(pwd)/src/main/resources/application.yml:/app/config/application.yml:ro \
  gitlab-deployment-manager:latest
```

3. **Voir les logs**

```bash
docker logs -f gitlab-deployment-manager
```

### Optimisations Docker

#### Paramètres JVM

Ajustez les paramètres JVM selon vos besoins :

```bash
docker run -d \
  -e JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC" \
  gitlab-deployment-manager:latest
```

#### Health Check

L'application expose un endpoint de santé :

```
http://localhost:8080/actuator/health
```

#### Image utilisée

L'application utilise l'image officielle Oracle :

```
container-registry.oracle.com/java/jdk:21-oraclelinux8
```

---

## Déploiement sur VM

### Prérequis sur la VM

#### Logiciels requis
- **Java 21** ou supérieur
- **Systemd** (pour gérer l'application comme un service)
- Connexion réseau vers :
  - Instance GitLab
  - Serveur SMTP
  - Port 8080 accessible (ou port personnalisé)

#### Installation de Java 21

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jre

# RHEL/CentOS/Rocky
sudo dnf install java-21-openjdk

# Vérifier l'installation
java -version
```

### Méthode 1 : Déploiement Manuel

#### 1. Créer un utilisateur dédié

```bash
sudo useradd -m -s /bin/bash deployment-manager
sudo su - deployment-manager
```

#### 2. Créer la structure de dossiers

```bash
mkdir -p ~/app
mkdir -p ~/logs
```

#### 3. Transférer le JAR

```bash
# Depuis votre machine locale
scp target/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar user@vm-host:~/app/

# Ou compiler directement sur la VM
git clone <repository-url>
cd poc-gitlab-operator
mvn clean package
cp target/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar ~/app/
```

#### 4. Créer le fichier de configuration

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

#### 5. Créer le fichier de variables d'environnement

Créez le fichier `~/app/.env` et protégez-le :

```bash
chmod 600 ~/app/.env
```

#### 6. Créer les scripts de démarrage/arrêt

**Script de démarrage** (`~/app/start.sh`) :

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

**Script d'arrêt** (`~/app/stop.sh`) :

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

Rendre les scripts exécutables :
```bash
chmod +x ~/app/start.sh ~/app/stop.sh
```

### Méthode 2 : Service Systemd (Recommandé)

#### 1. Créer le fichier de service

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

#### 2. Activer et démarrer le service

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

#### 3. Commandes utiles

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

### Configuration du Pare-feu

**Avec firewalld :**

```bash
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

**Avec ufw :**

```bash
sudo ufw allow 8080/tcp
sudo ufw reload
```

### Configuration Nginx (Reverse Proxy)

#### 1. Installer Nginx

```bash
sudo apt install nginx  # Ubuntu/Debian
sudo dnf install nginx  # RHEL/CentOS
```

#### 2. Créer la configuration

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

#### 3. Activer la configuration

```bash
sudo ln -s /etc/nginx/sites-available/deployment-manager /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### Mise à jour de l'application

```bash
# 1. Arrêter l'application
sudo systemctl stop deployment-manager

# 2. Sauvegarder l'ancienne version
cd /home/deployment-manager/app
mv gitlab-deployment-manager-1.0.0-SNAPSHOT.jar gitlab-deployment-manager-1.0.0-SNAPSHOT.jar.backup

# 3. Transférer la nouvelle version
scp target/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar user@vm-host:/home/deployment-manager/app/

# 4. Redémarrer l'application
sudo systemctl start deployment-manager
sudo systemctl status deployment-manager
```

### Monitoring

#### Vérifier l'état de l'application

```bash
# Via systemd
sudo systemctl status deployment-manager

# Via curl
curl http://localhost:8080

# Logs en temps réel
tail -f /home/deployment-manager/logs/application.log
```

#### Vérifier l'utilisation des ressources

```bash
# CPU et mémoire
ps aux | grep java

# Détails avec top
top -p $(pgrep -f deployment-manager)
```

### Performance

Pour une VM avec des ressources limitées, ajustez les paramètres JVM :

```ini
ExecStart=/usr/bin/java -Xmx512m -Xms256m -jar /home/deployment-manager/app/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar
```

**Paramètres recommandés :**
- **Minimum** : 512MB RAM, 1 CPU
- **Recommandé** : 1GB RAM, 2 CPU

---

## Utilisation

### Interface publique

1. **Page d'accueil** (`/`) : Vue d'ensemble de l'application
2. **Versions** (`/versions`) : Liste des versions actuelles sur develop
3. **Déploiement** (`/deployment`) : Formulaire de demande de déploiement

### Interface admin

1. Se connecter sur `/admin/login` avec les identifiants configurés
2. **Dashboard** (`/admin/dashboard`) : Vue d'ensemble de l'état du système
3. **Code Freeze** (`/admin/code-freeze`) : Gestion des gels de déploiement

### Intégration GitLab

L'application utilise l'API REST GitLab v4 (version 18.4.1-ee).

**Endpoints utilisés :**
- `GET /api/v4/projects/:id` - Récupération d'un projet
- `GET /api/v4/projects/:id/repository/branches/:branch` - Récupération d'une branche
- `GET /api/v4/projects/:id/repository/commits/:sha` - Récupération d'un commit

---

## Sécurité

### Bonnes pratiques

- Les pages publiques sont accessibles sans authentification
- L'interface admin nécessite une authentification (nom d'utilisateur/mot de passe)
- Un seul compte admin configuré via variables d'environnement
- Aucune base de données (données en mémoire uniquement)
- Token GitLab stocké de manière sécurisée dans les variables d'environnement

### Recommandations

- ❌ **Ne jamais committer le fichier `.env`**
- ✅ Protéger le fichier `.env` avec `chmod 600`
- ✅ Utiliser un mot de passe fort pour l'admin
- ✅ Utiliser HTTPS avec un certificat SSL (via Nginx + Let's Encrypt)
- ✅ Restreindre l'accès à la VM via firewall
- ✅ Utiliser un token GitLab avec les permissions minimales (`read_api`, `read_repository`)
- ✅ L'application s'exécute avec un utilisateur non-root dans Docker

---

## Développement

### Lancer en mode développement

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Le mode développement active :
- Hot reload des templates
- Logs de debug
- DevTools

### Tests

```bash
mvn test
```

### Dépannage

#### L'application ne démarre pas

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

#### Problème de connexion à GitLab

1. Tester la connectivité :
```bash
curl -H "PRIVATE-TOKEN: your-token" https://gitlab.entreprise.com/api/v4/version
```

2. Vérifier le token GitLab dans `.env`

#### Problème d'envoi d'emails

1. Tester la connexion SMTP :
```bash
telnet smtp.entreprise.com 587
```

2. Vérifier les credentials SMTP dans `.env`

### Sauvegarde

**Éléments à sauvegarder :**
- `/home/deployment-manager/app/.env` - Variables d'environnement
- `/home/deployment-manager/app/application-prod.yml` - Configuration
- `src/main/resources/applications.yaml` - Liste des applications

**Script de sauvegarde :**

```bash
#!/bin/bash
BACKUP_DIR="/backup/deployment-manager/$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR
cp /home/deployment-manager/app/.env $BACKUP_DIR/
cp /home/deployment-manager/app/application-prod.yml $BACKUP_DIR/
echo "Sauvegarde effectuée dans $BACKUP_DIR"
```

---

## Support

Pour toute question ou problème :
1. Vérifiez la configuration des variables d'environnement
2. Consultez les logs de l'application
3. Vérifiez la connectivité avec GitLab et le serveur SMTP

## Licence

Propriétaire - Usage interne uniquement
