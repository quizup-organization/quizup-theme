-- V3: Ajout des colonnes d'audit updated_by
--
-- Aligne le schema SQL avec les projections qui stockent l'identite du dernier acteur

ALTER TABLE topic_entry
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

ALTER TABLE question_entry
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

UPDATE topic_entry
SET updated_by = creator_id
WHERE updated_by IS NULL;

UPDATE question_entry
SET updated_by = creator_id
WHERE updated_by IS NULL;

COMMENT ON COLUMN topic_entry.updated_by IS 'Identifiant du dernier acteur ayant modifie le theme';
COMMENT ON COLUMN question_entry.updated_by IS 'Identifiant du dernier acteur ayant modifie la question';

