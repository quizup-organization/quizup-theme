# AGENTS.md — quizup-theme

> Service de **thèmes (topics) et questions** du quiz. Architecture : Axon Framework (CQRS/EDA) +
> JPA (projections). **Fournisseur** de données pour les autres services.
> Pour les règles de patterns : [
`../../best-practices/.backend/folder-structure.md`](../../best-practices/.backend/folder-structure.md).

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
(sans port `UseCase` dédié) — `TopicCommand.UpdateTopicName|Description|Category|Emoji|Color|ImageUrl|BannerUrlCommand`
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
- `quizup-matchmaking` → `QuestionDrawPort` → `QuestionQuery.GetRandomApprovedQuestionsQuery(topicId, count, languages, difficulties, excludeQuestionIds)`
  (sélection **stricte** sur les langues : la question doit avoir un contenu dans **toutes** les
  langues demandées ; `difficulties`/`excludeQuestionIds` sont des **préférences de tri**
  best-effort — jamais-jouées &gt; difficulté cible &gt; difficulté inconnue &gt; autres — et la
  `Question` retournée porte ses contenus localisés, le client choisit le sien)
- `quizup-bff` → `QuestionQuery.GetTopicLanguageCoverageQuery(topicId)` → `List<QuestionLanguageCoverage>`
  (couverture de **toutes** les combinaisons non vides de langues, sémantique stricte « contenu dans
  toutes les langues ») : préparée à la création d'un ticket d'appariement et portée par
  `MatchmakingStartedEvent`, pour que la saga compare **localement** (aucune query bloquante dans un
  handler de saga — voir `best-practices/.backend/anti-patterns.md` § 1). La garde
  `CountApprovedQuestionsByTopicAndLanguagesQuery` reste disponible (comptage d'une combinaison) et
  sert d'implémentation interne à la couverture.
- `quizup-bff` → `TopicQuery.CountImageReferencesQuery(imageUrls)` → `List<ImageReferenceCount>` :
  comptage batch des références d'images du read model (couverture et bannière de sujet,
  illustration de question), réservé à la **console média admin** du BFF (`/api/admin/media`,
  `ROLE_ADMIN` — une suppression d'objet référencé est bloquée). Port sortant
  `ImageReferenceRepositoryPort`.

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
  catalogue publié (tri `POPULAR|MOST_PLAYED|ALPHA|RECENT` — `MOST_PLAYED` = compteur
  **communautaire** de parties jouées `games_counter desc`, `RECENT` = `updatedAt desc`, alimente
  la section « nouveaux thèmes » de l'accueil), filtre texte normalisé côté handler. `updatedAt` =
  dernière mise à jour **de contenu** : publication, édition du sujet **et éditions de questions**
  (traduction/texte/réponses/bonne réponse/image — `TopicProjection`) ; jamais les follows ni la
  difficulté auto-calculée (`QuestionDifficultyUpdatedEvent` ignoré).

### Compteur de parties jouées (`games_counter`)

- Source : `topic_played_game_ref` (clé naturelle `game_id`), alimentée par
  `TopicGamesProjection` sur `GameEvent.GameCreatedEvent` de `quizup-game` (pin
  `quizup-game-domain`) ; le compteur est **recalculé** (`Topic.gamesCounter`) à chaque partie et
  **initialisé** à la création du thème — les flux game et thème étant distincts, une partie peut
  précéder son thème (table sans clé étrangère, rattrapage à la création). Rejouable sans dérive.
- Communautaire : toutes les parties comptent (bots compris), il ne dépend d'aucun joueur.
  Alimente le tri catalogue « Les plus joués » (`TopicSort.MOST_PLAYED`).
- `TopicQuery.GetTopicsByCreatorQuery(creatorId, page, size)` → `TopicPage` : sujets créés par un
  utilisateur (tous statuts, brouillons compris), tri `updatedAt desc` — alimente la vue
  « mes sujets » du BFF (`?mine=true`).
- `TopicQuery.TopicFacetsQuery(nameQuery, topicIds)` → `List<TopicFacetCount>` : compteurs par
  catégorie (facettes) appliqués aux mêmes filtres, `topicIds` pour le périmètre « suivis ».
- `TopicQuery.GetTopicsByIdsQuery(topicIds)` → `List<Topic>` : résolution batch (accueil, suivis).
- `TopicQuery.TopicSearchQuery` (pattern SDK) reste pour les **futures surfaces d'administration** ;
  les vues web n'y font plus appel.
- `GET /api/topic-categories` (BFF) — les 18 `TopicCategory` + libellé FR (`TopicCategory.label()`).
- `Topic` porte `emoji` + `color` + `imageUrl` + `bannerUrl` (données éditoriales) : agrégat,
  événements, commande, `TopicEntity` (colonnes `emoji`/`color`/`image_url`/`banner_url`), DTO.
  La **bannière** (`banner-url`, migration `V4__add_topic_banner_url.sql`) est un visuel
  panoramique de la fiche sujet, distinct de la couverture. Le seed ne renseigne plus `emoji`/`color`
  (fallback UI) et utilise `imageUrl` comme visuel de couverture.
- **Recherche insensible aux accents/casse** : `TopicEntity.name_normalized` (schéma
  `V1__create_theme_schema.sql`) dérivé de `name` par `TopicEntityMapper`
  (`SearchText.normalize`, domaine).

### Seed initial (YAML)

- **Un fichier par thème**, rangé par catégorie : `src/main/resources/seed/topics/<categorie>/<theme>.yml`
  (100 questions par thème ; 3 thèmes historiques plus fournis conservés tels quels :
  Chefs-d'œuvre 137, Capitales 185, Drapeaux 194). Découverte via `app.seed-data.location`
  (défaut `classpath*:seed/topics/*/*.yml`), activée par `app.seed-data.enabled`.
  > **Catalogue en réécriture** (lot « acquisition ») : nouveaux thèmes + scission des umbrellas de
  > licences + durcissement du contenu. Règle de contenu : **1 licence/fiction par thème** (pas
  > d'umbrella de licences) ; les thèmes factuels (drapeaux, capitales, monuments) et de genre
  > (années, rap français) restent autorisés.
- Schéma d'un fichier : `topic` (`id` déterministe, `name` ≤ 25, `description` ≤ 500, `category`,
  `imageUrl` optionnelle) + `questions` (`text` ≤ 255 — doublons autorisés, `answers` A–D,
  `correctAnswer`, `imageUrl` optionnelle, `difficulty` optionnelle : `EASY|MEDIUM|HARD|EXPERT`,
  `translations` optionnelle : `{ en: { text, answers } }`).
  Le texte de premier niveau est **le contenu français** ; `translations` porte les autres langues
  (pas de langue source explicite). Validation par `SeedDataLoader` : l'unicité de la paire
  `(text, imageUrl)` est exigée par fichier (clé de réparation), une traduction `fr` est refusée,
  un fichier invalide est loggé et ignoré sans bloquer les autres.
  `imageUrl` est un **chemin relatif** (`wikipedia/<categorie>/<fichier>`, ex.
  `wikipedia/technology/Ada_Lovelace_portrait.jpg`), résolu par `SeedDataLoader` en
  `app.seed-data.image-base-url + chemin` (les URLs absolues http(s) restent acceptées).
- **Difficulté initiale du seed** : la difficulté par défaut est **calculée** depuis les statistiques
  de réponses (`QuestionRules.difficultyFor`, seuils 80/60/40 %, min. 10 réponses —
  `QuestionDifficultyProjection`). Le seed porte une difficulté **initiale optionnelle**, posée par
  le seeder (`UpdateQuestionDifficultyCommand`) **uniquement si la question n'a encore aucune
  difficulté** : elle est remplacée par le calcul réel dès que l'échantillon est suffisant (aucun
  écrasement d'une classification issue des réponses). Garde-fou : `SeedDifficultyBalanceTest` —
  **deux répartitions acceptées** pendant la migration : historique **25 % ±1** par niveau, et
  cible **EASY 10 / MEDIUM 20 / HARD 35 / EXPERT 35 (±3)** visée par les thèmes (re)générés
  (`seed/generate-seed.py`). Retirer l'ancienne une fois le catalogue réécrit.
  `seed/annotate-difficulty.py --check` tolère les deux mêmes répartitions.
- **Seeder auto-réparateur** (`DataSeeder`) : thème absent → création + questions + approbation +
  publication ; `PUBLISHED` → complétion sans republication (questions manquantes créées et
  approuvées, traductions et difficultés initiales manquantes ajoutées) ; `DRAFT` → création des
  questions manquantes (clé stable = `(text, imageUrl)`, les IDs de questions étant aléatoires),
  approbation des non-approuvées, traductions et difficultés manquantes puis publication ;
  `AggregateStreamCreationException` toléré (projection en retard) ; erreurs isolées par thème.
  **Échec transitoire** (`CommandDispatchException`/connection refusée — instance Axon périmée
  pendant un rollout) → jusqu'à 4 tentatives du thème (backoff 2s×n) ; le seeding étant
  idempotent, un essai ultérieur reprend les questions manquantes. Résultats exposés via
  `quizup_theme_seed_topics_total{outcome}` + `quizup_theme_seed_retries_total` (alerte
  `QuizupThemeSeedFailures` si `outcome="failed"`).
- **Images** : les binaires (copie **octet-pour-octet** des rendus Wikimedia Commons
  `Special:FilePath/<fichier>?width=800`) vivent dans le repo **quizup-workspace** :
  `product/wikipedia/<categorie>/<fichier>` (+ `manifest.json`, + `CREDITS.md` pour
  l'auteur/licence). Aucun binaire dans ce repo. Outillage workspace :
  `python3 scripts/seed-images.py {fetch,normalize,rewrite,check,publish}` (`make seed-images
  ACTION=…`), publié vers MinIO (bucket `quizup-media`, préfixe `wikipedia/`).
  - **Règle** : images **libres** uniquement (domaine public ou licence libre avec attribution) ;
    jamais de logo, affiche, capture ou personnage officiel — les visuels des thèmes licences sont
    des **photos libres évoquant** la licence (cosplay, convention, lieu).
  - **Questions avec image** : le `text` fait **≤ 60 caractères** et **ne décrit pas** l'image
    (« Quel est ce pays ? », « À quel club ? ») — l'image porte l'information. Validé à la
    génération par `generate-seed.py`.
  - **Exception actée — écussons de clubs de football** (`sports/blasons-de-clubs.yml`) : les
    écussons ne sont **pas** libres (fair use Wikipédia + marques). Exception **explicitement
    assumée par le propriétaire** ; ces fichiers ne passent pas par `fetch` (ajout manuel dans
    `product/wikipedia/sports/`) et `CREDITS.md` les marque « non libre / marque ». Risque :
    disponibilité et licence non garanties.
- **Traductions EN** : chaque question porte `translations.en` (les duels bilingues ne sélectionnent
  que les questions disponibles dans **toutes** les langues demandées). Générées via
  `seed/translate-seed.py` (API DeepSeek, validation identique au loader, insertion textuelle à
  diff minimal, modes `--check`/`--dry-run`). Le thème `orthographe-grammaire` est volontairement
  **FR-only** (exercice intraduisible). Garde-fou : `SeedEnTranslationCoverageTest`.
- **Génération de contenu** : `seed/generate-seed.py` rédige un thème (questions FR) via l'API
  DeepSeek (JSON strict, validations identiques au loader : texte ≤ 255, texte imagé ≤ 60, 4
  réponses distinctes, `correctAnswer` A–D, dédoublonnage). La **difficulté** n'est pas demandée au
  modèle : les questions sont **notées 0–100** puis les niveaux sont affectés **aux percentiles de
  la cible** (`--targets`, défaut `10/20/35/35`) → distribution exacte garantie ; `--rebalance-only
  FILE` reclasse un fichier existant. Enchaînement : `generate-seed.py` → `translate-seed.py` →
  `rebalance-seed.py` (le modèle biaise fortement la lettre A → équilibrage A–D obligatoire).

### Hygiène du seed (règles vérifiées par tests)

- **Répartition des bonnes réponses** : ~25 % de A, B, C et D **par thème**, tolérance **±1** —
  jamais de lettre privilégiée (le biais A est exploité par les joueurs attentifs). Correction
  déterministe et idempotente : `python3 seed/rebalance-seed.py topics/*/*.yml` (`--check` sort en
  erreur si un thème dépasse ±1, `--dry-run` détaille les échanges, écriture textuelle minimale :
  échange des valeurs entre la lettre courante et la cible dans `answers` et les traductions).
  Garde-fou : `SeedAnswerBalanceTest`.
- **Couverture unique par thème** : deux thèmes ne partagent jamais le même `topic.imageUrl` (le
  catalogue les affiche côte à côte) ; un thème peut ne pas en avoir (fallback UI). Garde-fou :
  `SeedTopicCoverUniquenessTest`.
- **Images** : exclusivement Wikimedia Commons libres de droit ; toute nouvelle image doit être
  rapatriée dans `product/wikipedia/` (workspace) et figurer dans `product/wikipedia/CREDITS.md`
  (auteur + licence), puis référencée en chemin relatif dans le YAML.
- **Seed déjà publié en prod** : le seeder est idempotent par `(text, imageUrl)` (URL **résolue**
  via la base) et peut **compléter** un thème publié (questions manquantes, traductions, difficulté
  initiale) sans reset ; en revanche **modifier** les réponses, les textes, la couverture ou la
  base d'images exige un **reset data** (`devops/quizup-gitops/scripts/reset.sh`, § 7 du AGENTS
  gitops) coordonné avec la release.
