-- V1: Schema initial complet (topic + question + compteurs)
--
-- Ce script represente l'etat actuel des entites JPA du module theme.

CREATE TABLE IF NOT EXISTS topic_entry (
	topic_id VARCHAR(255) PRIMARY KEY,
	name VARCHAR(25) NOT NULL,
	description VARCHAR(500),
	category VARCHAR(255) NOT NULL,
	status VARCHAR(255) NOT NULL,
	creator_id VARCHAR(255) NOT NULL,
	followers_counter INTEGER DEFAULT 0,
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
	CONSTRAINT chk_question_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
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

COMMENT ON TABLE topic_entry IS 'Table des themes - projection read-only mise a jour via Event Handlers';
COMMENT ON COLUMN topic_entry.topic_id IS 'Identifiant unique du theme';
COMMENT ON COLUMN topic_entry.name IS 'Nom du theme (max 25 caracteres)';
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

