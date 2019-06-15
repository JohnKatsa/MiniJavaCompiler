import java_cup.runtime.*;
import java.io.*;

class Main {
    public static void main(String[] argv) throws Exception{
        Parser p = new Parser(new Scanner(new InputStreamReader(System.in)));
        p.parse();
    }

    public static boolean foo(){
        Boolean b = "aa" .endsWith("a");
        return b;
    }
}