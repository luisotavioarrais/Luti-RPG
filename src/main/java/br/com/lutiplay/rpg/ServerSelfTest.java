package br.com.lutiplay.rpg;
import java.util.*;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.projectile.component.ImpactModifiers;
import com.hypixel.hytale.server.core.modules.entitystats.*;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.*;
/** Opt-in integration tests with REAL loaded assets. Not a client/playthrough test. */
public final class ServerSelfTest {
 public static void run(ContentCatalog c){
  for(String role:List.of("Bear_Grizzly","Bear_Polar","Bear_Voidtaken"))check(c.matches("kill.hostile_basic",role),"urso hostil: "+role);
  check(!c.matches("kill.hostile_basic","Bear_Voidtaken_Tentacle_Marker")&&!c.matches("kill.hostile_basic","Cow"),"marcador invisivel e vaca nao sao hostis");
  check(RpgService.mobXpValue("Bear_Grizzly",c.matches("kill.hostile_basic","Bear_Grizzly"),124)==31,"urso pardo concede XP de hostil");
  check(c.matches("food","Plant_Fruit_Apple")&&c.matches("food","Plant_Fruit_Berries_Red")&&!c.matches("food","Plant_Seeds_Lettuce"),"frutas comestiveis contam, sementes nao");
  check(!new ItemStack("LutiRPG_Diario").getItem().dropsOnDeath(),"livro preservado na morte pelo asset nativo");
  var journalDrop=new com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent.Drop(new ItemStack("LutiRPG_Diario"),1);new RpgSystems.ProtectJournalDrop().handle(0,null,null,null,journalDrop);check(journalDrop.isCancelled(),"bloqueio do descarte do livro");
  var ordinaryDrop=new com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent.Drop(new ItemStack("Rubble_Stone"),1);new RpgSystems.ProtectJournalDrop().handle(0,null,null,null,ordinaryDrop);check(!ordinaryDrop.isCancelled(),"outros itens continuam descartaveis");
  check(RpgService.mobXpValue("Cow",false,200)>RpgService.mobXpValue("Cow",false,50)&&RpgService.mobXpValue("Spider",true,200)>RpgService.mobXpValue("Spider",true,50),"mais vida maxima concede mais XP");
  check(RpgService.mobXpValue("Cow",false,Double.NaN)==10&&RpgService.mobXpValue("Cow",false,999999)==250,"XP limitada e atributos invalidos tratados");
  check(c.matches("stone","Rubble_Stone")&&c.matches("stone","Rubble_Stone_Mossy_Medium")&&c.matches("stone","Rock_Stone"),"pedrinhas do chao e blocos contam como pedra");
  check(!c.matches("stone","Rubble_Ice")&&!c.matches("stone","Ingredient_Stick"),"gelo e graveto nao contam como pedra");
  check(com.hypixel.hytale.server.core.asset.type.item.config.Item.getAssetMap().getAsset("LutiRPG_Diario")!=null,"diario carregado");
  check(RpgService.mobXpValue("Trork_Warrior",true)==25&&RpgService.mobXpValue("Cow",false)==10,"XP por criatura independente de missao");
  check(RpgService.mobXpValue("Goblin_Gobliterator_Phase2",true)==0&&RpgService.mobXpValue("Goblin_Gobliterator_Phase3",true)==500,"transicao do chefe nao concede XP");
  var hero=new RpgCore.Hero(UUID.randomUUID());hero.chooseClass(RpgCore.HeroClass.MAGO);
  check(RpgHud.fillWidth(hero,50)==0,"barra inicialmente vazia");hero.addXp(125);check(RpgHud.fillWidth(hero,50)==240,"barra metade do nivel");hero.addXp(125);check(hero.level==2&&hero.unspentPoints==5&&RpgHud.fillWidth(hero,50)==0,"abates sobem nivel e reiniciam barra");
  hero.addXp(Long.MAX_VALUE);check(RpgHud.fillWidth(hero,50)==480&&RpgHud.xpLabel(hero,50).contains("MÁXIMO"),"barra cheia no nivel maximo");
  var hudCommands=new com.hypixel.hytale.server.core.ui.builder.UICommandBuilder();new RpgHud(null,"Teste",hero,50).build(hudCommands);
  var anchorCommand=Arrays.stream(hudCommands.getCommands()).filter(command->"#XpFill.Anchor".equals(command.selector)).findFirst().orElseThrow();
  var anchorJson=com.google.gson.JsonParser.parseString(anchorCommand.data).getAsJsonObject().getAsJsonObject("0");check(anchorJson.get("Width").getAsInt()==480&&anchorJson.get("Left").getAsInt()==0,"largura serializada como objeto Anchor completo");
  check(QuestNavigation.arrow(0,-1,0,-10).equals("↑")&&QuestNavigation.arrow(0,-1,10,0).equals("→")&&QuestNavigation.arrow(0,-1,-10,0).equals("←")&&QuestNavigation.arrow(0,-1,0,10).equals("↓"),"seta relativa a camera nas quatro direcoes");
  check(c.weapon(new ItemStack("Weapon_Battleaxe_Crude"),null)==RpgCore.WeaponType.DUAS_MAOS,"machado duas maos");
  check(c.weapon(new ItemStack("Weapon_Sword_Crude"),null)==RpgCore.WeaponType.OUTRA,"espada sem escudo");
  check(c.weapon(new ItemStack("Weapon_Sword_Crude"),new ItemStack("Weapon_Shield_Wood"))==RpgCore.WeaponType.ESPADA_ESCUDO,"espada com escudo");
  check(c.weapon(new ItemStack("Weapon_Daggers_Crude"),null)==RpgCore.WeaponType.ADAGAS,"par nativo adagas");
  check(c.weapon(new ItemStack("Weapon_Shortbow_Crude"),null)==RpgCore.WeaponType.ARCO_BESTA,"arco");
  check(c.weapon(new ItemStack("Weapon_Crossbow_Iron"),null)==RpgCore.WeaponType.ARCO_BESTA,"besta");
  check(new ItemStack("Rune_Fireball").getItem().getAbility() instanceof com.hypixel.hytale.server.core.asset.type.item.config.CoreItemAbility,"runa oficial");
  var container=new SimpleItemContainer((short)1);check(container.addItemStack(new ItemStack("Weapon_Sword_Crude"),true,false,true).succeeded(),"entrega nativa");
  var item=new ItemStack("Weapon_Shield_Wood");check(!container.canAddItemStack(item,true,false),"inventario cheio");var denied=container.addItemStack(item,true,false,true);check(!denied.succeeded()&&denied.getRemainder().getQuantity()==1,"all-or-nothing conserva item");
  var offense=new RpgSystems.Offense();var cause=DamageCause.getAssetMap().getAsset("Physical");
  for(float bonus:new float[]{0,.5f}){var damage=new Damage(Damage.NULL_SOURCE,cause,20);damage.putMetaObject(ImpactModifiers.DAMAGE_MODIFIERS,new ImpactModifiers(Map.of("LutiRPG.Damage",bonus,"LutiRPG.Crit",0f),null));offense.handle(0,null,null,null,damage);check(damage.getAmount()==20*(1+bonus),"dano nativo/especializado");}
  var nativeDamage=new Damage(Damage.NULL_SOURCE,cause,20);offense.handle(0,null,null,null,nativeDamage);check(nativeDamage.getAmount()==20,"sem snapshot intacto");
  var stats=new EntityStatMap();stats.update();int health=DefaultEntityStatTypes.getHealth();float before=stats.get(health).getMax();
  stats.putModifier(health,"LutiRPG.Test",new StaticModifier(Modifier.ModifierTarget.MAX,StaticModifier.CalculationType.ADDITIVE,20));check(stats.get(health).getMax()==before+20,"vida nativa");stats.removeModifier(health,"LutiRPG.Test");check(stats.get(health).getMax()==before,"vida restaurada");
  System.out.println("LUTIRPG_SERVER_SELFTEST_OK: armas, adagas, runa, inventario cheio, dano e vida com API real.");
 }
 private static void check(boolean value,String message){if(!value)throw new AssertionError("LutiRPG server self-test: "+message);}
}
