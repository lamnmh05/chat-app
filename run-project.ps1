param(
    [switch]$NoMongo,
    [switch]$NoFrontend,
    [switch]$KeepBackendAlive
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $RepoRoot "backend"
$FrontendDir = Join-Path $RepoRoot "frontend"
$RuntimeDir = Join-Path $RepoRoot ".runtime"
$BackendPidFile = Join-Path $RuntimeDir "backend.pid"
$BackendLogFile = Join-Path $RuntimeDir "backend.log"
$BackendErrFile = Join-Path $RuntimeDir "backend.err.log"
$MongoContainerName = "chat-app-mongo"
$BackendHealthUrl = "http://localhost:8080/actuator/health"

New-Item -ItemType Directory -Force -Path $RuntimeDir | Out-Null

function Write-Step {
    param([string]$Message)
    Write-Host "[chat-app] $Message"
}

function Test-BackendHealthy {
    try {
        $response = Invoke-RestMethod -Uri $BackendHealthUrl -TimeoutSec 3
        return $response.status -eq "UP"
    } catch {
        return $false
    }
}

function Get-ManagedBackendMetadata {
    if (-not (Test-Path $BackendPidFile)) {
        return $null
    }

    $rawContent = (Get-Content $BackendPidFile -Raw).Trim()
    if ([string]::IsNullOrWhiteSpace($rawContent)) {
        Remove-Item $BackendPidFile -Force -ErrorAction SilentlyContinue
        return $null
    }

    try {
        return $rawContent | ConvertFrom-Json
    } catch {
        try {
            $legacyPid = [int]$rawContent
            return [pscustomobject]@{
                RootPid = $legacyPid
                AppPid = $null
            }
        } catch {
            Remove-Item $BackendPidFile -Force -ErrorAction SilentlyContinue
            return $null
        }
    }
}

function Wait-BackendHealthy {
    param([int]$TimeoutSeconds = 120)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-BackendHealthy) {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Save-ManagedBackendMetadata {
    param(
        [int]$RootPid,
        [Nullable[int]]$AppPid
    )

    $payload = [pscustomobject]@{
        RootPid = $RootPid
        AppPid = $AppPid
    } | ConvertTo-Json -Compress

    Set-Content -Path $BackendPidFile -Value $payload
}

function Test-ProcessAlive {
    param([Nullable[int]]$ProcessId)

    if (-not $ProcessId) {
        return $false
    }

    try {
        Get-Process -Id $ProcessId -ErrorAction Stop | Out-Null
        return $true
    } catch {
        return $false
    }
}

function Get-ChildProcessIds {
    param([int]$ParentPid)

    $children = Get-CimInstance Win32_Process | Where-Object { $_.ParentProcessId -eq $ParentPid }
    $result = @()
    foreach ($child in $children) {
        $result += [int]$child.ProcessId
        $result += Get-ChildProcessIds -ParentPid ([int]$child.ProcessId)
    }
    return $result
}

function Stop-ProcessTree {
    param([int]$RootPid)

    $childIds = Get-ChildProcessIds -ParentPid $RootPid | Sort-Object -Descending -Unique
    foreach ($childPid in $childIds) {
        Stop-Process -Id $childPid -Force -ErrorAction SilentlyContinue
    }
    Stop-Process -Id $RootPid -Force -ErrorAction SilentlyContinue
}

function Resolve-BackendAppPid {
    try {
        return [int](Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object -First 1 -ExpandProperty OwningProcess)
    } catch {
        return $null
    }
}

function Ensure-DockerAvailable {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker CLI was not found in PATH."
    }

    & docker info | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Desktop is not ready. Start Docker Desktop first."
    }
}

function Ensure-MongoContainer {
    $existingName = (& docker ps -a --filter "name=^/${MongoContainerName}$" --format "{{.Names}}").Trim()
    if ($existingName -ne $MongoContainerName) {
        Write-Step "Creating MongoDB container '$MongoContainerName'."
        & docker run -d --name $MongoContainerName -p 27017:27017 mongo:7 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Cannot create MongoDB container."
        }
        return
    }

    $isRunning = (& docker inspect -f "{{.State.Running}}" $MongoContainerName).Trim()
    if ($isRunning -ne "true") {
        Write-Step "Starting existing MongoDB container '$MongoContainerName'."
        & docker start $MongoContainerName | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Cannot start MongoDB container."
        }
    } else {
        Write-Step "MongoDB container '$MongoContainerName' is already running."
    }
}

