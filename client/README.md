# FT Starter

POC front-end React 19, TypeScript et Vite pour préparer un dossier d'inscription. Le dépôt des documents et leur analyse utilisent le backend Spring Boot. Le formulaire peut être complété et validé localement, mais l'API ne propose pas encore de route de soumission du dossier.

## Démarrer

Depuis le dossier `client` :

```sh
npm install
npm run dev
```

Commandes de vérification :

```sh
npm run build
npm run lint
```

## Parcours

- Déposez jusqu'à 20 fichiers PDF, JPG ou PNG de 20 Mo maximum chacun. Le front les envoie à `POST /api/v1/documents/upload`, puis transmet l'`uploadId` retourné à `POST /api/v1/analysis/{uploadId}`.
- Les données retournées par l'analyse pré-remplissent les champs connus. Pour les attestations employeur, les champs professionnels sont également alimentés par l'attestation la plus récente et deviennent obligatoires.
- Les diplômes extraits sont affichés dans une liste modifiable sans limite, triée de l'année d'obtention la plus récente à la plus ancienne. Il est possible d'ajouter ou de supprimer des diplômes.
- Les erreurs renvoyées par le backend (code, message et détails) ainsi que les erreurs réseau et de délai d'attente sont affichées dans l'interface. Le dépôt ou l'analyse en échec peuvent être relancés.
- La soumission vérifie les champs obligatoires côté client. Aucune route backend ne permet encore de transmettre le dossier final.
- Le commutateur du bandeau permet de passer en thème sombre.

## Connexion au backend

Lancer le backend à la racine du dépôt (`mvn spring-boot:run`), puis le frontend (`npm run dev`). Le serveur Vite relaie `/api` vers `http://localhost:8080`. Pour utiliser une autre URL, définir `VITE_API_BASE_URL` (par exemple `http://localhost:8080/api`).

## Organisation

- `src/components` : composants UI réutilisables.
- `src/constants` et `src/types` : libellés, options et contrats TypeScript.
- `src/features/upload` et `src/features/registration-form` : fonctionnalités du parcours.
- `src/hooks` : logique de formulaire et progression.
- `src/layouts` et `src/pages` : structure de page.
- `src/services/api.ts` : client Axios et contrats des routes backend.
- `src/services/applicantMapper.ts` : adaptation du modèle Applicant backend vers le formulaire.

TanStack Query reste préconfiguré pour les évolutions futures.
