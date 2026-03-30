param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$IncidentCount = 3,
    [long]$RootNodeId = 1,
    [long[]]$AffectedNodeIds = @(2, 3),
    [int]$DelayMs = 150
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-TelcoApi {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet("GET", "POST", "PUT", "PATCH", "DELETE")]
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
$nodeTypes = @("ROUTER", "E_NODE_B", "G_NODE_B", "SBC")
$timestampSuffix = Get-Date -Format "yyyyMMddHHmmssfff"

Write-Host "Generating full CRUD demo API traffic for Kibana dashboards..." -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl"
Write-Host "Incident count: $IncidentCount"
Write-Host "Root node ID: $RootNodeId"
Write-Host "Affected node IDs: $($AffectedNodeIds -join ', ')"

$demoNodeA = Invoke-TelcoApi -Method POST -Uri "$BaseUrl/api/network-nodes" -Body @{
    nodeName = "ELK-DEMO-NODE-A-$timestampSuffix"
    nodeType = $nodeTypes[0]
    region = $regions[0]
    vendor = "Cisco"
    active = $true
}
Write-Host "Created network node $($demoNodeA.id)"
Start-Sleep -Milliseconds $DelayMs

$demoNodeA = Invoke-TelcoApi -Method PUT -Uri "$BaseUrl/api/network-nodes/$($demoNodeA.id)" -Body @{
    nodeName = "ELK-DEMO-NODE-A-$timestampSuffix-UPD"
    nodeType = $nodeTypes[3]
    region = $regions[1]
    vendor = "Oracle"
    active = $false
}
Write-Host "Updated network node $($demoNodeA.id)"
Start-Sleep -Milliseconds $DelayMs

$demoNodeB = Invoke-TelcoApi -Method POST -Uri "$BaseUrl/api/network-nodes" -Body @{
    nodeName = "ELK-DEMO-NODE-B-$timestampSuffix"
    nodeType = $nodeTypes[2]
    region = $regions[2]
    vendor = "Ericsson"
    active = $true
}
Write-Host "Created network node $($demoNodeB.id)"
Start-Sleep -Milliseconds $DelayMs

$createdIncidentIds = @()

for ($index = 1; $index -le $IncidentCount; $index++) {
    $priority = $priorities[($index - 1) % $priorities.Length]
    $region = $regions[($index - 1) % $regions.Length]
    $sourceAlarmType = $alarmTypes[($index - 1) % $alarmTypes.Length]
    $possiblyPlanned = ($index % 2 -eq 0)
    $incidentNumber = "INC-ELK-$timestampSuffix-$index"
    $title = "ELK demo incident $index"

    $createBody = New-IncidentCreateBody `
        -IncidentNumber $incidentNumber `
        -Title $title `
        -Priority $priority `
        -Region $region `
        -SourceAlarmType $sourceAlarmType `
        -PossiblyPlanned $possiblyPlanned `
        -RootNodeId $RootNodeId `
        -AffectedNodeIds $AffectedNodeIds

    $createdIncident = Invoke-TelcoApi -Method POST -Uri "$BaseUrl/api/incidents" -Body $createBody
    $createdIncidentIds += $createdIncident.id
    Write-Host "Created incident $($createdIncident.id): $($createdIncident.incidentNumber)"
    Start-Sleep -Milliseconds $DelayMs

    $updatedPriority = $priorities[$index % $priorities.Length]
    $updatedIncident = Invoke-TelcoApi -Method PATCH -Uri "$BaseUrl/api/incidents/$($createdIncident.id)" -Body @{
        title = "$title updated"
        priority = $updatedPriority
        possiblyPlanned = (-not $possiblyPlanned)
    }
    Write-Host "Updated incident $($updatedIncident.id)"
    Start-Sleep -Milliseconds $DelayMs

    $acknowledgedIncident = Invoke-TelcoApi -Method PATCH -Uri "$BaseUrl/api/incidents/$($createdIncident.id)/acknowledge" -Body @{
        note = "Dashboard demo acknowledgement for $incidentNumber"
    }
    Write-Host "Acknowledged incident $($acknowledgedIncident.id)"
    Start-Sleep -Milliseconds $DelayMs

    if ($index -ge 2) {
        $resolvedIncident = Invoke-TelcoApi -Method PATCH -Uri "$BaseUrl/api/incidents/$($createdIncident.id)/resolve" -Body @{
            note = "Dashboard demo resolution for $incidentNumber"
        }
        Write-Host "Resolved incident $($resolvedIncident.id)"
        Start-Sleep -Milliseconds $DelayMs
    }

    if ($index -ge 3) {
        $closedIncident = Invoke-TelcoApi -Method PATCH -Uri "$BaseUrl/api/incidents/$($createdIncident.id)/close" -Body @{
            note = "Dashboard demo closure for $incidentNumber"
        }
        Write-Host "Closed incident $($closedIncident.id)"
        Start-Sleep -Milliseconds $DelayMs
    }

    [void](Invoke-TelcoApi -Method GET -Uri "$BaseUrl/api/incidents?page=0&size=10&sortBy=openedAt&direction=desc")
    [void](Invoke-TelcoApi -Method GET -Uri "$BaseUrl/api/incidents/$($createdIncident.id)")
    [void](Invoke-TelcoApi -Method GET -Uri "$BaseUrl/api/incidents/$($createdIncident.id)/timeline")
}

