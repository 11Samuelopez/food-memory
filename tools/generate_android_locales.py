#!/usr/bin/env python3
"""Generate Android string resources from the reviewed English/Spanish JSON catalogs."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOGS = {
    "en": ROOT / "translations/en.json",
    "es": ROOT / "translations/es.json",
}
ANDROID_LOCALES = {"en": "values-en", "es": "values"}
NON_TRANSLATABLE = {"app_name", "home_brand", "home_manual_marker", "session_brand"}
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[a-zA-Z]")


def escape_android_xml(value: str) -> str:
    # Escape Android resource string quotes in addition to XML metacharacters.
    value = value.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def main() -> None:
    catalogs = {locale: json.loads(path.read_text(encoding="utf-8")) for locale, path in CATALOGS.items()}
    english_keys = set(catalogs["en"])
    spanish_keys = set(catalogs["es"])
    if english_keys != spanish_keys:
        raise SystemExit(f"Locale key mismatch: only en={english_keys - spanish_keys}; only es={spanish_keys - english_keys}")
    for key in english_keys:
        for locale, value in (("en", catalogs["en"][key]), ("es", catalogs["es"][key])):
            if not isinstance(value, str) or not value.strip():
                raise SystemExit(f"Empty/non-string translation: {locale}.{key}")
        if sorted(PLACEHOLDER.findall(catalogs["en"][key])) != sorted(PLACEHOLDER.findall(catalogs["es"][key])):
            raise SystemExit(f"Format placeholder mismatch: {key}")

    # Ensure every stringResource(R.string.*) in app code exists in both catalogs.
    source_names = set()
    for source in (ROOT / "app/src/main/java").rglob("*.kt"):
        source_names.update(re.findall(r"R\.string\.([A-Za-z0-9_]+)", source.read_text(encoding="utf-8")))
    missing = source_names - english_keys
    if missing:
        raise SystemExit(f"Missing app strings: {sorted(missing)}")

    for locale, catalog in catalogs.items():
        output = ROOT / "app/src/main/res" / ANDROID_LOCALES[locale] / "strings.xml"
        lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
        for key, value in catalog.items():
            translatable = ' translatable="false"' if key in NON_TRANSLATABLE else ""
            lines.append(f'    <string name="{key}"{translatable}>{escape_android_xml(value)}</string>')
        lines.append("</resources>")
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Generated {len(english_keys)} Android strings for English and Spanish.")


if __name__ == "__main__":
    main()
