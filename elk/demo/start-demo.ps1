param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$KibanaUrl = "http://localhost:5601",
    [int]$IncidentCount = 4,
    [long]$RootNodeId = 1,
    [long[]]$AffectedNodeIds = @(2, 3),
    [int]$DelayMs = 150,
    [switch]$SkipCompose,
    [switch]$SkipImport,
    [switch]$SkipData,
    [switch]$NoOpen
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$importScript = Join-Path $repoRoot "elk\kibana\import-saved-objects.ps1"
$generatorScript = Join-Path $repoRoot "elk\demo\generate-dashboard-data.ps1"
$composeFile = Join-Path $repoRoot "docker-compose.yml"

function Wait-HttpReady {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSeconds = 300,
        [int]$SleepSeconds = 5,
        [scriptblock]$IsReady = { param($response) $true }
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    do {
        try {
            $response = Invoke-RestMethod -UseBasicParsing -Uri $Url
            if (& $IsReady $response) {
                return
            }
        } catch {
        }

        Start-Sleep -Seconds $SleepSeconds
    } while ((Get-Date) -lt $deadline)

    throw "Service did not become ready in time: $Url"
}

if (-not (Test-Path $composeFile)) {
    throw "Compose file not found: $composeFile"
}

if (-not (Test-Path $importScript)) {
    throw "Import script not found: $importScript"
}

if (-not (Test-Path $generatorScript)) {
    throw "Generator script not found: $generatorScript"
}

Write-Host "Starting demo bootstrap..." -ForegroundColor Cyan
Write-Host "Repo root: $repoRoot"
Write-Host "Base URL: $BaseUrl"
Write-Host "Kibana URL: $KibanaUrl"

if (-not $SkipCompose) {
    Write-Host "Starting Docker Compose stack..." -ForegroundColor Cyan
    & docker compose -f $composeFile up -d --build
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed"
    }
}

Write-Host "Waiting for backend readiness..." -ForegroundColor Cyan
Wait-HttpReady -Url "$BaseUrl/actuator/health/readiness" -TimeoutSeconds 300 -SleepSeconds 5 -IsReady {
    param($response)
    $response.status -eq "UP"
}

if (-not $SkipImport) {
    Write-Host "Importing Kibana saved objects..." -ForegroundColor Cyan
    & $importScript
    if ($LASTEXITCODE -ne 0) {
        throw "Saved objects import failed"
    }
}

if (-not $SkipData) {
    Write-Host "Generating demo data..." -ForegroundColor Cyan
    & $generatorScript `
        -BaseUrl $BaseUrl `
        -IncidentCount $IncidentCount `
        -RootNodeId $RootNodeId `
        -AffectedNodeIds $AffectedNodeIds `
        -DelayMs $DelayMs
    if ($LASTEXITCODE -ne 0) {
        throw "Demo data generation failed"
    }
}

Write-Host ""
Write-Host "Demo environment is ready." -ForegroundColor Green
Write-Host "Swagger UI: $BaseUrl/swagger-ui.html"
Write-Host "Kibana: $KibanaUrl"
Write-Host "Kibana dashboard: Telco Platform Observability"
Write-Host "Set time range to 'Last 24 hours' and click Refresh."

if (-not $NoOpen) {
    Write-Host "Opening Swagger UI and Kibana in the default browser..." -ForegroundColor Cyan
    Start-Process "$BaseUrl/swagger-ui.html"
    Start-Process $KibanaUrl
}
