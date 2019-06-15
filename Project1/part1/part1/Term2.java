package part1;

public class Term2 implements IProduction
{
    IProduction Factor;
    IProduction Term2;

    public Term2(IProduction factor, IProduction term2)
    {
        Factor = factor;
        Term2 = term2;
    }

    public int operate()
    {
        if(Term2 != null)
            return Factor.operate() & Term2.operate();
        return Factor.operate();
    }
}