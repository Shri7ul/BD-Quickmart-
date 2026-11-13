<#
.SYNOPSIS
    Starts the BD QuickMart backend on Windows with the root .env loaded.

.DESCRIPTION
    Spring Boot and Maven do not read a .env file on their own. On Windows the
    root .env must be exported into the process environment BEFORE Maven forks
    the JVM; otherwise placeholders such as ${JWT_SECRET:} resolve to their
    blank defaults and JwtConfig fails fast at startup with
    "JWT_SECRET is not configured".

    This script:
      * reads the existing, git-ignored root .env,
      * sets each KEY=VALUE as a PROCESS-scoped environment variable, so Maven
        and the JVM it forks inherit it,
      * never prints, logs or stores any value,
      * leaves any variable already set in the environment untouched, so real
        CI / IDE / production configuration always takes precedence,
      * restores the caller's environment when the backend stops.

    It does NOT create or modify the database, does NOT weaken JWT validation,
    does NOT enable the ephemeral dev key, and changes no application code or
    API contract. Production deployments should keep running the packaged jar
    with real environment variables instead.

.PARAMETER MavenArgs
    Optional extra arguments forwarded to `mvn spring-boot:run`.

.EXAMPLE
    .\start-backend.ps1

.EXAMPLE
    .\start-backend.ps1 -MavenArgs '-Dspring-boot.run.profiles=dev'
#>
[CmdletBinding()]
param(
    [string[]] $MavenArgs = @()
)

$ErrorActionPreference = 'Stop'

$repoRoot = $PSScriptRoot
$envFile  = Join-Path $repoRoot '.env'
$backend  = Join-Path $repoRoot 'backend'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Root .env not found at '$envFile'. Copy .env.example to .env and set JWT_SECRET (see docs/development/local-setup.md)."
}
if (-not (Test-Path -LiteralPath (Join-Path $backend 'pom.xml'))) {
    throw "backend/pom.xml not found next to this script. Run this script from the repository checkout."
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "'mvn' is not on PATH. Add the JDK 21 and Maven bin directories to PATH (see docs/development/local-setup.md, section 0)."
}

# Names of the variables we actually set, so they can be removed afterwards.
$setKeys = New-Object 'System.Collections.Generic.List[string]'
$loaded  = 0
$skipped = 0

foreach ($rawLine in (Get-Content -LiteralPath $envFile -Encoding UTF8)) {
    $line = $rawLine.TrimEnd("`r", "`n")

    # Only KEY=VALUE lines are considered; comment and blank lines simply do
    # not match. Split on the FIRST '=' only: JDBC URLs and Base64 JWT secrets
    # legitimately contain further '=' characters. An optional 'export '
    # prefix is tolerated for cross-shell .env files.
    if ($line -match '^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
        $name  = $Matches[1]
        $value = $Matches[2]

        # Strip one pair of matching surrounding quotes (dotenv convention).
        # Inline '#' is intentionally NOT treated as a comment, so URLs and
        # secrets keep their exact bytes.
        if ($value.Length -ge 2) {
            $quote = $value[0]
            if (($quote -eq '"' -or $quote -eq "'") -and $value[$value.Length - 1] -eq $quote) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }

        # Real environment variables win. This keeps normal production / CI /
        # IDE configuration intact and lets a developer override one value
        # without editing .env.
        if (-not [string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name, 'Process'))) {
            $skipped++
            continue
        }

        # Process scope only: inherited by mvn and the forked JVM, never
        # persisted to the User or Machine environment.
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        $setKeys.Add($name)
        $loaded++
    }
}

# Safe summary: counts and the file path only. No value is ever emitted.
Write-Host "Loaded $loaded variable(s) from '$envFile' into this process (values hidden)."
if ($skipped -gt 0) {
    Write-Host "Left $skipped variable(s) unchanged because they are already set in the environment."
}

$exitCode = 1
Push-Location -LiteralPath $backend
try {
    & mvn spring-boot:run @MavenArgs
    $exitCode = $LASTEXITCODE
}
finally {
    # Leave no trace: the .env values must not leak into the shell session
    # after the backend stops.
    foreach ($key in $setKeys) {
        [Environment]::SetEnvironmentVariable($key, $null, 'Process')
    }
    Pop-Location
}

exit $exitCode
