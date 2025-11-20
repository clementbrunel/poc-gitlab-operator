---
description: Vérifie la configuration de l'application (GitLab, SMTP, etc.)
---

Analyse les fichiers de configuration (application.yml et .env si présent) et vérifie que toutes les variables importantes sont configurées :
- Configuration GitLab (URL, token)
- Configuration SMTP pour les emails
- Variables d'authentification admin

Affiche un résumé de l'état de la configuration.