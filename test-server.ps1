param([string]$JavaHome,[string]$AssetsPath="$env:APPDATA\Hytale\install\release\package\game\latest\Assets.zip")
$ErrorActionPreference='Stop'
if(!$JavaHome){$JavaHome=(Get-ChildItem (Join-Path $PSScriptRoot 'tools/jdk') -Directory | Select-Object -First 1).FullName}
$jar=Join-Path $PSScriptRoot 'build/libs/LutiRPG-0.3.4.jar'
$server=Join-Path $PSScriptRoot 'libs/HytaleServer.jar'
$directory=Join-Path $PSScriptRoot 'test-server'
New-Item -ItemType Directory -Force (Join-Path $directory 'mods'),(Join-Path $PSScriptRoot 'build/reports') | Out-Null
Get-ChildItem -LiteralPath (Join-Path $directory 'mods') -Filter 'LutiRPG*.jar' | Where-Object {$_.Name -ne 'LutiRPG-0.3.4.jar'} | ForEach-Object {Move-Item -LiteralPath $_.FullName -Destination ($_.FullName+'.previous') -Force}
Copy-Item -LiteralPath $jar -Destination (Join-Path $directory 'mods/LutiRPG-0.3.4.jar') -Force
$info=[Diagnostics.ProcessStartInfo]::new()
$info.FileName=Join-Path $JavaHome 'bin/java.exe'
$info.WorkingDirectory=$directory
$info.UseShellExecute=$false
$info.CreateNoWindow=$true
$info.WindowStyle=[Diagnostics.ProcessWindowStyle]::Hidden
$info.RedirectStandardOutput=$true
$info.RedirectStandardError=$true
foreach($argument in @('--enable-native-access=ALL-UNNAMED','-Dfile.encoding=UTF-8','-Dlutirpg.selfTest=true','-Xmx3G','-jar',$server,'--assets',$AssetsPath,'--auth-mode','offline','--bind','127.0.0.1:15520','--disable-sentry','--disable-file-watcher','--boot-command','stop')){$info.ArgumentList.Add($argument)}
$process=[Diagnostics.Process]::Start($info)
$stdout=$process.StandardOutput.ReadToEndAsync();$stderr=$process.StandardError.ReadToEndAsync()
if(!$process.WaitForExit(60000)){$process.Kill($true);throw 'Servidor de teste excedeu 60 segundos.'}
$output=$stdout.GetAwaiter().GetResult()+$stderr.GetAwaiter().GetResult()
$output | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'build/reports/server-selftest.txt') -Encoding utf8NoBOM
if($process.ExitCode -ne 0 -or !$output.Contains('LUTIRPG_SERVER_SELFTEST_OK') -or !$output.Contains('Enabled plugin br.com.lutiplay:LutiRPG')){throw 'Self-test falhou. Consulte build/reports/server-selftest.txt.'}
'OK: plugin carregado e self-tests com API/assets oficiais aprovados. Este teste nao e uma partida completa.'

