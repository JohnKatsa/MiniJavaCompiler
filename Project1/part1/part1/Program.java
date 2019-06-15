package part1;
import part1.Parser;
import java.io.*;

public class Program
{
    public static void main(String[] args)
    {
        InputStream is = System.in;
        int id = 0;
        
        try
        {
            while(is.available() != 0)
            {
                Parser p = new Parser(is);
                System.out.print(id + ". ");
                p.Parse();
                id++;
            }
        }
        catch(IOException e)
        {
            return;
        }
            
    }
}
