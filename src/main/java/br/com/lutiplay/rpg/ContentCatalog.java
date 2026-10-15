package br.com.lutiplay.rpg;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;

/** Generated from the local official assets, not localized display-name guesses. */
public final class ContentCatalog {
    private final Properties items=new Properties();
    private final Map<String,Set<String>> targets=new HashMap<>();
    private final Map<String,Set<String>> families=new HashMap<>();
    public ContentCatalog() throws IOException {
        try(var r=reader("items.properties")){items.load(r);}
        Properties p=new Properties();try(var r=reader("targets.properties")){p.load(r);}
        for(String key:p.stringPropertyNames())targets.put(key,Set.of(p.getProperty(key).split(",")));
        try(var r=new BufferedReader(reader("families.tsv"))){String line;while((line=r.readLine())!=null){String[] v=line.split("\t",2);families.put(v[0],Set.of(v[1].split(",")));}}
    }
    private static Reader reader(String name)throws IOException {
        var in=ContentCatalog.class.getResourceAsStream("/"+name);
        if(in==null)throw new IOException("Recurso ausente: "+name);
        return new InputStreamReader(in,StandardCharsets.UTF_8);
    }
    public String item(String key){String id=items.getProperty(key);if(id==null)throw new IllegalArgumentException("Item sem mapeamento: "+key);return id;}
    public String resolveReward(String value){return items.getProperty(value,value);}
    public boolean matches(String target,String id){return targets.getOrDefault(target,Set.of()).contains(id);}
    public boolean family(ItemStack item,String family){return !ItemStack.isEmpty(item)&&!item.isBroken()&&families.getOrDefault(item.getItemId(),Set.of()).contains(family);}
    public RpgCore.WeaponType weapon(ItemStack hand,ItemStack utility) {
        if(ItemStack.isEmpty(hand)||hand.isBroken())return RpgCore.WeaponType.DESARMADO;
        if(family(hand,"Battleaxe")||family(hand,"Mace")||family(hand,"Longsword"))return RpgCore.WeaponType.DUAS_MAOS;
        if((family(hand,"Sword")&&family(utility,"Shield"))||(family(hand,"Shield")&&family(utility,"Sword")))return RpgCore.WeaponType.ESPADA_ESCUDO;
        if(family(hand,"Daggers")||family(hand,"Dagger"))return RpgCore.WeaponType.ADAGAS;
        if(family(hand,"Shortbow")||family(hand,"Bow")||family(hand,"Crossbow"))return RpgCore.WeaponType.ARCO_BESTA;
        return RpgCore.WeaponType.OUTRA;
    }
    public void validate(List<RpgCore.Quest> quests) {
        for(String key:items.stringPropertyNames())if(Item.getAssetMap().getAsset(item(key))==null)throw new IllegalStateException("ID nao carregado: "+item(key));
        for(var q:quests){item(q.rewardId());for(var o:q.objectives())if(Set.of("craft","collect").contains(o.action())&&!targets.containsKey(o.target()))throw new IllegalStateException("Objetivo sem IDs: "+o);}
    }
}
