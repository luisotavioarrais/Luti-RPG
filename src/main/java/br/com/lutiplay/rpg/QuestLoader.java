package br.com.lutiplay.rpg;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** TSV UTF-8: id, fase, titulo, descricao, objetivos separados por ;, xp, item, qtde. */
public final class QuestLoader {
  private QuestLoader(){}
  public static List<RpgCore.Quest> load(InputStream stream) throws IOException {
    List<RpgCore.Quest> quests=new ArrayList<>(); Set<String> ids=new HashSet<>();
    try(BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){
      String line; int number=0;
      while((line=reader.readLine())!=null){
        number++;if(line.isBlank()||line.startsWith("#"))continue;
        String[] p=line.split("\t",-1);
        if(p.length!=8)throw new IOException("Linha "+number+": 8 colunas esperadas");
        if(!ids.add(p[0]))throw new IOException("Quest id repetido "+p[0]);
        List<RpgCore.Objective> objectives=new ArrayList<>();
        for(String raw:p[4].split(";")){
          String[] parts=raw.split(":",3);
          if(parts.length!=3)throw new IOException("Objetivo mal formado linha "+number);
          objectives.add(new RpgCore.Objective(parts[0],parts[1],Integer.parseInt(parts[2])));
        }
        quests.add(new RpgCore.Quest(p[0],p[1],p[2],p[3],objectives,Integer.parseInt(p[5]),p[6],Integer.parseInt(p[7])));
      }
    }
    return List.copyOf(quests);
  }
}
