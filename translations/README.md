# Translation catalogs

`en.json` is the canonical English catalog and `es.json` contains its Spanish translation. Keep the same keys and Android format placeholders (for example `%1$s`) in both files. Add user-facing copy to both catalogs rather than embedding it in Kotlin.

Android reads generated resources from `app/src/main/res/values/strings.xml` (Spanish) and `app/src/main/res/values-en/strings.xml` (English). The demo currently selects Spanish in `MainActivity`; change its active language tag when adding a language preference. Regenerate the resources after editing either JSON file:

```bash
python3 tools/generate_android_locales.py
```

The generator checks that both locales have matching keys and placeholders, every catalog value is non-empty, and every `R.string.*` reference in app source exists in both catalogs.
