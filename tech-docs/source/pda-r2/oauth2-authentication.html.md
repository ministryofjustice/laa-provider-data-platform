---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pda-r2/oauth2-authentication.html.md
---

# OAuth2 authentication for PDA-r2

Microsoft Entra ID **client credentials** only.

- The client app requests a token and calls PDA-r2 as an application.
- PDA-r2 validates issuer and audience, then enforces the required app role.

## Entra application model

- Client app registration: `laa-provider-data-testclient`
- Backend API app registration: `laa-provider-data-api-r2-uat`
- Required API app role value: `PDA_ACCESS` (allowed member type: Applications)

The client app must have the API application permission granted and tenant admin consent applied.

## Token request pattern

Use Entra v2 token endpoint with client credentials grant:

```bash
curl --request POST \
  --url "https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token" \
  --header "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "client_id=<client-app-id>" \
  --data-urlencode "client_secret=<client-secret>" \
  --data-urlencode "scope=api://<backend-api-application-id>/.default"
```

## PDA-r2 configuration

OAuth2 is provided by the LAA starter and is disabled by default in non-authenticated
environments.

Set the Entra issuer and audience when enabling OAuth2 in an environment:

```bash
export OAUTH2_ISSUER_URI=https://login.microsoftonline.com/<tenant-id>/v2.0
export OAUTH2_AUDIENCE=api://<backend-api-application-id>
```

For deployed environments, add the OAuth2 values to the Cloud Platform configuration secret used
by the Helm release (`app-secrets` for the stable release, `app-secrets-secondary` for the canary
release when present):

- `OAUTH2_ISSUER_URI`
- `OAUTH2_AUDIENCE`
- `OAUTH2_AUTHORIZED_ROLES`
- `OAUTH2_UNPROTECTED_URIS`

If `OAUTH2_AUTHORIZED_ROLES` is omitted, the service falls back to the default
`PDA_ACCESS` mapping from `application.yml`.

## Authorisation behaviour

- Missing or invalid bearer token: request rejected.
- Wrong audience: request rejected.
- Missing `PDA_ACCESS` app role in JWT `roles` claim: request rejected.
- Valid token with expected role and audience: request authenticated.

For local or non-authentication development, leave the OAuth2 starter properties unset.
