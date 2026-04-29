param(
    [switch]$StopMongo
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$RuntimeDir = Join-Path $RepoRoot ".runtime"
$BackendPidFile = Join-Path $RuntimeDir "backend.pid"
$MongoContainerName = "chat-app-mongo"

function Write-Step {
    param([string]$Message)
    Write-Host "[chat-app] $Message"
}

function Stop-ManagedBackend {
    if (-not (Test-Path $BackendPidFile)) {
        Write-Step "No managed backend PID file was found."
        return
    }

    $rawContent = (Get-Content $BackendPidFile -Raw).Trim()
    if ([string]::IsNullOrWhiteSpace($rawContent)) {
        Remove-Item $BackendPidFile -Force -ErrorAction SilentlyContinue
        Write-Step "Backend PID file was empty."
        return
    }

    try {
        $metadata = $rawContent | ConvertFrom-Json
    } catch {
        try {
            $metadata = [pscustomobject]@{
                RootPid = [int]$rawContent
                AppPid = $null
            }
        } catch {
            $metadata = $null
        }
    }

    if ($null -eq $metadata) {
        Remove-Item $BackendPidFile -Force -ErrorAction SilentlyContinue
        Write-Step "Backend PID file could not be parsed."
        return
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

Stop-ManagedBackend

if ($StopMongo) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker CLI was not found in PATH."
    }

    $existingName = (& docker ps -a --filter "name=^/${MongoContainerName}$" --format "{{.Names}}").Trim()
    if ($existingName -eq $MongoContainerName) {
        Write-Step "Stopping MongoDB container '$MongoContainerName'."
        & docker stop $MongoContainerName | Out-Null
    } else {
        Write-Step "MongoDB container '$MongoContainerName' does not exist."
    }
}
