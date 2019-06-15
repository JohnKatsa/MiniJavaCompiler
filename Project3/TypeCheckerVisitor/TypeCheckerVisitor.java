package TypeCheckerVisitor;

import syntaxtree.*;
import visitor.GJNoArguDepthFirst;

import java.util.Map;
import java.util.Arrays;
import java.util.List;

import SymbolTableBuilderVisitor.*;

/* String used to return type name */
public class TypeCheckerVisitor extends GJNoArguDepthFirst<String>
{
    /* Need to represent current class and method */
    ClassInfo CurrentClass;
    MethodInfo CurrentMethod;

    /* Need to represent class and method called */
    //ClassInfo ClassCall;    
    //MethodInfo MethodCall;

    boolean InMethod;   // used to know if identifier is in method or in class only

    /* Keep the contract from the first visitor */
    Map<String,ClassInfo> ClassMap;
    
    public TypeCheckerVisitor(Map<String,ClassInfo> classMap)
    {
        ClassMap = classMap;
    }

    boolean IsPrimitive(String type)
    {
        return type.equals("int") || type.equals("boolean") || type.equals("int[]");
    }

    /* Checks if type A is superclass of type B */
    public boolean isSubTypeOf(String classNameA, String classNameB, Map<String, ClassInfo> ClassMap)
    {
        // check if types are "primitives(int,boolean,int[]), then they must be equal types"
        if(IsPrimitive(classNameA))
            return classNameA.equals(classNameB);
        
        ClassInfo curr = ClassMap.get(classNameB);

        while(curr != null)
        {
            // better check names
            if(curr.getClassName().equals(classNameA))
                return true; 
            else
                curr = curr.getBaseClass();
        }
        return false;
    }

    List<String> ConvertStringToList(String args)
    {
        if(args == "" || args == null)
            return null;

        List<String> x = Arrays.asList(args.split(","));
        for (int i = 0; i < x.size(); i++) 
        {
            x.set(i, CurrentMethod.getVarType(x.get(i), ClassMap));
        }
        return x;
    }

    boolean EqualArgs(List<String> l1, List<String> l2)
    {
        if(l1 == null && l2 == null)
            return true;

        if(l1.size() != l2.size())
            return false;

        for (int i = 0; i < l1.size(); i++)
        {
            if(!isSubTypeOf((String)l1.toArray()[i], (String)l2.toArray()[i], ClassMap))
                return false;
        }

        return true;
    }

    /* LEVEL 0 -- TYPE SETTING */

    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
    public String visit(PrimaryExpression n)
    {
        return n.f0.accept(this);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n)
    {
        return "int";
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n) 
    {
        return "boolean";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n)
    {
        return "boolean";
    }

    /**
     * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n)
    {
        n.f0.accept(this);
        return n.f0.tokenImage;
    }

    /**
     * f0 -> "this"
    */
    public String visit(ThisExpression n)
    {
        return CurrentClass.getClassName();
    }

    /**
     * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(ArrayAllocationExpression n)
    {
        /* check if the expression type is int */
        if(!CurrentMethod.getVarType(n.f3.accept(this), ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Expression in array allocation should be of type Int");

        return "int[]";
    }

    /**
     * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n)
    {
        /* get the type of new */
        String a = n.f1.accept(this);
        return a;
    }

    /**
     * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n)
    {
        if(!CurrentMethod.getVarType(n.f1.accept(this), ClassMap).equals("boolean"))
            throw new ExceptionInInitializerError("Can't apply operator \"!\" to type other than boolean ");

        return "boolean";
    }

    /**
     * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n)
    {
        return n.f1.accept(this);
    }

    /////////////////////////////

    /* LEVEL 1 -- TYPE CHECKING & SETTING */

    /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | Clause()
    */
    public String visit(Expression n)
    {
        return n.f0.accept(this);
    }

