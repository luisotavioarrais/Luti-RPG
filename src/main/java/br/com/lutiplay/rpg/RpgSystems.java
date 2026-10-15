package br.com.lutiplay.rpg;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.dependency.*;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.*;
import com.hypixel.hytale.server.core.inventory.*;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.*;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.*;
import com.hypixel.hytale.server.core.modules.interaction.event.InteractionChainStartEvent;
import com.hypixel.hytale.server.core.modules.projectile.component.ImpactModifiers;
import com.hypixel.hytale.server.core.modules.projectile.system.AbilityCastSnapshotSystem;
import com.hypixel.hytale.server.core.asset.type.item.config.CoreItemAbility;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.universe.world.chunk.section.EnvironmentSection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.builtin.instances.config.InstanceWorldConfig;

public final class RpgSystems {
    private RpgSystems(){}
    static PlayerRef player(ArchetypeChunk<EntityStore> chunk,int i){return chunk.getComponent(i,PlayerRef.getComponentType());}
    static RpgCore.WeaponType equipped(RpgService service,Ref<EntityStore> ref,ComponentAccessor<EntityStore> store){
        var utility=store.getComponent(ref,InventoryComponent.Utility.getComponentType());
        ItemStack offhand=utility==null||utility.getActiveSlot()<0?null:utility.getInventory().getItemStack(utility.getActiveSlot());
        return service.catalog.weapon(InventoryComponent.getItemInHand(store,ref),offhand);
    }
    public static final class Craft extends EntityEventSystem<EntityStore,CraftRecipeEvent.Post> {
        private final RpgService service;
        public Craft(RpgService s){super(CraftRecipeEvent.Post.class);service=s;}
        public Query<EntityStore> getQuery(){return PlayerRef.getComponentType();}
        public void handle(int i,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer,CraftRecipeEvent.Post event){
            if(event.isCancelled())return;var p=player(chunk,i);
            for(var output:event.getCraftedRecipe().getOutputs())if(output.getItemId()!=null)service.crafted(p,output.getItemId(),Math.multiplyExact(output.getQuantity(),event.getQuantity()));
        }
    }
    public static final class InventoryChanged extends EntityEventSystem<EntityStore,InventoryChangeEvent> {
        private final RpgService service;
        public InventoryChanged(RpgService s){super(InventoryChangeEvent.class);service=s;}
        public Query<EntityStore> getQuery(){return PlayerRef.getComponentType();}
        public void handle(int i,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer,InventoryChangeEvent event){if(event.getTransaction().succeeded())service.inspectInventory(player(chunk,i),chunk.getReferenceTo(i),buffer);}
    }
    /** Freeze the equipment and invested damage at attack/cast creation, including projectiles. */
    public static final class AttackSnapshot extends EntityEventSystem<EntityStore,InteractionChainStartEvent> {
        private final RpgService service;
        public AttackSnapshot(RpgService s){super(InteractionChainStartEvent.class);service=s;}
        public Query<EntityStore> getQuery(){return PlayerRef.getComponentType();}
        public Set<Dependency<EntityStore>> getDependencies(){return Set.of(new SystemDependency<>(Order.AFTER,AbilityCastSnapshotSystem.class));}
        public void handle(int i,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer,InteractionChainStartEvent event){
            if(event.isCancelled())return;
            var h=service.eventHero(player(chunk,i));if(h==null||h.heroClass==null)return;
            var context=event.getContext();var hand=context.getHeldItem();
            boolean rune=context.getHeldItemSectionId()==InventoryComponent.ABILITIES_SECTION_ID&&!ItemStack.isEmpty(hand)&&hand.getItem().getAbility() instanceof CoreItemAbility;
            var utility=buffer.getComponent(chunk.getReferenceTo(i),InventoryComponent.Utility.getComponentType());
            var offhand=utility==null||utility.getActiveSlot()<0?null:utility.getInventory().getItemStack(utility.getActiveSlot());
            var type=rune?RpgCore.WeaponType.MAGIA:service.catalog.weapon(hand,offhand);
            var bonus=h.bonusesFor(type);
            var previous=context.getMetaStore().getIfPresentMetaObject(ImpactModifiers.CAST_SNAPSHOT);
            Map<String,Float> attributes=new HashMap<>();
            if(previous!=null&&previous.getAttributes()!=null)attributes.putAll(previous.getAttributes());
            attributes.put("LutiRPG.Damage",(float)(bonus.attackDamageBonus()*service.settings.damageScale()));
            attributes.put("LutiRPG.Crit",(float)bonus.critChanceBonus());
            // Preserve every native rune support attribute and effect.
            context.getMetaStore().putMetaObject(ImpactModifiers.CAST_SNAPSHOT,new ImpactModifiers(attributes,previous==null?null:previous.getOnHitEffectIds(),previous==null?null:previous.getElementalCauseId()));
        }
    }
    public static final class Offense extends DamageEventSystem {
        public Query<EntityStore> getQuery(){return Query.any();}
        public SystemGroup<EntityStore> getGroup(){return DamageModule.get().getGatherDamageGroup();}
        public void handle(int i,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer,Damage event){
            if(event.isCancelled()||event.getAmount()<=0)return;
            var snapshot=event.getIfPresentMetaObject(ImpactModifiers.DAMAGE_MODIFIERS);
            if(snapshot==null)return;
            double damage=snapshot.getAttribute("LutiRPG.Damage",0),crit=snapshot.getAttribute("LutiRPG.Crit",0);
            if(damage==0&&crit==0)return;
            event.setAmount((float)(event.getAmount()*(1+damage)*(ThreadLocalRandom.current().nextDouble()<crit?1.5:1)));
        }
    }
    public static final class Defense extends DamageEventSystem {
        private final RpgService service;
        public Defense(RpgService s){service=s;}
        public Query<EntityStore> getQuery(){return PlayerRef.getComponentType();}
        public SystemGroup<EntityStore> getGroup(){return DamageModule.get().getFilterDamageGroup();}
        public Set<Dependency<EntityStore>> getDependencies(){return Set.of(new SystemDependency<>(Order.AFTER,DamageSystems.WieldingDamageReduction.class));}
        public void handle(int i,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer,Damage event){
            if(event.isCancelled()||event.getAmount()<=0)return;
            var h=service.eventHero(player(chunk,i));if(h==null||h.heroClass==null)return;
            double reduction=h.resistanceReduction()*service.settings.defenseScale();
            var bonus=h.bonusesFor(equipped(service,chunk.getReferenceTo(i),buffer));
            if(Boolean.TRUE.equals(event.getIfPresentMetaObject(Damage.BLOCKED)))reduction=1-(1-reduction)*(1-bonus.blockBonus());
            if(reduction>0)event.setAmount((float)(event.getAmount()*(1-reduction)));
        }
    }
    public static final class Kills extends DeathSystems.OnDeathSystem {
        private final RpgService service;
        public Kills(RpgService s){service=s;}
        public Query<EntityStore> getQuery(){return NPCEntity.getComponentType();}
        public void onComponentAdded(Ref<EntityStore> ref,DeathComponent death,Store<EntityStore> store,CommandBuffer<EntityStore> buffer){
            var uuid=buffer.getComponent(ref,com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
            var credit=uuid==null?null:service.combatCredit.take(uuid.getUuid(),System.currentTimeMillis());
            PlayerRef p=null;
            if(death.getDeathInfo()!=null&&death.getDeathInfo().getSource() instanceof Damage.EntitySource source&&source.getRef()!=null&&source.getRef().isValid())p=resolvePlayer(source.getRef(),buffer);
            if(p==null&&credit!=null)p=com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(credit.player());
            var npc=buffer.getComponent(ref,NPCEntity.getComponentType());if(npc==null)return;
            String role=npc.getRoleName();
            RpgService.diagnostic("DEATH role="+role+" player="+(p==null?"none":p.getUuid())+" cachedCredit="+(credit!=null)+" hostile="+service.catalog.matches("kill.hostile_basic",role));
            if(p==null)return;
            var stats=buffer.getComponent(ref,EntityStatMap.getComponentType());var health=stats==null?null:stats.get(DefaultEntityStatTypes.getHealth());
            service.mobXp(p,role,credit!=null?credit.maxHealth():health==null?100:health.getMax());
            if(Set.of("Dungeon_Feastmaster_Boss","Dungeon_Feastmaster_Feeding").contains(role))service.action(p,"boss","feastmaster",1);
            // Intermediate boss bodies and transitions are NOT a victory.
            if(Set.of("Goblin_Gobliterator_Phase3","Goblin_Gobliterator_Phase3_HM").contains(role))service.action(p,"boss","gobliterator",1);
            String[] targets={"hostile_basic","cave_hostile","goblin_lab_guard","goblin","deepworks_enemy","deepworks_elite","final_elite"};
            for(String target:targets)if(service.catalog.matches("kill."+target,role))service.action(p,"kill",target,1);
        }
    }
    static PlayerRef resolvePlayer(Ref<EntityStore> ref,ComponentAccessor<EntityStore> store){if(ref==null||!ref.isValid())return null;var p=store.getComponent(ref,PlayerRef.getComponentType());if(p!=null)return p;var uuid=store.getComponent(ref,com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());return uuid==null?null:com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(uuid.getUuid());}
    /** Capture a successful hit while its source entity is valid, rather than retaining its Ref. */
    public static final class HitCredit extends DamageEventSystem {
        private final RpgService service;
        public HitCredit(RpgService service){this.service=service;}
        public Query<EntityStore> getQuery(){return NPCEntity.getComponentType();}
        public SystemGroup<EntityStore> getGroup(){return DamageModule.get().getInspectDamageGroup();}
        public void handle(int i,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer,Damage event){
            if(event.isCancelled()||event.getAmount()<=0)return;
            var victim=chunk.getComponent(i,com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());if(victim==null)return;
            var p=event.getSource() instanceof Damage.EntitySource source?resolvePlayer(source.getRef(),buffer):null;
            if(p==null){if(!(event.getSource() instanceof Damage.EntityEffectSource))service.combatCredit.clear(victim.getUuid());return;}
            var stats=chunk.getComponent(i,EntityStatMap.getComponentType());var health=stats==null?null:stats.get(DefaultEntityStatTypes.getHealth());
            service.combatCredit.remember(victim.getUuid(),p.getUuid(),health==null?100:health.getMax(),System.currentTimeMillis());
        }
    }
    public static final class Respawn extends RespawnSystems.OnRespawnSystem {
        private final RpgService service;
        public Respawn(RpgService service){this.service=service;}
        public Query<EntityStore> getQuery(){return PlayerRef.getComponentType();}
        public void onComponentRemoved(Ref<EntityStore> ref,DeathComponent death,Store<EntityStore> store,CommandBuffer<EntityStore> buffer){var p=resolvePlayer(ref,buffer);if(p!=null)service.respawn(p.getUuid());}
    }
    public static boolean isJournal(ItemStack item){return !ItemStack.isEmpty(item)&&item.getItemId().equals("LutiRPG_Diario");}
    public static final class ProtectJournalRequest extends EntityEventSystem<EntityStore,DropItemEvent.PlayerRequest> {
        public ProtectJournalRequest(){super(DropItemEvent.PlayerRequest.class);}
        public Query<EntityStore> getQuery(){return PlayerRef.getComponentType();}
        public void handle(int i,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer,DropItemEvent.PlayerRequest event){var type=InventoryComponent.getComponentTypeById(event.getInventorySectionId());if(type==null)return;var inventory=buffer.getComponent(chunk.getReferenceTo(i),type);if(inventory!=null&&event.getSlotId()>=0&&event.getSlotId()<inventory.getInventory().getCapacity()&&isJournal(inventory.getInventory().getItemStack(event.getSlotId())))event.setCancelled(true);}
    }
    public static final class ProtectJournalDrop extends EntityEventSystem<EntityStore,DropItemEvent.Drop> {
        public ProtectJournalDrop(){super(DropItemEvent.Drop.class);}
        public Query<EntityStore> getQuery(){return PlayerRef.getComponentType();}
        public void handle(int i,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer,DropItemEvent.Drop event){if(isJournal(event.getItemStack()))event.setCancelled(true);}
    }
    /** Sampling real environment sections also handles reentry, respawn, and already-discovered zones. */
    public static final class Players extends EntityTickingSystem<EntityStore> {
        private final RpgService service;
        private final Map<UUID,Float> elapsed=new ConcurrentHashMap<>();
        private final Map<UUID,String> hudText=new ConcurrentHashMap<>();
        public Players(RpgService s){service=s;}
        public Query<EntityStore> getQuery(){return Query.and(PlayerRef.getComponentType(),Player.getComponentType(),TransformComponent.getComponentType());}
        public void tick(float dt,int i,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer){
            var p=player(chunk,i);float time=elapsed.getOrDefault(p.getUuid(),0f)+dt;if(time<1){elapsed.put(p.getUuid(),time);return;}elapsed.put(p.getUuid(),0f);
            var h=service.eventHero(p);if(h==null)return;var ref=chunk.getReferenceTo(i);var player=chunk.getComponent(i,Player.getComponentType());
            // Gameplay accounting does not depend on the client's UI readiness after respawn.
            if(!chunk.getArchetype().contains(DeathComponent.getComponentType()))service.inspectInventory(p,ref,buffer);
            if(player.isWaitingForClientReady())return;
            if(service.consumeRespawn(p.getUuid())){hudText.remove(p.getUuid());elapsed.remove(p.getUuid());player.getHudManager().removeCustomHud(p,"LutiRPG");}
            var stats=buffer.getComponent(ref,EntityStatMap.getComponentType());
            if(stats!=null){
                modifier(stats,DefaultEntityStatTypes.getHealth(),"LutiRPG.Vitalidade",(float)h.vitalityBonus(),false);
                var slots=buffer.getComponent(ref,InventoryComponent.AbilitySlots.getComponentType());
                boolean hasRune=false;if(slots!=null)for(short s=0;s<slots.getInventory().getCapacity();s++){var item=slots.getInventory().getItemStack(s);if(!ItemStack.isEmpty(item)&&item.getItem().getAbility() instanceof CoreItemAbility){hasRune=true;break;}}
                float mana=hasRune?(float)h.bonusesFor(RpgCore.WeaponType.MAGIA).specialResourceBonus():0;
                modifier(stats,DefaultEntityStatTypes.getMana(),"LutiRPG.Mana",mana,true);
            }
            if(h.heroClass==null){
                if(!h.facts.contains("welcome")){RpgService.tell(p,"Bem-vindo! Escolha sua classe em /rpg. Todas as armas e magias continuam liberadas.");h.facts.add("welcome");service.save(h);player.getPageManager().openCustomPage(ref,store,new RpgPage(p,service));}
                player.getHudManager().removeCustomHud(p,"LutiRPG");return;
            }
            service.journalItem(p);
            service.advanceFacts(p);
            var transform=chunk.getComponent(i,TransformComponent.getComponentType());var world=store.getExternalData().getWorld();var section=transform.getSectionRef();
            movement(p,h,transform,world.getName());
            if(section!=null&&section.isValid()){
                var environment=world.getChunkStore().getStore().getComponent(section,EnvironmentSection.getComponentType());
                if(environment!=null){var pos=transform.getPosition();int index=environment.get((int)Math.floor(pos.x)&31,(int)Math.floor(pos.y)&31,(int)Math.floor(pos.z)&31);var asset=Environment.getAssetMap().getAsset(index);if(asset!=null)area(p,h,asset.getId());}
            }
            var instance=InstanceWorldConfig.get(world.getWorldConfig());
            if(instance!=null){String key=instance.getInstanceKey();if(key!=null){
                if(key.equals("Forgotten_Temple"))service.action(p,"explore","forgotten_temple",1);
                if(key.equals("Forgotten_Temple_Goblins"))service.action(p,"explore","goblin_lab",1);
                if(key.equals("The_Deepworks")){service.action(p,"explore","deepworks_entrance",1);service.action(p,"explore","deepworks",1);}
            }}
            String text=service.journal(h)+"\n"+QuestNavigation.hint(service,h,store,transform.getPosition(),buffer.getComponent(ref,com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType())),xp=RpgHud.xpLabel(h,service.settings.maxLevel());
            String state=text+xp+h.unspentPoints;
            if(service.settings.hud()){var current=player.getHudManager().getCustomHud("LutiRPG");if(!(current instanceof RpgHud))player.getHudManager().addCustomHud(p,new RpgHud(p,text,h,service.settings.maxLevel()));else if(!state.equals(hudText.get(p.getUuid())))((RpgHud)current).refresh(text,h,service.settings.maxLevel());hudText.put(p.getUuid(),state);}
        }
        private void movement(PlayerRef p,RpgCore.Hero h,TransformComponent transform,String world){
            if(h.questIndex>=service.quests.size())return;
            var q=service.quests.get(h.questIndex);var position=transform.getPosition();
            for(var o:q.objectives())if(o.action().equals("explore")&&Set.of("starting_area","distant_region").contains(o.target())){
                String prefix="origin."+q.id()+".";String origin=h.facts.stream().filter(f->f.startsWith(prefix)).findFirst().orElse(null);
                if(origin==null){h.facts.add(prefix+world+","+position.x+","+position.z);service.save(h);return;}
                String[] data=origin.substring(prefix.length()).split(",");
                if(!data[0].equals(world))return;
                double x=position.x-Double.parseDouble(data[1]),z=position.z-Double.parseDouble(data[2]);double distance=o.target().equals("starting_area")?64:1024;
                if(x*x+z*z>=distance*distance)service.action(p,"explore",o.target(),1);
            }
        }
        private void area(PlayerRef p,RpgCore.Hero h,String id){
            if(h.questIndex>=service.quests.size())return;
            var q=service.quests.get(h.questIndex);
            for(var o:q.objectives())if(o.action().equals("explore")&&service.catalog.matches("area."+o.target(),id))service.action(p,"explore",o.target(),1);
            if(service.catalog.matches("area.surface_biome",id)){
                String first=h.facts.stream().filter(f->f.startsWith("firstBiome.")).findFirst().orElse(null);
                if(first==null){h.facts.add("firstBiome."+id);service.save(h);}
                else if(!first.equals("firstBiome."+id))service.action(p,"explore","new_biome",1);
            }
        }
        private static void modifier(EntityStatMap stats,int index,String key,float amount,boolean multiply){
            if(index<0||stats.get(index)==null)return;
            if(amount==0){if(stats.getModifier(index,key)!=null)stats.removeModifier(index,key);return;}
            var next=new StaticModifier(Modifier.ModifierTarget.MAX,multiply?StaticModifier.CalculationType.MULTIPLICATIVE:StaticModifier.CalculationType.ADDITIVE,multiply?1+amount:amount);
            if(!next.equals(stats.getModifier(index,key)))stats.putModifier(index,key,next);
        }
    }
}
