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

## 2. Endpoints REST

### `QuestionController` — `/api/questions`

| Méthode | Chemin                                | Handler                                         | Response                         |
|---------|---------------------------------------|-------------------------------------------------|----------------------------------|
| POST    | `/api/questions/search`               | `search(SearchRequest)`                         | `PageResponse<QuestionResponse>` |
| POST    | `/api/questions`                      | `createQuestion(CreateQuestionRequest)`         | `IdResponse`                     |
| GET     | `/api/questions/{questionId}`         | `getQuestionById(String)`                       | `QuestionResponse`               |
| POST    | `/api/questions/{questionId}/approve` | `approveQuestion(String)`                       | `IdResponse`                     |
| POST    | `/api/questions/{questionId}/reject`  | `rejectQuestion(String, RejectQuestionRequest)` | `IdResponse`                     |

### `TopicController` — `/api/topics`

| Méthode | Chemin                          | Handler                           | Response                      |
|---------|---------------------------------|-----------------------------------|-------------------------------|
| POST    | `/api/topics/search`            | `search(SearchRequest)`           | `PageResponse<TopicResponse>` |
| POST    | `/api/topics`                   | `createTopic(CreateTopicRequest)` | `IdResponse`                  |
| POST    | `/api/topics/{topicId}/publish` | `publishTopic(String)`            | `IdResponse`                  |
| GET     | `/api/topics/{topicId}`         | `getTopicById(String)`            | `TopicResponse`               |

**DTO** :

-
`QuestionResponse(questionId, topicId, text, imageUrl, Map<QuestionChoice,String> answers, QuestionChoice correctAnswer, QuestionStatus status, QuestionDifficulty difficulty, creatorId, updatedBy, createdAt, updatedAt)`
- `CreateQuestionRequest` accepte un `imageUrl` optionnel (URL externe, ≤ 1024, validé `@Size`).
- `TopicResponse(topicId, name, description, TopicCategory category, status, creatorId, updatedBy, **`Integer
  followersCounter`**, **`Map<QuestionStatus,Integer> questionsCounter`**, createdAt, updatedAt)`

> **Illustration** : `Question.imageUrl` optionnelle (colonne `question_entry.image_url`, schéma
> `V1__create_theme_schema.sql`). Côté client, une question illustrée affiche l'image puis les
> réponses en **grille 2×2** ; sinon en colonne.

> **Difficulté** : `Question.difficulty` (`EASY`/`MEDIUM`/`HARD`/`EXPERT`, colonne
> `question_entry.difficulty`, schéma `V1__create_theme_schema.sql`) est **déduite** du taux
> de bonnes réponses par `QuestionRules` (seuils `≥80 %` / `60–79 %` / `40–59 %` / `<40 %`,
> minimum 10 réponses — sinon `null`). La difficulté appartient à l'**état de l'agrégat**
> `QuestionAggregate` : `QuestionDifficultyProjection` envoie une
> `QuestionCommand.UpdateQuestionDifficultyCommand` (idempotente), l'agrégat applique
> `QuestionDifficultyUpdatedEvent`, et `QuestionProjection` met à jour le read model (voir § 4).

---

## 3. Use cases (ports entrants — `domain/port/in/`)

- `CreateTopicUseCase` / `GetTopicUseCase` / `CheckTopicUseCase` / `SearchTopicUseCase` / `PublishTopicUseCase`
- `CreateQuestionUseCase` / `GetQuestionUseCase` / `SearchQuestionUseCase` / `GetRandomApprovedQuestionsUseCase`
- `ApproveQuestionUseCase` / `RejectQuestionUseCase`
- `CountApprovedQuestionsByTopicUseCase`

---

## 4. Dépendances inter-services

**Aucune dépendance sortante** au niveau contrat (queries). Theme est un **fournisseur** :

- `quizup-social` → `TopicRepositoryPort` → `TopicQuery.TopicExistsByIdQuery`
- `quizup-game` → `QuestionRepositoryPort` → `QuestionQuery.GetRandomApprovedQuestionsQuery`

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

- `GET /api/topics/categories` — les 17 `TopicCategory` + libellé FR (`TopicCategory.label()`).
- **Aucun endpoint dérivé** (facettes, suggestions) : le client s'appuie sur
  `POST /api/topics/search` + filtres/sorts et calcule (compteurs = `totalElements`).
- `Topic` porte `emoji` + `color` (données éditoriales) : agrégat, événements, commande,
  `TopicEntity` (colonnes `emoji`/`color`), DTO.
- **Recherche insensible aux accents/casse** : `TopicEntity.name_normalized` (schéma
  `V1__create_theme_schema.sql`) dérivé de `name` par `TopicEntityMapper`
  (`SearchText.normalize`, domaine). Le client filtre `nameNormalized CONTAINS normalize(q)`.
