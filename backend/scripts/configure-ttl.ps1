param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z][a-z0-9-]{4,28}[a-z0-9]$')]
    [string]$ProjectId
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw 'gcloud is required. Install the Google Cloud CLI and authenticate first.'
}

$policies = @(
    @{ CollectionGroup = 'typing'; Field = 'expiresAt' },
    @{ CollectionGroup = 'functionEvents'; Field = 'expireAt' }
)

foreach ($policy in $policies) {
    $arguments = @(
        'firestore',
        'fields',
        'ttls',
        'update',
        $policy.Field,
        "--collection-group=$($policy.CollectionGroup)",
        '--enable-ttl',
        "--project=$ProjectId",
        '--async'
    )
    & gcloud @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to enable TTL for $($policy.CollectionGroup).$($policy.Field)."
    }
}

& gcloud firestore fields ttls list "--project=$ProjectId"
if ($LASTEXITCODE -ne 0) {
    throw 'TTL policies were submitted, but listing their status failed.'
}
