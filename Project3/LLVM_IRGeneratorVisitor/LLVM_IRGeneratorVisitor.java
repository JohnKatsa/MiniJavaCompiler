package LLVM_IRGeneratorVisitor;

import visitor.*;
import syntaxtree.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import SymbolTableBuilderVisitor.*;

public class LLVM_IRGeneratorVisitor extends GJNoArguDepthFirst<String>
{
    int FilenameIterator;  // iterator to get new temp 
    int LabelIterator;     // iterator to get new label
    String BodyCodeBuffer;
    String HeadCodeBuffer;

    String CurrentClassName;
    String CurrentMethodName;
    String CurrentObjectType;   // object type that is called a method on (set on: AllocationExpr, Identifier)
    Boolean InMethod;
    Boolean IsVariable;
    Map<String,ClassInfo> ClassMap;

    // used to choose whether i want pointer or var, in clause
    Boolean IsPointer;
    String IsPointerType;
    
    public LLVM_IRGeneratorVisitor(Map<String,ClassInfo> classMap)
    {
        BodyCodeBuffer = "";
        HeadCodeBuffer = "";
        FilenameIterator = 0;
        emit("declare i8* @calloc(i32, i32)\n"
            + "declare i32 @printf(i8*, ...)\n" 
            + "declare void @exit(i32)\n" 
            + "\n"
            + "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n"
            + "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" 
            + "define void @print_int(i32 %i) {\n"
            + "    %_str = bitcast [4 x i8]* @_cint to i8*\n"
            + "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" 
            + "    ret void\n" 
            + "}\n" 
            + "\n"
            + "define void @throw_oob() {\n" 
            + "    %_str = bitcast [15 x i8]* @_cOOB to i8*\n"
            + "    call i32 (i8*, ...) @printf(i8* %_str)\n" 
            + "    call void @exit(i32 1)\n" 
            + "    ret void\n"
            + "}\n");
        ClassMap = classMap;
        IsVariable = true;
    }

    // creates llvm-ir code and adds to buffer
    public void emit(String code)
    {
        BodyCodeBuffer += code;
    }

    // creates llvm-ir code and adds to buffer
    public void emitVTABLE(String code)
    {
        HeadCodeBuffer += code;
    }

    // returns new temp
    public String new_temp()
    {
        return "%_" + FilenameIterator++;
    }

    // returns new label
    public String new_label()
    {
        return "label" + LabelIterator++;
    }

    // gets type name e.g. "int", "boolean" and returns its size in bits
    public String convertType(String type)
    {
        if(type.equals("int"))
            return "i32";
        else if (type.equals("boolean"))
            return "i1";
        else if (type.equals("int[]"))
            return "i32*";
        else
            return "i8*";
    }

    // returns method parameters in llvm-ir format (no variable names)
    // used for vtable declaration
    public String convertArgumentsTypes(Map<String,VarInfo> arguments)
    {
        String args = "";

        for (VarInfo argument : arguments.values())
        {
            args += ", " + convertType(argument.getType());
        }

        return args;
    }

    // returns method parameters in llvm-ir format
    public String convertArguments(Map<String,VarInfo> arguments)
    {
        String args = "";

        for (VarInfo argument : arguments.values())
        {
            args += ", " + convertType(argument.getType()) + " %." + argument.getName();
        }

        return args;
    }

    // returns llvm-ir code that copies parameter arguments to local variables
    public String copyArguments(Map<String,VarInfo> arguments)
    {
        String args = "";

        for (VarInfo argument : arguments.values())
        {
            args += "%" + argument.getName() + " = alloca " + convertType(argument.getType()) + "\n"
                + "store " + convertType(argument.getType()) + " %." + argument.getName() + ", " + convertType(argument.getType()) + "* %" + argument.getName() + "\n";
        }

        return args;
    }

