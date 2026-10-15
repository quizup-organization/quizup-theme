#!/usr/bin/env python3
"""
Génère un fichier de seed de thème (questions FR) via l'API DeepSeek (JSON strict).

Le français est la langue source ; l'anglais est ajouté ensuite par `translate-seed.py`.
La difficulté initiale est demandée au modèle avec une cible volontairement **dure**
(`--targets`, défaut `10/20/35/35`) puis validée ; l'équilibrage A–D des bonnes réponses est
assuré par `rebalance-seed.py`.

Validation **identique au loader** (SeedDataLoader) :
- `topic`: id non vide, `names` fr (+ en optionnel) ≤ 255, description ≤ 500, catégorie de l'enum ;
- chaque question : `text` non vide ≤ 255, exactement les réponses A–D non vides et distinctes,
  `correctAnswer` ∈ {A,B,C,D} présente dans `answers`, `difficulty` ∈ {EASY,MEDIUM,HARD,EXPERT} ;
- unicité des `(text, imageUrl)` par fichier ;
- **questions avec image** : texte ≤ 60 caractères et non descriptif (règle produit).

Prérequis :
    pip install pyyaml requests

Usage :
    set -a; source ../../../../../../.env; set +a   # DEEPSEEK
    export QUIZUP_DEEPSEEK_API_KEY="$DEEPSEEK"
    python3 generate-seed.py --id topic-naruto --name-fr "Naruto" --name-en "Naruto" \\
        --category ENTERTAINMENT --description "..." \\
        --image-url wikipedia/entertainment/naruto-cosplay.jpg \\
        --out topics/entertainment/naruto.yml
    python3 generate-seed.py ... --count 5 --dry-run   # échantillon sans écrire

Options :
    --count N        nombre de questions (défaut 100)
    --batch N        questions par appel API (défaut 20)
    --targets A/B/C/D  parts EASY/MEDIUM/HARD/EXPERT (défaut 10/20/35/35)
    --focus          angle éditorial précis (facultatif)
    --avoid          sujets/faits à éviter (facultatif)
    --model NAME     modèle DeepSeek (défaut deepseek-chat)
    --dry-run        n'écrit pas, affiche un échantillon
"""
import argparse
import json
import os
import sys
import time
from pathlib import Path

import requests

API_URL = "https://api.deepseek.com/chat/completions"
MAX_TEXT = 255
MAX_NAME = 255
MAX_DESCRIPTION = 500
MAX_IMAGE_TEXT = 60
CHOICES = ("A", "B", "C", "D")
LEVELS = ("EASY", "MEDIUM", "HARD", "EXPERT")
CATEGORIES = (
    "ARTS", "BUSINESS", "EDUCATION", "ENTERTAINMENT", "FOOD_AND_DRINK", "GAMES",
    "GENERAL", "HISTORY", "LITERATURE", "MOVIES", "MUSIC", "NATURE", "RELIGION",
    "SCIENCE", "SPORTS", "TELEVISION", "TECHNOLOGY", "WORLD",
)

SYSTEM_PROMPT = """\
Tu écris des questions de quiz FACTUELLES et VÉRIFIABLES pour un jeu de duel francophone.
Langue : français uniquement (l'anglais sera traduit plus tard).

Exigences :
- Chaque question a exactement 4 réponses (clés "A","B","C","D"), une seule correcte.
- Les 3 mauvaises réponses sont plausibles, distinctes et non absurdes ; jamais la réponse dans l'énoncé.
- Aucun doublon de question ; aucun fait inventé ni approximation ; évite l'actualité datée.
- Varie la position de la bonne réponse (A/B/C/D) : ne mets PAS systématiquement la bonne en A.
- Difficulté : bannis le trivia évident, privilégie les détails précis, chiffres, dates, pièges de proximité.
  Répartis globalement selon la cible fournie (peu de EASY, beaucoup de HARD/EXPERT).
- Champ "difficulty" ∈ {"EASY","MEDIUM","HARD","EXPERT"}.
- Si une question porte une "imageUrl", son TEXTE fait au plus 60 caractères et NE DÉCRIT PAS l'image
  (ex. "Quel est ce pays ?", "À quel club ?") : l'image porte l'information.
- Réponds UNIQUEMENT par un objet JSON, sans texte autour."""


def log(message):
    print(message, flush=True)


def parse_targets(value):
    parts = [float(p) for p in value.replace(" ", "").split("/")]
    if len(parts) != 4 or abs(sum(parts) - 100.0) > 0.01:
        raise SystemExit("--targets attend 4 parts dont la somme fait 100 (ex. 10/20/35/35)")
    return dict(zip(LEVELS, parts))


