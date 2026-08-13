
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

## Coexistence with API key authentication

PDA-r2 supports **both API key and OAuth2 simultaneously** (DSTEW-1940). This enables existing API key consumers to continue using their keys while new consumers can adopt OAuth2 bearer tokens.

### Configuration

Enable both mechanisms in the environment:

```bash
# API key authentication
export APP_SECURITY_API_KEY_ENABLED=true

# OAuth2 authentication
export APP_SECURITY_OAUTH2_ENABLED=true
export OAUTH2_ISSUER_URI=https://login.microsoftonline.com/<tenant-id>/v2.0
export OAUTH2_AUDIENCE=api://<backend-api-application-id>
```

### Request handling and precedence

When both mechanisms are enabled, the request is routed based on which credentials are supplied:

- **Bearer token present**: OAuth2 validation applies (issuer, audience, `PDA_ACCESS` role)
- **No bearer, API key present**: API key validation applies
- **Both supplied**: Bearer token takes precedence; API key is ignored
- **Neither supplied**: Request rejected with 401

### Examples

**API key request:**
```bash
curl -H "X-Authorization: <api-key>" https://api.example.com/providers
```

**OAuth2 request:**
```bash
curl -H "Authorization: Bearer <access-token>" https://api.example.com/providers
```

**Both supplied (bearer takes precedence):**
```bash
curl \
  -H "X-Authorization: <api-key>" \
  -H "Authorization: Bearer <access-token>" \
  https://api.example.com/providers
# → OAuth2 validation applies
```

### Disabling either mechanism

To disable one mechanism while keeping the other:

```bash
# Disable API key, keep OAuth2 only
export APP_SECURITY_API_KEY_ENABLED=false
export APP_SECURITY_OAUTH2_ENABLED=true

# Disable OAuth2, keep API key only
export APP_SECURITY_OAUTH2_ENABLED=false
export APP_SECURITY_API_KEY_ENABLED=true

# Disable both (development/testing)
export APP_SECURITY_API_KEY_ENABLED=false
export APP_SECURITY_OAUTH2_ENABLED=false
```
