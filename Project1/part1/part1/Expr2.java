package part1;

public class Expr2 implements IProduction
{
    IProduction Term;
    IProduction Expr2;

    public Expr2(IProduction term, IProduction expr2)
    {
        Term = term;
        Expr2 = expr2;
    }

    public int operate()
    {
        if(Expr2 != null)
            return Term.operate() ^ Expr2.operate();
        return Term.operate();
    }
}