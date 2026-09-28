#!/usr/bin/env python3
"""
Vérifie que chaque imageUrl des fichiers YAML répond correctement.

Usage :
    pip install pyyaml requests
    python3 check-images.py topics/*/*.yml

Affiche les URL cassées (404, timeout, contenu non-image) avec la question concernée.
Code de sortie 1 si au moins une URL est cassée.
"""
import sys
import time
import yaml
import requests

HEADERS = {"User-Agent": "QuizImageChecker/1.0 (verification de liens)"}


def collect_urls(path):
    with open(path, encoding="utf-8") as f:
        data = yaml.safe_load(f)
    urls = []
    if data["topic"].get("imageUrl"):
        urls.append(("topic", data["topic"]["imageUrl"]))
    for i, q in enumerate(data.get("questions", []), 1):
        if q.get("imageUrl"):
            urls.append((f"Q{i}: {q['text'][:50]}", q["imageUrl"]))
    return urls


def check(url):
    try:
        r = requests.get(url, headers=HEADERS, timeout=20, stream=True, allow_redirects=True)
        ctype = r.headers.get("Content-Type", "")
        r.close()
        if r.status_code != 200:
            return f"HTTP {r.status_code}"
        if not ctype.startswith("image/"):
            return f"type inattendu : {ctype}"
        return None
    except requests.RequestException as e:
        return f"erreur réseau : {e.__class__.__name__}"


def main(files):
    broken = 0
    for path in files:
        print(f"\n=== {path} ===")
        for label, url in collect_urls(path):
            err = check(url)
            if err:
                broken += 1
                print(f"  KO  [{label}] {err}\n      {url}")
            else:
                print(f"  OK  [{label}]")
            time.sleep(0.3)  # rester poli avec Wikimedia
    print(f"\n{broken} URL cassée(s)")
    sys.exit(1 if broken else 0)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(2)
    main(sys.argv[1:])