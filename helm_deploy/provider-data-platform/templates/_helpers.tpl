{{/*
_helpers.tpl
This file contains Helm template helpers that can be reused throughout the chart.
*/}}

{{/*
Expand the name of the chart.
*/}}
{{- define "provider-data-platform.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name (release name + chart name unless fullnameOverride is set).
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "provider-data-platform.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "provider-data-platform.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels (note that the Selector labels are included in here)
*/}}
{{- define "provider-data-platform.labels" -}}
{{ include "provider-data-platform.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ include "provider-data-platform.chart" . }}
helm.sh/revision: {{ .Release.Revision | quote }}
{{- end }}

{{/*
Selector labels (identify this instance of the appliction)
*/}}
{{- define "provider-data-platform.selectorLabels" -}}
{{ include "provider-data-platform.appLabels" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
App selector labels (identify all instances of the application)
*/}}
{{- define "provider-data-platform.appLabels" -}}
app.kubernetes.io/name: {{ include "provider-data-platform.name" . }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "provider-data-platform.serviceAccountName" -}}
{{- if (.Values.serviceAccount).create }}
{{- default (include "provider-data-platform.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" (.Values.serviceAccount).name }}
{{- end }}
{{- end }}

{{/*
Construct hostname with release suffix for dedicated ingress.
Takes base hostname from values and appends hostnameSuffix before the domain.
Example: "laa-provider-data-platform-dev.apps.live.cloud-platform.service.justice.gov.uk" + "-1"
      -> "laa-provider-data-platform-dev-1.apps.live.cloud-platform.service.justice.gov.uk"
*/}}
{{- define "provider-data-platform.hostnameWithSuffix" -}}
{{- $baseHost := .host -}}
{{- $suffix := .suffix -}}
{{- if $suffix -}}
{{- $parts := splitList "." $baseHost -}}
{{- $firstPart := first $parts -}}
{{- $rest := join "." (rest $parts) -}}
{{- printf "%s%s.%s" $firstPart $suffix $rest -}}
{{- else -}}
{{- $baseHost -}}
{{- end -}}
{{- end -}}
