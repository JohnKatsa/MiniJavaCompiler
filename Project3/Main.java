import syntaxtree.*;
import SymbolTableBuilderVisitor.*;
import TypeCheckerVisitor.*;
import LLVM_IRGeneratorVisitor.*;
import java.io.*;

class Main {
	public static void main (String [] args)
	{
		if(args.length < 1){
			System.err.println("Usage: java Driver <inputFile>");
			System.exit(1);
		}

		FileInputStream fis = null;
        
        int i = 0;
        while(args.length > i)
        {
            try
            {
                fis = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);
                System.err.println("Program parsed successfully.");
                
                Goal root = parser.Goal();

                /* Construct and Run Symbol Table Builder Visitor */
                SymbolTableBuilderVisitor stbVisitor = new SymbolTableBuilderVisitor();
                root.accept(stbVisitor);

                /* Construct and Run Type Checker Visitor */
                TypeCheckerVisitor tcVisitor = new TypeCheckerVisitor(stbVisitor.getClassMap());
                root.accept(tcVisitor);

                /* Construct and Run LLVM-IR Generator Visitor */
                LLVM_IRGeneratorVisitor llvmVisitor = new LLVM_IRGeneratorVisitor(stbVisitor.getClassMap());
                String output = root.accept(llvmVisitor);

                /* Write llvm-ir to new file */
                PrintWriter o = new PrintWriter(args[i].replace(".java", ".ll"));
                o.write(output);
                o.close();

                // function to print offsets
                //stbVisitor.printOffsets();
                
            }
            catch(ParseException ex)
            {
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex)
            {
                System.err.println(ex.getMessage());
            }
            catch(ExceptionInInitializerError ex)
            {
                System.out.println("Error : " + ex.getMessage());
            }
            finally
            {
                try
                {
                    if(fis != null) fis.close();
                }
                catch(IOException ex)
                {
                    System.err.println(ex.getMessage());
                }
            }
            
            // Onto next file
            i++;
        }
    }
}