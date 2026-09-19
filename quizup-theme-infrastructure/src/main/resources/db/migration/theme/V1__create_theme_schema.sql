-- V1: Schema initial complet (topic + question + illustration/difficulte + statistiques)
--
-- Ce script represente l'etat actuel des entites JPA du module theme.

CREATE TABLE IF NOT EXISTS topic_entry (
	topic_id VARCHAR(255) PRIMARY KEY,
	name VARCHAR(25) NOT NULL,
	name_normalized VARCHAR(25) NOT NULL,
	description VARCHAR(500),
	category VARCHAR(255) NOT NULL,
	status VARCHAR(255) NOT NULL,
	creator_id VARCHAR(255) NOT NULL,
	followers_counter INTEGER DEFAULT 0,
	emoji VARCHAR(16),
	color VARCHAR(16),
	created_at TIMESTAMP NOT NULL,
	updated_by VARCHAR(255),
	updated_at TIMESTAMP,
	CONSTRAINT chk_topic_name_not_blank CHECK (char_length(trim(name)) > 0),
	CONSTRAINT chk_topic_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
	CONSTRAINT chk_topic_category CHECK (category IN (
		'ARTS',
		'BUSINESS',
		'EDUCATION',
		'ENTERTAINMENT',
		'FOOD_AND_DRINK',
		'GAMES',
		'GENERAL',
		'HISTORY',
		'LITERATURE',
		'MOVIES',
		'MUSIC',
		'NATURE',
		'SCIENCE',
		'SPORTS',
		'TELEVISION',
		'TECHNOLOGY',
		'WORLD'
	))
);

CREATE INDEX IF NOT EXISTS idx_topic_entry_creator ON topic_entry(creator_id);
CREATE INDEX IF NOT EXISTS idx_topic_entry_status ON topic_entry(status);
CREATE INDEX IF NOT EXISTS idx_topic_entry_category ON topic_entry(category);
CREATE INDEX IF NOT EXISTS idx_topic_entry_name_normalized ON topic_entry(name_normalized);

CREATE TABLE IF NOT EXISTS topic_questions_counter (
	topic_id VARCHAR(255) NOT NULL,
	question_status VARCHAR(50) NOT NULL,
	counter INTEGER NOT NULL DEFAULT 0,
	PRIMARY KEY (topic_id, question_status),
	CONSTRAINT fk_topic_questions_counter_topic
		FOREIGN KEY (topic_id) REFERENCES topic_entry(topic_id) ON DELETE CASCADE,
	CONSTRAINT chk_topic_questions_status CHECK (question_status IN ('PENDING', 'APPROVED', 'REJECTED')),
	CONSTRAINT chk_topic_questions_counter_non_negative CHECK (counter >= 0)
);

CREATE INDEX IF NOT EXISTS idx_topic_questions_counter_topic ON topic_questions_counter(topic_id);

CREATE TABLE IF NOT EXISTS question_entry (
	question_id VARCHAR(255) PRIMARY KEY,
	topic_id VARCHAR(255) NOT NULL,
	text VARCHAR(255) NOT NULL,
	image_url VARCHAR(1024),
	difficulty VARCHAR(255),
	correct_answer VARCHAR(1) NOT NULL,
	status VARCHAR(255) NOT NULL,
	creator_id VARCHAR(255) NOT NULL,
	created_at TIMESTAMP NOT NULL,
	updated_by VARCHAR(255),
	updated_at TIMESTAMP,
	CONSTRAINT fk_question_entry_topic
		FOREIGN KEY (topic_id) REFERENCES topic_entry(topic_id) ON DELETE CASCADE,
	CONSTRAINT chk_question_text_not_blank CHECK (char_length(trim(text)) > 0),
	CONSTRAINT chk_question_choice CHECK (correct_answer IN ('A', 'B', 'C', 'D')),
	CONSTRAINT chk_question_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
	CONSTRAINT chk_question_difficulty CHECK (difficulty IS NULL OR difficulty IN ('EASY', 'MEDIUM', 'HARD', 'EXPERT'))
);

CREATE INDEX IF NOT EXISTS idx_question_entry_topic ON question_entry(topic_id);
CREATE INDEX IF NOT EXISTS idx_question_entry_status ON question_entry(status);
CREATE INDEX IF NOT EXISTS idx_question_entry_creator ON question_entry(creator_id);

