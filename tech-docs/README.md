# Deployed technical documentation

These files are used to build the technical documentations using the
[GOV.UK Tech Docs Template](https://github.com/alphagov/tech-docs-template).

The published technical documentation can be found
[here](https://ministryofjustice.github.io/laa-provider-data-platform/).

## How to modify the technical documentation

The technical documentation is built from the Markdown files in `/tech-docs/source/pdl-docs` and
`/tech-docs/source/pdp-docs`. To modify the published documentation just modify those files,
once your changes have been merged into the `main` branch they will be published.

### Adding new pages

New pages must include a `source_url` field in their YAML frontmatter pointing to their location
in this repository. This is required because the tech-docs build runs from a temporary directory,
and without it the "View source" link on each page will be incorrect.

```yaml
---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdp-docs/my-new-page.html.md.erb
title: My new page
---
```

## How to build the technical documentation locally

The makefile contains the commands to spin up a Docker container with the documentation

```bash
cd tech-docs
make preview
```

## How is the technical documentation published

The `.github/workflows/publish-tech-docs.yml` workflow is used to publish the technical
documentation using the
[MOJ Tech Docs GitHub Pages Publisher](https://github.com/ministryofjustice/tech-docs-github-pages-publisher)
action.

## How to configure your GitHub settings

- [Enable GitHub pages](https://docs.github.com/en/pages/quickstart) for the repo
- Under `Build and deployment`, select the `Source` to be `GitHub Actions`. This will use the
  `.github/workflows/publish-tech-docs.yml` workflow