    /**
     * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n)
    {
        if(!CurrentMethod.getVarType(n.f0.accept(this),ClassMap).equals("boolean"))
            throw new ExceptionInInitializerError("Can't apply operator \"&&\" to type other than boolean");
        
        if(!CurrentMethod.getVarType(n.f2.accept(this),ClassMap).equals("boolean"))
            throw new ExceptionInInitializerError("Can't apply operator \"&&\" to type other than boolean");
        
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n)
    {
        if(!CurrentMethod.getVarType(n.f0.accept(this),ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Can't apply operator \"<\" to type other than int");
        
        if(!CurrentMethod.getVarType(n.f2.accept(this),ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Can't apply operator \"<\" to type other than int");
        
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n)
    {
        if(!CurrentMethod.getVarType(n.f0.accept(this),ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Can't apply operator \"+\" to type other than int");
        
        if(!CurrentMethod.getVarType(n.f2.accept(this),ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Can't apply operator \"+\" to type other than int");
        
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n)
    {
        if(!CurrentMethod.getVarType(n.f0.accept(this),ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Can't apply operator \"-\" to type other than int");
        
        if(!CurrentMethod.getVarType(n.f2.accept(this),ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Can't apply operator \"-\" to type other than int");
        
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n)
    {
        if(!CurrentMethod.getVarType(n.f0.accept(this),ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Can't apply operator \"*\" to type other than int");
        
        if(!CurrentMethod.getVarType(n.f2.accept(this),ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Can't apply operator \"*\" to type other than int");
        
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n)
    {
        if(!CurrentMethod.getVarType(n.f0.accept(this), ClassMap).equals("int[]"))
            throw new ExceptionInInitializerError("Can't apply operator \"[]\" to type other than int[]");
        
        if(!CurrentMethod.getVarType(n.f2.accept(this), ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Inside the operator : \"[]\" must be type : int");
        
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n)
    {
        if(!CurrentMethod.getVarType(n.f0.accept(this),ClassMap).equals("int[]"))
            throw new ExceptionInInitializerError("Can't apply method \"length\" to type other than int[]");
        
        n.f1.accept(this);
        n.f2.accept(this);
        
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n)
    {
        /* Get object type */
        String type1 = CurrentMethod.getVarType(n.f0.accept(this),ClassMap);
        
        if(IsPrimitive(type1))
            throw new ExceptionInInitializerError("Primitive type doesn't have methods");

        /* Get Method return type */
        MethodInfo _methodCall;
        ClassInfo _classCall;
        _classCall = ClassMap.get(type1);
        if(_classCall == null)
            throw new ExceptionInInitializerError("Class \"" + type1 + "\" doesn't exist");
        
        _methodCall = _classCall.getMethodByName(n.f2.f0.tokenImage);
        if(_methodCall == null)
            throw new ExceptionInInitializerError("Method \"" + n.f2.f0.tokenImage + "\" doesn't exist");
        
        String type2 = _methodCall.getRetType();
        
        /* construct a string like this "type1,type2,..." */
        /* it will contain the method arguments */
        String type3 = n.f4.accept(this);
        if(type3 == null) type3 = "";
        if(!EqualArgs(_methodCall.getArgTypeList(),ConvertStringToList(type3)))
            throw new ExceptionInInitializerError("Method doesn't take this number of arguments or type of arguments");
        