$referenceIncidentId = $createdIncidentIds[0]

$scopedTimeline = Invoke-TelcoApi -Method POST -Uri "$BaseUrl/api/incidents/$referenceIncidentId/timeline" -Body @{
    eventType = "MANUAL_NOTE"
    message = "Scoped timeline demo entry"
}
Write-Host "Created scoped incident timeline entry $($scopedTimeline.id)"
Start-Sleep -Milliseconds $DelayMs

$scopedTimeline = Invoke-TelcoApi -Method PUT -Uri "$BaseUrl/api/incidents/$referenceIncidentId/timeline/$($scopedTimeline.id)" -Body @{
    eventType = "MANUAL_NOTE"
    message = "Scoped timeline demo entry updated"
}
Write-Host "Updated scoped incident timeline entry $($scopedTimeline.id)"
Start-Sleep -Milliseconds $DelayMs

[void](Invoke-TelcoApi -Method DELETE -Uri "$BaseUrl/api/incidents/$referenceIncidentId/timeline/$($scopedTimeline.id)")
Write-Host "Deleted scoped incident timeline entry $($scopedTimeline.id)"
Start-Sleep -Milliseconds $DelayMs

$directTimeline = Invoke-TelcoApi -Method POST -Uri "$BaseUrl/api/incident-timeline" -Body @{
    incidentId = $referenceIncidentId
    eventType = "MANUAL_NOTE"
    message = "Direct timeline demo entry"
}
Write-Host "Created direct incident timeline entry $($directTimeline.id)"
Start-Sleep -Milliseconds $DelayMs

$directTimeline = Invoke-TelcoApi -Method PUT -Uri "$BaseUrl/api/incident-timeline/$($directTimeline.id)" -Body @{
    incidentId = $referenceIncidentId
    eventType = "MANUAL_NOTE"
    message = "Direct timeline demo entry updated"
}
Write-Host "Updated direct incident timeline entry $($directTimeline.id)"
Start-Sleep -Milliseconds $DelayMs

[void](Invoke-TelcoApi -Method DELETE -Uri "$BaseUrl/api/incident-timeline/$($directTimeline.id)")
Write-Host "Deleted direct incident timeline entry $($directTimeline.id)"
Start-Sleep -Milliseconds $DelayMs

$incidentNode = Invoke-TelcoApi -Method POST -Uri "$BaseUrl/api/incident-nodes" -Body @{
    incidentId = $referenceIncidentId
    networkNodeId = $demoNodeA.id
    role = "AFFECTED"
}
Write-Host "Created incident node relation $($incidentNode.id)"
Start-Sleep -Milliseconds $DelayMs

$incidentNode = Invoke-TelcoApi -Method PUT -Uri "$BaseUrl/api/incident-nodes/$($incidentNode.id)" -Body @{
    incidentId = $referenceIncidentId
    networkNodeId = $demoNodeB.id
    role = "AFFECTED"
}
Write-Host "Updated incident node relation $($incidentNode.id)"
Start-Sleep -Milliseconds $DelayMs

[void](Invoke-TelcoApi -Method DELETE -Uri "$BaseUrl/api/incident-nodes/$($incidentNode.id)")
Write-Host "Deleted incident node relation $($incidentNode.id)"
Start-Sleep -Milliseconds $DelayMs

$maintenanceWindow = Invoke-TelcoApi -Method POST -Uri "$BaseUrl/api/maintenance-windows" -Body @{
    title = "ELK maintenance window $timestampSuffix"
    description = "Generated for dashboard demo"
    status = "PLANNED"
    startTime = "2026-04-05T10:00:00"
    endTime = "2026-04-05T12:00:00"
    networkNodeIds = @($demoNodeA.id)
}
Write-Host "Created maintenance window $($maintenanceWindow.id)"
Start-Sleep -Milliseconds $DelayMs

