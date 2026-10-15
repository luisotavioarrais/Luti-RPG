package br.com.lutiplay.rpg;
import java.util.*;
/** Regras independentes do Hytale. Efeitos precisam ser aplicados pelo adapter do servidor. */
public final class RpgCore {
 private RpgCore(){}
 public enum HeroClass { GUERREIRO, PALADINO, MAGO, ASSASSINO, ARQUEIRO }
 public enum Attribute { FORCA, VITALIDADE, RESISTENCIA, FURIA, POTENCIA, PROTECAO, FE, PODER_ARCANO, MANA, AGILIDADE, PRECISAO, DESTREZA, PONTARIA }
 public enum WeaponType { DUAS_MAOS, ESPADA_ESCUDO, ADAGAS, ARCO_BESTA, MAGIA, OUTRA, DESARMADO }
 public static final int DEFAULT_MAX_LEVEL=50, POINTS_PER_LEVEL=5;
 public static final Map<HeroClass,Set<Attribute>> ALLOWED = Map.of(
  HeroClass.GUERREIRO,Set.of(Attribute.FORCA,Attribute.VITALIDADE,Attribute.RESISTENCIA,Attribute.FURIA,Attribute.POTENCIA),
  HeroClass.PALADINO,Set.of(Attribute.FORCA,Attribute.VITALIDADE,Attribute.RESISTENCIA,Attribute.PROTECAO,Attribute.FE),
  HeroClass.MAGO,Set.of(Attribute.FORCA,Attribute.VITALIDADE,Attribute.RESISTENCIA,Attribute.PODER_ARCANO,Attribute.MANA),
  HeroClass.ASSASSINO,Set.of(Attribute.FORCA,Attribute.VITALIDADE,Attribute.RESISTENCIA,Attribute.AGILIDADE,Attribute.PRECISAO),
  HeroClass.ARQUEIRO,Set.of(Attribute.FORCA,Attribute.VITALIDADE,Attribute.RESISTENCIA,Attribute.DESTREZA,Attribute.PONTARIA));
 public static final Map<HeroClass,WeaponType> SPECIALTY = Map.of(
  HeroClass.GUERREIRO,WeaponType.DUAS_MAOS,HeroClass.PALADINO,WeaponType.ESPADA_ESCUDO,
  HeroClass.MAGO,WeaponType.MAGIA,HeroClass.ASSASSINO,WeaponType.ADAGAS,
  HeroClass.ARQUEIRO,WeaponType.ARCO_BESTA);
 public static long totalXpForLevel(int level){if(level<1)throw new IllegalArgumentException("nivel");long n=level-1;return 100L*n*n+150L*n;}
 public static class Hero {
  public final UUID playerId; public HeroClass heroClass; public long xp; public int level=1,unspentPoints=0,questIndex=0;
  public boolean starterKitGranted=false;
  /** Durable facts are separate from the current quest's counters. */
  public final Set<String> facts=new HashSet<>();
  /** Reserved deliveries are never retried automatically after a process crash. */
  public final List<String> uncertainDeliveries=new ArrayList<>();
  public final EnumMap<Attribute,Integer> invested=new EnumMap<>(Attribute.class);
  public final Map<String,Integer> objectiveProgress=new HashMap<>(); public final List<String> pendingRewards=new ArrayList<>();
  public Hero(UUID id){playerId=Objects.requireNonNull(id);for(Attribute a:Attribute.values())invested.put(a,0);}
  public void chooseClass(HeroClass choice){if(heroClass!=null)throw new IllegalStateException("classe ja escolhida");heroClass=Objects.requireNonNull(choice);}
  public int value(Attribute a){return 10+invested.get(a);}
  public void spend(Attribute a,int count){if(heroClass==null)throw new IllegalStateException("escolha a classe");if(!ALLOWED.get(heroClass).contains(a))throw new IllegalArgumentException("atributo bloqueado para classe");if(count<=0||count>unspentPoints)throw new IllegalArgumentException("pontos insuficientes");invested.merge(a,count,Integer::sum);unspentPoints-=count;}
  public int addXp(long gain,int maxLevel){if(gain<0||maxLevel<1)throw new IllegalArgumentException("xp/nivel");long cap=totalXpForLevel(Math.max(maxLevel,level));xp=Math.max(xp,Math.min(cap,gain>Long.MAX_VALUE-xp?Long.MAX_VALUE:xp+gain));int n=0;while(level<maxLevel&&xp>=totalXpForLevel(level+1)){level++;unspentPoints=Math.addExact(unspentPoints,POINTS_PER_LEVEL);n++;}return n;}
  public int addXp(long gain){return addXp(gain,DEFAULT_MAX_LEVEL);}
  /** Bonus comum: vida e resistencia sempre valem. FORCA fora da especializacao NAO aumenta dano da arma. */
  public double vitalityBonus(){return Math.min(100,invested.get(Attribute.VITALIDADE)*2.0);}
  public double resistanceReduction(){return Math.min(.30,invested.get(Attribute.RESISTENCIA)*.003);}
  /** Aplicar somente se arma equipada combina com a especializacao. Valores sugestivos, sujeitos a testes. */
  public CombatBonuses bonusesFor(WeaponType equipped){
   if(heroClass==null||equipped!=SPECIALTY.get(heroClass))return new CombatBonuses(0,0,0,0,0);
   var p=invested;
   return switch(heroClass){
    case GUERREIRO -> new CombatBonuses(cap(p.get(Attribute.FORCA)*.006+p.get(Attribute.POTENCIA)*.009,.85),cap(p.get(Attribute.FURIA)*.004,.30),0,0,0);
    case PALADINO -> new CombatBonuses(cap(p.get(Attribute.FORCA)*.004,.45),0,0,cap(p.get(Attribute.PROTECAO)*.004,.20),cap(p.get(Attribute.FE)*.005,.35));
    case MAGO -> new CombatBonuses(cap(p.get(Attribute.PODER_ARCANO)*.009,.90),0,0,0,cap(p.get(Attribute.MANA)*.01,.80));
    case ASSASSINO -> new CombatBonuses(cap(p.get(Attribute.FORCA)*.003,.30),cap(p.get(Attribute.AGILIDADE)*.005,.35),cap(p.get(Attribute.PRECISAO)*.003,.25),0,0);
    case ARQUEIRO -> new CombatBonuses(cap(p.get(Attribute.PONTARIA)*.008,.75),cap(p.get(Attribute.DESTREZA)*.004,.30),cap(p.get(Attribute.PONTARIA)*.0015,.15),0,0);
   };
  }
 }
 private static double cap(double n,double max){return Math.min(n,max);}
 /** attackDamageBonus %, attackSpeedBonus %, critChanceBonus %, blockBonus %, specialResourceBonus %. */
 public record CombatBonuses(double attackDamageBonus,double attackSpeedBonus,double critChanceBonus,double blockBonus,double specialResourceBonus){}
 public record Objective(String action,String target,int required){public Objective{if(required<1||action.isBlank()||target.isBlank())throw new IllegalArgumentException("objetivo");}public String key(){return action+":"+target;}}
 public record Quest(String id,String phase,String title,String description,List<Objective> objectives,int xp,String rewardId,int rewardQuantity){public Quest{objectives=List.copyOf(objectives);if(objectives.isEmpty()||xp<0||rewardQuantity<0)throw new IllegalArgumentException("missao");}}
 public record Progress(boolean matched,boolean finished,int current,int required,int levelsGained,String rewardPending){}
 /** Chamar APENAS de eventos reais autenticados pelo servidor; nao expor como comando publico. */
 public static Progress recordAction(Hero hero,List<Quest> quests,String action,String target,int qty,int maxLevel){
  if(hero.heroClass==null)throw new IllegalStateException("escolha classe");if(qty<=0)throw new IllegalArgumentException("quantidade");
  if(hero.questIndex>=quests.size())return new Progress(false,false,0,0,0,null);
  Quest q=quests.get(hero.questIndex);Objective obj=q.objectives().stream().filter(o->o.action().equals(action)&&o.target().equals(target)).findFirst().orElse(null);
  if(obj==null)return new Progress(false,false,0,0,0,null);
  int current=(int)Math.min(obj.required(),(long)hero.objectiveProgress.getOrDefault(obj.key(),0)+qty);hero.objectiveProgress.put(obj.key(),current);
  boolean finished=q.objectives().stream().allMatch(o->hero.objectiveProgress.getOrDefault(o.key(),0)>=o.required());
  if(!finished)return new Progress(true,false,current,obj.required(),0,null);
  int levels=hero.addXp(q.xp(),maxLevel);String reward=q.rewardQuantity()>0?q.rewardId()+" x"+q.rewardQuantity():null;
  if(reward!=null)hero.pendingRewards.add(reward);hero.questIndex++;hero.objectiveProgress.clear();return new Progress(true,true,current,obj.required(),levels,reward);
 }
 public static Progress recordAction(Hero h,List<Quest> q,String a,String t,int count){return recordAction(h,q,a,t,count,DEFAULT_MAX_LEVEL);}
}
