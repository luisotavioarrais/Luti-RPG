package br.com.lutiplay.rpg;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/** All inventory/UI operations are invoked only on the owning world thread by ECS/commands. */
public final class RpgService {
    public final ContentCatalog catalog;
    public final RpgSettings settings;
    public final List<RpgCore.Quest> quests;
    private final HeroRepository repository;
    private final Map<UUID,RpgCore.Hero> heroes=new ConcurrentHashMap<>();
    private final Set<UUID> failed=ConcurrentHashMap.newKeySet();
    private final Set<UUID> refreshAfterRespawn=ConcurrentHashMap.newKeySet();
    final CombatCredit combatCredit=new CombatCredit();
    static void diagnostic(String text){java.util.logging.Logger.getLogger("LutiRPG.Diagnostics").info(text);}
    void respawn(UUID id){refreshAfterRespawn.add(id);diagnostic("RESPAWN player="+id);}
    boolean consumeRespawn(UUID id){return refreshAfterRespawn.remove(id);}
    public RpgService(HeroRepository repository,ContentCatalog catalog,RpgSettings settings,List<RpgCore.Quest> quests){this.repository=repository;this.catalog=catalog;this.settings=settings;this.quests=quests;}
    public RpgCore.Hero hero(UUID id){
        if(failed.contains(id))throw new IllegalStateException("RPG suspenso para proteger seu save. Consulte o administrador.");
        return heroes.computeIfAbsent(id,key->{try{return repository.load(key);}catch(IOException|RuntimeException e){failed.add(key);throw new IllegalStateException("Nao foi possivel ler o save "+key,e);}});
    }
    public void save(RpgCore.Hero hero){try{repository.save(hero);}catch(IOException e){failed.add(hero.playerId);throw new IllegalStateException("Falha ao salvar; RPG suspenso para "+hero.playerId,e);}}
    RpgCore.Hero eventHero(PlayerRef player){if(failed.contains(player.getUuid()))return null;try{return hero(player.getUuid());}catch(IllegalStateException e){tell(player,e.getMessage());return null;}}
    public void choose(PlayerRef player,RpgCore.HeroClass type){var h=hero(player.getUuid());synchronized(h){h.chooseClass(type);queueKit(h);save(h);}tell(player,"Classe escolhida: "+Names.of(type)+". Kit disponível em Resgatar. A escolha é permanente.");if(type==RpgCore.HeroClass.MAGO)tell(player,"Sua runa Bola de Fogo usa Mana. Abra /rpg runas para a introdução e a bancada oficial de habilidades. Novas runas continuam na progressão normal do laboratório Goblin.");}
    private void queueKit(RpgCore.Hero h){
        if(!StarterKits.eligible(h))return;
        for(var item:StarterKits.KITS.get(h.heroClass)){
            // Native Daggers is one item rendering TWO blades, not two separate weapons.
            int quantity=item.logicalId().equals("starter_dagger")?1:item.quantity();
            h.pendingRewards.add(catalog.item(item.logicalId())+" x"+quantity);
        }
        h.starterKitGranted=true; // Means durably QUEUED; delivery remains in the persisted outbox.
    }
    public void spend(PlayerRef player,RpgCore.Attribute attribute,int count){var h=hero(player.getUuid());synchronized(h){h.spend(attribute,count);save(h);}}
    public void journalItem(PlayerRef player){var h=hero(player.getUuid());synchronized(h){if(h.heroClass!=null&&h.facts.add("journal.queued")){h.pendingRewards.add("LutiRPG_Diario x1");save(h);tell(player,"Diário disponível em /rpg resgatar. Coloque na hotbar e clique para abrir seus status.");}}}
    public void action(PlayerRef player,String action,String target,int quantity){
        var h=eventHero(player);if(h==null)return;synchronized(h){
            if(h.heroClass==null)return;
            if(action.equals("boss")&&h.facts.add("boss."+target))save(h);
            var progress=RpgCore.recordAction(h,quests,action,target,quantity,settings.maxLevel());
            if(progress.matched()){
                save(h);
                diagnostic("PROGRESS player="+player.getUuid()+" action="+action+" target="+target+" quantity="+quantity+" quest="+h.questIndex+" finished="+progress.finished());
                if(progress.finished())tell(player,"Missao concluida! +XP; recompensa em /rpg resgatar. "+journal(h));
            }
            finishCampaign(player,h);
        }
    }
    private void finishCampaign(PlayerRef player,RpgCore.Hero h){
        // Death evidence persists even if the following objectives are evaluated on the next tick.
        while(h.questIndex>=58&&h.questIndex<60&&h.facts.contains("boss.gobliterator")){
            String target=h.questIndex==58?"chapter1_complete":"victory_claim";
            RpgCore.recordAction(h,quests,"milestone",target,1,settings.maxLevel());save(h);
            tell(player,h.questIndex==60?"Capitulo 1 concluido! Todas as 60 missoes finalizadas.":"Vitoria contra Gobliterator registrada automaticamente.");
        }
    }
    public void crafted(PlayerRef player,String id,int quantity){var h=eventHero(player);if(h==null)return;synchronized(h){if(h.heroClass==null||h.questIndex>=quests.size())return;var q=quests.get(h.questIndex);for(var o:q.objectives())if(o.action().equals("craft")&&catalog.matches(o.target(),id)){action(player,"craft",o.target(),quantity);break;}}}
    /** Collection means possessing the required quantity. Moving/dropping the same stack cannot farm progress. */
    public void inspectInventory(PlayerRef player,Ref<EntityStore> ref,ComponentAccessor<EntityStore> store){
        var h=eventHero(player);if(h==null)return;synchronized(h){
            if(h.heroClass==null||h.questIndex>=quests.size())return;
            var q=quests.get(h.questIndex);
            var containers=new ArrayList<com.hypixel.hytale.server.core.inventory.container.ItemContainer>();
            for(int section:new int[]{InventoryComponent.HOTBAR_SECTION_ID,InventoryComponent.STORAGE_SECTION_ID,InventoryComponent.BACKPACK_SECTION_ID,InventoryComponent.RUNE_BAG_SECTION_ID,InventoryComponent.ABILITIES_SECTION_ID}){
                var type=InventoryComponent.getComponentTypeById(section);if(type==null)continue;
                var component=store.getComponent(ref,type);if(component!=null)containers.add(component.getInventory());
            }
            // Processing benches (furnace/campfire) have no player-attributed CraftRecipeEvent.
            // For these three goals, the evidence is possession of the processed output, not a fictitious craft event.
            for(var o:q.objectives())if(o.action().equals("collect")||(o.action().equals("craft")&&Set.of("copper_ingot","iron_ingot","cooked_food").contains(o.target()))){
                long count=0;
                for(var c:containers)for(short slot=0;slot<c.getCapacity();slot++){var item=c.getItemStack(slot);if(!ItemStack.isEmpty(item)&&catalog.matches(o.target(),item.getItemId()))count+=item.getQuantity();}
                int old=h.objectiveProgress.getOrDefault(o.key(),0);
                int current=(int)Math.min(o.required(),count);
                if(current>old)action(player,o.action(),o.target(),current-old);
            }
        }
    }
    public void claim(PlayerRef player,Ref<EntityStore> ref,Store<EntityStore> store){
        var h=hero(player.getUuid());synchronized(h){
            if(h.heroClass==null)throw new IllegalStateException("Escolha uma classe primeiro.");
            queueKit(h);save(h);
            var inventory=InventoryComponent.getCombined(store,ref,InventoryComponent.HOTBAR_STORAGE_BACKPACK);
            int delivered=0;
            while(!h.pendingRewards.isEmpty()){
                String reward=h.pendingRewards.getFirst();int split=reward.lastIndexOf(" x");
                String id=catalog.resolveReward(reward.substring(0,split)); // migrate schema 2 logical rewards
                var item=new ItemStack(id,Integer.parseInt(reward.substring(split+2)));
                if(!inventory.canAddItemStack(item,true,false))break;
                // Write-ahead reservation prevents double delivery across reentry/crashes.
                h.pendingRewards.removeFirst();h.uncertainDeliveries.add(reward);save(h);
                var transaction=inventory.addItemStack(item,true,false,true);
                if(!transaction.succeeded()||!ItemStack.isEmpty(transaction.getRemainder())){
                    // Unexpected/partial outcomes stay quarantined; NEVER replay automatically.
                    tell(player,"Entrega interrompida. Consulte o administrador com seu UUID.");return;
                }
                h.uncertainDeliveries.removeLast();save(h);delivered++;
            }
            tell(player,delivered+" entrega(s) concluida(s); "+h.pendingRewards.size()+" pendente(s)."+(h.uncertainDeliveries.isEmpty()?"":" Entrega interrompida registrada para conferencia do administrador."));
        }
    }
    public String status(RpgCore.Hero h){return "Classe: "+(h.heroClass==null?"escolha abaixo":Names.of(h.heroClass))+" | Nível "+h.level+" | XP "+h.xp+" | Pontos livres: "+h.unspentPoints;}
    public void advanceFacts(PlayerRef player){var h=eventHero(player);if(h==null)return;synchronized(h){if(h.heroClass==null||h.questIndex>=quests.size())return;for(var o:quests.get(h.questIndex).objectives())if(o.action().equals("boss")&&h.facts.contains("boss."+o.target())){action(player,"boss",o.target(),1);break;}finishCampaign(player,h);}}
    public String journal(RpgCore.Hero h){if(h.questIndex>=quests.size())return "60/60 — Capítulo 1 concluído";var q=quests.get(h.questIndex);StringBuilder b=new StringBuilder("Missão "+(h.questIndex+1)+"/60 — "+q.title());for(var o:q.objectives())b.append('\n').append(h.objectiveProgress.getOrDefault(o.key(),0)).append('/').append(o.required()).append(' ').append(Names.of(o.target()));return b+"\nRecompensa: +"+q.xp()+" XP, "+Names.of("reward."+q.rewardId())+" x"+q.rewardQuantity();}
    public void mobXp(PlayerRef player,String role,double maxHealth){var h=eventHero(player);if(h==null||h.heroClass==null)return;int gain=mobXpValue(role,catalog.matches("kill.hostile_basic",role),maxHealth);if(gain==0)return;synchronized(h){long before=h.xp;int levels=h.addXp(gain,settings.maxLevel());if(h.xp!=before){save(h);tell(player,"+"+(h.xp-before)+" XP por abate. "+RpgHud.xpLabel(h,settings.maxLevel()));}if(levels>0)tell(player,"Nível "+h.level+"! +"+(levels*5)+" pontos disponíveis em /rpg.");}}
    static int mobXpValue(String role,boolean hostile){return mobXpValue(role,hostile,100);}
    static int mobXpValue(String role,boolean hostile,double health){if(role==null||role.startsWith("Goblin_Gobliterator_")&&!Set.of("Goblin_Gobliterator_Phase3","Goblin_Gobliterator_Phase3_HM").contains(role))return 0;if(Set.of("Goblin_Gobliterator_Phase3","Goblin_Gobliterator_Phase3_HM","Dungeon_Feastmaster_Boss","Dungeon_Feastmaster_Feeding").contains(role))return 500;if(!Double.isFinite(health)||health<=0)health=100;return (int)Math.max(hostile?10:5,Math.min(250,Math.round(health*(hostile?.25:.10))));}
    public void openRunes(PlayerRef p,Ref<EntityStore> ref,Store<EntityStore> store){
        var intro=store.ensureAndGetComponent(ref,com.hypixel.hytale.builtin.abilities.component.AbilityBenchIntro.getComponentType());
        if(!intro.hasSeenIntro()){
            if(hero(p.getUuid()).heroClass!=RpgCore.HeroClass.MAGO)throw new IllegalStateException("Encontre o Templo Esquecido para descobrir as runas. Todas as classes podem usá-las após essa descoberta.");
            // Official native onboarding flag + native introduction, matching the starting mage access requirement.
            intro.markSeen();
            store.getComponent(ref,Player.getComponentType()).getPageManager().openCustomPage(ref,store,new com.hypixel.hytale.builtin.abilities.page.AbilityBenchIntroPage(p));
        }else com.hypixel.hytale.builtin.abilities.interaction.OpenAbilityBenchInteraction.openBench(ref,store);
    }
    public void reset(PlayerRef admin,UUID id,String confirmation){
        if(!confirmation.equals("APAGAR-CLASSE-PONTOS-MISSOES"))throw new IllegalArgumentException("ATENCAO: apaga classe, nivel, pontos e missoes. Confirme com APAGAR-CLASSE-PONTOS-MISSOES. Os itens existentes nao sao removidos; o kit NAO sera concedido novamente.");
        var old=hero(id);synchronized(old){old.heroClass=null;old.xp=0;old.level=1;old.unspentPoints=0;old.questIndex=0;old.invested.replaceAll((attribute,value)->0);old.objectiveProgress.clear();old.facts.clear();old.starterKitGranted=true;save(old);}tell(admin,"RPG resetado para "+id+". Kit anterior preservado, sem segunda concessao.");
    }
    public static void tell(PlayerRef player,String text){player.sendMessage(Message.raw("[Luti RPG] "+text));}
}
