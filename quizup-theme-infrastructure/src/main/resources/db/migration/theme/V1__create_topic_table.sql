-- V1: Création de la table topic
--
-- Cette table stocke les thèmes de quiz

CREATE TABLE IF NOT EXISTS topic_entry (
	topic_id VARCHAR(255) PRIMARY KEY,
	name VARCHAR(255) NOT NULL,
	description VARCHAR(500),
	category VARCHAR(255) NOT NULL,
	status VARCHAR(255) NOT NULL,
	creator_id VARCHAR(255) NOT NULL,
	question_count INTEGER NOT NULL DEFAULT 0,
	created_at TIMESTAMP NOT NULL,
	updated_at TIMESTAMP,
	CONSTRAINT chk_topic_name_not_blank CHECK (char_length(trim(name)) > 0),
	CONSTRAINT chk_topic_question_count_non_negative CHECK (question_count >= 0),
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

COMMENT ON TABLE topic_entry IS 'Table des themes - projection read-only mise a jour via Event Handlers';
COMMENT ON COLUMN topic_entry.topic_id IS 'Identifiant unique du theme';
COMMENT ON COLUMN topic_entry.name IS 'Nom du theme (max 25 caracteres)';
COMMENT ON COLUMN topic_entry.description IS 'Description du theme (max 500 caracteres)';
COMMENT ON COLUMN topic_entry.category IS 'Categorie fonctionnelle du theme';
COMMENT ON COLUMN topic_entry.status IS 'Etat du theme (DRAFT, PUBLISHED, ARCHIVED)';
COMMENT ON COLUMN topic_entry.creator_id IS 'Identifiant du createur du theme';
COMMENT ON COLUMN topic_entry.question_count IS 'Nombre total de questions rattachees au theme';
COMMENT ON COLUMN topic_entry.created_at IS 'Date de creation du theme';
COMMENT ON COLUMN topic_entry.updated_at IS 'Date de derniere mise a jour du theme';
