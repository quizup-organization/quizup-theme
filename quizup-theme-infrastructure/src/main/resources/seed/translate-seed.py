#!/usr/bin/env python3
"""
Enrichit les fichiers de seed des questions avec leur traduction anglaise (`translations.en`).

Le français reste la langue source (`text`/`answers`) ; on ajoute l'anglais lu par les duels
bilingues. Le schéma n'est pas modifié (le loader refuse une traduction `fr`).

Génération via l'API DeepSeek (mode JSON strict) puis validation **identique au loader** :
text non vide ≤ 255, exactement les réponses A–D non vides, aucune clé `fr`.

L'écriture est **textuelle et minimale** : le bloc `translations:` est inséré après le
`correctAnswer:` de chaque question (aucun reformatage du YAML existant).

Prérequis :
    pip install pyyaml requests

Usage :
    set -a; source ../../../../../../.env; set +a   # QUIZUP_DEEPSEEK_API_KEY
    python3 translate-seed.py --check topics/*/*.yml
    python3 translate-seed.py topics/*/*.yml
    python3 translate-seed.py --dry-run topics/movies/cinema.yml

Options :
    --check          rapport de couverture, aucune génération, code 1 si manquant
    --dry-run        affiche des blocs générés sans écrire
    --force          retraduit les questions qui ont déjà un `en`
    --batch N        questions par appel API (défaut 15)
    --model NAME     modèle DeepSeek (défaut deepseek-chat)
    --exclude NAME   fichier (sans .yml) non traduit (défaut : orthographe-grammaire)
"""
import argparse
import json
import os
import sys
import time
from pathlib import Path

import requests
import yaml

API_URL = "https://api.deepseek.com/chat/completions"
MAX_TEXT = 255
CHOICES = ("A", "B", "C", "D")
DEFAULT_EXCLUDES = {"orthographe-grammaire"}

SYSTEM_PROMPT = (
    "You are a professional French-to-English translator for a quiz game. "
    "Translate each question and its four answers into natural, idiomatic English. "
    "Keep proper nouns, brand names, titles, dates, numbers and units unchanged. "
    "Keep the four answers distinct and aligned with the same A/B/C/D keys. "
    "Do not add explanations. Keep the question text under 240 characters. "
    "Answer ONLY with a JSON object."
)


def log(message):
    print(message, flush=True)


def chunks(items, size):
    for i in range(0, len(items), size):
        yield items[i : i + size]


def call_api(api_key, model, batch, attempts=4):
    payload = [
        {"index": index, "question": source["text"], "answers": source["answers"]}
        for index, source in batch
    ]
    user = (
        'Translate these quiz questions to English. Return JSON: {"translations":'
        '[{"index": <int>, "text": "...", "answers": {"A": "...", "B": "...", '
        '"C": "...", "D": "..."}}]}\n' + json.dumps(payload, ensure_ascii=False)
    )
    last_error = None
    for attempt in range(attempts):
        try:
            response = requests.post(
                API_URL,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "temperature": 0.2,
                    "max_tokens": 8192,
                    "response_format": {"type": "json_object"},
                    "messages": [
                        {"role": "system", "content": SYSTEM_PROMPT},
                        {"role": "user", "content": user},
                    ],
                },
                timeout=240,
            )
            if response.status_code in (429, 500, 502, 503, 504):
                raise RuntimeError(f"HTTP {response.status_code}")
            response.raise_for_status()
            content = response.json()["choices"][0]["message"]["content"]
            return json.loads(content)
        except Exception as error:  # noqa: BLE001 — retry réseau/API/JSON
            last_error = error
            wait = 2**attempt
            log(f"    ! tentative {attempt + 1}/{attempts} échouée ({error}), retry {wait}s")
            time.sleep(wait)
    raise RuntimeError(f"échec API après {attempts} tentatives : {last_error}")


def validate_translation(entry):
    text = str(entry.get("text", "")).strip()
    if not text:
        raise ValueError("texte vide")
    if len(text) > MAX_TEXT:
        raise ValueError(f"texte trop long ({len(text)} > {MAX_TEXT})")
    answers = entry.get("answers") or {}
    if set(answers.keys()) != set(CHOICES):
        raise ValueError(f"réponses != A–D : {sorted(answers.keys())}")
    clean = {}
    for choice in CHOICES:
        value = str(answers.get(choice, "")).strip()
        if not value:
            raise ValueError(f"réponse {choice} vide")
        clean[choice] = value
    return text, clean


def yaml_scalar(value):
    """Chaîne YAML double-quotée valide (JSON est un sous-ensemble de YAML)."""
    return json.dumps(value, ensure_ascii=False)


def translation_block(text, answers, indent=4):
    pad = " " * indent
    lines = [
        f"{pad}translations:\n",
        f"{pad}  en:\n",
        f"{pad}    text: {yaml_scalar(text)}\n",
        f"{pad}    answers:\n",
    ]
    for choice in CHOICES:
        lines.append(f"{pad}      {choice}: {yaml_scalar(answers[choice])}\n")
    return lines


def question_segments(lines):
    """Découpe les lignes par question (liste `questions:`) et localise les blocs existants."""
    starts = [i for i, line in enumerate(lines) if line.startswith("  - ")]
    segments = []
    for position, start in enumerate(starts):
        end = starts[position + 1] if position + 1 < len(starts) else len(lines)
        correct = None
        translation_start = None
        translation_end = None
        for i in range(start, end):
            line = lines[i]
            if line.startswith("    correctAnswer:"):
                correct = i
            if line.startswith("    translations:"):
                translation_start = i
                j = i + 1
                while j < end and (not lines[j].strip() or lines[j].startswith("      ")):
                    j += 1
                translation_end = j
        segments.append((start, end, correct, translation_start, translation_end))
    return segments


