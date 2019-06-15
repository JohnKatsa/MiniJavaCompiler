package part1;

public class Term implements IProduction
{
    IProduction Factor;
    IProduction Term2;

    public Term(IProduction factor, IProduction term2)
    {
        Factor = factor;
        Term2 = term2;
    }

    public int operate()
    {
        if(Term2 != null)
            return Factor.operate() & Term2.operate();
        else
            return Factor.operate();
    }
}