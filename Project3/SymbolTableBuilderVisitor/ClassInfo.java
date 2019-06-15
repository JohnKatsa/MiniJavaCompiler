package SymbolTableBuilderVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // returns sum of all field sizes
    /* Theory: iterate over current class variables (not inherited) and get last one's offset plus its size 
     * if current class has no variables, go to base class
     * -> thats the size of the class for the variables 
     */
    public int getSumOfSizesOfClassFields()
    {
        int sum = 0;
        ClassInfo currClass = this;
        boolean flag = true;

        while(currClass != null && flag)
        {
            for (VarInfo var : VarMap.values())
            {
                flag = false;
                if(var.Type.equals("int"))
                    sum = 4 + var.Offset;
                else if(var.Type.equals("boolean"))
                    sum = 1 + var.Offset; 
                else 
                    sum = 8 + var.Offset;
            }
            currClass = currClass.BaseClass;
        }

        // it will have kept the last value only
        return sum;
    }

    // return number of methods that this class has (for vtable).
    public int getNumOfMethods()
    {
        Set<Integer> visitedOffsets = new HashSet<Integer>();
        ClassInfo currentClass = this;

        while(currentClass != null)
        {
            for (MethodInfo method : currentClass.MethodMap.values())
            {
                // if overriden don't include
                if(!visitedOffsets.contains(method.getOffset()))
                    // mark as visited
                    visitedOffsets.add(method.getOffset());
                   
            }
            
            currentClass = currentClass.BaseClass;
        }

        return visitedOffsets.size();
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
        int sz = 0;
        ClassInfo currClass = this;
        while(currClass != null)
        {
            sz += currClass.getMethodMap().size();
            currClass = currClass.BaseClass;
        }

        if(sz == 0)
            throw new ExceptionInInitializerError("Class \"" + ClassName + "\" doesn't have methods");

        MethodInfo x = getMethodMap().get(mName);
        if(x == null)
            return BaseClass.getMethodByName(mName);
        return MethodMap.get(mName);
    }

    // return -1 if local variable
    // > 0 if class field
    public int getOffset(String varName, String methName)
    {
        // local variable
        Map<String,VarInfo> args = MethodMap.get(methName).Arguments;
        Map<String,VarInfo> vars = MethodMap.get(methName).Variables;
        if( (args!=null && args.containsKey(varName))
          || (vars!=null && vars.containsKey(varName)))
            return -1;
        // class field
        else
            return getVarInfo(varName).Offset;
    }

    public VarInfo getVarInfo(String varName)
    {
        ClassInfo currentClass = this;

        while(currentClass != null)
        {
            if(currentClass.VarMap.containsKey(varName))
                return currentClass.VarMap.get(varName);
            currentClass = currentClass.BaseClass;
        }

        return null;
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