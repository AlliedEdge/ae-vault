# Generate a secure JWT secret for HS512 algorithm
# HS512 requires at least 512 bits (64 bytes)

Write-Host "Generating secure JWT secret for HS512 algorithm..." -ForegroundColor Cyan
Write-Host ""

# Generate 64 random bytes (512 bits) - secure for HS512
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)

# Convert to Base64
$base64Secret = [Convert]::ToBase64String($bytes)

Write-Host "✅ Generated secure JWT secret (512 bits / 64 bytes)" -ForegroundColor Green
Write-Host ""
Write-Host "Your new JWT_SECRET:" -ForegroundColor Yellow
Write-Host $base64Secret -ForegroundColor White
Write-Host ""
Write-Host "To use this secret:" -ForegroundColor Cyan
Write-Host "1. Copy the secret above" -ForegroundColor Gray
Write-Host "2. Update your .env file:" -ForegroundColor Gray
Write-Host "   JWT_SECRET=$base64Secret" -ForegroundColor Gray
Write-Host ""
Write-Host "Security Info:" -ForegroundColor Magenta
Write-Host "- Key size: 512 bits (64 bytes)" -ForegroundColor Gray
Write-Host "- Algorithm: HS512 compatible" -ForegroundColor Gray
Write-Host "- Format: Base64 encoded" -ForegroundColor Gray
Write-Host "- Entropy: Cryptographically secure random" -ForegroundColor Gray
