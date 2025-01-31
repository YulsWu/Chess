import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

public class test {
    public static void test1 (){
        String filepath = "test_games.pgn";
        StringBuilder sb = new StringBuilder();
        String line;

        try(BufferedReader bf = new BufferedReader(new FileReader(filepath))){
            while (bf.ready()){
                line = bf.readLine();
                if (line.equals("")){
                    break;
                }
                sb.append(line);
                sb.append("_");
            }

            StringTokenizer tokenizer1 = new StringTokenizer(sb.toString(), "_");
            while (tokenizer1.hasMoreTokens()){
                String currentMeta = tokenizer1.nextToken();
                StringTokenizer tokenizer2 = new StringTokenizer(currentMeta, "\"[] ");
                
                if (tokenizer2.countTokens() < 2){
                    System.out.println("Blank field for " + tokenizer2.nextToken());
                    continue;
                }
                while (tokenizer2.hasMoreTokens()){
                    System.out.println(tokenizer2.nextToken());
                }

            }

        }
        catch (Exception e){
            System.out.println("Testing error: " + e);
        }
    }
}
