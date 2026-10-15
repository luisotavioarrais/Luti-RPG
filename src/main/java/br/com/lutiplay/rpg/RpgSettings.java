package br.com.lutiplay.rpg;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** External settings; invalid configuration fails startup instead of silently changing rules. */
public record RpgSettings(int maxLevel, double damageScale, double defenseScale, boolean hud) {
    public static RpgSettings load(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file=directory.resolve("config.properties");
        if(!Files.exists(file))Files.writeString(file,"maxLevel=50\ndamageScale=1.0\ndefenseScale=1.0\nhud=true\n");
        Properties p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}
        int level=Integer.parseInt(p.getProperty("maxLevel","50"));
        double damage=Double.parseDouble(p.getProperty("damageScale","1"));
        double defense=Double.parseDouble(p.getProperty("defenseScale","1"));
        if(level<1||level>10000||!Double.isFinite(damage)||damage<0||damage>1||!Double.isFinite(defense)||defense<0||defense>1)throw new IOException("Configuracao RPG invalida");
        return new RpgSettings(level,damage,defense,Boolean.parseBoolean(p.getProperty("hud","true")));
    }
}
