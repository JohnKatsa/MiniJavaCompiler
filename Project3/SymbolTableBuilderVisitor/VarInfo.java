package SymbolTableBuilderVisitor;

public class VarInfo
{
    String Name;
    String Type;
    Boolean Initialized;
    int Offset;

    public VarInfo(String name, String type)
    {
        Name = name;
        Type = type;
        Initialized = false;
        Offset = -1;
    }

    public void setOffset(int off)
    {
        Offset = off;
    }

    public void print()
    {
        System.out.println("("+Name+","+Type+","+Offset+")");
    }

    public String getName()
    {
        return Name;
    }

    public String getType()
    {
        return Type;
    }
}