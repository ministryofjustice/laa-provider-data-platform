# Runbook Template (Public/Sanitised Version)

This runbook provides a high-level overview of the service and its operation. Sensitive operational and 
access details are maintained in a separate internal runbook.

---

## 1. Last Review Date

The date this runbook was last reviewed for accuracy.

---

## 2. The Service

### 2.1 Description
A short (less than 50 words) description of what the service does and who it is for.

### 2.2 Expected Speed and Frequency of Releases
Describe how often releases occur and typical deployment timelines (high-level only).

### 2.3 Impact of an Outage
Outline the business impact if the service is unavailable.

### 2.4 Consumers of This Service
List dependent services at a high level (avoid internal links or sensitive system names).

### 2.5 Providers to This Service
List external or high-level dependencies (avoid detailed architecture or internal system identifiers).

### 2.6 Automatic Alerts
Describe categories of alerts (e.g. availability, performance), without linking to monitoring systems.

### 2.7 Service Documentation
Links to publicly accessible or non-sensitive documentation.

### 2.8 Postman Collections and Environment Files
Links to publicly shareable API documentation or collections (ensure no credentials included).

---

## 3. The Team

### 3.1 Team Contact Channel
Provide a shared contact method (e.g. support portal or generic contact channel).
> Do not include internal Slack or private communication channels.

### 3.2 Team Information
Provide a shared team contact (e.g. team email).
> Do not include individual contact details.

### 3.3 Incident Response Hours
Clearly define support coverage (e.g. `24/7`, `business hours`, etc.).

### 3.4 Out of Hours Response Types
High-level description of incident handling outside normal hours (no operational detail).

---

## 4. Running the Service

### 4.1 Work Tracking
Reference to a ticketing or tracking system at a high level (no direct internal links if restricted).

### 4.2 Code Repository
Link to public repositories only (if applicable).
> Internal repositories should not be exposed.

### 4.3 Deployment
Provide a high-level description of the deployment approach (e.g. automated CI/CD pipeline).

### 4.4 View Deployment Version
Explain at a high level how versions are tracked (avoid environment-specific details).

### 4.5 Common Issues
Provide high-level descriptions of known issues and resolutions.
> Detailed operational steps should be maintained in an internal runbook.

- Issue type 1 (summary only)
- Issue type 2 (summary only)

### 4.6 Restrictions on Access
Describe access limitations at a high level (e.g. restricted environments, authentication required), 
without specifying implementation details.

---

## 5. Access to The Service

### 5.1 Hosting Environment
Describe hosting at a high level (e.g. cloud provider or platform).
> Do not include account details, login URLs, or infrastructure specifics.

### 5.2 Service URLs
Provide only publicly accessible endpoints or documentation links.
> Internal or admin URLs must not be included.

### 5.3 Application Logs
State that logging exists and is monitored.
> Do not provide access instructions or links to log systems.

### 5.4 Other URLs
Optional links to public dashboards or health endpoints (if intentionally exposed).

### 5.5 Permissions
Provide a general statement on access control (e.g. “role-based access is required”).
> Do not document how to request or grant access.

---

## 🔒 Note on Sensitive Information

Detailed operational procedures, access instructions, infrastructure details, and incident response processes are 
maintained in a **restricted internal runbook** and are not included here for security reasons.

## Internal Runbook
[Internal runbook](https://dsdmoj.atlassian.net/wiki/spaces/WHCZA/pages/6153535921/PDP+Internal+Runbook) - Internal runbook (restricted access)
