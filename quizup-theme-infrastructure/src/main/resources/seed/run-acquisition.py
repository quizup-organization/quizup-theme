#!/usr/bin/env python3
"""
Runner du lot « acquisition » : enchaîne la production des thèmes (nouveaux + réécritures).

Piloté par `seed/acquisition.yml` (thèmes neufs) et par la découverte des fichiers existants
(phase `rewrite`). Pour chaque thème :

1. `generate-seed.py` (nouveau) ou `--rewrite` (existant) → questions FR ;
2. `translate-seed.py` → `translations.en` ;
3. `rebalance-seed.py` → équilibrage A–D.

Idempotent et **reprenable** : un état (`seed/.acquisition-state.json`) mémorise les thèmes
terminés ; relancer saute ce qui l'est déjà. La couverture image (Wikimedia → MinIO) n'est pas
gérée ici : voir le workspace (`make seed-images ACTION=fetch|rewrite|check` puis `publish`).

Prérequis :
    set -a; source .env; set +a          # DEEPSEEK
    export QUIZUP_DEEPSEEK_API_KEY="$DEEPSEEK"
    pip install pyyaml requests

Usage :
    python3 run-acquisition.py --list
    python3 run-acquisition.py --phase new                 # tous les nouveaux thèmes
    python3 run-acquisition.py --phase new --limit 3       # 3 thèmes (validation)
    python3 run-acquisition.py --phase new --only topic-one-piece
    python3 run-acquisition.py --phase rewrite --limit 5   # réécritures (contenu durci)
    python3 run-acquisition.py --phase all

Options :
    --phase new|rewrite|all   (défaut new)
    --only ID                 ne traite qu'un thème (id du manifeste ou nom de fichier)
    --limit N                 plafonne le nombre de thèmes traités
    --list                    liste l'état des thèmes, sans rien produire
    --dry-run                 affiche les commandes, sans exécuter
    --model NAME              modèle DeepSeek (défaut deepseek-chat)
    --targets A/B/C/D         répartition de difficulté (défaut 10/20/35/35)
"""
import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
MANIFEST = HERE / "acquisition.yml"
STATE = HERE / ".acquisition-state.json"
TOPICS = HERE / "topics"
# Thèmes volontairement FR-only (pas de traduction EN).
FR_ONLY = {"orthographe-grammaire"}
PYTHON = sys.executable


def log(message):
    print(message, flush=True)


def load_state():
    if STATE.exists():
        return json.loads(STATE.read_text(encoding="utf-8"))
    return {"done": []}