        /* if all requirements are met, return the method's return type */
        return type2;
    }

    /**
     * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n)
    {
        /* TI PREPEI NA EPISTREFEI AUTO EDW ?!?!?! */
        String nullRescue = n.f0.accept(this);
        String nullRescue2 = n.f1.accept(this);
        return nullRescue + nullRescue2;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
    */
    public String visit(ExpressionTail n)
    {
        String nullRescue = n.f0.accept(this);
        return nullRescue == null ? "" : nullRescue;
    }

    /**
     * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n)
    {
        /* make method arguments in string */
        String nullRescue = n.f1.accept(this);
        return "," + nullRescue;
    }

    /**
     * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    public String visit(Clause n)
    {
        return n.f0.accept(this);
    }

    ////////////////////////////////////////

    /* LEVEL 2 -- Assignment and Operations Integration (Return values don't have any information) */

    /* statement doesn't need to be overriden */
    /* "block" doesn't need to be overriden */

    /**
     * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n)
    {
        /* If we have parent-child or primitive-equality relationship, we are legal */
        String a = n.f0.accept(this);
        String b = n.f2.accept(this);
        if(!isSubTypeOf(CurrentMethod.getVarType(a,ClassMap), CurrentMethod.getVarType(b,ClassMap), ClassMap))
            throw new ExceptionInInitializerError("Incompatible types in assignment");

        return "";
    }

    /**
     * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String visit(ArrayAssignmentStatement n)
    {
        if(!CurrentMethod.getVarType(n.f0.accept(this),ClassMap).equals("int[]"))
            throw new ExceptionInInitializerError("Can't apply operator \"[]\" to type other than int[]");
        
        if(!CurrentMethod.getVarType(n.f2.accept(this),ClassMap).equals("int"))
            throw new ExceptionInInitializerError("operator \"[]\" takes type int inside");

        if(!CurrentMethod.getVarType(n.f5.accept(this),ClassMap).equals("int"))
            throw new ExceptionInInitializerError("Can't assign type \"int\" to other type than int");
        
        return "";
    }

    /**
     * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String visit(IfStatement n)
    {
        if(!CurrentMethod.getVarType(n.f2.accept(this),ClassMap).equals("boolean"))
            throw new ExceptionInInitializerError("Can't calculate other type than boolean in if statement");
        
        n.f4.accept(this);

        n.f6.accept(this);

        return "";
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n)
    {
        if(!n.f2.accept(this).equals("boolean"))
            throw new ExceptionInInitializerError("Can't calculate other type than boolean in while statement");
        
        n.f4.accept(this);

        return "";
    }

    /**
     * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n)
    {
        String type = n.f2.accept(this);
        if(!(CurrentMethod.getVarType(type,ClassMap).equals("int") || CurrentMethod.getVarType(type,ClassMap).equals("boolean")))
            throw new ExceptionInInitializerError("Can't print something different from \"int\" or \"boolean\"");
        
        return "";
    }

    //////////////////////////////////////////////////////

    /* HIGHEST LEVEL -- Here we only care to set current class object & current method object */
    /* At this level we have to check method return values */
    /* Obviously return types are not functional in this level */

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    public String visit(MainClass n)
    {
        InMethod = false;
        
        /* Set current class entity */
        n.f1.accept(this);
        String _className = n.f1.f0.tokenImage;
        CurrentClass = ClassMap.get(_className);
        if(CurrentClass == null) throw new ExceptionInInitializerError("Class \""+ _className +"\" doesn't exist");

        InMethod = true;

        /* Set current method entity */
        String _methodName = "main";
        CurrentMethod = CurrentClass.getMethodMap().get(_methodName);

        n.f15.accept(this);

        InMethod = false;

        // After we typechecked main class, we can exclude it from any other activity
        // That is ok, because minijava doesn't support static calls 
        // and main class constists only of one static method
        //ClassMap.remove(CurrentClass.getClassName());

        return "";
    }

    /**
     * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public String visit(ClassDeclaration n)
    {
        InMethod = false;
        
        /* Set current class entity */
        n.f1.accept(this);
        String _className = n.f1.f0.tokenImage;
        CurrentClass = ClassMap.get(_className);
        if(CurrentClass == null) throw new ExceptionInInitializerError("Class \""+ _className +"\" doesn't exist");

        n.f4.accept(this);

        return "";
    }

    /**
     * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    public String visit(ClassExtendsDeclaration n)
    {
        InMethod = false;
        
        /* Set current class entity */
        n.f1.accept(this);
        String _className = n.f1.f0.tokenImage;
        CurrentClass = ClassMap.get(_className);
        if(CurrentClass == null) throw new ExceptionInInitializerError("Class \""+ _className +"\" doesn't exist");
        
        n.f6.accept(this);

        return "";
    }

    /**
     * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    public String visit(MethodDeclaration n) 
    {
        InMethod = false;
        
        /* Set current method entity */
        n.f2.accept(this);
        String _methodName = n.f2.f0.tokenImage;
        //CurrentMethod = CurrentClass.getMethodMap().get(_methodName);
        CurrentMethod = CurrentClass.getMethodByName(_methodName);
        if(CurrentMethod == null) throw new ExceptionInInitializerError("Method \""+ _methodName +"\" doesn't exist");

        InMethod = true;

        n.f8.accept(this);

        /* We have to check if the return type of this method is compatible with this return statement */
        if(!isSubTypeOf(CurrentMethod.getRetType(),CurrentMethod.getVarType(n.f10.accept(this),ClassMap),ClassMap))
            throw new ExceptionInInitializerError("Incompatible return type");

        InMethod = false;

        return "";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
}