-- Compteur de parties jouées par thème : alimente le tri catalogue « Les plus joués »
-- (`TopicSort.MOST_PLAYED`). Source du recalcul : `topic_played_game_ref`, alimentée par les
-- `GameCreatedEvent` de quizup-game (idempotent/rejouable, comme `followers_counter`).
ALTER TABLE topic_entry
    ADD COLUMN IF NOT EXISTS games_counter INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN topic_entry.games_counter IS 'Parties créées sur ce thème (recalculé depuis topic_played_game_ref)';

-- Parties connues par thème. Volontairement **sans clé étrangère** vers `topic_entry` : les
-- événements de jeu et de thème sont consommés sur des flux distincts (aucun ordre entre les
-- deux), une partie peut donc précéder la projection de son thème — le compteur est recalculé
-- dès que le thème apparaît (`TopicProjection.on(TopicCreatedEvent)`).
CREATE TABLE IF NOT EXISTS topic_played_game_ref (
	game_id VARCHAR(255) PRIMARY KEY,
	topic_id VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_topic_played_game_ref_topic ON topic_played_game_ref(topic_id);
