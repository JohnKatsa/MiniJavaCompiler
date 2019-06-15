package part1;

public class Factor implements IProduction
{
    IProduction Expr;
    char Num;
    
    public Factor(IProduction expr, char num)
    {
        Expr = expr;
        Num = num;
    }

    public int operate()
    {
        if(Expr != null)
            return Expr.operate();
        
        return Num - '0';
    }
}