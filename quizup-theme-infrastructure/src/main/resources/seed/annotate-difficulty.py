#!/usr/bin/env python3
"""
AnnOte la difficulté initiale (`difficulty`) des questions d'un fichier de seed YAML.

La difficulté du seed est une classification **initiale** (remplacée par le calcul sur les
réponses réelles dès que l'échantillon est suffisant). L'écriture est **textuelle et minimale** :
la ligne `difficulty: <NIVEAU>` est insérée après `correctAnswer:` (ou remplacée si elle existe),
sans reformatage du YAML.

Prérequis :
    pip install pyyaml

Usage :
    python3 annotate-difficulty.py --template topics/movies/cinema.yml     # questions à classer (JSON)
    python3 annotate-difficulty.py topics/movies/cinema.yml --mapping /tmp/mapping.json
    python3 annotate-difficulty.py --check topics/*/*.yml                  # ratio ±1 par niveau + complétude
    python3 annotate-difficulty.py --counts topics/movies/cinema.yml       # comptes et déficits vers 25 %

Mapping : objet JSON `{"<index>": "EASY|MEDIUM|HARD|EXPERT"}`, index = position de la question
dans le fichier (0-based), tel que listé par `--template`.
"""
import argparse
import json
import sys
from pathlib import Path

LEVELS = ("EASY", "MEDIUM", "HARD", "EXPERT")
START = "  - "
CORRECT = "    correctAnswer:"
DIFFICULTY = "    difficulty:"


def segments(lines):
    starts = [i for i, line in enumerate(lines) if line.startswith(START)]
    out = []
    for position, start in enumerate(starts):
        end = starts[position + 1] if position + 1 < len(starts) else len(lines)
        correct = difficulty = None
        for i in range(start, end):
            if lines[i].startswith(CORRECT):
                correct = i
            if lines[i].startswith(DIFFICULTY):
                difficulty = i
        out.append({"start": start, "end": end, "correct": correct, "difficulty": difficulty})
    return out


def read(path):
    return Path(path).read_text(encoding="utf-8").splitlines(keepends=True)


def question_text(lines, segment):
    value = lines[segment["start"]].strip()
    prefix = "- text: "
    if value.startswith(prefix):
        value = value[len(prefix):].strip()
    if value.startswith('"') and value.endswith('"'):
        value = value[1:-1]
    return value


def parse_level(value):
    level = str(value).strip().upper()
    if level not in LEVELS:
        raise SystemExit(f"niveau invalide : {value!r} (attendu : {', '.join(LEVELS)})")
    return level


def apply_mapping(path, mapping):
    lines = read(path)
    questions = segments(lines)
    for key, value in mapping.items():
        index = int(key)
        if index < 0 or index >= len(questions):
            raise SystemExit(f"{path}: index {index} hors bornes (0..{len(questions) - 1})")
        segment = questions[index]
        level = parse_level(value)
        line = f"{DIFFICULTY} {level}\n"
        if segment["difficulty"] is not None:
            lines[segment["difficulty"]] = line
        elif segment["correct"] is not None:
            lines.insert(segment["correct"] + 1, line)
        else:
            raise SystemExit(f"{path}: question {index} sans correctAnswer")
    Path(path).write_text("".join(lines), encoding="utf-8")
    print(f"✓ {Path(path).name}: {len(mapping)} difficulté(s) appliquée(s)")


def counts_of(path):
    lines = read(path)
    questions = segments(lines)
    counts = {level: 0 for level in LEVELS}
    missing = 0
    for segment in questions:
        if segment["difficulty"] is None:
            missing += 1
            continue
        value = lines[segment["difficulty"]].split(":", 1)[1].strip().upper()
        if value not in LEVELS:
            raise SystemExit(f"{path}: niveau inconnu {value!r}")
        counts[value] += 1
    return counts, len(questions), missing


def check(paths):
    failures = 0
    for path in paths:
        counts, total, missing = counts_of(path)
        target = total / len(LEVELS)
        problems = []
        if missing:
            problems.append(f"{missing} question(s) sans difficulté")
        for level in LEVELS:
            if abs(counts[level] - target) > 1:
                problems.append(f"{level}={counts[level]} (cible {target:.2f} ±1)")
        status = "OK" if not problems else "KO"
        print(f"{status} {Path(path).name:32s} n={total:3d} "
              f"A/EASY={counts['EASY']} MEDIUM={counts['MEDIUM']} HARD={counts['HARD']} EXPERT={counts['EXPERT']}"
              + ("  — " + "; ".join(problems) if problems else ""))
        failures += 1 if problems else 0
    if failures:
        print(f"\n{failures} fichier(s) hors tolérance")
        return 1
    print("\nTous les fichiers sont complets et équilibrés (±1).")
    return 0


def template(path):
    lines = read(path)
    questions = segments(lines)
    data = {
        str(index): {"text": question_text(lines, segment), "difficulty": None}
        for index, segment in enumerate(questions)
    }
    print(json.dumps(data, ensure_ascii=False, indent=1))
    print(f"\n# {len(questions)} question(s) — remplir les 'difficulty' puis relancer avec --mapping",
          file=sys.stderr)


def show_counts(path):
    counts, total, missing = counts_of(path)
    target = total / len(LEVELS)
    print(f"{Path(path).name}: n={total} (sans difficulté: {missing})")
    for level in LEVELS:
        deficit = max(0, round(target) - counts[level])
        print(f"  {level:6s} présent={counts[level]:3d}  cible≈{target:.2f}  déficit={deficit}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("files", nargs="*", help="fichiers YAML de seed")
    parser.add_argument("--template", metavar="FILE", help="liste JSON des questions à classer")
    parser.add_argument("--mapping", metavar="FILE", help="mapping JSON index → niveau à appliquer")
    parser.add_argument("--check", action="store_true", help="vérifie complétude et ratio ±1")
    parser.add_argument("--counts", action="store_true", help="comptes par niveau et déficits vers 25 %")
    args = parser.parse_args()

    if args.template:
        template(args.template)
    if args.mapping:
        apply_mapping(args.files[0], json.loads(Path(args.mapping).read_text(encoding="utf-8")))
    if args.check:
        return check(args.files)
    if args.counts:
        for path in args.files:
            show_counts(path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
