param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$IncidentCount = 3,
    [long]$RootNodeId = 1,
    [long[]]$AffectedNodeIds = @(2, 3),
    [int]$DelayMs = 150
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-IncidentApi {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet("GET", "POST", "PATCH")]
        [string]$Method,
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [object]$Body = $null
    )

    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $Uri
    }

    $jsonBody = $Body | ConvertTo-Json -Depth 8
    return Invoke-RestMethod -Method $Method -Uri $Uri -ContentType "application/json" -Body $jsonBody
}

function New-IncidentCreateBody {
    param(
        [Parameter(Mandatory = $true)]
        [string]$IncidentNumber,
        [Parameter(Mandatory = $true)]
        [string]$Title,
        [Parameter(Mandatory = $true)]
        [string]$Priority,
        [Parameter(Mandatory = $true)]
        [string]$Region,
        [Parameter(Mandatory = $true)]
        [string]$SourceAlarmType,
        [Parameter(Mandatory = $true)]
        [bool]$PossiblyPlanned,
        [Parameter(Mandatory = $true)]
        [long]$RootNodeId,
        [Parameter(Mandatory = $true)]
        [long[]]$AffectedNodeIds
    )

    $nodes = @(
        @{
            networkNodeId = $RootNodeId
            role = "ROOT"
        }
    )

    foreach ($affectedNodeId in $AffectedNodeIds) {
        $nodes += @{
            networkNodeId = $affectedNodeId
            role = "AFFECTED"
        }
    }

    return @{
        incidentNumber = $IncidentNumber
        title = $Title
        priority = $Priority
        region = $Region
        sourceAlarmType = $SourceAlarmType
        possiblyPlanned = $PossiblyPlanned
        rootNodeId = $RootNodeId
        nodes = $nodes
    }
}

$priorities = @("LOW", "MEDIUM", "HIGH", "CRITICAL")
$regions = @("MAZOWIECKIE", "SLASKIE", "MALOPOLSKIE")
$alarmTypes = @("POWER", "HARDWARE", "PERFORMANCE", "NETWORK")

Write-Host "Generating demo API traffic for Kibana dashboard..." -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl"
Write-Host "Incident count: $IncidentCount"
Write-Host "Root node ID: $RootNodeId"
Write-Host "Affected node IDs: $($AffectedNodeIds -join ', ')"

