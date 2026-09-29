$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"

function Invoke-Api {
    param([string]$Method, [string]$Url, [string]$Token, [string]$Body)
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    if ($Body) {
        return Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -ContentType "application/json" -Body $Body
    }
    return Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers
}

$health = Invoke-Api GET "$base/api/health"
if ($health.status -ne "up") { throw "health failed" }

$admin = Invoke-Api POST "$base/api/auth/login" $null '{"username":"admin","password":"admin"}'
$viewer = Invoke-Api POST "$base/api/auth/login" $null '{"username":"viewer","password":"viewer"}'

$list = Invoke-Api GET "$base/api/labworks?page=0&size=5" $admin.token
if ($list.total -lt 12) { throw "expected seeded rows, got $($list.total)" }
if ($list.items.Count -ne 5) { throw "page size mismatch" }

$exact = Invoke-Api GET "$base/api/labworks?name=$([uri]::EscapeDataString('Сортировка слиянием'))" $admin.token
if ($exact.total -ne 1) { throw "exact name filter failed: $($exact.total)" }
$partial = Invoke-Api GET "$base/api/labworks?name=$([uri]::EscapeDataString('Сортировка'))" $admin.token
if ($partial.total -ne 0) { throw "partial name must not match" }

$fullDescription = [uri]::EscapeDataString("Внешняя сортировка и алгоритм слияния прогонов")
$byExactDescription = Invoke-Api GET "$base/api/labworks?description=$fullDescription" $admin.token
if ($byExactDescription.total -ne 1) { throw "exact description filter failed" }
$byPiece = Invoke-Api GET "$base/api/labworks?description=$([uri]::EscapeDataString('алгоритм'))" $admin.token
if ($byPiece.total -ne 0) { throw "description filter must be exact" }

$card = Invoke-Api GET "$base/api/labworks/$($exact.items[0].id)" $viewer.token
if (-not $card.coordinates -or -not $card.discipline -or -not $card.author) { throw "related objects missing" }

$average = Invoke-Api GET "$base/api/special/average-minimal-point" $viewer.token
if ($null -eq $average.average) { throw "average missing" }
$found = Invoke-Api GET "$base/api/special/by-description?substring=$([uri]::EscapeDataString('алгоритм'))" $viewer.token
if ($found.Count -lt 1) { throw "substring search failed" }
$unique = Invoke-Api GET "$base/api/special/unique-minimal-points" $viewer.token
if ($unique.values.Count -lt 2) { throw "unique points failed" }

try {
    Invoke-Api POST "$base/api/labworks" $viewer.token '{"name":"x","minimalPoint":1,"coordinates":{"x":1,"y":1},"discipline":{"name":"d","lectureHours":1,"selfStudyHours":1,"labsCount":1}}' | Out-Null
    throw "viewer must not create"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 403) { throw }
}

$bad = $null
try {
    Invoke-Api POST "$base/api/labworks" $admin.token '{"name":"Плохие координаты","minimalPoint":1,"coordinates":{"x":200,"y":1},"discipline":{"name":"Дисциплина проверки","lectureHours":1,"selfStudyHours":1,"labsCount":1}}' | Out-Null
    throw "invalid coordinates accepted"
} catch {
    $bad = $_.ErrorDetails.Message
    if (-not $bad -and $_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        if ($stream) { $bad = [System.IO.StreamReader]::new($stream).ReadToEnd() }
    }
    if ($bad -notmatch "139") { throw "validation message missing: $bad" }
}

$suffix = Get-Random
$created = Invoke-Api POST "$base/api/labworks" $admin.token (@{
    name = "Проверка $suffix"
    minimalPoint = 7
    difficulty = "HARD"
    description = "текст проверки"
    coordinates = @{ x = 5; y = 1.5 }
    discipline = @{ name = "Дисциплина $suffix"; lectureHours = 2; selfStudyHours = 3; labsCount = 1 }
} | ConvertTo-Json -Compress)
if ($created.difficulty -ne "HARD") { throw "create failed" }

$created.name = "Проверка $suffix изменена"
$updated = Invoke-Api PUT "$base/api/labworks/$($created.id)" $admin.token ($created | ConvertTo-Json -Depth 6 -Compress)
if ($updated.name -notlike "*изменена") { throw "update failed" }

$lowered = Invoke-Api POST "$base/api/special/decrease-difficulty" $admin.token (@{ labWorkId = $created.id; steps = 1 } | ConvertTo-Json -Compress)
if ($lowered.difficulty -ne "NORMAL") { throw "difficulty was $($lowered.difficulty)" }

$disciplines = Invoke-Api GET "$base/api/catalog/disciplines" $admin.token
$target = $disciplines | Where-Object { $_.name -eq "Информатика" } | Select-Object -First 1
$added = Invoke-Api POST "$base/api/special/add-hardest" $admin.token (@{ disciplineId = $target.id } | ConvertTo-Json -Compress)
if ($added.labWorkIds.Count -lt 1) { throw "hardest list empty" }

Write-Host "smoke ok: total=$($list.total) average=$($average.average) hardest=$($added.inserted)"
