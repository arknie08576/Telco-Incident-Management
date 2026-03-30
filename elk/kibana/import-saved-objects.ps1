$ErrorActionPreference = "Stop"

$kibanaUrl = "http://localhost:5601"
$savedObjectsFile = Join-Path $PSScriptRoot "saved-objects/telco-incident-observability.ndjson"
$deadline = (Get-Date).AddMinutes(3)

if (-not (Test-Path $savedObjectsFile)) {
    throw "Saved objects file not found: $savedObjectsFile"
}

do {
    try {
        $status = Invoke-RestMethod -UseBasicParsing "$kibanaUrl/api/status"

        if ($status.status.overall.level -eq "available") {
            break
        }
    } catch {
    }

    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)

if ((Get-Date) -ge $deadline) {
    throw "Kibana did not become ready in time. Check 'docker compose -f docker-compose.elk.yml logs kibana'."
}

curl.exe `
  -X POST "$kibanaUrl/api/saved_objects/_import?overwrite=true" `
  -H "kbn-xsrf: true" `
  --form "file=@$savedObjectsFile"
