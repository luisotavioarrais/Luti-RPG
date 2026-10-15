package br.com.lutiplay.rpg;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
public final class RpgHud extends CustomUIHud {
    private String text;private RpgCore.Hero hero;private int maxLevel;
    public RpgHud(PlayerRef player,String text,RpgCore.Hero hero,int maxLevel){super(player,"LutiRPG",5);this.text=text;this.hero=hero;this.maxLevel=maxLevel;}
    protected void build(UICommandBuilder b){b.append("LutiRPG/Hud.ui");values(b);}
    private void values(UICommandBuilder b){b.set("#RpgHudText.Text",text);b.set("#XpText.Text",xpLabel(hero,maxLevel));var anchor=new com.hypixel.hytale.server.core.ui.Anchor();anchor.setLeft(com.hypixel.hytale.server.core.ui.Value.of(0));anchor.setTop(com.hypixel.hytale.server.core.ui.Value.of(0));anchor.setBottom(com.hypixel.hytale.server.core.ui.Value.of(0));anchor.setWidth(com.hypixel.hytale.server.core.ui.Value.of(fillWidth(hero,maxLevel)));b.setObject("#XpFill.Anchor",anchor);b.set("#StatusHint.Text","Status: Diário na hotbar ou /rpg | "+hero.unspentPoints+" pontos livres");}
    public void refresh(String text,RpgCore.Hero hero,int maxLevel){this.text=text;this.hero=hero;this.maxLevel=maxLevel;var b=new UICommandBuilder();values(b);update(false,b);}
    static int fillWidth(RpgCore.Hero h,int max){if(h.level>=max)return 480;long base=RpgCore.totalXpForLevel(h.level),next=RpgCore.totalXpForLevel(h.level+1);return (int)Math.max(0,Math.min(480,480.0*(h.xp-base)/(next-base)));}
    static String xpLabel(RpgCore.Hero h,int max){if(h.level>=max)return "Nível "+h.level+" — NÍVEL MÁXIMO";long base=RpgCore.totalXpForLevel(h.level);return "Nível "+h.level+" — XP "+(h.xp-base)+" / "+(RpgCore.totalXpForLevel(h.level+1)-base);}
}
