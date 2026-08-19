#!/bin/sh
# Run from a directory containing the unpacked artifact.tar site files.
# Usage: sh tech-docs/scripts/link-check.sh
set -e
npx --yes linkinator . --recurse --markdown \
  --skip 'https://ministryofjustice\.github\.io/laa-provider-data-platform/' \
  --skip 'javascripts/govuk_frontend\.js' \
  --skip 'http://localhost:.*' \
  --skip 'https://github\.com/ministryofjustice/laa-data-provider-data' \
  --skip 'https://github\.com/ministryofjustice/laa-provider-data-platform/issues/new(\?.*)?' \
  --skip 'images/favicon' \
  --skip '\.apps\.live\.cloud-platform\.service\.justice\.gov\.uk' \
  --skip 'https://netflixtechblog\.com/'
