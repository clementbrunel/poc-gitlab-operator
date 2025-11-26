# Docker Deployment Guide

## Build et lancement avec Docker

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
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@example.com
MAIL_PASSWORD=your-email-password
DEPLOYMENT_EMAIL_TO=deployment-team@example.com
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
  -v $(pwd)/src/main/resources/applications.yml:/app/config/applications.yml:ro \
  gitlab-deployment-manager:latest
```

3. **Voir les logs**

```bash
docker logs -f gitlab-deployment-manager
```

4. **Arrêter le container**

```bash
docker stop gitlab-deployment-manager
docker rm gitlab-deployment-manager
```

## Configuration

### Volumes

Le Dockerfile copie automatiquement le fichier `application.yml` dans l'image, mais vous pouvez le surcharger avec un volume mount :

```yaml
volumes:
  - ./custom-application.yml:/app/config/application.yml:ro
```

### Variables d'environnement

Toutes les propriétés Spring Boot peuvent être surchargées avec des variables d'environnement :

```bash
# Format : SPRING_PROPERTY_NAME (remplacer . par _ et mettre en majuscules)
-e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/mydb
-e SPRING_PROFILES_ACTIVE=prod
```

### Health Check

L'application expose un endpoint de santé pour Docker :

```
http://localhost:8080/actuator/health
```

## Optimisations JVM

Ajustez les paramètres JVM selon vos besoins :

```bash
-e JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
```

## Image Oracle OpenJDK 21

L'application utilise l'image officielle Oracle :

```
container-registry.oracle.com/java/jdk:21-oraclelinux8
```

## Sécurité

- L'application s'exécute avec un utilisateur non-root (`appuser`)
- Le port 8080 est exposé par défaut
- Utilisez des secrets Docker pour les données sensibles en production

## Troubleshooting

### L'application ne démarre pas

Vérifiez les logs :
```bash
docker logs gitlab-deployment-manager
```

### Impossible de se connecter à GitLab

Vérifiez que :
- L'URL GitLab est accessible depuis le container
- Le token a les bonnes permissions
- Les certificats SSL sont valides

### Problèmes de mémoire

Augmentez les limites JVM :
```bash
-e JAVA_OPTS="-Xms512m -Xmx1024m"
```
