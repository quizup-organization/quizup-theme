-- V4: Bannière de la fiche sujet (visuel panoramique distinct de la couverture `image_url`).
-- URL externe optionnelle (http/https), longueur max 1024, effaçable (NULL).

ALTER TABLE topic_entry ADD COLUMN IF NOT EXISTS banner_url VARCHAR(1024);

COMMENT ON COLUMN topic_entry.banner_url IS 'URL externe optionnelle de la bannière de la fiche sujet (distincte de la couverture)';
