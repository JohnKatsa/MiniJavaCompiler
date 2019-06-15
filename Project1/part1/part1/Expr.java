package part1;

public class Expr implements IProduction
{
    IProduction Term;
    IProduction Expr2;

    public Expr(IProduction term, IProduction expr2)
    {
        Term = term;
        Expr2 = expr2;
        // op = '^' because it is right recursive, the op is always on right child
    }

    public int operate()
    {
        if(Expr2 != null)
            return Term.operate() ^ Expr2.operate();
        else
            return Term.operate();
    }
}