CREATE TABLE IF NOT EXISTS question_answer_entry (
	question_id VARCHAR(255) NOT NULL,
	choice VARCHAR(1) NOT NULL,
	answer_text VARCHAR(255) NOT NULL,
	CONSTRAINT fk_question_answer_entry_question
		FOREIGN KEY (question_id) REFERENCES question_entry(question_id) ON DELETE CASCADE,
	CONSTRAINT pk_question_answer_entry PRIMARY KEY (question_id, choice),
	CONSTRAINT chk_question_answer_choice CHECK (choice IN ('A', 'B', 'C', 'D')),
	CONSTRAINT chk_question_answer_text_not_blank CHECK (char_length(trim(answer_text)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_question_answer_entry_question_id ON question_answer_entry(question_id);

CREATE TABLE IF NOT EXISTS question_answer_stats (
	question_id VARCHAR(255) PRIMARY KEY,
	answer_count INTEGER NOT NULL DEFAULT 0,
	correct_count INTEGER NOT NULL DEFAULT 0,
	CONSTRAINT fk_question_answer_stats_question
		FOREIGN KEY (question_id) REFERENCES question_entry(question_id) ON DELETE CASCADE,
	CONSTRAINT chk_question_answer_count_non_negative CHECK (answer_count >= 0),
	CONSTRAINT chk_question_correct_count_non_negative CHECK (correct_count >= 0),
	CONSTRAINT chk_question_correct_lte_answers CHECK (correct_count <= answer_count)
);

COMMENT ON TABLE topic_entry IS 'Table des themes - projection read-only mise a jour via Event Handlers';
COMMENT ON COLUMN topic_entry.topic_id IS 'Identifiant unique du theme';
COMMENT ON COLUMN topic_entry.name IS 'Nom du theme (max 25 caracteres)';
COMMENT ON COLUMN topic_entry.name_normalized IS 'Nom normalise (minuscules, sans accents) pour la recherche';
COMMENT ON COLUMN topic_entry.description IS 'Description du theme (max 500 caracteres)';
COMMENT ON COLUMN topic_entry.category IS 'Categorie fonctionnelle du theme';
COMMENT ON COLUMN topic_entry.status IS 'Etat du theme (DRAFT, PUBLISHED, ARCHIVED)';
COMMENT ON COLUMN topic_entry.creator_id IS 'Identifiant du createur du theme';
COMMENT ON COLUMN topic_entry.followers_counter IS 'Utilisateurs suivant ce theme';
COMMENT ON COLUMN topic_entry.created_at IS 'Date de creation du theme';
COMMENT ON COLUMN topic_entry.updated_by IS 'Identifiant du dernier acteur ayant modifie le theme';
COMMENT ON COLUMN topic_entry.updated_at IS 'Date de derniere mise a jour du theme';

COMMENT ON TABLE topic_questions_counter IS 'Compteurs de questions par statut pour chaque theme';
COMMENT ON COLUMN topic_questions_counter.topic_id IS 'Identifiant du theme';
COMMENT ON COLUMN topic_questions_counter.question_status IS 'Statut de la question (PENDING, APPROVED, REJECTED)';
COMMENT ON COLUMN topic_questions_counter.counter IS 'Nombre de questions avec ce statut';

COMMENT ON TABLE question_entry IS 'Table des questions - projection read-only mise a jour via Event Handlers';
COMMENT ON COLUMN question_entry.question_id IS 'Identifiant unique de la question';
COMMENT ON COLUMN question_entry.topic_id IS 'Reference vers topic_entry';
COMMENT ON COLUMN question_entry.text IS 'Texte de la question';
COMMENT ON COLUMN question_entry.image_url IS 'URL externe optionnelle de l''illustration de la question';
COMMENT ON COLUMN question_entry.difficulty IS 'Difficulte deduite du taux de bonnes reponses (EASY, MEDIUM, HARD, EXPERT), NULL tant que l''echantillon est insuffisant';
COMMENT ON COLUMN question_entry.correct_answer IS 'Bonne reponse (A, B, C ou D)';
COMMENT ON COLUMN question_entry.status IS 'Etat de moderation (PENDING, APPROVED, REJECTED)';
COMMENT ON COLUMN question_entry.creator_id IS 'Identifiant du createur de la question';
COMMENT ON COLUMN question_entry.created_at IS 'Date de creation de la question';
COMMENT ON COLUMN question_entry.updated_by IS 'Identifiant du dernier acteur ayant modifie la question';
COMMENT ON COLUMN question_entry.updated_at IS 'Date de derniere mise a jour de la question';

COMMENT ON TABLE question_answer_entry IS 'Reponses possibles des questions (QCM)';
COMMENT ON COLUMN question_answer_entry.question_id IS 'Reference vers question_entry';
COMMENT ON COLUMN question_answer_entry.choice IS 'Choix de reponse (A, B, C ou D)';
COMMENT ON COLUMN question_answer_entry.answer_text IS 'Texte de la reponse';

COMMENT ON TABLE question_answer_stats IS 'Compteurs de reponses par question (base du calcul de difficulte)';
COMMENT ON COLUMN question_answer_stats.question_id IS 'Reference vers question_entry';
COMMENT ON COLUMN question_answer_stats.answer_count IS 'Nombre total de reponses humaines (timeouts inclus)';
COMMENT ON COLUMN question_answer_stats.correct_count IS 'Nombre de reponses humaines correctes';
