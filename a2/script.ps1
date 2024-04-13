Start-Transcript -Path "diff.txt" -Append
Set-Location -Path "200050048-pa2/"
Remove-Item -Path "*.class" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "testcase" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "sootOutput" -Force -ErrorAction SilentlyContinue

javac -cp ".;../../soot.jar" PA2.java

$passed = 0
$total = 0

foreach ($folder in Get-ChildItem -Directory "../testcases/") {
    New-Item -ItemType Directory -Name "testcase" | Out-Null
    Copy-Item -Path "$($folder.FullName)\*" -Destination "testcase\" -Recurse
    Set-Location -Path "testcase\"
    javac -g *.java
    Set-Location -Path ".."

    Write-Host "####################################################################### $($folder.FullName)"
    $output = & java -cp ".;../../soot.jar" PA2 | Select-String -Pattern "^Soot " -NotMatch
    Write-Host "Your Output:"
    Write-Host "$output"
    $expected = Get-Content "testcase\answer\Test*"
    Write-Host "Expected Output:"
    Write-Host "$expected"
    if ("$output" -eq "$expected") {
        Write-Host "############################################################# PASSED"
        $passed++
    } else {
        Write-Host "############################################################# FAILED"
    }
    $total++
    Remove-Item -Path "testcase" -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "########################################################################### RESULTS"
Write-Host "Total: $total"
Write-Host "Passed: $passed"

Remove-Item -Path "*.class" -Recurse -Force -ErrorAction SilentlyContinue
Set-Location -Path ".."
Stop-Transcript