def rewrite(lines, segments, blocks, force):
    output = []
    cursor = 0
    for index, (start, end, correct, translation_start, translation_end) in enumerate(segments):
        block = blocks.get(index)
        # Préambule (`topic:`…) et lignes hors question préservés tels quels.
        output.extend(lines[cursor:start])
        for i in range(start, end):
            if (
                force
                and translation_start is not None
                and translation_start <= i < translation_end
            ):
                continue
            output.append(lines[i])
            if block is not None and i == correct:
                if output and not output[-1].endswith("\n"):
                    output[-1] += "\n"
                output.extend(block)
        cursor = end
    output.extend(lines[cursor:])
    return output


def process_file(path, api_key, args, totals):
    text = Path(path).read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    data = yaml.safe_load(text)
    questions = data.get("questions") or []
    segments = question_segments(lines)
    if len(segments) != len(questions):
        raise SystemExit(
            f"{path}: {len(segments)} segment(s) pour {len(questions)} question(s) — structure inattendue"
        )

    todo = []
    for index, question in enumerate(questions):
        has_en = bool((question.get("translations") or {}).get("en"))
        if has_en and not args.force:
            continue
        answers = question.get("answers") or {}
        if set(answers.keys()) != set(CHOICES):
            raise SystemExit(f"{path}: Q{index + 1} n'a pas exactement les réponses A–D")
        todo.append(
            (index, {"text": question["text"], "answers": {c: str(answers[c]) for c in CHOICES}})
        )

    if not todo:
        log(f"  = {Path(path).name}: complet ({len(questions)} questions)")
        return 0
    if args.check:
        log(f"  ! {Path(path).name}: {len(todo)}/{len(questions)} sans traduction EN")
        totals["missing"] += len(todo)
        return 0

    if args.dry_run:
        result = call_api(api_key, args.model, todo[: args.batch])
        entries = {int(entry.get("index")): entry for entry in result.get("translations") or []}
        for index, _source in todo[: min(len(todo), 3)]:
            text_en, answers_en = validate_translation(entries[index])
            log(f"  [dry-run] Q{index + 1}: {text_en} | {answers_en}")
        return 0

    generated = {}
    generated_data = {}
    for batch in chunks(todo, args.batch):
        result = call_api(api_key, args.model, batch)
        entries = {int(entry.get("index")): entry for entry in result.get("translations") or []}
        for index, _source in batch:
            entry = entries.get(index)
            if entry is None:
                raise RuntimeError(f"{path}: réponse API sans l'index {index}")
            text_en, answers_en = validate_translation(entry)
            generated[index] = translation_block(text_en, answers_en)
            generated_data[index] = (text_en, answers_en)
        log(f"    {Path(path).name}: {len(generated)}/{len(todo)} traduites")

    output = rewrite(lines, segments, generated, args.force)
    new_text = "".join(output)
    reparsed = yaml.safe_load(new_text)
    reparsed_questions = reparsed.get("questions") or []
    if len(reparsed_questions) != len(questions):
        raise RuntimeError(f"{path}: relecture YAML invalide après insertion")
    for index, (text_en, answers_en) in generated_data.items():
        english = reparsed_questions[index]["translations"]["en"]
        if english["text"] != text_en or english["answers"] != answers_en:
            raise RuntimeError(f"{path}: relecture incohérente pour la question {index + 1}")

    tmp = Path(f"{path}.tmp")
    tmp.write_text(new_text, encoding="utf-8")
    os.replace(tmp, path)
    totals["translated"] += len(generated)
    log(f"  ✓ {Path(path).name}: {len(generated)} traduction(s) ajoutée(s)")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("files", nargs="+", help="fichiers YAML de seed (glob expansé par le shell)")
    parser.add_argument("--check", action="store_true", help="rapport de couverture, sans API")
    parser.add_argument("--dry-run", action="store_true", help="génère sans écrire")
    parser.add_argument("--force", action="store_true", help="retraduit les `en` existants")
    parser.add_argument("--batch", type=int, default=15, help="questions par appel API")
    parser.add_argument("--model", default="deepseek-chat", help="modèle DeepSeek")
    parser.add_argument(
        "--exclude",
        action="append",
        default=[],
        help="nom de fichier (sans .yml) à ignorer (cumulable)",
    )
    args = parser.parse_args()

    excludes = DEFAULT_EXCLUDES | set(args.exclude or [])
    files = []
    for pattern in args.files:
        files.extend(sorted(Path().glob(pattern)) if any(c in pattern for c in "*?[") else [Path(pattern)])
    files = [f for f in files if f.suffix == ".yml" and f.stem not in excludes]

    if not files:
        raise SystemExit("aucun fichier de seed à traiter")

    totals = {"missing": 0, "translated": 0}
    if args.check:
        for path in files:
            process_file(path, None, args, totals)
        log(f"\nCouverture : {totals['missing']} question(s) sans EN")
        return 1 if totals["missing"] else 0

    api_key = os.environ.get("QUIZUP_DEEPSEEK_API_KEY")
    if not api_key:
        raise SystemExit("QUIZUP_DEEPSEEK_API_KEY manquant (source .env)")

    limit = 1 if args.dry_run else len(files)
    for path in files[:limit]:
        process_file(path, api_key, args, totals)
        if args.dry_run:
            log("dry-run : aucun fichier écrit")
    log(f"\nTerminé : {totals['translated']} traduction(s) ajoutée(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