def target_counts(count, targets):
    return {level: round(count * targets[level] / 100.0) for level in LEVELS}


def call_api(api_key, model, user_prompt, attempts=4, system_prompt=SYSTEM_PROMPT):
    last_error = None
    for attempt in range(attempts):
        try:
            response = requests.post(
                API_URL,
                headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
                json={
                    "model": model,
                    "temperature": 1.0 if system_prompt is SYSTEM_PROMPT else 0.0,
                    "max_tokens": 8192,
                    "response_format": {"type": "json_object"},
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt},
                    ],
                },
                timeout=300,
            )
            if response.status_code in (429, 500, 502, 503, 504):
                raise RuntimeError(f"HTTP {response.status_code}")
            response.raise_for_status()
            return json.loads(response.json()["choices"][0]["message"]["content"])
        except Exception as error:  # noqa: BLE001 — retry réseau/API/JSON
            last_error = error
            wait = 2**attempt
            log(f"    ! tentative {attempt + 1}/{attempts} échouée ({error}), retry {wait}s")
            time.sleep(wait)
    raise RuntimeError(f"échec API après {attempts} tentatives : {last_error}")


def validate_question(entry, index):
    text = str(entry.get("text", "")).strip()
    if not text:
        raise ValueError(f"Q{index}: texte vide")
    if len(text) > MAX_TEXT:
        raise ValueError(f"Q{index}: texte trop long ({len(text)} > {MAX_TEXT})")
    image = entry.get("imageUrl")
    if image and len(text) > MAX_IMAGE_TEXT:
        raise ValueError(f"Q{index}: texte imagé > {MAX_IMAGE_TEXT} caractères ({len(text)})")
    answers = entry.get("answers") or {}
    if set(answers.keys()) != set(CHOICES):
        raise ValueError(f"Q{index}: réponses != A–D ({sorted(answers.keys())})")
    clean = {}
    for choice in CHOICES:
        value = str(answers.get(choice, "")).strip()
        if not value:
            raise ValueError(f"Q{index}: réponse {choice} vide")
        clean[choice] = value
    if len(set(clean.values())) != len(CHOICES):
        raise ValueError(f"Q{index}: réponses non distinctes")
    correct = str(entry.get("correctAnswer", "")).strip().upper()
    if correct not in CHOICES:
        raise ValueError(f"Q{index}: correctAnswer invalide ({correct!r})")
    # La difficulté demandée à la génération n'est pas fiable : elle est recalculée par
    # `assign_difficulty` (scores + percentile de la cible). On la neutralise ici.
    return {
        "text": text,
        "answers": clean,
        "correctAnswer": correct,
        "difficulty": "MEDIUM",
        "imageUrl": image,
    }


def yaml_scalar(value):
    return json.dumps(value, ensure_ascii=False)


def render(topic, questions):
    lines = ["topic:\n", f"  id: {topic['id']}\n", "  names:\n"]
    lines.append(f"    fr: {yaml_scalar(topic['nameFr'])}\n")
    if topic.get("nameEn"):
        lines.append(f"    en: {yaml_scalar(topic['nameEn'])}\n")
    lines.append(f"  description: {yaml_scalar(topic['description'])}\n")
    lines.append(f"  category: {topic['category']}\n")
    if topic.get("imageUrl"):
        lines.append(f"  imageUrl: {topic['imageUrl']}\n")
    lines.append("questions:\n")
    for question in questions:
        lines.append(f"  - text: {yaml_scalar(question['text'])}\n")
        if question.get("imageUrl"):
            lines.append(f"    imageUrl: {question['imageUrl']}\n")
        lines.append("    answers:\n")
        for choice in CHOICES:
            lines.append(f"      {choice}: {yaml_scalar(question['answers'][choice])}\n")
        lines.append(f"    correctAnswer: {question['correctAnswer']}\n")
        lines.append(f"    difficulty: {question['difficulty']}\n")
    return "".join(lines)


def batch_prompt(topic, count, targets, existing, focus, avoid, image_mode):
    payload = {
        "theme": topic["nameFr"],
        "description": topic["description"],
        "questions_to_write": count,
        "difficulty_percent_target": targets,
        "existing_questions_do_not_repeat": existing[-40:],
        "image_mode": image_mode,
    }
    if focus:
        payload["editorial_focus"] = focus
    if avoid:
        payload["avoid"] = avoid
    return (
        "Écris de nouvelles questions pour ce thème. "
        'Retourne un JSON {"questions":[{"text":"...","answers":{"A":"..","B":"..","C":"..","D":".."},'
        '"correctAnswer":"A","difficulty":"HARD"}]}. '
        "Aucune question ne doit répéter celles déjà listées. JSON uniquement.\n"
        + json.dumps(payload, ensure_ascii=False)
    )


