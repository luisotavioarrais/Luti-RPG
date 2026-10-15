param([string]$JavaHome,[string]$ServerJar="$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar")
$ErrorActionPreference='Stop'
Push-Location $PSScriptRoot
try {
  if(!$JavaHome){$JavaHome=(Get-ChildItem tools/jdk -Directory | Select-Object -First 1).FullName}
  if(!$JavaHome -or !(Test-Path -LiteralPath "$JavaHome/bin/javac.exe")){throw 'Informe -JavaHome com um JDK 25 ou mais recente.'}
  if(!(Test-Path -LiteralPath $ServerJar)){throw 'HytaleServer.jar legitimo nao encontrado. Informe -ServerJar.'}
  New-Item -ItemType Directory -Force libs,build/classes,build/libs,build/reports | Out-Null
  if((Resolve-Path $ServerJar).Path -ne (Join-Path $PSScriptRoot 'libs/HytaleServer.jar')){Copy-Item -LiteralPath $ServerJar -Destination libs/HytaleServer.jar}
  $sources=(Get-ChildItem src/main/java -Recurse -Filter *.java).FullName
  & "$JavaHome/bin/javac.exe" --release 25 -encoding UTF-8 -classpath libs/HytaleServer.jar -d build/classes $sources
  if($LASTEXITCODE -ne 0){throw 'Compilacao falhou.'}
  & "$JavaHome/bin/java.exe" -cp 'build/classes;src/main/resources' br.com.lutiplay.rpg.CoreSmokeTest | Tee-Object build/reports/core-tests.txt
  if($LASTEXITCODE -ne 0){throw 'Testes falharam.'}
  & "$JavaHome/bin/java.exe" -cp 'build/classes;src/main/resources' br.com.lutiplay.rpg.CoreRegressionTest | Tee-Object build/reports/regression-tests.txt
  if($LASTEXITCODE -ne 0){throw 'Regressao falhou.'}
  & "$JavaHome/bin/jar.exe" --create --file build/libs/LutiRPG-0.3.4.jar -C build/classes . -C src/main/resources .
  if($LASTEXITCODE -ne 0){throw 'Empacotamento falhou.'}
  Get-FileHash -LiteralPath build/libs/LutiRPG-0.3.4.jar | Format-List | Out-String | Set-Content build/reports/jar-sha256.txt
  Write-Host 'Gerado build/libs/LutiRPG-0.3.4.jar'
} finally {Pop-Location}

