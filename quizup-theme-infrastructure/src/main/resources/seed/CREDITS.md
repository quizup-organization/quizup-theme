# Crédits images — déplacés

Les images du seed et leurs crédits (auteur + licence) vivent désormais dans le repo
**quizup-workspace** :

- fichiers : `product/wikipedia/<categorie>/<fichier>` (copie octet-pour-octet des rendus
  Wikimedia Commons `Special:FilePath/...?width=800`) ;
- crédits : `product/wikipedia/CREDITS.md` ;
- correspondance source → fichier : `product/wikipedia/manifest.json` ;
- outillage (fetch / normalize / rewrite / check / publish) : `scripts/seed-images.py` ;
- **exception** : les écussons de clubs (`sports/blasons-de-clubs.yml`, marques déposées)
  sont servis par football-data.org (`crests.football-data.org`) et rapatriés par
  `scripts/crest-theme.py` — fair use assumé, ils ne sont pas réutilisables hors de l'app.

Les YAML `topics/` référencent les images en chemins relatifs
(`imageUrl: wikipedia/<categorie>/<fichier>`), résolus par `app.seed-data.image-base-url`.