function Start-ManagedBackend {
    $metadata = Get-ManagedBackendMetadata
    if ($metadata -and ((Test-ProcessAlive -ProcessId $metadata.RootPid) -or (Test-ProcessAlive -ProcessId $metadata.AppPid))) {
        Write-Step "Backend process is already managed by this repo."
        return @{
            Metadata = $metadata
            StartedByScript = $false
        }
    }

    if ($metadata) {
        Remove-Item $BackendPidFile -Force -ErrorAction SilentlyContinue
    }

    if (Test-BackendHealthy) {
        Write-Step "Backend is already running on port 8080."
        return @{
            Metadata = $null
            StartedByScript = $false
        }
    }

    if (Test-Path $BackendLogFile) {
        Remove-Item $BackendLogFile -Force
    }
    if (Test-Path $BackendErrFile) {
        Remove-Item $BackendErrFile -Force
    }

    Write-Step "Starting backend in the background."
    $process = Start-Process `
        -FilePath "cmd.exe" `
        -ArgumentList "/c", "gradlew.bat --no-daemon bootRun" `
        -WorkingDirectory $BackendDir `
        -RedirectStandardOutput $BackendLogFile `
        -RedirectStandardError $BackendErrFile `
        -WindowStyle Hidden `
        -PassThru

    Save-ManagedBackendMetadata -RootPid $process.Id -AppPid $null

    if (-not (Wait-BackendHealthy)) {
        $stdoutTail = if (Test-Path $BackendLogFile) { Get-Content $BackendLogFile -Tail 40 } else { @() }
        $stderrTail = if (Test-Path $BackendErrFile) { Get-Content $BackendErrFile -Tail 40 } else { @() }
        throw ("Backend did not become healthy within timeout.`nSTDOUT:`n{0}`nSTDERR:`n{1}" -f ($stdoutTail -join "`n"), ($stderrTail -join "`n"))
    }

    $appPid = Resolve-BackendAppPid
    Save-ManagedBackendMetadata -RootPid $process.Id -AppPid $appPid

    Write-Step "Backend is healthy at $BackendHealthUrl."
    return @{
        Metadata = [pscustomobject]@{
            RootPid = $process.Id
            AppPid = $appPid
        }
        StartedByScript = $true
    }
}

function Stop-ManagedBackend {
    $metadata = Get-ManagedBackendMetadata
    if (-not $metadata) {
        return
    }

    if (Test-ProcessAlive -ProcessId $metadata.RootPid) {
        Write-Step "Stopping backend process tree rooted at PID $($metadata.RootPid)."
        Stop-ProcessTree -RootPid $metadata.RootPid
    }

    if (Test-ProcessAlive -ProcessId $metadata.AppPid) {
        Write-Step "Stopping backend application PID $($metadata.AppPid)."
        Stop-Process -Id $metadata.AppPid -Force -ErrorAction SilentlyContinue
    }

    Remove-Item $BackendPidFile -Force -ErrorAction SilentlyContinue
}

$backendStartedByScript = $false

try {
    if (-not $NoMongo) {
        Ensure-DockerAvailable
        Ensure-MongoContainer
    }

    $backendResult = Start-ManagedBackend
    $backendStartedByScript = [bool]$backendResult.StartedByScript

    if ($NoFrontend) {
        Write-Step "Backend is ready. Frontend launch was skipped."
        return
    }

    Write-Step "Launching frontend. Close the JavaFX window to finish this run."
    Push-Location $FrontendDir
    try {
        & .\gradlew.bat run
        if ($LASTEXITCODE -ne 0) {
            throw "Frontend exited with code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
} finally {
    if ($backendStartedByScript -and -not $KeepBackendAlive) {
        Stop-ManagedBackend
    }
}
