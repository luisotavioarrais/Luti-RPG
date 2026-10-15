package br.com.lutiplay.rpg;
import java.util.*;import java.nio.file.*;
public final class CoreSmokeTest {
 public static void main(String[] args)throws Exception{
  var h=new RpgCore.Hero(UUID.randomUUID());h.chooseClass(RpgCore.HeroClass.GUERREIRO);
  h.addXp(RpgCore.totalXpForLevel(2));assertTrue(h.level==2&&h.unspentPoints==5,"XP e 5 pontos");
  h.spend(RpgCore.Attribute.POTENCIA,3);assertTrue(h.unspentPoints==2,"gasto");
  boolean blocked=false;try{h.spend(RpgCore.Attribute.MANA,1);}catch(IllegalArgumentException ex){blocked=true;}assertTrue(blocked,"atributo da outra classe bloqueado");
  assertTrue(h.bonusesFor(RpgCore.WeaponType.DUAS_MAOS).attackDamageBonus()>0,"bonus com arma da classe");
  assertTrue(h.bonusesFor(RpgCore.WeaponType.OUTRA).attackDamageBonus()==0,"arma de fora nao recebe bonus");
  assertTrue(StarterKits.KITS.get(RpgCore.HeroClass.ARQUEIRO).get(1).quantity()==50,"50 flechas");
  var qs=List.of(new RpgCore.Quest("Q1","F1","Teste","Teste",List.of(new RpgCore.Objective("collect","wood",2)),100,"stone",3));
  RpgCore.recordAction(h,qs,"collect","wood",1);assertTrue(h.questIndex==0,"nao conclui antes");
  var result=RpgCore.recordAction(h,qs,"collect","wood",1);assertTrue(result.finished()&&h.questIndex==1,"concluida");
  var dir=Files.createTempDirectory("luti-test");h.starterKitGranted=true;new HeroRepository(dir).save(h);
  var loaded=new HeroRepository(dir).load(h.playerId);assertTrue(loaded.starterKitGranted&&loaded.questIndex==1,"salvamento");
  try(var in=CoreSmokeTest.class.getResourceAsStream("/quests.tsv")){assertTrue(in!=null&&QuestLoader.load(in).size()==60,"60 missoes");}
  var mage=new RpgCore.Hero(UUID.randomUUID());mage.chooseClass(RpgCore.HeroClass.MAGO);mage.addXp(RpgCore.totalXpForLevel(50));assertTrue(mage.unspentPoints==245,"maximo 50 e 245 pontos");
  System.out.println("OK: classes, pontos travados, bonus por arma, kit, quest, save e 60 missoes");
 }
 static void assertTrue(boolean b,String m){if(!b)throw new AssertionError(m);}
}