SCORE_SYSTEM_PROMPT = """\
Tu évalues la difficulté factuelle de questions de quiz, de 0 (trivial, culture très large)
à 100 (expert : détail pointu, chiffre, date, piège de proximité). Sois discriminant et utilise
toute l'échelle. Réponds UNIQUEMENT par un objet JSON {"scores":{"<index>": <0-100>, ...}}."""


def score_difficulty(api_key, model, questions, batch=50):
    """Note chaque question de 0 à 100 (indépendant de la note demandée à la génération)."""
    scores = {}
    for start in range(0, len(questions), batch):
        chunk = questions[start : start + batch]
        payload = [{"index": start + i, "text": q["text"]} for i, q in enumerate(chunk)]
        result = call_api(
            api_key, model,
            "Note ces questions de 0 à 100.\n" + json.dumps(payload, ensure_ascii=False),
            system_prompt=SCORE_SYSTEM_PROMPT,
        )
        raw = result.get("scores") or {}
        for key, value in raw.items():
            try:
                scores[int(key)] = max(0.0, min(100.0, float(value)))
            except (TypeError, ValueError):
                continue
        log(f"    scores {len(scores)}/{len(questions)}")
    missing = [i for i in range(len(questions)) if i not in scores]
    for index in missing:
        scores[index] = 50.0
    return scores


def assign_difficulty(questions, scores, targets):
    """Assigne les niveaux aux percentiles de la cible : distribution exacte garantie."""
    count = len(questions)
    expected = target_counts(count, targets)
    order = sorted(range(count), key=lambda i: (scores[i], i))
    labels = [None] * count
    cursor = 0
    for level in LEVELS:
        for _ in range(expected[level]):
            if cursor >= count:
                break
            labels[order[cursor]] = level
            cursor += 1
    for index in range(count):
        if labels[index] is None:
            labels[index] = LEVELS[-1]
    for index, question in enumerate(questions):
        question["difficulty"] = labels[index]
    return expected


def generate(api_key, args, topic):
    questions = []
    seen = set()
    targets = parse_targets(args.targets)
    image_mode = bool(args.image_questions)
    zero_streak = 0
    while len(questions) < args.count:
        remaining = args.count - len(questions)
        # Toujours demander au moins 5 questions (marge pour les doublons/rejets).
        size = min(args.batch, max(5, remaining))
        log(f"  -> génération de {size} question(s) ({len(questions)}/{args.count})")
        result = call_api(
            api_key,
            args.model,
            batch_prompt(topic, size, targets, [q["text"] for q in questions], args.focus,
                         args.avoid, image_mode),
        )
        entries = result.get("questions") or []
        added = 0
        for entry in entries:
            try:
                question = validate_question(entry, len(questions) + added + 1)
            except ValueError as error:
                log(f"    x rejetée ({error})")
                continue
            if question["text"].lower() in seen:
                log(f"    x doublon ignoré : {question['text'][:60]}")
                continue
            seen.add(question["text"].lower())
            questions.append(question)
            added += 1
            if len(questions) >= args.count:
                break
        if added == 0:
            zero_streak += 1
            if zero_streak >= 6:
                raise SystemExit("aucune question valide reçue après 6 tentatives")
            log(f"    ! aucune question retenue, nouvelle tentative ({zero_streak}/6)")
        else:
            zero_streak = 0
    log("  -> notation de la difficulté (0-100) et affectation aux percentiles de la cible")
    scores = score_difficulty(api_key, args.model, questions)
    expected = assign_difficulty(questions, scores, targets)
    log("  difficulté : " + ", ".join(f"{lv}={expected[lv]}" for lv in LEVELS))
    return questions


