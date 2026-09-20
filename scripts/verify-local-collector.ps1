# 로컬 Docker 전용. 매 실행 새 프로젝트와 임시 PostgreSQL을 만들며 운영 환경은 사용하지 않는다.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$project = 'pickdeal-collector-verify-' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
$variables = @('VERIFY_DB_PASSWORD', 'VERIFY_TOKEN', 'VERIFY_IMAGE_TAG')
$saved = @{}
foreach ($name in $variables) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$env:VERIFY_DB_PASSWORD = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:VERIFY_TOKEN = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:VERIFY_IMAGE_TAG = $project
$compose = @('compose', '-p', $project, '-f', (Join-Path $root 'compose.collector-test.yml'))
function Invoke-Compose {
    param([string[]]$DockerArguments)
    & docker @compose @DockerArguments
    if ($LASTEXITCODE -ne 0) { throw 'Docker verification command failed. See preceding output.' }
}
Push-Location $root
try {
    Write-Output "Verification project: $project (isolated; no host ports; temporary database)"
    Invoke-Compose -DockerArguments @('config', '--quiet')
    Invoke-Compose -DockerArguments @('build', 'backend', 'verify')
    Invoke-Compose -DockerArguments @('up', '-d', 'backend')
    Invoke-Compose -DockerArguments @('run', '--rm', '--no-deps', 'verify')
    $sql = "SELECT (SELECT count(*) FROM deal)=4, (SELECT count(*) FROM source)=1, (SELECT count(*) FROM deal WHERE status='EXPIRED')=1, NOT EXISTS(SELECT source_id,external_id FROM deal GROUP BY source_id,external_id HAVING count(*)>1), (SELECT count(*) FROM flyway_schema_history WHERE success)>0;"
    $result = & docker @compose exec -T postgres psql -U collector_verify -d collector_verify -At -c $sql
    if ($LASTEXITCODE -ne 0 -or ($result -join '').Trim() -ne 't|t|t|t|t') { throw 'PostgreSQL data verification failed' }
    Write-Output 'PostgreSQL: PASS (4 deals, 1 source, expired preserved, no duplicates, Flyway applied)'
    Invoke-Compose -DockerArguments @('ps')
} finally {
    # 이 스크립트에서 방금 만든 임의 프로젝트만 정리한다. 기존 프로젝트/볼륨은 건드리지 않는다.
    & docker @compose down --remove-orphans
    if ($LASTEXITCODE -ne 0) { Write-Warning "Cleanup failed; inspect project $project" }
    foreach ($name in $variables) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
    Pop-Location
}
