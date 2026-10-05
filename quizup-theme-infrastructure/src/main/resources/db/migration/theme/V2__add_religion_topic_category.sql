-- Nouvelle catégorie RELIGION (enum TopicCategory) : élargit la contrainte du read model.
-- Sans cette migration, la projection rejette les événements TopicCreatedEvent avec la
-- catégorie RELIGION (chk_topic_category) et boucle en retry sur le flux.
ALTER TABLE topic_entry DROP CONSTRAINT chk_topic_category;

ALTER TABLE topic_entry ADD CONSTRAINT chk_topic_category CHECK (category IN (
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
    'RELIGION',
    'SCIENCE',
    'SPORTS',
    'TELEVISION',
    'TECHNOLOGY',
    'WORLD'
));
