package SymbolTableBuilderVisitor;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MethodInfo
{
    String RetType;
    String MethName;
    Map<String,VarInfo> Arguments;
    Map<String,VarInfo> Variables;
    int Offset;
    boolean overrides;

    ClassInfo MyClass;              // used to know what variables the class has

    public MethodInfo(String rettype, String methname, ClassInfo myclass)
    {
        RetType = rettype;
        MethName = methname;
        Arguments = new LinkedHashMap<String,VarInfo>();
        Variables = new LinkedHashMap<String,VarInfo>();
        MyClass = myclass;
        overrides = false;
    }

    /* Add variable in method body */
    public boolean AddVar(String varName, String varType)
    {
        if(!(Arguments.containsKey(varName) || Variables.containsKey(varName)))
        {
            VarInfo vi = new VarInfo(varName,varType);
            Variables.put(varName, vi);
            return true;
        }

        return false;
    }

    public boolean AddArg(String varName, String varType)
    {
        if(!Arguments.containsKey(varName))
        {
            VarInfo vi = new VarInfo(varName,varType);
            Arguments.put(varName, vi);
            return true;
        }

        return false;
    }

    public void doesOverride()
    {
        overrides = true;
    }

    public String getMethName()
    {
        return MethName;
    }

    public Map<String,VarInfo> getVarMap()
    {
        return Variables;
    } 

    public Map<String,VarInfo> getArgMap()
    {
        return Arguments;
    }

    public int getOffset()
    {
        return Offset;
    }

    public List<String> getArgTypeList()
    {
        if(Arguments.size() == 0)
            return null;

        List<String> l = new ArrayList<String>();
        for (VarInfo type : Arguments.values())
        {
            l.add(type.Type); 
        }

        return l;
    }

    public String getRetType()
    {
        return RetType;
    }

    // Return variable type inside method or type
    // Used in identifier node to check type for the type checker
    public String getVarType(String varName, Map<String,ClassInfo> cMap)
    {
        // in case of types
        if(varName.equals("boolean") || varName.equals("int") || varName.equals("int[]") || cMap.containsKey(varName))
            return varName;

        if(Arguments.containsKey(varName))
            return Arguments.get(varName).Type;
        else if(Variables.containsKey(varName))
            return Variables.get(varName).Type;
        else
            return MyClass.getVarType(varName);    // throws exception if not found

    } 

    /* Returns arguments as a string */
    public String getArgumentsInStringFormat()
    {
        String ret = "";
        for (VarInfo var : Arguments.values())
        {
            ret += var.Type + ",";
        }
        return ret != "" ? ret.substring(0, ret.length()-1) : "";
    }

    // Information printer
    public void print()
    {
        System.out.println("Method Name : " + MethName + ", Method Return Type : " + RetType);
        System.out.println("Method Offset : " + Offset);
        System.out.println("Method Arguments : ");
        for (VarInfo x : Arguments.values())
        {
            x.print();
        }
        System.out.println("Method Variables : ");
        for (VarInfo x : Variables.values())
        {
            x.print();
        }
    }

    // return true if we need to change method offset counter
    public boolean setOffset(int off)
    {
        ClassInfo myclass = MyClass.BaseClass;

        // if it is an override don't change offset (recurse to all previous generations)
        while(myclass != null)
        {
            if(myclass.MethodMap.containsKey(MethName))
            {
                Offset = myclass.MethodMap.get(MethName).Offset;
                doesOverride();
                return false;
            }
            myclass = myclass.BaseClass;
        }
        
        Offset = off;
        return true;
    }
    
}