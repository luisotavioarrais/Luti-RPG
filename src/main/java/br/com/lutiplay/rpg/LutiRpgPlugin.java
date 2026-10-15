package br.com.lutiplay.rpg;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

/** Adapter compiled against the user's legitimate Hytale Server 0.7.2. */
public final class LutiRpgPlugin extends JavaPlugin {
    public LutiRpgPlugin(JavaPluginInit init) { super(init); }
    @Override protected void setup() {
        try {
            var catalog=new ContentCatalog();
            java.util.List<RpgCore.Quest> quests;
            try(var in=getClass().getResourceAsStream("/quests.tsv")){quests=QuestLoader.load(in);}
            var service=new RpgService(new HeroRepository(getDataDirectory().resolve("players")),catalog,RpgSettings.load(getDataDirectory()),quests);
            getCommandRegistry().registerCommand(new RpgCommand(service));
            getCodecRegistry(com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.CODEC).register("LutiRPGMenu",RpgMenuInteraction.class,com.hypixel.hytale.codec.builder.BuilderCodec.builder(RpgMenuInteraction.class,()->new RpgMenuInteraction(service),com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction.CODEC).build());
            getEntityStoreRegistry().registerSystem(new RpgSystems.Craft(service));
            getEntityStoreRegistry().registerSystem(new RpgSystems.InventoryChanged(service));
            getEntityStoreRegistry().registerSystem(new RpgSystems.AttackSnapshot(service));
            getEntityStoreRegistry().registerSystem(new RpgSystems.Offense());
            getEntityStoreRegistry().registerSystem(new RpgSystems.Defense(service));
            getEntityStoreRegistry().registerSystem(new RpgSystems.Kills(service));
            getEntityStoreRegistry().registerSystem(new RpgSystems.HitCredit(service));
            getEntityStoreRegistry().registerSystem(new RpgSystems.Respawn(service));
            getEntityStoreRegistry().registerSystem(new RpgSystems.ProtectJournalRequest());
            getEntityStoreRegistry().registerSystem(new RpgSystems.ProtectJournalDrop());
            getEntityStoreRegistry().registerSystem(new RpgSystems.Players(service));
            this.catalog=catalog;this.quests=quests;
            getLogger().atInfo().log("LutiRPG: comandos e sistemas registrados; %s missoes",quests.size());
        } catch(java.io.IOException e){throw new IllegalStateException("Falha ao iniciar LutiRPG",e);}
    }
    private ContentCatalog catalog;
    private java.util.List<RpgCore.Quest> quests;
    @Override protected void start(){catalog.validate(quests);if(Boolean.getBoolean("lutirpg.selfTest"))ServerSelfTest.run(catalog);getLogger().atInfo().log("LutiRPG: IDs dos kits e recompensas validados contra os assets carregados.");}
}
