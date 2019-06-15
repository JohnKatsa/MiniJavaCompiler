package SymbolTableBuilderVisitor;

import java.util.HashMap;
import java.util.Map;

import syntaxtree.*;
import visitor.DepthFirstVisitor;

public class SymbolTableBuilderVisitor extends DepthFirstVisitor
{
    /* Data used for internal info storage, about declarations, ... */

    /* Class Name ---> Class Info */
    Map<String, ClassInfo> ClassMap;

    /* helping current data */
    ClassInfo CurrentClass; // current class entity

    MethodInfo CurrentMethod; // current method entity
    Boolean InMethod; // if we are in class or in class' method

    int CurrentMethodOffset;
    int CurrentVariableOffset;

    String CurrentType; // current type of variable, method, parameter...
    //////////////////////////

    public SymbolTableBuilderVisitor()
    {
        ClassMap = new HashMap<String, ClassInfo>();
        CurrentMethodOffset = 0;
        CurrentVariableOffset = 0;
    }

    public Map<String, ClassInfo> getClassMap()
    {
        return ClassMap;
    }

    // update method offset, if needed (no override)
    void addMethodOffset()
    {
        // true if no override
        if (CurrentMethod.setOffset(CurrentMethodOffset))
            CurrentMethodOffset += 8;
    }

    // ONLY update variable offset
    void incrementVariableOffset(String type) 
    {
        if (type.equals("boolean"))
            CurrentVariableOffset += 1;
        else if (type.equals("int"))
            CurrentVariableOffset += 4;
        else
            CurrentVariableOffset += 8;
    }

    /**
     * f0 -> "class" f1 -> Identifier() f2 -> "{" f3 -> "public" f4 -> "static" f5
     * -> "void" f6 -> "main" f7 -> "(" f8 -> "String" f9 -> "[" f10 -> "]" f11 ->
     * Identifier() f12 -> ")" f13 -> "{" f14 -> ( VarDeclaration() )* f15 -> (
     * Statement() )* f16 -> "}" f17 -> "}"
     */
    public void visit(MainClass m)
    {
        InMethod = false;

        m.f0.accept(this);

        /* get main class name */
        m.f1.accept(this);
        String _className = m.f1.f0.tokenImage;

        /* make class entity */
        CurrentClass = new ClassInfo(_className);

        /* add main as a class method */
        CurrentMethod = new MethodInfo("void", "main", CurrentClass);

        //addMethodOffset();

        if (!CurrentClass.AddMeth(CurrentMethod))
            throw new ExceptionInInitializerError("Method : \"main\" already exists");

        InMethod = true;
        m.f2.accept(this);
        m.f3.accept(this);
        m.f4.accept(this);
        m.f5.accept(this);
        m.f6.accept(this);
        m.f7.accept(this);
        m.f8.accept(this);
        m.f9.accept(this);
        m.f10.accept(this);

        /* set main function args name */
        m.f11.accept(this);
        String _argsName = m.f11.f0.tokenImage;
        CurrentMethod.AddArg(_argsName, "string[]");

        m.f12.accept(this);
        m.f13.accept(this);
        m.f14.accept(this); /* variable declarations in main method */
        m.f15.accept(this);
        m.f16.accept(this);
        m.f17.accept(this);

        /* Main class ended */
        ClassMap.put(_className, CurrentClass);
    }

    /*
     * Grammar production: f0 -> "class" f1 -> Identifier() f2 -> "{" f3 -> (
     * VarDeclaration() )* f4 -> ( MethodDeclaration() )* f5 -> "}"
     */
    public void visit(ClassDeclaration cd) 
    {
        InMethod = false;

        cd.f0.accept(this);

        /* Class Name */
        cd.f1.accept(this);
        String _className = cd.f1.f0.tokenImage;

        /*
         * if class name doesn't exist continue else throw error
         */
        if (ClassMap.containsKey(_className))
            throw new ExceptionInInitializerError("Class with name : \"" + _className + "\" already exists");

        /* Create new class node */
        CurrentClass = new ClassInfo(_className);

        cd.f2.accept(this);

        /* Get Class variable declarations */
        cd.f3.accept(this);

        /* Get Class method declarations */
        cd.f4.accept(this);
        InMethod = false;

        cd.f5.accept(this);

        /* Random class ended */
        ClassMap.put(_className, CurrentClass);
    }

    /**
     * f0 -> "class" f1 -> Identifier() f2 -> "extends" f3 -> Identifier() f4 -> "{"
     * f5 -> ( VarDeclaration() )* f6 -> ( MethodDeclaration() )* f7 -> "}"
     */
    public void visit(ClassExtendsDeclaration ced) 
    {
        InMethod = false;

        ced.f0.accept(this);

        ced.f1.accept(this);
        String _className = ced.f1.f0.tokenImage;

        ced.f2.accept(this);

        ced.f3.accept(this);
        String _classNameBase = ced.f3.f0.tokenImage;

        /*
         * if class name doesn't exist continue else throw error
         */
        if (ClassMap.containsKey(_className))
            throw new ExceptionInInitializerError("Class with name : \"" + _className + "\" already exists");

        /* check if base class is declared */
        if (!ClassMap.containsKey(_classNameBase))
            throw new ExceptionInInitializerError("Base class : \"" + _classNameBase + "\" doesn't exist");

        /* Create new class node and inherit from base */
        CurrentClass = new ClassInfo(_className);
        ClassInfo _classEntityBase = ClassMap.get(_classNameBase);
        CurrentClass.extendFrom(_classEntityBase); /* extend */

        ced.f4.accept(this);
        ced.f5.accept(this);
        ced.f6.accept(this);
        ced.f7.accept(this);

        ClassMap.put(_className, CurrentClass); /* put in map */
    }

