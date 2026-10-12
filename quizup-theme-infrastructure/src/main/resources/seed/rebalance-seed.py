#!/usr/bin/env python3
"""
Rééquilibre la position de la bonne réponse (A/B/C/D) dans les fichiers de seed des questions.

Chaque thème doit viser ~25 % de bonnes réponses par lettre (±1) : sans ça, un joueur attentif
comprend vite qu'il suffit de répondre A (constat prod : 57,7 % de A au global, jusqu'à 100 %
sur certains thèmes).

Pour chaque question, la lettre cible est atteinte en **échangeant les valeurs des réponses**
entre la lettre courante et la lettre cible — dans le bloc `answers` et dans chaque bloc
`translations.<lang>.answers` — puis en réécrivant `correctAnswer`. Le texte correct suit la
lettre : aucune question ni réponse n'est modifiée, seul l'ordre affiché change.

L'écriture est **textuelle et minimale** : aucun reformattage YAML (guillemets, indentation et
ordre des clés préservés), donc les diffs restent lisibles.

Déterminisme : la séquence de lettres cibles est tirée par un PRNG seedé sur l'id du thème ;
une ré-exécution ne change plus rien (idempotent) et deux personnes obtiennent le même résultat.
Seuls les thèmes hors tolérance sont effectivement modifiés.

Usage :
    python3 rebalance-seed.py --check topics/*/*.yml   # code 1 si un thème dépasse ±1
    python3 rebalance-seed.py --dry-run topics/*/*.yml # détail des échanges, sans écrire
    python3 rebalance-seed.py topics/*/*.yml           # applique
"""
import argparse
import random
import re
import sys
from pathlib import Path

LETTERS = ("A", "B", "C", "D")
TOLERANCE = 1

QUESTION_START = re.compile(r"^(\s*)-\s+text:")
ANSWERS_HEADER = re.compile(r"^(\s*)answers:\s*$")
ANSWER_LINE = re.compile(r"^(\s*)([A-D]):(\s*)(.*?)(\s*)$")
CORRECT_LINE = re.compile(r"^(\s*correctAnswer:\s*)(\"?)([A-D])(\"?)(\s*)$")
TOPIC_ID = re.compile(r"^\s*id:\s*\"?([^\"\s]+)\"?\s*$")


class SeedError(Exception):
    pass


def read_topic_id(lines):
    for line in lines[:8]:
        match = TOPIC_ID.match(line)
        if match:
            return match.group(1)
    raise SeedError("id du thème introuvable")


def parse_questions(lines):
    """Découpe les questions et repère les blocs `answers` + la ligne `correctAnswer`."""
    starts = [index for index, line in enumerate(lines) if QUESTION_START.match(line)]
    if not starts:
        raise SeedError("aucune question trouvée")

    questions = []
    for position, start in enumerate(starts):
        end = starts[position + 1] if position + 1 < len(starts) else len(lines)
        question = {"answers_blocks": [], "correct_index": None, "correct_letter": None}

        index = start
        while index < end:
            header = ANSWERS_HEADER.match(lines[index])
            if header:
                block = []
                letter_indent = len(header.group(1)) + 2
                cursor = index + 1
                while cursor < end:
                    letter = ANSWER_LINE.match(lines[cursor])
                    if not letter or len(letter.group(1)) != letter_indent:
                        break
                    block.append((letter.group(2), cursor))
                    cursor += 1
                if [letter for letter, _ in block] != list(LETTERS):
                    raise SeedError(f"bloc answers incomplet ligne {index + 1}")
                question["answers_blocks"].append(block)
                index = cursor
                continue
            correct = CORRECT_LINE.match(lines[index])
            if correct:
                question["correct_index"] = index
                question["correct_letter"] = correct.group(3)
            index += 1

        if not question["answers_blocks"] or question["correct_index"] is None:
            raise SeedError(f"question incomplète ligne {start + 1}")
        questions.append(question)

    return questions


