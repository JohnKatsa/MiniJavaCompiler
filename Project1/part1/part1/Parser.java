package part1;

import java.io.*;
import java.util.Stack;

public class Parser
{
    char lookahead;                 // my lookahead
    Boolean checked;                // check if lookahead really consumed
    String nums = "0123456789";     // valid numbers
    InputStream inputstream;        // input stream
    Stack<IProduction> tree;        // expression tree as stack
    
    public Parser(InputStream is)
    {
        tree = new Stack<IProduction>();
        inputstream = is;
        nextToken();
    }

    public void Parse()
    {
        if(E() && checked)
            System.out.println(tree.pop().operate());
        else
            System.out.println("Error");
    }

    private Boolean E()
    {
        if(T() && E2())
        {
            Expr2 expr2 = (Expr2) tree.pop();
            Term term = (Term) tree.pop();
            Expr expr = new Expr(term,expr2);
            tree.push(expr);
             
            return true;
        }
        return false;
    }

    private Boolean E2()
    {
        if(match('^'))
        {
            nextToken();

            if(T() && E2())
            {
                Expr2 expr2 = (Expr2) tree.pop();
                Term term = (Term) tree.pop();
                Expr2 newexpr2 = new Expr2(term,expr2);
                tree.push(newexpr2);
                return true;
            }
            return false;
        }

        // insert null expr2 item
        tree.push(null);
        return true;
    }

    private Boolean T()
    {
        if(F() && T2())
        {
            Term2 term2 = (Term2) tree.pop();
            Factor factor = (Factor) tree.pop();
            Term term = new Term(factor,term2);
            tree.push(term);
            return true;
        }
        return false;
    }

    private Boolean T2()
    {
        if(match('&'))
        {
            nextToken();

            if(F() && T2())
            {
                Term2 term2 = (Term2) tree.pop();
                Factor factor = (Factor) tree.pop();
                Term2 newterm2 = new Term2(factor,term2);
                tree.push(newterm2);
                return true; 
            }
            return false;
        }

        // insert null term item
        tree.push(null);
        return true;
    }

    private Boolean F()
    {
        if(match('('))
        {
            nextToken();

            if(E() && match(')'))
            {
                nextToken();

                Expr expr = (Expr) tree.pop();
                Factor factor = new Factor(expr,'\0');
                tree.push(factor);

                return true;
            }
            return false;      
        }
        
        else if(match('!'))     // ! denotes integer 
        {            
            Factor factor = new Factor(null,lookahead);
            tree.push(factor);

            nextToken();
                
            return true;
        }

        // invalid character
        return false;
    }

    private void nextToken()
    {
        try
        {
            lookahead = (char) inputstream.read();
            if("0123456789()^&".indexOf(lookahead) != -1)
                checked = false;
        }
        catch(IOException ioe)
        {
            System.out.println("Error/// " + lookahead);
        }
    }

    private Boolean match(char current)
    {
        if((lookahead == current) || (current == '!' && nums.indexOf(lookahead) != -1))
        {
            checked = true;
            return true;
        }
        return false;
    }

}
