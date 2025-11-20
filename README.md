# GitLab Deployment Manager

Application Spring Boot permettant de gérer les déploiements en coopération avec une instance GitLab d'entreprise.

## Fonctionnalités

### Pour tous les utilisateurs (non authentifiés)
- **Visualisation des versions** : Consultation rapide des versions des applications sur la branche `develop`
- **Demande de déploiement** : Formulaire permettant de demander le déploiement d'applications par email à l'équipe de déploiement

### Pour l'administrateur
- **Gestion du Code Freeze** :
  - Activation/désactivation du code freeze global
  - Gel/dégel d'applications spécifiques
  - Protection de certaines applications pendant les périodes de test

## Architecture

### Technologies utilisées
- **Spring Boot 3.5.7** - Framework principal
- **Spring Security** - Authentification admin
- **Thymeleaf** - Templates HTML
- **Spring Mail** - Envoi d'emails
- **WebClient** - Appels API GitLab
- **Java 25** - Version Java

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
│       ├── applications.yaml # Liste des applications
│       └── gitlab-openapi-v2.yaml # Spec API GitLab 18.4.1
└── test/                     # Tests unitaires
```

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

# SMTP (Email)
SMTP_HOST=smtp.entreprise.com
SMTP_PORT=587
SMTP_USERNAME=user@entreprise.com
SMTP_PASSWORD=password_smtp
SMTP_FROM=noreply@deployment-manager.com

# Déploiement
DEPLOYMENT_EMAIL=equipe-deploy@entreprise.com

# Serveur (optionnel)
SERVER_PORT=8080
```

### Configuration des applications

Modifiez le fichier `src/main/resources/applications.yaml` pour ajouter vos applications :

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

## Installation et lancement

### Prérequis
- Java 25 ou supérieur
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
```

L'application sera accessible sur `http://localhost:8080`

### Lancement sur une VM

Voir le fichier [DEPLOYMENT.md](DEPLOYMENT.md) pour les instructions de déploiement sur une VM.

## Utilisation

### Interface publique

1. **Page d'accueil** (`/`) : Vue d'ensemble de l'application
2. **Versions** (`/versions`) : Liste des versions actuelles sur develop
3. **Déploiement** (`/deployment`) : Formulaire de demande de déploiement

### Interface admin

1. Se connecter sur `/admin/login` avec les identifiants configurés
2. **Dashboard** (`/admin/dashboard`) : Vue d'ensemble de l'état du système
3. **Code Freeze** (`/admin/code-freeze`) : Gestion des gels de déploiement

## Intégration GitLab

L'application utilise l'API REST GitLab v4 (version 18.4.1-ee).

### Token GitLab requis

Créez un Personal Access Token dans GitLab avec les permissions :
- `read_api` : Lecture des projets et branches
- `read_repository` : Lecture des commits

### Endpoints utilisés
- `GET /api/v4/projects/:id` - Récupération d'un projet
- `GET /api/v4/projects/:id/repository/branches/:branch` - Récupération d'une branche
- `GET /api/v4/projects/:id/repository/commits/:sha` - Récupération d'un commit

La spécification OpenAPI complète est disponible dans `src/main/resources/gitlab-openapi-v2.yaml`.

## Sécurité

- Les pages publiques sont accessibles sans authentification
- L'interface admin nécessite une authentification (nom d'utilisateur/mot de passe)
- Un seul compte admin configuré via variables d'environnement
- Aucune base de données (données en mémoire uniquement)
- Token GitLab stocké de manière sécurisée dans les variables d'environnement

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

## Support

Pour toute question ou problème :
1. Vérifiez la configuration des variables d'environnement
2. Consultez les logs de l'application
3. Vérifiez la connectivité avec GitLab

## Licence

Propriétaire - Usage interne uniquement