    // returns if type is primitive (int,boolean), or pointer(int[],object)
    public Boolean isPrimitive(String type)
    {
        if(type.equals("i32") || type.equals("i1"))
            return true;
        else
            return false;
    }

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n)
    {
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);

        return HeadCodeBuffer + BodyCodeBuffer;
    }

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
        InMethod = true;
        emit("define i32 @main() {\n");

        String _ret=null;
        n.f0.accept(this);

        IsVariable = false;
        n.f1.accept(this);
        IsVariable = true;

        CurrentClassName = n.f1.f0.tokenImage;
        CurrentMethodName = "main";

        // make vtable 
        emitVTABLE("@." + n.f1.f0.tokenImage + "_vtable = global [0 x i8*] []\n");

        n.f14.accept(this);
        n.f15.accept(this);

        emit("ret i32 0\n"
              + "}\n");

        InMethod = false;

        return _ret;
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
        CurrentMethodName = null;

        String _ret=null;
        n.f0.accept(this);

        IsVariable = false;
        n.f1.accept(this);
        IsVariable = true;

        CurrentClassName = n.f1.f0.tokenImage;

        // make vtable 
        Set<Integer> visitedOffsets = new HashSet<Integer>();
        emitVTABLE("@." + n.f1.f0.tokenImage + "_vtable = global [" + ClassMap.get(n.f1.f0.tokenImage).getMethodMap().size() + " x i8*] [");
        for (MethodInfo method : ClassMap.get(n.f1.f0.tokenImage).getMethodMap().values())
        {
            // if overriden don't include
            if(visitedOffsets.contains(method.getOffset()))
                continue;

            emitVTABLE(" i8* bitcast (" + convertType(method.getRetType()) 
            + " (i8*" 
            + convertArgumentsTypes(ClassMap.get(CurrentClassName).getMethodMap().get(method.getMethName()).getArgMap()) 
            + ")* "
            + "@" + CurrentClassName + "." + method.getMethName() + " to i8*"
            + "),");

            // mark as visited
            visitedOffsets.add(method.getOffset());
        }
        // delete extra "," character
        if(HeadCodeBuffer.substring(HeadCodeBuffer.length()-1).equals(","))
            HeadCodeBuffer = HeadCodeBuffer.substring(0,HeadCodeBuffer.length()-1);
        emitVTABLE("]\n");
        ///////////////

        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        return _ret;
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
        CurrentMethodName = null;

        String _ret=null;
        n.f0.accept(this);

        IsVariable = false;
        n.f1.accept(this);
        IsVariable = true;

        CurrentClassName = n.f1.f0.tokenImage;

        // make vtable 
        Set<Integer> visitedOffsets = new HashSet<Integer>();
        emitVTABLE("@." + n.f1.f0.tokenImage + "_vtable = global [" + ClassMap.get(n.f1.f0.tokenImage).getNumOfMethods() + " x i8*] [");
        ClassInfo currentClass = ClassMap.get(CurrentClassName);
        while(currentClass != null)
        {
            for (MethodInfo method : currentClass.getMethodMap().values())
            {
                // if overriden don't include
                if(visitedOffsets.contains(method.getOffset()))
                    continue;

                emitVTABLE(" i8* bitcast (" + convertType(method.getRetType()) 
                + "(i8*" 
                + convertArgumentsTypes(currentClass.getMethodMap().get(method.getMethName()).getArgMap()) 
                + ")* "
                + "@" + currentClass.getClassName() + "." + method.getMethName() + " to i8*"
                + "),");

                // mark as visited
                visitedOffsets.add(method.getOffset());
            }
            currentClass = currentClass.getBaseClass();
        }
        // delete extra "," character
        //HeadCodeBuffer = HeadCodeBuffer.substring(0,HeadCodeBuffer.length()-1);
        if(HeadCodeBuffer.substring(HeadCodeBuffer.length()-1).equals(","))
            HeadCodeBuffer = HeadCodeBuffer.substring(0,HeadCodeBuffer.length()-1);
        emitVTABLE("]\n");
        ///////////////
        
        n.f2.accept(this);
        //n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        return _ret;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n)
    {
        IsVariable = false;
        n.f0.accept(this);
        IsVariable = true;

        String register = n.f1.accept(this);
        
        n.f2.accept(this);

        if(InMethod)
        {
            String type = convertType(ClassMap.get(CurrentClassName).getMethodMap().get(CurrentMethodName).getVarType(n.f1.f0.tokenImage, ClassMap));
            emit(register + " = alloca " + type + "\n");
        }

        return register;
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
        InMethod = true;

        IsVariable = false;
        n.f1.accept(this);
        IsVariable = false;
        n.f2.accept(this);
        IsVariable = true;

        String methName = n.f2.f0.tokenImage;
        CurrentMethodName = methName;
        String llvmMethName = "@" + CurrentClassName + "." + methName;
        String llvmMethRetType = convertType(ClassMap.get(CurrentClassName).getMethodType(methName));
        emit("define " + llvmMethRetType + " " 
                       + llvmMethName 
                       + "(i8* %this"
                       + convertArguments(ClassMap.get(CurrentClassName).getMethodByName(methName).getArgMap())
                       + ") {\n" );
        emit(copyArguments(ClassMap.get(CurrentClassName).getMethodByName(methName).getArgMap()));
        
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);
        
        String returnRegister = n.f10.accept(this);
        String returnRegister2 = new_temp();
        emit(returnRegister2 + " = load " + llvmMethRetType + ", " + llvmMethRetType + "* " + returnRegister + "\n"
            + "ret " + llvmMethRetType  + " " + returnRegister2 + "\n"
            + "}\n");
        
        n.f11.accept(this);
        n.f12.accept(this);

        InMethod = false;

        // no need to return something, no one needs it
        return null;
    }


    /* LEVEL 0 -- Assign result register of right value and return it */
    /* returns pointer (to register) */

    /**
     * f0 -> IntegerLiteral() 
     * | TrueLiteral() 
     * | FalseLiteral() 
     * | Identifier() 
     * | ThisExpression() 
     * | ArrayAllocationExpression() 
     * | AllocationExpression() 
     * | BracketExpression()
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
        IsPointer = false;
        IsPointerType = "i32";
        CurrentObjectType = "int";

        n.f0.accept(this);

        String register = new_temp();
        String code = register + " = alloca i32\n"
                    + "store i32 " + n.f0.tokenImage + ", i32* " + register + "\n";
        emit(code);

        return register;
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n)
    {
        IsPointer = false;
        IsPointerType = "i1";
        CurrentObjectType = "boolean";

        n.f0.accept(this);

        String register = new_temp();
        String code = register + " = alloca i1\n"
                    + "store i1 1, i1* " + register + "\n";
        emit(code);

        return register;
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n)
    {
        IsPointer = false;
        IsPointerType = "i1";
        CurrentObjectType = "boolean";

        n.f0.accept(this);

        String register = new_temp();
        String code = register + " = alloca i1\n"
                    + "store i1 0, i1* " + register + "\n";
        emit(code);

        return register;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n)
    {
        IsPointer = true;
        n.f0.accept(this);

        if(CurrentClassName != null && CurrentMethodName != null && IsVariable /*&& !CurrentMethodName.equals("main")*/)
        {
            CurrentObjectType = ClassMap.get(CurrentClassName).getMethodByName(CurrentMethodName).getVarType(n.f0.tokenImage, ClassMap);
            int offset = ClassMap.get(CurrentClassName).getOffset(n.f0.tokenImage, CurrentMethodName);
            if(offset != -1)
            {
                String obj = new_temp();
                String obj2 = new_temp();
                offset += 8;
                emit(obj + " = getelementptr i8, i8* %this, i32 " + offset + "\n");

                // get type
                String type = convertType(ClassMap.get(CurrentClassName).getVarType(n.f0.tokenImage));
                emit(obj2 + " = bitcast i8* " + obj + " to " + type + "*\n");

                return obj2;
            }    
        }

        return "%" + n.f0.tokenImage;
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n)
    {
        IsPointer = true;
        n.f0.accept(this);
        CurrentObjectType = CurrentClassName;
        return "%this";
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
        IsPointer = true;
        CurrentObjectType = "int[]";
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        
        String register1 = n.f3.accept(this);   // register that has the value of the expression
        String register1Loaded = new_temp();
        
        n.f4.accept(this);

        String register2 = new_temp();
        String register3 = new_temp();
        String register4 = new_temp();
        String result = new_temp();

        String code = register1Loaded + " = load i32, i32* " + register1 + "\n"
                    + register4 + " = add i32 " + register1Loaded + ", 1\n"
                    + register2 + " = call i8* @calloc(i32 " + register4 + ", i32 4)\n"     // keep 1 more place for size
                    + register3 + " = bitcast i8* " + register2 + " to i32*\n"               // i8* -> i32*
                    + "store i32 " + register4 + ", i32* " + register3 + "\n"              // store length
                    + result + " = alloca i32*\n"
                    + "store i32* " + register3 + ", i32** " + result + "\n";

        emit(code);

        return result;
    }

    /**
     * f0 -> "new" 
     * f1 -> Identifier() 
     * f2 -> "(" 
     * f3 -> ")"
     */
    public String visit(AllocationExpression n)
    {
        IsPointer = true;
        n.f0.accept(this);
        
        IsVariable = false;
        n.f1.accept(this);
        IsVariable = true;

        CurrentObjectType = n.f1.f0.tokenImage;

        n.f2.accept(this);
        n.f3.accept(this);

        int sz = ClassMap.get(n.f1.f0.tokenImage).getNumOfMethods();

        String register1 = new_temp();
        String register2 = new_temp();
        String register3 = new_temp();
        String result = new_temp();

        String code = register1 + " = call i8* @calloc(i32 1, i32 " + (ClassMap.get(n.f1.f0.tokenImage).getSumOfSizesOfClassFields() + 8) + ")\n" // 8 for vtable + all field sizes
	                + register2 + " = bitcast i8* " + register1 + " to i8***\n"
	                + register3 + " = getelementptr [" + sz + " x i8*], [" + sz + " x i8*]* @." + CurrentObjectType + "_vtable, i32 0, i32 0\n"
                    + "store i8** " + register3 + ", i8*** " + register2 + "\n"
                    + result + " = alloca i8*\n"
                    + "store i8* " + register1 + ", i8** " + result + "\n";

        emit(code);

        return result;
    }

    /**
     * f0 -> "(" 
     * f1 -> Expression() 
     * f2 -> ")"
     */
    public String visit(BracketExpression n)
    {
        n.f0.accept(this);
        
        String temp = n.f1.accept(this);
        
        n.f2.accept(this);
        return temp;
    }

    /* LEVEL 1 -- Calculate sub-expressions to registers and compose whole expressions */
    /* returns pointer (to register)*/

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
        CurrentObjectType = "boolean";

        String label1 = new_label();
        String label2 = new_label();
        String labelret = new_label();

        String register1 = n.f0.accept(this);
        String register11 = new_temp();
        n.f1.accept(this);
        String register2 = n.f2.accept(this);
        String register22 = new_temp();

        // will hold result
        String register3 = new_temp();
        String register4 = new_temp();
        String register5 = new_temp();
        String result = new_temp();
        
        emit(register11 + " = load i1, i1* " + register1 + "\n"
            + register22 + " = load i1, i1* " + register2 + "\n"
            + "br i1 " + register11 + ", label %" + label1 + ", label %" + label2 + "\n"
            + label1 + ":\n"
            + "    " + register3 + " = add i1 " + register22 + ", 0\n"
            + "    br label %" + labelret + "\n"
            + label2 + ":\n"
            + "    " + register4 + " = add i1 0, 0\n"
            + "    br label %" + labelret + "\n"
            + labelret + ":\n"
            + "    " + register5 + " = phi i1 [" + register3 + ", %" + label1 + "], [" + register4 + ", %" + label2 + "]\n"
            + "    " + result + " = alloca i1\n"
            + "    store i1 " + register5 + ", i1* " + result + "\n");
        
        return result;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n)
    {
        CurrentObjectType = "boolean";

        String register1 = n.f0.accept(this);
        String register11 = new_temp();
        n.f1.accept(this);
        String register2 = n.f2.accept(this);
        String register22 = new_temp();

        String register3 = new_temp();

        String result = new_temp();

        emit(register11 + " = load i32, i32* " + register1 + "\n"
            + register22 + " = load i32, i32* " + register2 + "\n"
            + register3 + " = icmp slt i32 " + register11 + ", " + register22 + "\n"
            + result + " = alloca i1\n"
            + "store i1 " + register3 + ", i1* " + result + "\n");

        return result;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n)
    {
        CurrentObjectType = "int";

        String register1 = n.f0.accept(this);
        String register11 = new_temp();
        n.f1.accept(this);
        String register2 = n.f2.accept(this);
        String register22 = new_temp();

        String register3 = new_temp();

        String result = new_temp();

        emit(register11 + " = load i32, i32* " + register1 + "\n"
            + register22 + " = load i32, i32* " + register2 + "\n"
            + register3 + " = add i32 " + register11 + ", " + register22 + "\n"
            + result + " = alloca i32\n"
            + "store i32 " + register3 + ", i32* " + result + "\n");

        return result;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n)
    {
        CurrentObjectType = "int";

        String register1 = n.f0.accept(this);
        String register11 = new_temp();
        n.f1.accept(this);
        String register2 = n.f2.accept(this);
        String register22 = new_temp();

        String register3 = new_temp();

        String result = new_temp();

        emit(register11 + " = load i32, i32* " + register1 + "\n"
            + register22 + " = load i32, i32* " + register2 + "\n"
            + register3 + " = sub i32 " + register11 + ", " + register22 + "\n"
            + result + " = alloca i32\n"
            + "store i32 " + register3 + ", i32* " + result + "\n");

        return result;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n)
    {
        CurrentObjectType = "int";

        String register1 = n.f0.accept(this);
        String register11 = new_temp();
        n.f1.accept(this);
        String register2 = n.f2.accept(this);
        String register22 = new_temp();

        String register3 = new_temp();

        String result = new_temp();

        emit(register11 + " = load i32, i32* " + register1 + "\n"
            + register22 + " = load i32, i32* " + register2 + "\n"
            + register3 + " = mul i32 " + register11 + ", " + register22 + "\n"
            + result + " = alloca i32\n"
            + "store i32 " + register3 + ", i32* " + result + "\n");

        return result;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n)
    {
        CurrentObjectType = "int";

        String register1 = n.f0.accept(this);
        String register11 = new_temp();
        n.f1.accept(this);
        String register2 = n.f2.accept(this);
        String register22 = new_temp();
        n.f3.accept(this);

        String size = new_temp();
        String sz = new_temp();
        String register3 = new_temp();
        String result = new_temp();
        String offset = new_temp();

        String labelOut = new_label();
        String labelIn = new_label();
        String labelret = new_label();

        emit(register11 + " = load i32*, i32** " + register1 + "\n"
            + size + " = getelementptr i32, i32* " + register11 + ", i32  0\n"      // get array size
            + sz + " = load i32, i32* " + size + "\n"
            + register22 + " = load i32, i32* " + register2 + "\n"
            + register3 + " = icmp slt i32 " + sz + ", " + register22 + "\n"   // check for oob
            + "br i1 " + register3 + ", label %" + labelOut + ", label %" + labelIn + "\n"
            + labelOut + ":\n"
            + "    call void @throw_oob()\n"
            + "    br label %" + labelret + "\n"
            + labelIn + ":\n"
            + "    " + offset + " = add i32 " + register22 + ", 1\n"
            + "    " + result + " = getelementptr i32, i32* " + register11 + ", i32  " + offset + "\n"
            + "    br label %" + labelret + "\n"
            + labelret + ":\n");

        return result;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n)
    {
        String register1 = n.f0.accept(this);
        String register11 = new_temp();
        n.f1.accept(this);
        
        n.f2.accept(this);

        String result = new_temp();

        CurrentObjectType = "int";

        emit(register11 + " = load i32*, i32** " + register1 + "\n"
            + result + " = getelementptr i32, i32* " + register11 + ", i32 0\n");

        return result;
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
        String r0 = new_temp();
        String r1 = new_temp();
        String r2 = new_temp();
        String r3 = new_temp();
        String r4 = new_temp();
        String r5 = new_temp();

        IsVariable = true;
        String register1 = n.f0.accept(this);
        IsVariable = false;
        String register1Loaded = new_temp();

        String calledClass = CurrentObjectType;

        n.f1.accept(this);

        IsVariable = false;
        n.f2.accept(this);
        IsVariable = true;

        // from CurrentObjectType get method
        MethodInfo methodCalled = ClassMap.get(calledClass).getMethodByName(n.f2.f0.tokenImage);

        n.f3.accept(this);
        String arguments = n.f4.accept(this);
        n.f5.accept(this);

        int offset = methodCalled.getOffset()/8; 
        String methodRetType = convertType(methodCalled.getRetType());
        String methodArguments = convertArgumentsTypes(methodCalled.getArgMap());

        String result = new_temp();
        
        if(register1.equals("%this"))
            register1Loaded = register1;
        else
            emit(register1Loaded + " = load i8*, i8** " + register1 + "\n");

        CurrentObjectType = methodCalled.getRetType();

        emit(r0 + " = bitcast i8* " + register1Loaded + " to i8***\n"
            + r1 + " = load i8**, i8*** " + r0 + "\n"
            + r2 + " = getelementptr i8*, i8** " + r1 + ", i32 " + offset + "\n"
            + r3 + " = load i8*, i8** " + r2 + "\n"
            + r4 + " = bitcast i8* " + r3 + " to " + methodRetType + " (i8*" + methodArguments + ")*\n"
            + r5 + " = call " + methodRetType + " " + r4 + "(i8* " + register1Loaded + (arguments != null ? ", " + arguments : "") + ")\n"
            + result + " = alloca " + methodRetType + "\n"
            + "store " + methodRetType + " " + r5 + ", " + methodRetType + "* " + result + "\n");
        //%_1 = load i8**, i8*** %_0
	    //%_2 = getelementptr i8*, i8** %_1, i32 0
        //%_3 = load i8*, i8** %_2
        //%_4 = bitcast i8* %_3 to i32 (i8*,i32)*
        //%_5 = call i32 %_4(i8* %_0, i32 10)

        return result;

    }

    /**
     * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n)
    {
        String expr1 = n.f0.accept(this);
        String type1 = convertType(CurrentObjectType);

        String argumentLoaded = new_temp();
        if(!expr1.equals("%this"))
            emit(argumentLoaded + " = load " + type1 + ", " + type1 + "* " + expr1 + "\n"); // load argument to pass it to method
        else
            argumentLoaded = expr1;

        String expr2 = n.f1.accept(this);
        return type1 + " " + argumentLoaded + expr2;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
    */
    public String visit(ExpressionTail n)
    {
        String term = n.f0.accept(this);
        return term == null ? "" : term;
    }

    /**
     * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n)
    {
        n.f0.accept(this);
        String expr = n.f1.accept(this);

        String type = convertType(CurrentObjectType);
        String argumentLoaded = new_temp();
        emit(argumentLoaded + " = load " + type + ", " + type + "* " + expr + "\n"); // load argument to pass it to method

        return "," + type + " " + argumentLoaded;
    }

    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public String visit(Type n)
    {
        IsVariable = false;
        String type = n.f0.accept(this);
        IsVariable = true;

        return type;
    }

    /**
     * f0 -> "!" 
     * f1 -> Clause()
     */
    public String visit(NotExpression n)
    {
        CurrentObjectType = "boolean";

        n.f0.accept(this);
        
        String register = n.f1.accept(this);
        String registerLoaded = new_temp();

        String label1 = new_label();
        String label2 = new_label();
        String labelret = new_label();

        String register2 = new_temp();
        String register3 = new_temp();
        String register4 = new_temp();
        String result = new_temp();
        
        String code = registerLoaded + " = load i1, i1* " + register + "\n"
                    + "br i1 " + registerLoaded + ", label %" + label1 + ", label %" + label2 + "\n"
                    + label1 + ":\n"
                    + "    " + register2 + " = add i1 0, 0\n"
                    + "    br label %" + labelret + "\n"
                    + label2 + ":\n"
                    + "    " + register3 + " = add i1 0, 1\n"
                    + "    br label %" + labelret + "\n"
                    + labelret + ":\n"
                    + "    " + register4 + " = phi i1 [" + register2 + ", %" + label1 + "], [" + register3 + ", %" + label2 + "]\n"
                    + result + " = alloca i1\n"
                    + "    store i1 " + register4 + ", i1* " + result;

        emit(code);

        return result;
    }

    /**
     * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    public String visit(Clause n)
    {
        return n.f0.accept(this);
    }

    /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
    public String visit(Statement n)
    {
        return n.f0.accept(this);
    }

    /**
     * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
    public String visit(Block n)
    {
        n.f0.accept(this);
        String register = n.f1.accept(this);
        n.f2.accept(this);

        return register;
    }

    /**
     * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n)
    {
        String register1 = n.f0.accept(this);
        n.f1.accept(this);

        String register2 = n.f2.accept(this);
        
        n.f3.accept(this);
        
        String type = convertType(ClassMap.get(CurrentClassName).getMethodByName(CurrentMethodName).getVarType(n.f0.f0.tokenImage, ClassMap));
        String temp = new_temp();

        if(!register2.equals("%this"))
            emit(temp + " = load " + type + ", " + type + "* " + register2 + "\n");
        else
            temp = register2;
        
        emit("store " + type + " " + temp + ", " + type + "* " + register1 + "\n");

        return null;
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
        String register1 = n.f0.accept(this);
        String register11 = new_temp();
        n.f1.accept(this);
        String register2 = n.f2.accept(this);
        String register22 = new_temp();
        n.f3.accept(this);
        n.f4.accept(this);
        String exprresult = n.f5.accept(this);
        String exprresultLoaded = new_temp();
        n.f6.accept(this);

        String size = new_temp();
        String register3 = new_temp();
        String result = new_temp();
        String register5 = new_temp();

        String labelOut = new_label();
        String labelIn = new_label();
        String labelret = new_label();

        emit(register11 + " = load i32*, i32** " + register1 + "\n"
            + register22 + " = load i32, i32* " + register2 + "\n"
            + size + " = load i32, i32* " + register11 + "\n"      // get array size
            + register3 + " = icmp slt i32 " + size + ", " + register22 + "\n"   // check for oob
            + "br i1 " + register3 + ", label %" + labelOut + ", label %" + labelIn + "\n"
            + labelOut + ":\n"
            + "    call void @throw_oob()\n"
            + "    br label %" + labelret + "\n"
            + labelIn + ":\n"
            + "    " + register5 + " = add i32 " + register22 + ", 1\n"
            + "    " + result + " = getelementptr i32, i32* " + register11 + ", i32  " + register5 + "\n"
            + "    " + exprresultLoaded + " = load i32, i32* " + exprresult + "\n"
            + "    store i32 " + exprresultLoaded + ", i32* " + result + "\n"
            + "    br label %" + labelret + "\n"
            + labelret + ":\n");

        return null;

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
        String iflabel = new_label();
        String elselabel = new_label();
        String finallabel = new_label();

        n.f0.accept(this);
        n.f1.accept(this);
        String register = n.f2.accept(this);
        String registerLoaded = new_temp();
        emit(registerLoaded + " = load i1, i1* " + register + "\n"
            + "br i1 " + registerLoaded + ", label %" + iflabel + ", label %" + elselabel + "\n");

        n.f3.accept(this);
        
        // statement in if clause
        emit(iflabel + ":\n");
        n.f4.accept(this);
        emit("br label %" + finallabel + "\n");
        
        n.f5.accept(this);
        
        // statement in else clause
        emit(elselabel + ":\n");
        n.f6.accept(this);

        emit("br label %" + finallabel + "\n"
            + finallabel + ":\n");

        return null;
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
        String label = new_label();
        String labelin = new_label();
        String labelout = new_label();

        n.f0.accept(this);
        n.f1.accept(this);

        emit("br label %" + label + "\n"
            + label + ":\n");
        
        String expr = n.f2.accept(this);    // put here: to iterate every time
        String exprLoaded = new_temp();
        
        emit(exprLoaded + " = load i1, i1* " + expr + "\n"
            + "br i1 " + exprLoaded + ", label %" + labelin + ", label %" + labelout + "\n"
            + labelin + ":\n");
        n.f3.accept(this);
        n.f4.accept(this);
        emit("br label %" + label + "\n");

        emit(labelout + ":\n");

        return null;
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
        n.f0.accept(this);
        n.f1.accept(this);
        String expr = n.f2.accept(this);
        String exprLoaded = new_temp();
        n.f3.accept(this);
        n.f4.accept(this);

        if(CurrentObjectType.equals("int"))
            emit(exprLoaded + " = load i32, i32* " + expr + "\n");
        else
        {
            String exprLoaded2 = new_temp();
            emit(exprLoaded2 + " = load i1, i1* " + expr + "\n"
                + exprLoaded + " = zext i1 " + exprLoaded2 + " to i32\n");
        }

        emit("call void (i32) @print_int(i32 " + exprLoaded + ")\n");

        return null;
    }
}