for ($index = 1; $index -le $IncidentCount; $index++) {
    $suffix = Get-Date -Format "yyyyMMddHHmmssfff"
    $priority = $priorities[($index - 1) % $priorities.Length]
    $region = $regions[($index - 1) % $regions.Length]
    $sourceAlarmType = $alarmTypes[($index - 1) % $alarmTypes.Length]
    $possiblyPlanned = ($index % 2 -eq 0)
    $incidentNumber = "INC-ELK-$suffix-$index"
    $title = "ELK demo incident $index"
    $networkNode = Invoke-IncidentApi -Method POST -Uri "$BaseUrl/api/network-nodes" -Body @{
        nodeName = "ELK-DEMO-$suffix"
        nodeType = if ($sourceAlarmType -eq "CAPACITY") { "SBC" } elseif ($sourceAlarmType -eq "MAINTENANCE") { "E_NODE_B" } else { "G_NODE_B" }
        region = $region
        vendor = "DemoVendor"
        active = $true
    }
    Write-Host "Created network node $($networkNode.id): $($networkNode.nodeName)"
    Start-Sleep -Milliseconds $DelayMs

    $updatedNode = Invoke-IncidentApi -Method PATCH -Uri "$BaseUrl/api/network-nodes/$($networkNode.id)" -Body @{
        vendor = "DemoVendor-$index"
        active = $true
    }
    Write-Host "Updated network node $($updatedNode.id) -> vendor $($updatedNode.vendor)"
    Start-Sleep -Milliseconds $DelayMs

    $createBody = New-IncidentCreateBody `
        -IncidentNumber $incidentNumber `
        -Title $title `
        -Priority $priority `
        -Region $region `
        -SourceAlarmType $sourceAlarmType `
        -PossiblyPlanned $possiblyPlanned `
        -RootNodeId $RootNodeId `
        -AffectedNodeIds $AffectedNodeIds

    $createdIncident = Invoke-IncidentApi -Method POST -Uri "$BaseUrl/api/incidents" -Body $createBody
    Write-Host "Created incident $($createdIncident.id): $($createdIncident.incidentNumber)"
    Start-Sleep -Milliseconds $DelayMs

    $updatedPriority = $priorities[$index % $priorities.Length]
    $updateBody = @{
        title = "$title updated"
        priority = $updatedPriority
        possiblyPlanned = (-not $possiblyPlanned)
    }

    $updatedIncident = Invoke-IncidentApi -Method PATCH -Uri "$BaseUrl/api/incidents/$($createdIncident.id)" -Body $updateBody
    Write-Host "Updated incident $($updatedIncident.id) -> priority $($updatedIncident.priority)"
    Start-Sleep -Milliseconds $DelayMs

    $acknowledgedIncident = Invoke-IncidentApi -Method PATCH -Uri "$BaseUrl/api/incidents/$($createdIncident.id)/acknowledge" -Body @{
        note = "Dashboard demo acknowledgement for $incidentNumber"
    }
    Write-Host "Acknowledged incident $($acknowledgedIncident.id)"
    Start-Sleep -Milliseconds $DelayMs

    if ($index -ge 2) {
        $resolvedIncident = Invoke-IncidentApi -Method PATCH -Uri "$BaseUrl/api/incidents/$($createdIncident.id)/resolve" -Body @{
            note = "Dashboard demo resolution for $incidentNumber"
        }
        Write-Host "Resolved incident $($resolvedIncident.id)"
        Start-Sleep -Milliseconds $DelayMs
    }

    if ($index -ge 3) {
        $closedIncident = Invoke-IncidentApi -Method PATCH -Uri "$BaseUrl/api/incidents/$($createdIncident.id)/close" -Body @{
            note = "Dashboard demo closure for $incidentNumber"
        }
        Write-Host "Closed incident $($closedIncident.id)"
        Start-Sleep -Milliseconds $DelayMs
    }

    $maintenanceWindow = Invoke-IncidentApi -Method POST -Uri "$BaseUrl/api/maintenance-windows" -Body @{
        title = "ELK maintenance $suffix"
        description = "Dashboard demo maintenance window $index"
        status = if ($index % 2 -eq 0) { "PLANNED" } else { "IN_PROGRESS" }
        startTime = (Get-Date).AddMinutes(15).ToString("s")
        endTime = (Get-Date).AddMinutes(75).ToString("s")
        nodeIds = @($networkNode.id)
    }
    Write-Host "Created maintenance window $($maintenanceWindow.id)"
    Start-Sleep -Milliseconds $DelayMs

    $alarmEvent = Invoke-IncidentApi -Method POST -Uri "$BaseUrl/api/alarm-events" -Body @{
        sourceSystem = "DEMO"
        externalId = "ALARM-DEMO-$suffix"
        networkNodeId = $networkNode.id
        incidentId = $createdIncident.id
        alarmType = if ($sourceAlarmType -eq "POWER") { "POWER_FAILURE" } elseif ($sourceAlarmType -eq "PERFORMANCE") { "LATENCY_SPIKE" } elseif ($sourceAlarmType -eq "NETWORK") { "BGP_FLAP" } elseif ($sourceAlarmType -eq "CAPACITY") { "CPU_HIGH" } elseif ($sourceAlarmType -eq "MAINTENANCE") { "MAINTENANCE_MODE" } else { "CARD_FAILURE" }
        severity = if ($sourceAlarmType -eq "CAPACITY") { "CRITICAL" } elseif ($sourceAlarmType -eq "PERFORMANCE" -or $sourceAlarmType -eq "MAINTENANCE") { "MINOR" } else { "MAJOR" }
        status = if ($index % 2 -eq 0) { "ACKNOWLEDGED" } else { "OPEN" }
        description = "Dashboard demo alarm for $incidentNumber"
        suppressedByMaintenance = $false
        occurredAt = (Get-Date).AddMinutes(-2).ToString("s")
    }
    Write-Host "Created alarm event $($alarmEvent.id): $($alarmEvent.externalId)"
    Start-Sleep -Milliseconds $DelayMs

    [void](Invoke-IncidentApi -Method GET -Uri "$BaseUrl/api/incidents?page=0&size=10&sortBy=openedAt&direction=desc")
    [void](Invoke-IncidentApi -Method GET -Uri "$BaseUrl/api/incidents/$($createdIncident.id)")
    [void](Invoke-IncidentApi -Method GET -Uri "$BaseUrl/api/incidents/$($createdIncident.id)/timeline")
    [void](Invoke-IncidentApi -Method GET -Uri "$BaseUrl/api/network-nodes?q=ELK-DEMO&region=$region")
    [void](Invoke-IncidentApi -Method GET -Uri "$BaseUrl/api/maintenance-windows")
    [void](Invoke-IncidentApi -Method GET -Uri "$BaseUrl/api/alarm-events")
}

Write-Host "Demo traffic generation completed." -ForegroundColor Green
