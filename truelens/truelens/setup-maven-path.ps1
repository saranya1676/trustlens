# Permanently adds Maven to the current user's PATH
$mvnBin = "C:\Program Files\apache-maven-3.9.16\bin"
$currentPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*apache-maven*") {
    $newPath = $currentPath + ";" + $mvnBin
    [System.Environment]::SetEnvironmentVariable("Path", $newPath, "User")
    Write-Host "SUCCESS: Maven added to user PATH. Please restart your terminal."
} else {
    Write-Host "Maven is already in user PATH."
}
# Also update current session so it works right now without restart
$env:PATH += ";" + $mvnBin
Write-Host "Current session PATH updated. Testing mvn..."
& "$mvnBin\mvn.cmd" --version