    /**
     * f0 -> Type() f1 -> Identifier() f2 -> ";"
     */
    public void visit(VarDeclaration vd) 
    {
        vd.f0.accept(this);
        String _varType = CurrentType;

        vd.f1.accept(this);
        String _varName = vd.f1.f0.tokenImage;

        vd.f2.accept(this);

        if (InMethod) 
        {
            if (!CurrentMethod.AddVar(_varName, _varType))
                throw new ExceptionInInitializerError("Variable name: \"" + _varName + "\" already exists");
        } 
        else 
        {
            if (!CurrentClass.AddVar(_varName, _varType))
                throw new ExceptionInInitializerError("Variable name: \"" + _varName + "\" already exists");

            // calculate and store offset
            CurrentClass.VarMap.get(_varName).setOffset(CurrentVariableOffset);
            incrementVariableOffset(_varType);
        }
    }

    /**
     * f0 -> "public" f1 -> Type() f2 -> Identifier() f3 -> "(" f4 -> (
     * FormalParameterList() )? f5 -> ")" f6 -> "{" f7 -> ( VarDeclaration() )* f8
     * -> ( Statement() )* f9 -> "return" f10 -> Expression() f11 -> ";" f12 -> "}"
     */
    public void visit(MethodDeclaration md)
    {
        InMethod = true;

        md.f0.accept(this);

        md.f1.accept(this);
        String _retType = CurrentType;

        md.f2.accept(this);
        String _methName = md.f2.f0.tokenImage;

        CurrentMethod = new MethodInfo(_retType, _methName, CurrentClass);

        addMethodOffset();

        md.f3.accept(this);
        md.f4.accept(this); // get method arguments
        md.f5.accept(this);
        md.f6.accept(this);
        md.f7.accept(this); // get var declaration
        md.f8.accept(this);
        md.f9.accept(this);
        md.f10.accept(this);
        md.f11.accept(this);
        md.f12.accept(this);

        if (!CurrentClass.AddMeth(CurrentMethod))
            throw new ExceptionInInitializerError("Method : \"" + _methName + "\" already exists");

        InMethod = false;
    }

    /**
     * f0 -> Type() f1 -> Identifier()
     */
    /* happens only in arguments list */
    public void visit(FormalParameter fp) 
    {
        if (!InMethod)
            throw new ExceptionInInitializerError("Error in arguments declaration, not in method scope");

        fp.f0.accept(this);
        String _varType = CurrentType;

        fp.f1.accept(this);
        String _varName = fp.f1.f0.tokenImage;

        if (!CurrentMethod.AddArg(_varName, _varType))
            throw new ExceptionInInitializerError("Argument : \"" + _varName + "\" in parameter list already defined");
    }

    /**
     * Grammar production: f0 -> "int" f1 -> "[" f2 -> "]"
     */
    public void visit(ArrayType at)
    {
        at.f0.accept(this);
        at.f1.accept(this);
        at.f2.accept(this);

        // set current type
        CurrentType = at.f0.tokenImage + at.f1.tokenImage + at.f2.tokenImage;
    }

    /**
     * f0 -> "boolean"
     */
    public void visit(BooleanType bt)
    {
        bt.f0.accept(this);

        // set current type
        CurrentType = bt.f0.tokenImage;
    }

    /**
     * f0 -> "int"
     */
    public void visit(IntegerType it)
    {
        it.f0.accept(this);

        // set current type
        CurrentType = it.f0.tokenImage;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public void visit(Identifier i) 
    {
        i.f0.accept(this);

        // set current type
        CurrentType = i.f0.tokenImage;
    }

    public void print() 
    {
        for (ClassInfo x : ClassMap.values()) 
        {
            x.print();
        }
    }

    public void printOffsets() 
    {
        String offsets = "";

        for (ClassInfo classes : ClassMap.values())
        {
            offsets += "-----------" + classes.ClassName + "-----------" + "\n";
            offsets += "--Variables---" + "\n";
            for (VarInfo vars : classes.VarMap.values())
            {
                offsets += classes.ClassName + "." + vars.Name + " : " + vars.Offset + "\n";
            }

            offsets += "---Methods---" + "\n";
            for (MethodInfo meths : classes.MethodMap.values())
            {
                /*offsets += classes.ClassName + "." + meths.MethName + " : " + meths.Offset 
                + (meths.overrides ? " (Override)" : "") + "\n";*/

                offsets += (!meths.overrides ?
                classes.ClassName + "." + meths.MethName + " : " + meths.Offset +"\n" 
                : "\n");
            }
        }

        System.out.println(offsets);

    }
}