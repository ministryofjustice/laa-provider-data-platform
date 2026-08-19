IMAGE := ghcr.io/ministryofjustice/tech-docs-github-pages-publisher:v6.2.0

.PHONY: preview package prepare link-check clean

# Generates files that must be built before previewing or deploying:
#   - pda-r2/api/laa-data-pda.yml  (spec copy for CDN-based renderers)
#   - pda-r2/api/widdershins.html.md  (Widdershins Markdown)
prepare:
	docker run --rm \
		-v $$(pwd)/..:/workspace \
		-v ~/.npm:/root/.npm \
		-w /workspace \
		node:lts-slim \
		sh tech-docs/scripts/prepare-api-spec.sh

preview: prepare
	docker run -it --rm \
		-v $$(pwd)/config:/tech-docs-github-pages-publisher/config \
		-v $$(pwd)/source:/tech-docs-github-pages-publisher/source \
		-p 4567:4567 \
		$(IMAGE) /usr/local/bin/preview

package: prepare
	docker run --rm \
		-v $$(pwd)/config:/tech-docs-github-pages-publisher/config \
		-v $$(pwd)/source:/tech-docs-github-pages-publisher/source \
		$(IMAGE) /usr/local/bin/package

link-check: prepare
	rm -rf .link-check
	mkdir -p .link-check
	docker run --rm \
		-v $$(pwd)/config:/tech-docs-github-pages-publisher/config \
		-v $$(pwd)/source:/tech-docs-github-pages-publisher/source \
		-v $$(pwd)/.link-check:/output \
		--entrypoint sh \
		$(IMAGE) \
		-c '/usr/local/bin/package && cp artifact.tar /output/artifact.tar'
	cd .link-check && tar -xf artifact.tar
	cd .link-check && sh ../scripts/link-check.sh

clean:
	rm -f source/pda-r2/api/laa-data-pda.yml \
	      source/pda-r2/api/widdershins.html.md
	rm -rf .link-check
