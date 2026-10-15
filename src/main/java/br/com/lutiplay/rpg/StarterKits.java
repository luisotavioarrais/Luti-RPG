package br.com.lutiplay.rpg;
import java.util.*;
/** IDS LOGICOS: Codex deve mapear para IDs canonicos do Hytale instalado. */
public final class StarterKits {
 private StarterKits(){}
 public record Item(String logicalId,int quantity){}
 public static final Map<RpgCore.HeroClass,List<Item>> KITS=Map.of(
  RpgCore.HeroClass.GUERREIRO,List.of(new Item("starter_two_handed_axe",1)),
  RpgCore.HeroClass.PALADINO,List.of(new Item("starter_one_hand_sword",1),new Item("starter_shield",1)),
  RpgCore.HeroClass.ARQUEIRO,List.of(new Item("starter_bow",1),new Item("starter_arrow",50)),
  RpgCore.HeroClass.ASSASSINO,List.of(new Item("starter_dagger",2)),
  RpgCore.HeroClass.MAGO,List.of(new Item("starter_runestone",1)));
 /** O adapter deve marcar esta flag e persistir SOMENTE depois de conceder/guardar todos itens com sucesso. */
 public static boolean eligible(RpgCore.Hero h){return h.heroClass!=null&&!h.starterKitGranted;}
}
