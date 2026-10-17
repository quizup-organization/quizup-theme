-- V6: signalements de questions, snapshot de la version jouée et état de modération.
--
-- Les signalements vivent dans un thread déterministe (question × joueur) : un seul peut être
-- OPEN à la fois, l'historique des signalements clos est conservé. La suspension et l'archivage
-- sont des flags de modération : le statut éditorial (PENDING/APPROVED/REJECTED) est inchangé.

ALTER TABLE question_entry
    ADD COLUMN IF NOT EXISTS suspended BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS suspension_reason VARCHAR(40),
    ADD COLUMN IF NOT EXISTS suspension_note VARCHAR(500),
    ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS suspended_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reinstated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS archive_note VARCHAR(500),
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS archived_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS restored_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_question_entry_suspended ON question_entry(suspended);
CREATE INDEX IF NOT EXISTS idx_question_entry_archived ON question_entry(archived);

CREATE TABLE IF NOT EXISTS question_report (
    report_id VARCHAR(255) PRIMARY KEY,
    thread_id VARCHAR(255) NOT NULL,
    question_id VARCHAR(255) NOT NULL,
    topic_id VARCHAR(255) NOT NULL,
    reporter_id VARCHAR(255) NOT NULL,
    game_id VARCHAR(255),
    comment VARCHAR(500),
    language VARCHAR(5),
    snapshot_json TEXT,
    snapshot_source VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP,
    resolution_note VARCHAR(500),
    CONSTRAINT fk_question_report_question
        FOREIGN KEY (question_id) REFERENCES question_entry(question_id) ON DELETE CASCADE,
    CONSTRAINT chk_question_report_status CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT chk_question_report_snapshot_source
        CHECK (snapshot_source IS NULL OR snapshot_source IN ('PLAYED', 'CURRENT'))
);

CREATE INDEX IF NOT EXISTS idx_question_report_question ON question_report(question_id, status);
CREATE INDEX IF NOT EXISTS idx_question_report_status ON question_report(status, created_at);
CREATE INDEX IF NOT EXISTS idx_question_report_thread ON question_report(thread_id);

-- Garde-fou : un seul signalement ouvert par thread, même en cas de course ou de rejeu.
CREATE UNIQUE INDEX IF NOT EXISTS uq_question_report_open_thread
    ON question_report(thread_id) WHERE status = 'OPEN';

CREATE TABLE IF NOT EXISTS question_report_reason (
    report_id VARCHAR(255) NOT NULL,
    reason VARCHAR(32) NOT NULL,
    CONSTRAINT fk_question_report_reason_report
        FOREIGN KEY (report_id) REFERENCES question_report(report_id) ON DELETE CASCADE,
    CONSTRAINT pk_question_report_reason PRIMARY KEY (report_id, reason),
    CONSTRAINT chk_question_report_reason
        CHECK (reason IN ('QUESTION', 'IMAGE', 'ANSWERS'))
);

CREATE INDEX IF NOT EXISTS idx_question_report_reason_report
    ON question_report_reason(report_id);

COMMENT ON TABLE question_report IS 'Signalements de questions - projection read-only';
COMMENT ON COLUMN question_report.thread_id IS 'Thread déterministe (question × joueur)';
COMMENT ON COLUMN question_report.snapshot_json IS 'Contenu de la question tel que joué au moment du signalement';
COMMENT ON COLUMN question_report.status IS 'OPEN, RESOLVED ou DISMISSED';
COMMENT ON COLUMN question_entry.suspended IS 'Question temporairement retirée du tirage (signalements ou action admin)';
COMMENT ON COLUMN question_entry.archived IS 'Question archivée (soft delete) : conservée pour l’historique, plus jouée';
