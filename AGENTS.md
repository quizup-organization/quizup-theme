# AGENTS.md — quizup-theme

> Service de **thèmes (topics) et questions** du quiz. Architecture : Axon Framework (CQRS/EDA) +
> JPA (projections). **Fournisseur** de données pour les autres services.
> Pour les règles de patterns : [
`../../best-practices/.backend/hexagonal-architecture.md`](../../best-practices/.backend/hexagonal-architecture.md).

---

## 1. Rôle

Gestion des **topics** (sujets) et des **questions** qui y sont rattachées : création,
recherche paginée, modération (approve/reject), publication d'un topic. **Fournisseur** de données pour `quizup-social`
(vérification d'existence de topic) et
`quizup-game` (questions aléatoires approuvées).

**Package** : `io.github.quizup.theme` (note : le repo s'appelle `theme`, le package est `theme`)

---

## 2. Surface (headless)

Service **headless** : aucun contrôleur REST ni WebSocket. La surface applicative unique est le
**`quizup-bff`** (`/api/**` + `/ws`) ; il interroge ce service via le **query bus** Axon et consomme
ses événements. Les handlers de requête/commande, sagas et projections restent la seule surface
exposée par le service.
## 3. Use cases (ports entrants — `domain/port/in/`)

- `CreateTopicUseCase` / `GetTopicUseCase` / `CheckTopicUseCase` / `SearchTopicUseCase` / `PublishTopicUseCase`
- `CreateQuestionUseCase` / `GetQuestionUseCase` / `SearchQuestionUseCase` / `GetRandomApprovedQuestionsUseCase`
- `ApproveQuestionUseCase` / `RejectQuestionUseCase`
- `CountApprovedQuestionsByTopicUseCase`

**Édition champ par champ (surface d'auteur du BFF)** : commandes directes sur les agrégats
(sans port `UseCase` dédié) — `TopicCommand.UpdateTopicName|Description|Category|Emoji|Color|ImageUrlCommand`
et `QuestionCommand.AddQuestionTranslation|UpdateQuestionText|Answers|CorrectAnswer|ImageUrlCommand`.
Toutes sont **propriétaire uniquement** (le `TopicAggregate` vérifie `requestedBy == creatorId` ;
les questions sont gardées côté BFF), **idempotentes** (aucun événement si la valeur est
inchangée) et validées par les règles (`TopicRules` : nom ≤ 25, description ≤ 500, emoji/couleur
≤ 16, URL ≤ 1024 ; `QuestionRules` : texte ≤ 255, URL ≤ 1024, 4 réponses, bonne réponse présente
dans chaque langue). La publication vérifie aussi le propriétaire.

---

## 4. Dépendances inter-services

**Aucune dépendance sortante** au niveau contrat (queries). Theme est un **fournisseur** :

- `quizup-social` → `TopicRepositoryPort` → `TopicQuery.TopicExistsByIdQuery`
- `quizup-game` → `QuestionRepositoryPort` → `QuestionQuery.GetRandomApprovedQuestionsQuery(topicId, count, languages)`
  (sélection **stricte** : la question doit avoir un contenu dans **toutes** les langues demandées ;
  la `Question` retournée porte ses contenus localisés, le client choisit le sien)
- `quizup-matchmaking` / `quizup-social` → `QuestionQuery.CountApprovedQuestionsByTopicAndLanguagesQuery`
  (gardes de disponibilité linguistique avant d'ouvrir/apparier/valider un duel)

**Conso (événements)** : `quizup-theme-infrastructure` → `quizup-game-domain` (artifact Maven) —
`QuestionDifficultyProjection` (`@EventHandler`) consomme `GameEvent.QuestionAnsweredEvent` du bus
partagé. Seules les réponses **humaines** (`playerType == HUMAN`) sont comptées : BOT et GHOST sont
ignorés ; une non-réponse (timeout) arrive en réponse fausse et compte comme incorrecte. Les
compteurs sont stockés dans `question_answer_stats` (port `QuestionAnswerStatsRepositoryPort`) ;
lorsque la difficulté calculée change, la projection envoie une
`UpdateQuestionDifficultyCommand` à l'agrégat (la difficulté est un attribut de
`QuestionAggregate`, exposé ensuite via `Question`).

**Commandes/événements de difficulté** : `QuestionCommand.UpdateQuestionDifficultyCommand` +
`QuestionEvent.QuestionDifficultyUpdatedEvent` (no-op si la valeur est inchangée).

**Ports sortants locaux** : `TopicRepositoryPort`, `QuestionRepositoryPort`,
`QuestionAnswerStatsRepositoryPort` (persistance).

### Enrichissements catalogue

- `TopicQuery.GetTopicPageQuery(nameQuery, category, sort, page, size)` → `TopicPage` : page du
  catalogue publié (tri `POPULAR|ALPHA`), filtre texte normalisé côté handler.
- `TopicQuery.GetTopicsByCreatorQuery(creatorId, page, size)` → `TopicPage` : sujets créés par un
  utilisateur (tous statuts, brouillons compris), tri `updatedAt desc` — alimente la vue
  « mes sujets » du BFF (`?mine=true`).
- `TopicQuery.TopicFacetsQuery(nameQuery, topicIds)` → `List<TopicFacetCount>` : compteurs par
  catégorie (facettes) appliqués aux mêmes filtres, `topicIds` pour le périmètre « suivis ».
- `TopicQuery.GetTopicsByIdsQuery(topicIds)` → `List<Topic>` : résolution batch (accueil, suivis).
- `TopicQuery.TopicSearchQuery` (pattern SDK) reste pour les **futures surfaces d'administration** ;
  les vues web n'y font plus appel.
- `GET /api/topic-categories` (BFF) — les 17 `TopicCategory` + libellé FR (`TopicCategory.label()`).
- `Topic` porte `emoji` + `color` + `imageUrl` (données éditoriales) : agrégat, événements, commande,
  `TopicEntity` (colonnes `emoji`/`color`/`image_url`), DTO. Le seed ne renseigne plus `emoji`/`color`
  (fallback UI) et utilise `imageUrl` comme visuel de couverture.
- **Recherche insensible aux accents/casse** : `TopicEntity.name_normalized` (schéma
  `V1__create_theme_schema.sql`) dérivé de `name` par `TopicEntityMapper`
  (`SearchText.normalize`, domaine).

### Seed initial (YAML)

- **Un fichier par thème**, rangé par catégorie : `src/main/resources/seed/topics/<categorie>/<theme>.yml`
  (21 thèmes, 20 à ~190 questions chacun). Découverte via `app.seed-data.location`
  (défaut `classpath*:seed/topics/*/*.yml`), activée par `app.seed-data.enabled`.
- Schéma d'un fichier : `topic` (`id` déterministe, `name` ≤ 25, `description` ≤ 500, `category`,
  `imageUrl` optionnelle) + `questions` (`text` ≤ 255 — doublons autorisés, `answers` A–D,
  `correctAnswer`, `imageUrl` optionnelle, `translations` optionnelle : `{ en: { text, answers } }`).
  Le texte de premier niveau est **le contenu français** ; `translations` porte les autres langues
  (pas de langue source explicite). Validation par `SeedDataLoader` : l'unicité de la paire
  `(text, imageUrl)` est exigée par fichier (clé de réparation), une traduction `fr` est refusée,
  un fichier invalide est loggé et ignoré sans bloquer les autres.
- **Seeder auto-réparateur** (`DataSeeder`) : thème absent → création + questions + approbation +
  publication ; `PUBLISHED` → remplissage des traductions manquantes ; `DRAFT` → création des
  questions manquantes (clé stable = `(text, imageUrl)`, les IDs de questions étant aléatoires),
  approbation des non-approuvées, traductions manquantes puis publication ;
  `AggregateStreamCreationException` toléré (projection en retard) ; erreurs isolées par thème.
- **Images externes libres de droit** (Wikimedia Commons, `Special:FilePath` + `?width=800`) :
  source et licence listées dans `src/main/resources/seed/CREDITS.md`. Aucun binaire dans le repo,
  aucune image sous copyright (logos, affiches, captures, personnages officiels exclus).
