package br.com.lutiplay.rpg;
import java.util.*;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.command.system.*;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.*;
import com.hypixel.hytale.server.core.universe.world.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
public final class RpgCommand extends AbstractPlayerCommand {
    private final RpgService service;
    public RpgCommand(RpgService s){super("rpg","Classes, atributos e diario Luti RPG");service=s;requireNoPermission();setAllowsExtraArguments(true);}
    protected void execute(CommandContext context,Store<EntityStore> store,Ref<EntityStore> ref,PlayerRef player,World world){
        try{
            String[] args=context.getInputString().trim().split("\\s+");
            String command=args.length<2?"menu":args[1].toLowerCase(Locale.ROOT);
            switch(command){
                case "menu" -> store.getComponent(ref,Player.getComponentType()).getPageManager().openCustomPage(ref,store,new RpgPage(player,service));
                case "classe" -> {if(args.length<3){RpgService.tell(player,"Classes: GUERREIRO, PALADINO, MAGO, ASSASSINO, ARQUEIRO. /rpg classe NOME");return;}service.choose(player,RpgCore.HeroClass.valueOf(normalize(args[2])));service.claim(player,ref,store);}
                case "status" -> {var h=service.hero(player.getUuid());RpgService.tell(player,service.status(h));if(h.heroClass!=null)for(var a:RpgCore.Attribute.values())if(RpgCore.ALLOWED.get(h.heroClass).contains(a))RpgService.tell(player,a+": "+h.value(a)+" (+"+h.invested.get(a)+")");}
                case "add" -> {if(args.length!=4)throw new IllegalArgumentException("Uso: /rpg add ATRIBUTO QUANTIDADE");service.spend(player,RpgCore.Attribute.valueOf(normalize(args[2])),Integer.parseInt(args[3]));RpgService.tell(player,"Pontos aplicados.");}
                case "missao","quests" -> RpgService.tell(player,service.journal(service.hero(player.getUuid())));
                case "resgatar" -> service.claim(player,ref,store);
                case "runas" -> service.openRunes(player,ref,store);
                case "reset" -> {if(!player.hasPermission("lutirpg.admin.reset"))throw new IllegalArgumentException("Sem permissao lutirpg.admin.reset.");if(args.length!=4)throw new IllegalArgumentException("ATENCAO: reset apaga classe, nivel, pontos e missoes! Uso: /rpg reset UUID APAGAR-CLASSE-PONTOS-MISSOES");service.reset(player,UUID.fromString(args[2]),args[3]);}
                default -> RpgService.tell(player,"/rpg | classe NOME | status | add ATRIBUTO QUANTIDADE | missao | resgatar");
            }
        }catch(IllegalArgumentException|IllegalStateException e){RpgService.tell(player,e.getMessage());}
    }
    static String normalize(String s){return java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toUpperCase(Locale.ROOT);}
}
