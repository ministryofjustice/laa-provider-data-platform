#!/usr/bin/env sh
# Generates API spec pages for the tech-docs site.
# Run from the repository root (done automatically by `make preview` in tech-docs/).

set -e

SPEC=provider-data-api/laa-data-pda.yml
OUT=tech-docs/source/pdp-docs/api

# Copy spec YAML so the CDN-based renderers (Redoc, Swagger UI, Scalar, Elements) can load it
cp "$SPEC" "$OUT/laa-data-pda.yml"

# Widdershins — Markdown rendered inside the MoJ tech-docs chrome
npx --yes widdershins \
  --search false \
  --language_tabs 'shell:Shell' \
  --outfile /tmp/widdershins-raw.md \
  "$SPEC"

# Strip widdershins' own front matter; prepend a minimal title so Middleman
# applies the MoJ layout (header/footer) but omit weight so it has no nav entry
{
  printf -- '---\ntitle: LAA Provider Data API – Widdershins\n---\n\n'
  awk 'BEGIN{p=0;n=0} !p && /^---/{n++; if(n==2){p=1}; next} p{print}' /tmp/widdershins-raw.md
} > "$OUT/widdershins.html.md"

echo "API spec pages generated in $OUT"
