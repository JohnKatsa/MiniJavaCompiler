package SymbolTableBuilderVisitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassInfo
{
    /* class name */
    String ClassName;

    /* variables of this class */
    Map<String,VarInfo> VarMap;

    /* methods of this class */
    Map<String,MethodInfo> MethodMap;

    /* If extended from a class store it here */
    ClassInfo BaseClass;

    public ClassInfo(String cn)
    {
        ClassName = cn;
        VarMap = new LinkedHashMap<String,VarInfo>();
        MethodMap = new LinkedHashMap<String,MethodInfo>();
        BaseClass = null;       // in real life we have object class :p
    }

    public void extendFrom(ClassInfo ci)
    {
        BaseClass = ci;
    }

    public boolean AddVar(String varName, String varType)
    {
        if(!VarMap.containsKey(varName))
        {
            VarInfo vi = new VarInfo(varName,varType);
            VarMap.put(varName, vi);
            return true;
        }

        return false;
    }

    public boolean EqualArguments(List<VarInfo> l1, List<VarInfo> l2)
    {
        if(l1.size() != l2.size())
            return false;

        for (int i = 0; i < l1.size(); i++)
        {
            if(l1.get(i).Type != l2.get(i).Type)
                return false;    
        }

        return true;
    }

    // Add method to current class
    public boolean AddMeth(MethodInfo mi)
    {
        // check if an overload occurs (from base class)
        ClassInfo currClass = this;
        while(currClass != null)
        {
            if(currClass.MethodMap.containsKey(mi.MethName))
            {
                MethodInfo currMeth;
                currMeth = currClass.MethodMap.get(mi.MethName);
                if(!EqualArguments(new ArrayList<VarInfo>(currMeth.Arguments.values()),new ArrayList<VarInfo>(mi.Arguments.values())))
                    return false;

                break;
            }

            currClass = currClass.BaseClass;
        }

        if(!MethodMap.containsKey(mi.MethName))
        {
            MethodMap.put(mi.MethName, mi);
            return true;
        }

        return false;
    }

    public String getClassName()
    {
        return ClassName;
    }

    public ClassInfo getBaseClass()
    {
        return BaseClass;
    }

    public Map<String,VarInfo> getVarMap()
    {
        return VarMap;
    }

    public Map<String,MethodInfo> getMethodMap()
    {
        return MethodMap;
    }

    // Need to make it recursive for base class
    public String getMethodType(String methName)
    {
        if(MethodMap.containsKey(methName))
            return MethodMap.get(methName).RetType;
        else
            /* if we reach this point there is an error */
            throw new ExceptionInInitializerError("Can't find method " + "\"" + methName + "\"");
    }

    // Returns type of variable.
    // Checks full parency - scope stack
    public String getVarType(String varName)
    {
        if(VarMap.containsKey(varName))
            return VarMap.get(varName).Type;
        else if(BaseClass != null)
            return BaseClass.getVarType(varName);
        else
            /* if we reach this point there is an error */
            throw new ExceptionInInitializerError("Can't find variable " + "\"" + varName + "\"");
    }


    public MethodInfo getMethodByName(String mName)
    {
        if(getMethodMap().size() == 0)
            throw new ExceptionInInitializerError("Class \"" + ClassName + "\" doesn't have methods");

        MethodInfo x = getMethodMap().get(mName);
        if(x == null)
            return BaseClass.getMethodByName(mName);
        return x;
    }

    public void print()
    {
        for (VarInfo x : VarMap.values())
        {
            x.print();
        }
        for (MethodInfo x : MethodMap.values())
        {
            x.print();
        }
        
        if(BaseClass != null)
        {
            BaseClass.print();
        }
    }
}