def rebalance_only(api_key, args):
    """Réassigne la difficulté d'un fichier existant (scores + percentiles), sans regénérer."""
    import yaml  # noqa: PLC0415

    path = Path(args.rebalance_only)
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    names = data["topic"].get("names") or {}
    topic = {
        "id": data["topic"]["id"],
        "nameFr": names.get("fr"),
        "nameEn": names.get("en"),
        "description": data["topic"]["description"],
        "category": data["topic"]["category"],
        "imageUrl": data["topic"].get("imageUrl"),
    }
    questions = []
    for raw in data.get("questions") or []:
        questions.append({
            "text": raw["text"],
            "answers": {c: str(raw["answers"][c]) for c in CHOICES},
            "correctAnswer": raw["correctAnswer"],
            "difficulty": raw.get("difficulty", "MEDIUM"),
            "imageUrl": raw.get("imageUrl"),
        })
    log(f"  -> {len(questions)} question(s) à reclasser")
    scores = score_difficulty(api_key, args.model, questions)
    expected = assign_difficulty(questions, scores, parse_targets(args.targets))
    path.write_text(render(topic, questions), encoding="utf-8")
    log(f"✓ {path} : difficulté réaffectée ({', '.join(f'{lv}={expected[lv]}' for lv in LEVELS)})")
    return 0


def rewrite(api_key, args):
    """Régénère les questions d'un thème existant (durci), métadonnées et image conservées."""
    import yaml  # noqa: PLC0415

    path = Path(args.rewrite)
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    names = data["topic"].get("names") or {}
    topic = {
        "id": data["topic"]["id"],
        "nameFr": names.get("fr"),
        "nameEn": names.get("en"),
        "description": data["topic"]["description"],
        "category": data["topic"]["category"],
        "imageUrl": data["topic"].get("imageUrl"),
    }
    existing = data.get("questions") or []
    args.count = len(existing) or args.count
    args.avoid = (
        (args.avoid + "; " if args.avoid else "")
        + "réécris entièrement : ne réutilise PAS ces questions existantes — "
        + " | ".join(q["text"] for q in existing[:25])
    )
    log(f"  -> réécriture de {path.name} ({args.count} questions, contenu durci, sans doublon)")
    questions = generate(api_key, args, topic)
    text = render(topic, questions)
    reparsed = yaml.safe_load(text)
    path.write_text(text, encoding="utf-8")
    log(f"✓ {path} : {len(reparsed['questions'])} question(s) réécrites")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--id")
    parser.add_argument("--name-fr")
    parser.add_argument("--name-en")
    parser.add_argument("--description")
    parser.add_argument("--category", choices=CATEGORIES)
    parser.add_argument("--image-url")
    parser.add_argument("--out")
    parser.add_argument("--count", type=int, default=100)
    parser.add_argument("--batch", type=int, default=20)
    parser.add_argument("--targets", default="10/20/35/35")
    parser.add_argument("--focus")
    parser.add_argument("--avoid")
    parser.add_argument("--model", default="deepseek-chat")
    parser.add_argument("--image-questions", action="store_true",
                        help="le thème illustre chaque question (le modèle pose des textes ≤ 60 c)")
    parser.add_argument("--rebalance-only", metavar="FILE",
                        help="réaffecte uniquement la difficulté d'un fichier existant")
    parser.add_argument("--rewrite", metavar="FILE",
                        help="régénère les questions d'un thème existant (métadonnées conservées)")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    api_key = os.environ.get("QUIZUP_DEEPSEEK_API_KEY")
    if not api_key:
        raise SystemExit("QUIZUP_DEEPSEEK_API_KEY manquant (export QUIZUP_DEEPSEEK_API_KEY=\"$DEEPSEEK\")")

    if args.rebalance_only:
        return rebalance_only(api_key, args)

    if args.rewrite:
        return rewrite(api_key, args)

    if not (args.id and args.name_fr and args.description and args.category and args.out):
        raise SystemExit("--id/--name-fr/--description/--category/--out sont requis")
    if len(args.name_fr) > MAX_NAME:
        raise SystemExit(f"--name-fr trop long ({len(args.name_fr)} > {MAX_NAME})")
    if len(args.description) > MAX_DESCRIPTION:
        raise SystemExit(f"--description trop longue ({len(args.description)} > {MAX_DESCRIPTION})")

    topic = {
        "id": args.id,
        "nameFr": args.name_fr,
        "nameEn": args.name_en,
        "description": args.description,
        "category": args.category,
        "imageUrl": args.image_url,
    }

    questions = generate(api_key, args, topic)

    if args.dry_run:
        for question in questions[: min(len(questions), 3)]:
            log(f"[dry-run] {question['text']} | {question['answers']} -> {question['correctAnswer']}"
                f" ({question['difficulty']})")
        log("dry-run : aucun fichier écrit")
        return 0

    import yaml  # noqa: PLC0415 — dépendance facultative (dry-run sans écriture)

    text = render(topic, questions)
    reparsed = yaml.safe_load(text)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(text, encoding="utf-8")
    log(f"✓ {out} : {len(reparsed['questions'])} question(s) écrites")
    return 0


if __name__ == "__main__":
    sys.exit(main())
