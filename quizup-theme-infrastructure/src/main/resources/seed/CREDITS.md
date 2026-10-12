# Crédits images — déplacés

Les images du seed et leurs crédits (auteur + licence) vivent désormais dans le repo
**quizup-workspace** :

- fichiers : `product/wikipedia/<categorie>/<fichier>` (copie octet-pour-octet des rendus
  Wikimedia Commons `Special:FilePath/...?width=800`) ;
- crédits : `product/wikipedia/CREDITS.md` ;
- correspondance source → fichier : `product/wikipedia/manifest.json` ;
- outillage (fetch / normalize / rewrite / check / publish) : `scripts/seed-images.py`.

Les YAML `topics/` référencent les images en chemins relatifs
(`imageUrl: wikipedia/<categorie>/<fichier>`), résolus par `app.seed-data.image-base-url`.
