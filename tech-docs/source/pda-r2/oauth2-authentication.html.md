
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

OAuth2 is implemented but disabled by default.

```yaml
app:
  security:
    oauth2:
      enabled: ${APP_SECURITY_OAUTH2_ENABLED:false}
```

Set the Entra issuer and audience when enabling OAuth2 in an environment:

```bash
export APP_SECURITY_OAUTH2_ENABLED=true
export OAUTH2_ISSUER_URI=https://login.microsoftonline.com/<tenant-id>/v2.0
export OAUTH2_AUDIENCE=api://<backend-api-application-id>
```

## Authorisation behaviour

- Missing or invalid bearer token: request rejected.
- Wrong audience: request rejected.
- Missing `PDA_ACCESS` app role in JWT `roles` claim: request rejected.
- Valid token with expected role and audience: request authenticated.

For local or non-authentication development, keep `APP_SECURITY_OAUTH2_ENABLED=false`.
