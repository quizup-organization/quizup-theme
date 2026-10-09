-- Big-bang : noms de thème multilingues (FR obligatoire, EN optionnel).
-- Ancien champ mono-langue `name`/`name_normalized` supprimé au profit de colonnes par langue,
-- indexées pour la recherche (le nom est cherché dans les deux langues).

DROP INDEX IF EXISTS idx_topic_entry_name_normalized;
ALTER TABLE topic_entry DROP COLUMN IF EXISTS name_normalized;
ALTER TABLE topic_entry DROP COLUMN IF EXISTS name;

ALTER TABLE topic_entry
    ADD COLUMN name_fr VARCHAR(255) NOT NULL,
    ADD COLUMN name_en VARCHAR(255),
    ADD COLUMN name_fr_normalized VARCHAR(255),
    ADD COLUMN name_en_normalized VARCHAR(255);

ALTER TABLE topic_entry
    ADD CONSTRAINT chk_topic_name_fr_not_blank CHECK (char_length(trim(name_fr)) > 0);

CREATE INDEX IF NOT EXISTS idx_topic_entry_name_fr_normalized ON topic_entry(name_fr_normalized);
CREATE INDEX IF NOT EXISTS idx_topic_entry_name_en_normalized ON topic_entry(name_en_normalized);

COMMENT ON COLUMN topic_entry.name_fr IS 'Nom du theme (francais, reference, max 255)';
COMMENT ON COLUMN topic_entry.name_en IS 'Nom du theme (anglais, optionnel, max 255)';
COMMENT ON COLUMN topic_entry.name_fr_normalized IS 'name_fr normalise (recherche insensible accents/casse)';
COMMENT ON COLUMN topic_entry.name_en_normalized IS 'name_en normalise (recherche insensible accents/casse)';
