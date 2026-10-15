package br.com.lutiplay.rpg;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/** UUIDs survive player entity replacement on respawn; credits are consumed once per victim. */
final class CombatCredit {
 record Credit(UUID player,double maxHealth,long at){}
 private final Map<UUID,Credit> hits=new ConcurrentHashMap<>();
 void remember(UUID victim,UUID player,double health,long now){if(hits.size()>4096)hits.entrySet().removeIf(e->now-e.getValue().at()>30_000);hits.put(victim,new Credit(player,health,now));}
 void clear(UUID victim){hits.remove(victim);}
 Credit take(UUID victim,long now){var credit=hits.remove(victim);return credit!=null&&now-credit.at()<=30_000?credit:null;}
}