def save_state(state):
    STATE.write_text(json.dumps(state, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def run(command, dry_run):
    log("  $ " + " ".join(command))
    if dry_run:
        return 0
    result = subprocess.run(command, cwd=HERE, env=os.environ)
    return result.returncode


def run_steps(steps, dry_run):
    """Exécute une suite de commandes ; s'arrête à la première en échec (thème non marqué fait)."""
    for command in steps:
        if run(command, dry_run) != 0:
            log(f"  ! échec, thème laissé pour reprise : {' '.join(command[:2])}")
            return False
    return True


def new_themes(dry_run, only, limit, model, targets):
    if not MANIFEST.exists():
        raise SystemExit(f"manifeste introuvable : {MANIFEST}")
    manifest = yaml.safe_load(MANIFEST.read_text(encoding="utf-8")) or {}
    themes = manifest.get("themes") or []
    state = load_state()
    done = set(state["done"])
    processed = 0
    failures = []
    for theme in themes:
        theme_id = theme["id"]
        if only and theme_id != only and theme.get("file") != only:
            continue
        if theme_id in done:
            log(f"= {theme_id} (déjà fait)")
            continue
        if limit and processed >= limit:
            break
        out = HERE / theme["file"]
        if out.exists():
            log(f"= {theme_id} (fichier présent, marqué fait)")
            done.add(theme_id)
            continue
        processed += 1
        log(f"\n== {theme_id} ({theme['category']})")
        generate = [
            PYTHON, "generate-seed.py",
            "--id", theme_id,
            "--name-fr", theme["name_fr"],
            "--category", theme["category"],
            "--description", theme["description"],
            "--out", theme["file"],
            "--model", model,
            "--targets", targets,
        ]
        if theme.get("name_en"):
            generate += ["--name-en", theme["name_en"]]
        if theme.get("image_url"):
            generate += ["--image-url", theme["image_url"]]
        if theme.get("focus"):
            generate += ["--focus", theme["focus"]]
        if theme.get("avoid"):
            generate += ["--avoid", theme["avoid"]]
        if theme.get("count"):
            generate += ["--count", str(theme["count"])]
        steps = [generate]
        if theme_id.removeprefix("topic-") not in FR_ONLY:
            steps.append([PYTHON, "translate-seed.py", theme["file"]])
        steps.append([PYTHON, "rebalance-seed.py", theme["file"]])
        if run_steps(steps, dry_run):
            done.add(theme_id)
            if not dry_run:
                save_state({"done": sorted(done)})
        else:
            failures.append(theme_id)
    log(f"\nPhase new : {processed} traité(s), {len(failures)} échec(s)"
        + (f" : {', '.join(failures)}" if failures else "."))
    return 0


def rewrite_themes(dry_run, only, limit, model, targets):
    state = load_state()
    done = set(state["done"])
    manifest = {}
    if MANIFEST.exists():
        manifest = yaml.safe_load(MANIFEST.read_text(encoding="utf-8")) or {}
    new_files = {t.get("file") for t in (manifest.get("themes") or [])}
    processed = 0
    failures = []
    for path in sorted(TOPICS.glob("*/*.yml")):
        relative = str(path.relative_to(HERE))
        if relative in new_files:
            continue
        key = f"rewrite:{path.stem}"
        if only and only not in {path.stem, relative}:
            continue
        if key in done:
            log(f"= {relative} (déjà réécrit)")
            continue
        if limit and processed >= limit:
            break
        processed += 1
        log(f"\n== réécriture {relative}")
        steps = [[PYTHON, "generate-seed.py", "--rewrite", relative,
                  "--model", model, "--targets", targets]]
        if path.stem not in FR_ONLY:
            steps.append([PYTHON, "translate-seed.py", relative])
        steps.append([PYTHON, "rebalance-seed.py", relative])
        if run_steps(steps, dry_run):
            done.add(key)
            if not dry_run:
                save_state({"done": sorted(done)})
        else:
            failures.append(relative)
    log(f"\nPhase rewrite : {processed} traité(s), {len(failures)} échec(s)"
        + (f" : {', '.join(failures)}" if failures else "."))
    return 0


def list_state():
    state = load_state()
    done = set(state["done"])
    manifest = {}
    if MANIFEST.exists():
        manifest = yaml.safe_load(MANIFEST.read_text(encoding="utf-8")) or {}
    log("== Nouveaux thèmes ==")
    for theme in manifest.get("themes") or []:
        mark = "x" if theme["id"] in done else " "
        exists = "fichier" if (HERE / theme["file"]).exists() else "—"
        log(f"  [{mark}] {theme['id']:28s} {theme['category']:14s} {exists}")
    log("== Réécritures ==")
    new_files = {t.get("file") for t in (manifest.get("themes") or [])}
    rewritten = 0
    for path in sorted(TOPICS.glob("*/*.yml")):
        if str(path.relative_to(HERE)) in new_files:
            continue
        mark = "x" if f"rewrite:{path.stem}" in done else " "
        rewritten += 0 if mark == "x" else 1
        log(f"  [{mark}] {path.relative_to(HERE)}")
    log(f"\n  reste à réécrire : {rewritten}")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phase", choices=("new", "rewrite", "all"), default="new")
    parser.add_argument("--only")
    parser.add_argument("--limit", type=int)
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--model", default="deepseek-chat")
    parser.add_argument("--targets", default="10/20/35/35")
    args = parser.parse_args()

    if args.list:
        return list_state()

    if not os.environ.get("QUIZUP_DEEPSEEK_API_KEY"):
        raise SystemExit("QUIZUP_DEEPSEEK_API_KEY manquant (export QUIZUP_DEEPSEEK_API_KEY=\"$DEEPSEEK\")")

    if args.phase in ("new", "all"):
        new_themes(args.dry_run, args.only, args.limit, args.model, args.targets)
    if args.phase in ("rewrite", "all"):
        rewrite_themes(args.dry_run, args.only, args.limit, args.model, args.targets)
    return 0


if __name__ == "__main__":
    sys.exit(main())