$maintenanceWindow = Invoke-TelcoApi -Method PUT -Uri "$BaseUrl/api/maintenance-windows/$($maintenanceWindow.id)" -Body @{
    title = "ELK maintenance window $timestampSuffix updated"
    description = "Generated for dashboard demo updated"
    status = "IN_PROGRESS"
    startTime = "2026-04-05T10:30:00"
    endTime = "2026-04-05T13:00:00"
    networkNodeIds = @($demoNodeA.id)
}
Write-Host "Updated maintenance window $($maintenanceWindow.id)"
Start-Sleep -Milliseconds $DelayMs

$maintenanceNode = Invoke-TelcoApi -Method POST -Uri "$BaseUrl/api/maintenance-nodes" -Body @{
    maintenanceWindowId = $maintenanceWindow.id
    networkNodeId = $demoNodeB.id
}
Write-Host "Created maintenance node relation $($maintenanceNode.id)"
Start-Sleep -Milliseconds $DelayMs

$maintenanceNode = Invoke-TelcoApi -Method PUT -Uri "$BaseUrl/api/maintenance-nodes/$($maintenanceNode.id)" -Body @{
    maintenanceWindowId = $maintenanceWindow.id
    networkNodeId = $RootNodeId
}
Write-Host "Updated maintenance node relation $($maintenanceNode.id)"
Start-Sleep -Milliseconds $DelayMs

[void](Invoke-TelcoApi -Method DELETE -Uri "$BaseUrl/api/maintenance-nodes/$($maintenanceNode.id)")
Write-Host "Deleted maintenance node relation $($maintenanceNode.id)"
Start-Sleep -Milliseconds $DelayMs

$alarmEvent = Invoke-TelcoApi -Method POST -Uri "$BaseUrl/api/alarm-events" -Body @{
    sourceSystem = "OSS"
    externalId = "ELK-ALARM-$timestampSuffix"
    networkNodeId = $RootNodeId
    incidentId = $referenceIncidentId
    alarmType = "LINK_DOWN"
    severity = "MAJOR"
    status = "OPEN"
    description = "Generated for dashboard demo"
    suppressedByMaintenance = $false
    occurredAt = "2026-03-30T10:15:00"
    receivedAt = "2026-03-30T10:16:00"
}
Write-Host "Created alarm event $($alarmEvent.id)"
Start-Sleep -Milliseconds $DelayMs

$alarmEvent = Invoke-TelcoApi -Method PUT -Uri "$BaseUrl/api/alarm-events/$($alarmEvent.id)" -Body @{
    sourceSystem = "OSS"
    externalId = "ELK-ALARM-$timestampSuffix"
    networkNodeId = $RootNodeId
    incidentId = $referenceIncidentId
    alarmType = "LINK_DOWN"
    severity = "CRITICAL"
    status = "ACKNOWLEDGED"
    description = "Generated for dashboard demo updated"
    suppressedByMaintenance = $false
    occurredAt = "2026-03-30T10:15:00"
    receivedAt = "2026-03-30T10:16:00"
}
Write-Host "Updated alarm event $($alarmEvent.id)"
Start-Sleep -Milliseconds $DelayMs

[void](Invoke-TelcoApi -Method DELETE -Uri "$BaseUrl/api/alarm-events/$($alarmEvent.id)")
Write-Host "Deleted alarm event $($alarmEvent.id)"
Start-Sleep -Milliseconds $DelayMs

[void](Invoke-TelcoApi -Method DELETE -Uri "$BaseUrl/api/maintenance-windows/$($maintenanceWindow.id)")
Write-Host "Deleted maintenance window $($maintenanceWindow.id)"
Start-Sleep -Milliseconds $DelayMs

[void](Invoke-TelcoApi -Method GET -Uri "$BaseUrl/api/network-nodes")
[void](Invoke-TelcoApi -Method GET -Uri "$BaseUrl/api/maintenance-windows")
[void](Invoke-TelcoApi -Method GET -Uri "$BaseUrl/api/alarm-events")
[void](Invoke-TelcoApi -Method GET -Uri "$BaseUrl/api/incident-nodes")
[void](Invoke-TelcoApi -Method GET -Uri "$BaseUrl/api/maintenance-nodes")
[void](Invoke-TelcoApi -Method GET -Uri "$BaseUrl/api/incident-timeline")

[void](Invoke-TelcoApi -Method DELETE -Uri "$BaseUrl/api/network-nodes/$($demoNodeA.id)")
Write-Host "Deleted network node $($demoNodeA.id)"
Start-Sleep -Milliseconds $DelayMs

[void](Invoke-TelcoApi -Method DELETE -Uri "$BaseUrl/api/network-nodes/$($demoNodeB.id)")
Write-Host "Deleted network node $($demoNodeB.id)"

Write-Host "Full CRUD demo traffic generation completed." -ForegroundColor Green
