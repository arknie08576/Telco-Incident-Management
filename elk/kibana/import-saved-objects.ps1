$ErrorActionPreference = "Stop"

$kibanaUrl = "http://localhost:5601"
$savedObjectsFile = Join-Path $PSScriptRoot "saved-objects/telco-incident-observability.ndjson"

if (-not (Test-Path $savedObjectsFile)) {
    throw "Saved objects file not found: $savedObjectsFile"
}

curl.exe `
  -X POST "$kibanaUrl/api/saved_objects/_import?overwrite=true" `
  -H "kbn-xsrf: true" `
  --form "file=@$savedObjectsFile"
