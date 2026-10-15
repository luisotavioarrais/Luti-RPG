package br.com.lutiplay.rpg;
import java.util.*;
import org.joml.Vector3d;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
/** Only directs to real loaded entities or official POIs. Never invents a quest coordinate. */
public final class QuestNavigation {
 private QuestNavigation(){}
 public static String hint(RpgService service,RpgCore.Hero hero,Store<EntityStore> store,Vector3d position,HeadRotation head){
  if(hero.questIndex>=service.quests.size())return "Jornada concluída!";
  var objective=service.quests.get(hero.questIndex).objectives().stream().filter(o->hero.objectiveProgress.getOrDefault(o.key(),0)<o.required()).findFirst().orElse(null);
  if(objective==null)return "Concluindo missão...";
  Vector3d[] closest={null};double[] distance={128*128};
  if(objective.action().equals("collect"))store.forEachChunk(Query.and(ItemComponent.getComponentType(),TransformComponent.getComponentType()),(java.util.function.BiConsumer<ArchetypeChunk<EntityStore>,CommandBuffer<EntityStore>>)(chunk,buffer)->{
   for(int i=0;i<chunk.size();i++){var item=chunk.getComponent(i,ItemComponent.getComponentType()).getItemStack();if(!ItemStack.isEmpty(item)&&service.catalog.matches(objective.target(),item.getItemId()))consider(position,chunk.getComponent(i,TransformComponent.getComponentType()).getPosition(),closest,distance);}
  });
  if(objective.action().equals("kill")||objective.action().equals("boss"))store.forEachChunk(Query.and(NPCEntity.getComponentType(),TransformComponent.getComponentType()),(java.util.function.BiConsumer<ArchetypeChunk<EntityStore>,CommandBuffer<EntityStore>>)(chunk,buffer)->{
   for(int i=0;i<chunk.size();i++){if(chunk.getArchetype().contains(DeathComponent.getComponentType()))continue;String role=chunk.getComponent(i,NPCEntity.getComponentType()).getRoleName();boolean match=service.catalog.matches("kill."+objective.target(),role)||(objective.target().equals("feastmaster")&&Set.of("Dungeon_Feastmaster_Boss","Dungeon_Feastmaster_Feeding").contains(role))||(objective.target().equals("gobliterator")&&role!=null&&role.startsWith("Goblin_Gobliterator_"));if(match)consider(position,chunk.getComponent(i,TransformComponent.getComponentType()).getPosition(),closest,distance);}
  });
  if(objective.action().equals("explore")&&objective.target().equals("point_of_interest")){
   distance[0]=Double.MAX_VALUE;
   for(var marker:store.getExternalData().getWorld().getWorldMapManager().getPointsOfInterest().values())if(marker.transform!=null&&marker.transform.position!=null){var p=marker.transform.position;consider(position,new Vector3d(p.x,p.y,p.z),closest,distance);}
  }
  if(closest[0]!=null&&head!=null){var delta=new Vector3d(closest[0]).sub(position);var direction=head.getDirection();return arrow(direction.x,direction.z,delta.x,delta.z)+" "+Names.of(objective.target())+" — "+Math.round(Math.sqrt(distance[0]))+" m";}
  return switch(objective.action()){case "craft"->"Fabrique: "+Names.of(objective.target());case "collect"->"Procure e recolha: "+Names.of(objective.target());case "kill","boss"->"Procure: "+Names.of(objective.target());default->"Explore: "+Names.of(objective.target());};
 }
 private static void consider(Vector3d origin,Vector3d candidate,Vector3d[] nearest,double[] distance){double d=origin.distanceSquared(candidate);if(d<distance[0]){distance[0]=d;nearest[0]=new Vector3d(candidate);}}
 static String arrow(double fx,double fz,double dx,double dz){double angle=Math.atan2(fx*dz-fz*dx,fx*dx+fz*dz);String[] arrows={"↑","↗","→","↘","↓","↙","←","↖"};return arrows[Math.floorMod((int)Math.round(angle/(Math.PI/4)),8)];}
}