def target_sequence(theme_id, count):
    """Lettres cibles équilibrées (±1), mélangées de façon déterministe par thème."""
    generator = random.Random(theme_id)
    counts = {letter: count // 4 for letter in LETTERS}
    for letter in generator.sample(LETTERS, count % 4):
        counts[letter] += 1
    sequence = [letter for letter in LETTERS for _ in range(counts[letter])]
    generator.shuffle(sequence)
    return sequence


def count_letters(questions):
    counts = {letter: 0 for letter in LETTERS}
    for question in questions:
        counts[question["correct_letter"]] += 1
    return counts


def balanced(counts):
    values = list(counts.values())
    return max(values) - min(values) <= TOLERANCE


def swap_answers(lines, question, current, target):
    """Échange les valeurs des lettres `current` et `target` dans chaque bloc answers."""
    for block in question["answers_blocks"]:
        lines_by_letter = {letter: index for letter, index in block}
        left = ANSWER_LINE.match(lines[lines_by_letter[current]])
        right = ANSWER_LINE.match(lines[lines_by_letter[target]])
        lines[lines_by_letter[current]] = (
            left.group(1) + left.group(2) + ":" + left.group(3) + right.group(4) + left.group(5)
        )
        lines[lines_by_letter[target]] = (
            right.group(1) + right.group(2) + ":" + right.group(3) + left.group(4) + right.group(5)
        )

    match = CORRECT_LINE.match(lines[question["correct_index"]])
    lines[question["correct_index"]] = (
        match.group(1) + match.group(2) + target + match.group(4) + match.group(5)
    )


def rebalance_file(path, check_only, dry_run):
    original = path.read_text(encoding="utf-8")
    lines = original.splitlines(keepends=True)
    theme_id = read_topic_id(lines)
    questions = parse_questions(lines)
    before = count_letters(questions)

    if balanced(before):
        return {
            "path": path,
            "theme": theme_id,
            "questions": len(questions),
            "before": before,
            "after": before,
            "swaps": 0,
            "changed": False,
        }

    targets = target_sequence(theme_id, len(questions))
    swaps = 0
    for question, target in zip(questions, targets):
        current = question["correct_letter"]
        if current == target:
            continue
        swap_answers(lines, question, current, target)
        question["correct_letter"] = target
        swaps += 1

    after = count_letters(questions)
    updated = "".join(lines)

    if not check_only and not dry_run and updated != original:
        path.write_text(updated, encoding="utf-8")

    return {
        "path": path,
        "theme": theme_id,
        "questions": len(questions),
        "before": before,
        "after": after,
        "swaps": swaps,
        "changed": updated != original,
    }


def format_counts(counts):
    return " ".join(f"{letter}={counts[letter]}" for letter in LETTERS)


def main():
    parser = argparse.ArgumentParser(description="Rééquilibre les bonnes réponses A/B/C/D du seed.")
    parser.add_argument("files", nargs="+", type=Path)
    parser.add_argument("--check", action="store_true",
                        help="rapport seulement, code 1 si un thème dépasse ±1")
    parser.add_argument("--dry-run", action="store_true",
                        help="détaille les échanges sans écrire")
    args = parser.parse_args()

    unbalanced = []
    changed_files = 0
    total_swaps = 0

    for path in sorted(args.files):
        try:
            result = rebalance_file(path, args.check, args.dry_run)
        except SeedError as error:
            print(f"ERREUR {path}: {error}", file=sys.stderr)
            return 2

        if not balanced(result["before"]) or not balanced(result["after"]):
            unbalanced.append(result)
        if result["changed"]:
            changed_files += 1
        total_swaps += result["swaps"]

        if args.check or not balanced(result["before"]):
            status = "OK " if balanced(result["after"]) else "KO "
            print(f"{status}{result['theme']:<40} {format_counts(result['before'])}"
                  f"  ->  {format_counts(result['after'])}  ({result['swaps']} échanges)")

    if args.check:
        if unbalanced:
            print(f"\n{len(unbalanced)} thème(s) hors tolérance ±{TOLERANCE}.", file=sys.stderr)
            return 1
        print(f"\nTous les thèmes sont équilibrés (±{TOLERANCE}).")
        return 0

    verb = "simulé" if args.dry_run else "appliqué"
    print(f"\n{total_swaps} échange(s) {verb} sur {changed_files} fichier(s).")
    if not args.dry_run and total_swaps:
        print("Relancer --check pour vérifier.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
