# FT Starter

POC de première inscription France Travail avec un frontend React/Vite et un backend Java 21 / Spring Boot 3.5. Le backend reçoit et conserve temporairement les documents, puis délègue leur analyse au workflow n8n. Il ne contient aucun OCR, LLM ni moteur documentaire.

## Backend

Prérequis : Java 21 et Maven 3.9+.

```sh
mvn spring-boot:run
```

Le backend écoute sur `http://localhost:8080`. L'interface Swagger UI est disponible sur [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html), la santé sur [`/actuator/health`](http://localhost:8080/actuator/health) et les informations sur [`/actuator/info`](http://localhost:8080/actuator/info).

### APIs

- `POST /api/v1/documents/upload` — multipart avec un ou plusieurs champs `files[]`. Maximum 20 fichiers, PDF/PNG/JPG/JPEG, 20 Mo par fichier. Répond avec l'`uploadId`, le statut et le nombre de fichiers.
- `POST /api/v1/analysis/{uploadId}` — lit les documents stockés, les encode en Base64, appelle `POST {n8n.base-url}/document-analysis`, puis renvoie la réponse normalisée.
- `GET /api/v1/analysis/{uploadId}` — consulte le résultat en mémoire ; renvoie `404 ANALYSIS_NOT_FOUND` avant l'analyse.

Les erreurs ont le format `{ timestamp, code, message, details }`. Les réponses d'analyse n8n sont normalisées vers `confidenceScore`, `documentsDetected` et `applicant`, avec `employmentCertificates` pour les attestations employeur.

### Configuration

Valeurs par défaut dans `src/main/resources/application.yml` :

- `app.storage.root-path=uploads` : répertoire local des fichiers, organisé par upload.
- `app.n8n.base-url=http://localhost:5678/webhook` : URL de base ; le backend appelle `/document-analysis`.
- `app.n8n.timeout=30s` : délai maximal de l'analyse.
- Resilience4j retente automatiquement les erreurs réseau, timeouts et erreurs serveur n8n.

Les variables `APP_STORAGE_ROOT_PATH`, `N8N_BASE_URL`, `N8N_TIMEOUT` et `SERVER_PORT` permettent de surcharger les principaux paramètres.

Les sessions de dépôt et les résultats d'analyse sont conservés dans des `ConcurrentHashMap` et sont perdus au redémarrage. Les fichiers restent dans `uploads/`. Aucune base de données n'est utilisée.

Le serveur MVC est nécessaire pour les API et les tests MockMvc ; `spring-boot-starter-webflux` est utilisé pour le client sortant `WebClient` vers n8n.

### Tests

```sh
mvn test
```

Les tests couvrent les cas d'usage, le stockage, l'API avec MockMvc, la normalisation n8n et les retries Resilience4j. Le rapport JaCoCo est généré dans `target/site/jacoco/`.

## Frontend

Depuis `client/` :

```sh
npm install
npm run dev
```

Le frontend utilise le dépôt de documents et l'analyse (`POST /api/v1/documents/upload`, `POST /api/v1/analysis/{uploadId}`) via Axios. En développement, Vite relaie `/api` vers le backend `http://localhost:8080`. La soumission finale du dossier n'est pas encore exposée par le backend. Voir [client/README.md](./client/README.md).
