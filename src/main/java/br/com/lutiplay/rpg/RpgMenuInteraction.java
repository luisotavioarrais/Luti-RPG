package br.com.lutiplay.rpg;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
/** Clicking the journal in the hotbar opens the interactive status page. */
public final class RpgMenuInteraction extends SimpleInstantInteraction {
    private final RpgService service;
    public RpgMenuInteraction(RpgService service){this.service=service;}
    protected void firstRun(InteractionType type,InteractionContext context,CooldownHandler cooldown){
        var buffer=context.getCommandBuffer();var ref=context.getEntity();if(buffer==null||ref==null||!ref.isValid())return;
        buffer.run(store->{if(!ref.isValid())return;var player=store.getComponent(ref,Player.getComponentType());var p=store.getComponent(ref,PlayerRef.getComponentType());if(player!=null&&p!=null)player.getPageManager().openCustomPage(ref,store,new RpgPage(p,service));});
    }
    protected void simulateFirstRun(InteractionType type,InteractionContext context,CooldownHandler cooldown){}
}
