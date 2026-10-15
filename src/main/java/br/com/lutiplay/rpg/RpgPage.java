package br.com.lutiplay.rpg;
import java.util.*;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.protocol.packets.interface_.*;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class RpgPage extends CustomUIPage {
    private final RpgService service;
    public RpgPage(PlayerRef p,RpgService s){super(p,CustomPageLifetime.CanDismiss);service=s;}
    public void build(Ref<EntityStore> ref,UICommandBuilder b,UIEventBuilder events,Store<EntityStore> store){
        b.append("LutiRPG/Menu.ui");var h=service.hero(playerRef.getUuid());
        b.set("#Status.Text",service.status(h));b.set("#Journal.Text",service.journal(h));
        b.set("#Classes.Visible",h.heroClass==null);b.set("#Stats.Visible",h.heroClass!=null);
        for(var type:RpgCore.HeroClass.values())events.addEventBinding(CustomUIEventBindingType.Activating,"#Class"+type,new EventData().append("Action","classe:"+type));
        for(var a:RpgCore.Attribute.values()){
            String id=a.name().replace("_","");
            boolean allowed=h.heroClass!=null&&RpgCore.ALLOWED.get(h.heroClass).contains(a);
            b.set("#Row"+id+".Visible",allowed);
            b.set("#Value"+id+".Text",Names.of(a)+": "+h.value(a)+" (+"+h.invested.get(a)+")");
            events.addEventBinding(CustomUIEventBindingType.Activating,"#Add"+id,new EventData().append("Action","add:"+a));
        }
        events.addEventBinding(CustomUIEventBindingType.Activating,"#Claim",new EventData().append("Action","resgatar"));
        events.addEventBinding(CustomUIEventBindingType.Activating,"#Runes",new EventData().append("Action","runas"));
        events.addEventBinding(CustomUIEventBindingType.Activating,"#Close",new EventData().append("Action","fechar"));
    }
    public void handleDataEvent(Ref<EntityStore> ref,Store<EntityStore> store,String raw){
        try{
            String action=JsonParser.parseString(raw).getAsJsonObject().get("Action").getAsString();
            if(action.equals("fechar")){close();return;}
            if(action.equals("runas")){service.openRunes(playerRef,ref,store);return;}
            if(action.startsWith("classe:")){service.choose(playerRef,RpgCore.HeroClass.valueOf(action.substring(7)));service.claim(playerRef,ref,store);}
            else if(action.startsWith("add:"))service.spend(playerRef,RpgCore.Attribute.valueOf(action.substring(4)),1);
            else if(action.equals("resgatar"))service.claim(playerRef,ref,store);
            rebuild();
        }catch(IllegalArgumentException|IllegalStateException e){RpgService.tell(playerRef,e.getMessage());sendUpdate();}
    }
}
