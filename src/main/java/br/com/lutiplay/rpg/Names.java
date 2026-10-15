package br.com.lutiplay.rpg;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
public final class Names {
 private static final Properties LABELS=new Properties();
 static {try(var in=Names.class.getResourceAsStream("/pt-BR.properties")){LABELS.load(new InputStreamReader(in,StandardCharsets.UTF_8));}catch(IOException e){throw new ExceptionInInitializerError(e);}}
 public static String of(Object key){String value=key.toString();return LABELS.getProperty(value,value.replace('_',' '));}
 private Names(){}
}
