package br.com.lutiplay.rpg;
import java.util.*;
import java.nio.file.*;
import java.io.*;
/** Runs without proprietary API classes. */
public final class CoreRegressionTest {
 private static int assertions;
 public static void main(String[] args)throws Exception{
  var ledger=new CombatCredit();var victim=UUID.randomUUID();var killer=UUID.randomUUID();ledger.remember(victim,killer,200,1000);check(ledger.take(victim,1001).player().equals(killer),"credito por UUID sobrevive troca de entidade do jogador");check(ledger.take(victim,1002)==null,"morte nao concede credito duas vezes");ledger.remember(victim,killer,200,1000);check(ledger.take(victim,31001)==null,"credito antigo expira");ledger.remember(victim,killer,200,1000);ledger.clear(victim);check(ledger.take(victim,1001)==null,"outro atacante remove credito anterior");
  for(var type:RpgCore.HeroClass.values()){
   var h=new RpgCore.Hero(UUID.randomUUID());h.chooseClass(type);
   check(RpgCore.ALLOWED.get(type).size()==5,"cinco atributos por classe");
   check(h.bonusesFor(RpgCore.SPECIALTY.get(type)).equals(new RpgCore.CombatBonuses(0,0,0,0,0)),"sem bonus automatico");
   reject(()->h.chooseClass(type));h.addXp(RpgCore.totalXpForLevel(50));check(h.level==50&&h.unspentPoints==245,"245 pontos");
   for(var a:RpgCore.Attribute.values())if(!RpgCore.ALLOWED.get(type).contains(a))reject(()->h.spend(a,1));
   for(var a:RpgCore.ALLOWED.get(type))h.spend(a,10);
   for(var w:RpgCore.WeaponType.values())if(w!=RpgCore.SPECIALTY.get(type))check(h.bonusesFor(w).equals(new RpgCore.CombatBonuses(0,0,0,0,0)),"sem perk fora da especializacao");
   check(h.vitalityBonus()==20&&Math.abs(h.resistanceReduction()-.03)<1e-8,"vida e resistencia universais");
   reject(()->h.spend(RpgCore.Attribute.FORCA,0));reject(()->h.spend(RpgCore.Attribute.FORCA,-1));reject(()->h.spend(RpgCore.Attribute.FORCA,999));
   long xp=h.xp;h.addXp(0,10);check(h.xp==xp&&h.level==50,"reduzir teto nao apaga XP");
   h.addXp(Long.MAX_VALUE,60);check(h.level==60&&h.unspentPoints==245,"expandir teto preserva pontos");
  }
  var h=new RpgCore.Hero(UUID.randomUUID());reject(()->h.spend(RpgCore.Attribute.FORCA,1));reject(()->h.addXp(-1));h.chooseClass(RpgCore.HeroClass.PALADINO);
  var qs=List.of(new RpgCore.Quest("T1","T","Teste","Teste",List.of(new RpgCore.Objective("collect","wood",2),new RpgCore.Objective("kill","goblin",1)),500,"wood",3));
  check(!RpgCore.recordAction(h,qs,"collect","stone",999).matched(),"evento errado ignorado");
  check(!RpgCore.recordAction(h,qs,"collect","wood",Integer.MAX_VALUE).finished(),"exige todos objetivos");
  check(RpgCore.recordAction(h,qs,"kill","goblin",1).finished(),"conclusao");
  RpgCore.recordAction(h,qs,"kill","goblin",1);check(h.pendingRewards.size()==1&&h.xp==500,"nao duplica XP/premio");
  var repo=new HeroRepository(Files.createTempDirectory("lutirpg-test-"));h.starterKitGranted=true;h.facts.add("boss.gobliterator");h.uncertainDeliveries.add("Weapon_Sword_Crude x1");repo.save(h);
  var loaded=repo.load(h.playerId);check(loaded.starterKitGranted&&!StarterKits.eligible(loaded),"reentrada nao repete kit");check(loaded.facts.equals(h.facts)&&loaded.pendingRewards.equals(h.pendingRewards)&&loaded.uncertainDeliveries.equals(h.uncertainDeliveries),"persistencia completa");
  var bad=new RpgCore.Hero(UUID.randomUUID());bad.unspentPoints=999;repo.save(bad);boolean rejected=false;try{repo.load(bad.playerId);}catch(IOException e){rejected=true;}check(rejected,"save invalido nao e resetado");
  List<RpgCore.Quest> campaign;try(var in=CoreRegressionTest.class.getResourceAsStream("/quests.tsv")){campaign=QuestLoader.load(in);}check(campaign.size()==60,"60 missoes");
  var finisher=new RpgCore.Hero(UUID.randomUUID());finisher.chooseClass(RpgCore.HeroClass.MAGO);
  for(var q:campaign)for(var o:q.objectives())RpgCore.recordAction(finisher,campaign,o.action(),o.target(),o.required());
  check(finisher.questIndex==60&&finisher.pendingRewards.size()==60,"campanha completa no motor");
  Set<String> ids=new HashSet<>();try(var r=new BufferedReader(new InputStreamReader(CoreRegressionTest.class.getResourceAsStream("/canonical-items.txt")))){r.lines().forEach(ids::add);}
  Properties items=new Properties();try(var in=CoreRegressionTest.class.getResourceAsStream("/items.properties")){items.load(in);}for(var v:items.values())check(ids.contains(v),"ID oficial: "+v);
  Properties targets=new Properties();try(var in=CoreRegressionTest.class.getResourceAsStream("/targets.properties")){targets.load(in);}
  Set<String> special=Set.of("starting_area","distant_region","new_biome","goblin_lab","deepworks","deepworks_entrance");
  for(var q:campaign)for(var o:q.objectives()){
   String key=switch(o.action()){case "kill"->"kill."+o.target();case "explore"->"area."+o.target();default->o.target();};
   if(Set.of("craft","collect","kill").contains(o.action())||(o.action().equals("explore")&&!special.contains(o.target())))check(targets.containsKey(key),"objetivo mapeado: "+key);
  }
  var settingsDir=Files.createTempDirectory("lutirpg-config-test-");check(RpgSettings.load(settingsDir).maxLevel()==50,"configuracao externa padrao");
  Files.writeString(settingsDir.resolve("config.properties"),"maxLevel=60\ndamageScale=1.1\n");boolean badConfig=false;try{RpgSettings.load(settingsDir);}catch(IOException e){badConfig=true;}check(badConfig,"escala nao ultrapassa limite dos bonus");
  check(Names.of(RpgCore.Attribute.PODER_ARCANO).equals("Poder Arcano"),"interface localizada");
  try(var in=CoreRegressionTest.class.getResourceAsStream("/Common/UI/Custom/LutiRPG/Menu.ui")){String ui=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);check(!java.util.regex.Pattern.compile("#[A-Za-z][A-Za-z0-9]*_").matcher(ui).find(),"seletores UI sem sublinhado");}
  System.out.println("OK: "+assertions+" verificacoes de regressao — classes, matriz de armas, pontos, XP, saves, premios e 60 missoes.");
 }
 static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
 static void reject(Runnable operation){boolean result=false;try{operation.run();}catch(IllegalArgumentException|IllegalStateException e){result=true;}check(result,"operacao invalida bloqueada");}
}
