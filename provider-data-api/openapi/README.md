# OpenAPI spec source

This directory holds the OpenAPI specification split into per-concept files, so that changes to
one area (e.g. offices, liaison managers) are reviewable in isolation instead of as a diff against
one large file.

- `index.yaml` - root document (`openapi`, `info`, `security`), and a `$ref` into every path,
  schema and parameter fragment below.
- `paths/*.yaml` - one file per resource area, each keyed by the path(s) it defines.
- `components/schemas/*.yaml` - one file per resource area, each keyed by schema name.
- `components/parameters/common.yaml` - shared path/query parameters.
- `components/security-schemes.yaml` - shared security scheme definitions.

## Editing

1. Edit the relevant fragment file(s) under `paths/` and `components/`.
2. Lint the spec (see [Linting](#linting) below) and fix any errors.
3. Re-bundle into the committed single-file spec:

   ```sh
   npx --yes @redocly/cli bundle provider-data-api/openapi/index.yaml \
     -o provider-data-api/bundled-openapi-spec.yaml --ext yaml
   ```

   Or via Gradle: `./gradlew :provider-data-api:bundleOpenApi`.

4. Commit both the fragment changes and the regenerated `bundled-openapi-spec.yaml`.

CI checks that `bundled-openapi-spec.yaml` matches what bundling the fragments produces, and fails
if they have drifted apart (e.g. someone forgot to re-bundle, or hand-edited the bundle directly).
CI also lints the spec (see below).

## Linting

```sh
npx --yes @redocly/cli lint provider-data-api/openapi/index.yaml
```

Or via Gradle: `./gradlew :provider-data-api:lintOpenApi`.

Lint rules are configured in `provider-data-api/redocly.yaml`. Some rules are deliberately
downgraded there, with a comment explaining why - check that file before assuming a warning can be
ignored.

## Why a committed bundle

`bundled-openapi-spec.yaml` is the single file actually consumed by the build
(`openApiGenerate`), the e2e response validator, and the published tech-docs site. Committing it
means most contributors and CI jobs never need Node/`npx` - only people editing the OpenAPI spec,
and the one CI job that verifies the bundle is up to date.

## Adding a new fragment

Cross-file references must point at the fragment file the referenced schema/parameter actually
lives in, e.g. `$ref: '../components/schemas/office.yaml#/OfficeV2'`. References within the same
file stay as local pointers, e.g. `$ref: '#/OfficeV2'`. When adding a new schema or path, also add
a corresponding `$ref` entry in `index.yaml` so it's included in the bundle.
