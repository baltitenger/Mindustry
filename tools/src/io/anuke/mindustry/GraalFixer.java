package io.anuke.mindustry;

import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.ucore.util.Log;

import java.nio.file.Files;
import java.nio.file.Paths;

public class GraalFixer{
    static String out = "";

    public static void main(String[] args) throws Exception{
        String dir = new String(Files.readAllBytes(Paths.get("/home/anuke/Documents/log.txt")));
        int index = 0;
        ObjectSet<String> set = new ObjectSet<>();

        do{
            int begin = "Error: Class initialization failed: ".length() + index;
            int nextline = dir.indexOf("\n", begin);
            String result = dir.substring(begin, nextline);
            set.add("'" + result + "'");
            index += result.length();
        }while((index = dir.indexOf("Error: Class initialization failed: ", index)) != -1);
        String result = set.toString();
        Log.info(result.substring(1, result.length() - 1).replace(" ", ""));
    }
}
