-- V2: Création de la table question
--
-- Cette table stocke les questions de quiz

CREATE TABLE IF NOT EXISTS question_entry (
	question_id VARCHAR(255) PRIMARY KEY,
	topic_id VARCHAR(255) NOT NULL,
	text VARCHAR(255) NOT NULL,
	correct_answer VARCHAR(1) NOT NULL,
	status VARCHAR(255) NOT NULL,
	creator_id VARCHAR(255) NOT NULL,
	created_at TIMESTAMP NOT NULL,
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

COMMENT ON TABLE question_entry IS 'Table des questions - projection read-only mise a jour via Event Handlers';
COMMENT ON COLUMN question_entry.question_id IS 'Identifiant unique de la question';
COMMENT ON COLUMN question_entry.topic_id IS 'Reference vers topic_entry';
COMMENT ON COLUMN question_entry.text IS 'Texte de la question (max 135 caracteres)';
COMMENT ON COLUMN question_entry.correct_answer IS 'Bonne reponse (A, B, C ou D)';
COMMENT ON COLUMN question_entry.status IS 'Etat de moderation (PENDING, APPROVED, REJECTED)';
COMMENT ON COLUMN question_entry.creator_id IS 'Identifiant du createur de la question';
COMMENT ON COLUMN question_entry.created_at IS 'Date de creation de la question';
COMMENT ON COLUMN question_entry.updated_at IS 'Date de derniere mise a jour de la question';

COMMENT ON TABLE question_answer_entry IS 'Reponses possibles des questions (QCM)';
COMMENT ON COLUMN question_answer_entry.question_id IS 'Reference vers question_entry';
COMMENT ON COLUMN question_answer_entry.choice IS 'Choix de reponse (A, B, C ou D)';
COMMENT ON COLUMN question_answer_entry.answer_text IS 'Texte de la reponse (max 30 caracteres)';
