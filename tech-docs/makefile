IMAGE := ministryofjustice/tech-docs-github-pages-publisher:v3

.PHONY: preview prepare clean deploy check

# Generates files that must be built before previewing or deploying:
#   - pdp-docs/api/laa-data-pda.yml  (spec copy for CDN-based renderers)
#   - pdp-docs/api/widdershins.html.md  (Widdershins Markdown)
prepare:
	docker run --rm \
		-v $$(pwd)/..:/workspace \
		-v ~/.npm:/root/.npm \
		-w /workspace \
		node:lts-slim \
		sh tech-docs/scripts/prepare-api-spec.sh

preview: prepare
	docker run --rm \
		-v $$(pwd)/config:/app/config \
		-v $$(pwd)/source:/app/source \
		-p 4567:4567 \
		-it $(IMAGE) /scripts/preview.sh

deploy: prepare
	docker run --rm \
		-v $$(pwd)/config:/app/config \
		-v $$(pwd)/source:/app/source \
		-it $(IMAGE) /scripts/deploy.sh

check:
	docker run --rm \
		-v $$(pwd)/config:/app/config \
		-v $$(pwd)/source:/app/source \
		-it $(IMAGE) /scripts/check-url-links.sh

clean:
	rm -f source/pdp-docs/api/laa-data-pda.yml \
	      source/pdp-docs/api/widdershins.html.md
