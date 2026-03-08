$BASE_URL = "http://localhost:8080"

Write-Host "`n=== REGISTER ===" -ForegroundColor Cyan

$registerBody = @{
    fullName = "Test User"
    email    = "testuser@kuet.ac.bd"
    password = "password123"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody

    Write-Host "SUCCESS" -ForegroundColor Green
    Write-Host "Email  : $($registerResponse.email)"
    Write-Host "Roles  : $($registerResponse.roles -join ', ')"
    Write-Host "Token  : $($registerResponse.token.Substring(0, 40))..."
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Write-Host "FAILED (HTTP $status)" -ForegroundColor Red
    Write-Host $_.ErrorDetails.Message
}

Write-Host "`n=== LOGIN ===" -ForegroundColor Cyan

$loginBody = @{
    email    = "testuser@kuet.ac.bd"
    password = "password123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody

    Write-Host "SUCCESS" -ForegroundColor Green
    Write-Host "Email  : $($loginResponse.email)"
    Write-Host "Roles  : $($loginResponse.roles -join ', ')"
    Write-Host "Token  : $($loginResponse.token.Substring(0, 40))..."

    # Decode payload to verify roles inside JWT
    Write-Host "`n=== JWT PAYLOAD ===" -ForegroundColor Cyan
    $parts = $loginResponse.token -split '\.'
    $payload = $parts[1]
    # Add padding if needed
    $pad = 4 - ($payload.Length % 4)
    if ($pad -ne 4) { $payload += "=" * $pad }
    $decoded = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload))
    Write-Host $decoded
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Write-Host "FAILED (HTTP $status)" -ForegroundColor Red
    Write-Host $_.ErrorDetails.Message
}
