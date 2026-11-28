#!/usr/bin/env python3
"""Format every resource JSON file with four-space indentation."""

import json
import os
import sys

ROOTS = ("src/main/resources", "src/generated/resources")


def reject_duplicates(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate key: {key}")
        result[key] = value
    return result


def format_file(path):
    with open(path, "r", encoding="utf-8-sig") as handle:
        original = handle.read()
    data = json.loads(original, object_pairs_hook=reject_duplicates)
    formatted = json.dumps(data, indent=4, ensure_ascii=False, allow_nan=False) + "\n"
    if original == formatted:
        return False
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(formatted)
    return True


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    total = 0
    changed = 0
    for relative in ROOTS:
        base = os.path.join(root, relative)
        for directory, _, files in os.walk(base):
            for name in files:
                if not name.endswith(".json"):
                    continue
                total += 1
                if format_file(os.path.join(directory, name)):
                    changed += 1
    print(f"formatted {changed}/{total} json files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
