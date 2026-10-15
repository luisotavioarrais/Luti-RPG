package br.com.lutiplay.rpg;
import java.nio.file.*;
import java.io.*;
import java.util.*;
/** Salvamento por jogador em Properties, gravacao atomica por arquivo. */
public final class HeroRepository {
  private final Path dir;
  public HeroRepository(Path dir){this.dir=dir;}
  private Path path(UUID id){return dir.resolve(id+".properties");}
  public synchronized void save(RpgCore.Hero h) throws IOException {
    Files.createDirectories(dir);
    Properties p=new Properties();p.setProperty("schema","3");p.setProperty("starterKitGranted",String.valueOf(h.starterKitGranted));p.setProperty("xp",String.valueOf(h.xp));
    p.setProperty("level",String.valueOf(h.level));p.setProperty("points",String.valueOf(h.unspentPoints));
    p.setProperty("quest",String.valueOf(h.questIndex));
    if(h.heroClass!=null)p.setProperty("class",h.heroClass.name());
    for(var e:h.invested.entrySet())p.setProperty("stat."+e.getKey(),String.valueOf(e.getValue()));
    for(var e:h.objectiveProgress.entrySet())p.setProperty("objective."+e.getKey(),String.valueOf(e.getValue()));
    for(int i=0;i<h.pendingRewards.size();i++)p.setProperty("reward."+i,h.pendingRewards.get(i));
    for(String fact:h.facts)p.setProperty("fact."+fact,"true");
    for(int i=0;i<h.uncertainDeliveries.size();i++)p.setProperty("uncertain."+i,h.uncertainDeliveries.get(i));
    Path tmp=Files.createTempFile(dir,"hero-",".tmp");
    try(OutputStream output=Files.newOutputStream(tmp)){p.store(output,"Luti RPG save v1");}
    try{Files.move(tmp,path(h.playerId),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
    catch(AtomicMoveNotSupportedException ignored){Files.move(tmp,path(h.playerId),StandardCopyOption.REPLACE_EXISTING);}
  }
  public synchronized RpgCore.Hero load(UUID id) throws IOException {
    RpgCore.Hero h=new RpgCore.Hero(id);if(!Files.exists(path(id)))return h;
    Properties p=new Properties();try(InputStream in=Files.newInputStream(path(id))){p.load(in);}
    h.starterKitGranted=Boolean.parseBoolean(p.getProperty("starterKitGranted","false"));
    h.xp=Long.parseLong(p.getProperty("xp","0"));h.level=Integer.parseInt(p.getProperty("level","1"));
    h.unspentPoints=Integer.parseInt(p.getProperty("points","0"));h.questIndex=Integer.parseInt(p.getProperty("quest","0"));
    String klass=p.getProperty("class");if(klass!=null)h.heroClass=RpgCore.HeroClass.valueOf(klass);
    for(var a:RpgCore.Attribute.values())h.invested.put(a,Integer.parseInt(p.getProperty("stat."+a,"0")));
    for(String key:p.stringPropertyNames())if(key.startsWith("objective."))h.objectiveProgress.put(key.substring(10),Integer.parseInt(p.getProperty(key)));
    for(int i=0;p.getProperty("reward."+i)!=null;i++)h.pendingRewards.add(p.getProperty("reward."+i));
    for(String key:p.stringPropertyNames())if(key.startsWith("fact."))h.facts.add(key.substring(5));
    for(int i=0;p.getProperty("uncertain."+i)!=null;i++)h.uncertainDeliveries.add(p.getProperty("uncertain."+i));
    if(h.level<1||h.xp<0||h.unspentPoints<0||h.questIndex<0||h.questIndex>60)throw new IOException("Save invalido: "+id);
    int spent=0;for(var e:h.invested.entrySet()){
      if(e.getValue()<0||(e.getValue()>0&&(h.heroClass==null||!RpgCore.ALLOWED.get(h.heroClass).contains(e.getKey()))))throw new IOException("Atributos invalidos: "+id);
      spent=Math.addExact(spent,e.getValue());
    }
    if((long)spent+h.unspentPoints!=(long)(h.level-1)*RpgCore.POINTS_PER_LEVEL||h.xp<RpgCore.totalXpForLevel(h.level))throw new IOException("Pontos/XP invalidos: "+id);
    return h;
  }
}
