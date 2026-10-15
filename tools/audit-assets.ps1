param([string]$AssetsPath="$env:APPDATA\Hytale\install\release\package\game\latest\Assets.zip")
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$project=Split-Path $PSScriptRoot
$zip=[IO.Compression.ZipFile]::OpenRead($AssetsPath)
try {
  $items=@{}; $paths=@{}
  foreach($entry in $zip.Entries) {
    if($entry.FullName -notmatch '^Server/Item/Items/.+\.json$'){continue}
    $reader=[IO.StreamReader]::new($entry.Open())
    try {$raw=$reader.ReadToEnd(); $data=$raw | ConvertFrom-Json -AsHashtable} finally {$reader.Dispose()}
    $id=[IO.Path]::GetFileNameWithoutExtension($entry.Name)
    $items[$id]=$data; $paths[$id]=$entry.FullName
  }
  function Resolve-Field([string]$Id,[string]$Field,[int]$Depth=0) {
    if($Depth -gt 30 -or !$items.ContainsKey($Id)){return $null}
    $data=$items[$Id]
    if($data.ContainsKey($Field)){return ,$data[$Field]}
    if($data.Parent){return Resolve-Field $data.Parent $Field ($Depth+1)}
    return $null
  }
  $groups=@{}; $families=[Collections.Generic.List[string]]::new()
  function Add-Group([string]$Name,[string]$Id) {
    if(!$groups.ContainsKey($Name)){$groups[$Name]=[Collections.Generic.List[string]]::new()}
    $groups[$Name].Add($Id)
  }
  foreach($id in ($items.Keys | Sort-Object)) {
    if($id -match '^(Template_|Debug_|Prototype_)' -or $paths[$id] -match '/(_Debug|Deprecated|_Deprecated)/'){continue}
    $tags=Resolve-Field $id 'Tags'; $family=if($tags){$tags.Family -join ','}else{''}
    if($family){$families.Add("$id`t$family")}
    $resources=Resolve-Field $id 'ResourceTypes'
    if(@($resources | Where-Object {$_.Id -eq 'Wood_Trunk'}).Count -gt 0){Add-Group wood $id}
    if($id -match '^Rock_(Stone|Sandstone|Shale|Slate|Basalt|Granite|Marble|Limestone)$' -or $id -match '^Rubble_(Stone|Stone_Mossy|Sandstone|Sandstone_Red|Sandstone_White|Shale|Slate|Basalt|Marble|Calcite|Chalk|Lime|Quartzite|Dawnstone|Aqua|Volcanic|Magma_Cooled)(_Medium)?$'){Add-Group stone $id}
    if($id -match '^Ore_Copper($|_)'){Add-Group copper_ore $id}
    if($id -match '^Ore_Iron($|_)'){Add-Group iron_ore $id}
    if($id -eq 'Ingredient_Bar_Copper'){Add-Group copper_ingot $id}
    if($id -eq 'Ingredient_Bar_Iron'){Add-Group iron_ingot $id}
    if(($id -match '^Food_' -or $id -match '^Plant_(Fruit_|Crop_.*_Item$|Mushroom_)') -and (Resolve-Field $id 'Consumable')){Add-Group food $id}
    if($id -match '^Food_' -and (Resolve-Field $id 'Recipe')){Add-Group cooked_food $id}
    if($id -match '^Tool_.*_(Crude|Wood)$'){Add-Group basic_tool $id}
    if($id -match '^Tool_.*_(Copper|Iron)$'){Add-Group improved_tool $id}
    if($id -match '^Weapon_.*_Copper$'){Add-Group copper_weapon $id}
    if($id -match '^Armor_Copper_'){Add-Group copper_armor $id}
    if($id -match '^Weapon_.*_Iron$'){Add-Group iron_weapon $id}
    if($id -match '^Armor_Iron_'){Add-Group iron_armor $id}
    if($id -match '^Weapon_.*_(Thorium|Cobalt|Scrap)$'){Add-Group advanced_weapon $id}
    if($id -match '^(Weapon_.*_(Thorium|Cobalt|Scrap|Iron)$|Armor_(Thorium|Cobalt|Scrap|Iron)_)'){Add-Group endgame_gear $id}
    if($id -match '^Potion_Health'){Add-Group healing_supply $id;Add-Group endgame_supply $id}
    if($id -eq 'Ingredient_Scraps'){Add-Group breach_resource $id}
    if($id -eq 'Ingredient_Rune_Shard'){Add-Group goblin_clue $id}
    if($id -match '^Rune_' -and (Resolve-Field $id 'Ability')){Add-Group runestone $id}
    if($id -match '^(Wood_.*_Planks|Rock_Stone_Brick)$'){Add-Group building_piece $id}
    if($id -match '^Bench_' -and (Resolve-Field $id 'Recipe')){Add-Group workstation_upgrade $id}
  }
  foreach($entry in $zip.Entries) {
    $id=[IO.Path]::GetFileNameWithoutExtension($entry.Name)
    if($entry.FullName -match '^Server/NPC/Roles/' -and $entry.FullName.EndsWith('.json') -and $entry.FullName -notmatch 'Template|Deprecated|Tests|Spectator|Scientist|Trader') {
      if($id -match '^(Goblin_|Dungeon_Goblin_|Trork_|Skeleton_|Zombie_|Spider_|Scarak_)'){Add-Group 'kill.hostile_basic' $id;Add-Group 'kill.cave_hostile' $id}
      # Official predator bears; invisible Hunt markers are deliberately excluded.
      if($id -in @('Bear_Grizzly','Bear_Polar','Bear_Voidtaken')){Add-Group 'kill.hostile_basic' $id}
      if($id -match '^(Goblin_Lab_Scrapper|Temple_Goblin_Scrapper)'){Add-Group 'kill.goblin_lab_guard' $id}
      if($id -match '^(Goblin_|Dungeon_Goblin_)'){Add-Group 'kill.goblin' $id}
      if($id -match '^Dungeon_Goblin_'){Add-Group 'kill.deepworks_enemy' $id}
      if($id -match '^Dungeon_Goblin_(Guardian|Mace|Burner|Scrapper)'){Add-Group 'kill.deepworks_elite' $id;Add-Group 'kill.final_elite' $id}
    }
    if($entry.FullName -match '^Server/Environments/.+\.json$'){
      if($id -match '^Env_Zone[1-4]_(Plains|Forests|Swamps|Mountains|Shores|Deserts|Scrub|Savanna|Plateaus|Tundra|Taiga|Volcanic|Jungle|Ash)$'){Add-Group 'area.surface_biome' $id}
      if($id -match 'Caves'){Add-Group 'area.cave' $id;Add-Group 'area.deep_cave' $id}
      if($id -match 'Ruins|Forgotten_Temple'){Add-Group 'area.ruins' $id}
      if($id -match 'Ruins|Forgotten_Temple|Kweebec|Trork|Mage_Tower'){Add-Group 'area.point_of_interest' $id}
      if($id -match '^Env_Forgotten_Temple_'){Add-Group 'area.forgotten_temple' $id}
      if($id -match '^Env_Forgotten_Temple_Interior|^Env_Forgotten_Temple_Heart$'){Add-Group 'area.temple_ruins' $id}
      if($id -eq 'Env_Portal_Goblin_Surface'){Add-Group 'area.goblin_breach_entry' $id}
      if($id -match '^Env_Portal_Goblin_Cave'){Add-Group 'area.goblin_breach' $id;Add-Group 'area.guttering_hollows' $id}
      if($id -eq 'Env_Portal_Goblin_Cave_Deep'){Add-Group 'area.goblin_ruins' $id}
      if($id -eq 'Env_Dungeon_Goblin_Feastmaster'){Add-Group 'area.deepworks_core' $id}
      if($id -eq 'Env_Dungeon_Goblin_Arena'){Add-Group 'area.gobliterator_arena' $id;Add-Group 'area.final_boss_room' $id}
    }
  }
  $reward=@{wood='Wood_Oak_Trunk';stone='Rock_Stone';copper_ore='Ore_Copper';iron_ore='Ore_Iron';healing_supply='Potion_Health_Lesser';food='Food_Bread';breach_resource='Ingredient_Scraps';runestone='Ingredient_Rune_Shard';victory_token='Ingredient_Gobliterator_Core';starter_two_handed_axe='Weapon_Battleaxe_Crude';starter_one_hand_sword='Weapon_Sword_Crude';starter_shield='Weapon_Shield_Wood';starter_bow='Weapon_Shortbow_Crude';starter_arrow='Weapon_Arrow_Crude';starter_dagger='Weapon_Daggers_Crude';starter_runestone='Rune_Fireball'}
  foreach($id in $reward.Values){if(!$items.ContainsKey($id)){throw "Item nao encontrado: $id"}}
  $quest=Get-Content (Join-Path $project 'src/main/resources/quests.tsv') | Where-Object {$_ -and !$_.StartsWith('#')}
  foreach($line in $quest){$cols=$line.Split("`t");foreach($obj in $cols[4].Split(';')){$parts=$obj.Split(':');if($parts[0] -in @('collect','craft') -and !$groups.ContainsKey($parts[1])){throw "Objetivo sem IDs: $obj"}}}
  $out=Join-Path $project 'src/main/resources'
  $reward.GetEnumerator() | Sort-Object Key | ForEach-Object {"$($_.Key)=$($_.Value)"} | Set-Content (Join-Path $out 'items.properties') -Encoding utf8NoBOM
  $groups.GetEnumerator() | Sort-Object Key | ForEach-Object {"$($_.Key)=$($_.Value -join ',')"} | Set-Content (Join-Path $out 'targets.properties') -Encoding utf8NoBOM
  $families | Set-Content (Join-Path $out 'families.tsv') -Encoding utf8NoBOM
  $items.Keys | Sort-Object | Set-Content (Join-Path $out 'canonical-items.txt') -Encoding utf8NoBOM
  $groups.GetEnumerator() | Sort-Object Key | ForEach-Object {"$($_.Key): $($_.Value.Count) IDs"}
  "Auditados $($items.Count) itens do Assets.zip; nenhum arquivo oficial redistribuido."
} finally {$zip.Dispose()}


