param([Parameter(Mandatory=$true)][string]$ModsDirectory,[switch]$Enable)
$ErrorActionPreference='Stop'
$jar=Join-Path $PSScriptRoot 'build/libs/LutiRPG-0.3.4.jar'
if(!(Test-Path -LiteralPath $jar)){throw 'Compile primeiro com build.ps1.'}
$target=[IO.Path]::GetFullPath($ModsDirectory)
if((Split-Path $target -Leaf) -ne 'mods'){throw 'Informe explicitamente a pasta mods do mundo/servidor de teste, com ele fechado.'}
New-Item -ItemType Directory -Force -Path $target | Out-Null
$destination=Join-Path $target 'LutiRPG-0.3.4.jar'
if(Test-Path -LiteralPath $destination){Copy-Item -LiteralPath $destination -Destination "$destination.bak" -Force}
$other=Get-ChildItem -LiteralPath $target -Filter 'LutiRPG*.jar' | Where-Object {$_.FullName -ne $destination}
foreach($old in $other){Move-Item -LiteralPath $old.FullName -Destination ($old.FullName+'.'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.bak')}
Copy-Item -LiteralPath $jar -Destination $destination -Force
if($Enable){
  $config=Join-Path (Split-Path $target) 'config.json'
  if(!(Test-Path -LiteralPath $config)){throw 'JAR copiado, mas config.json nao encontrado. Habilite o mod pela interface do mundo.'}
  Copy-Item -LiteralPath $config -Destination "$config.before-lutirpg.bak" -Force
  $data=Get-Content -Raw -LiteralPath $config | ConvertFrom-Json -AsHashtable
  if(!$data.ContainsKey('Mods')){$data.Mods=@{}}
  $data.Mods['br.com.lutiplay:LutiRPG']=@{Enabled=$true}
  $data | ConvertTo-Json -Depth 100 | Set-Content -LiteralPath $config -Encoding utf8NoBOM
}
Write-Host "Instalado: $destination. Habilite LutiRPG em Editar Mundo > Mods (ou use -Enable com o mundo fechado). No jogo: /rpg."

