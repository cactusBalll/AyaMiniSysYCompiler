import backend.BakaAllocator;
import backend.CodeGen;
import backend.MCUnit;
import backend.coloralloc.ColorAllocDriver;
import exceptions.BackEndErr;
import exceptions.IRGenErr;
import exceptions.LexErr;
import exceptions.ParseErr;
import frontend.*;
import frontend.ast.TreeNode;
import ir.opt.*;
import ir.value.CompUnit;

import java.io.*;
import java.util.List;

public class Compiler {
    public static void main(String[] argv) throws Exception {

        try {
            emitMIPS(argv[0],argv[1]);
            //emitMIPSSubmit();
        } catch (LexErr|ParseErr|IRGenErr e) {
            System.out.println("error occurred");
        }
    }

    public static void emitMIPSSubmit() throws IOException,LexErr,ParseErr,IRGenErr, BackEndErr {
        final String srcFile = "testfile.txt";
        final String IRTarget = "ir.txt";
        final String MIPSTarget = "mips.txt";
        final String errTarget = "error.txt";
        StringBuilder src = new StringBuilder();
        try (Reader reader = new FileReader(srcFile)) {
            int c;
            while ((c = reader.read()) != -1) {
                src.append((char)c);
            }
        }
        PrintStream out;

        Scanner scanner = new Scanner(src.toString());
        List<Token> tokens = scanner.run();

        Parser parser = new Parser(tokens);
        TreeNode root = parser.run();
        IRGen irGen = new IRGen(root);
        CompUnit compUnit = irGen.run();
        if (ErrorHandler.getInstance().compileError()) {
            out = new PrintStream(errTarget);
            System.setOut(out);
            System.out.print(ErrorHandler.getInstance());
        } else {
            compUnit.fullMaintain();
            compUnit.setValueName();
            new PrecSucc().run(compUnit);
            new SimplifyG().run(compUnit);

            new PrecSucc().run(compUnit);
            compUnit.fullMaintain();
            new BBInfo().run(compUnit);
            new Mem2Reg().run(compUnit);

            compUnit.maintainUser();
            new SimpleCP().run(compUnit);

            compUnit.fullMaintain();
            new PrecSucc().run(compUnit);
            new BBInfo().run(compUnit);
            new GVNGCM().run(compUnit);

            compUnit.setValueName();
            CodeGen codeGen = new CodeGen(compUnit);
            MCUnit mcUnit = codeGen.run();
            //BakaAllocator bakaAllocator = new BakaAllocator(mcUnit);
            //bakaAllocator.run();

            ColorAllocDriver.run(mcUnit);
            out = new PrintStream(MIPSTarget);
            System.setOut(out);
            System.out.print(mcUnit);
        }
    }
    public static void emitMIPS(String srcFile, String MIPSTarget) throws IOException,LexErr,ParseErr,IRGenErr, BackEndErr {
        //final String srcFile = "testfile.txt";
        final String IRTarget = "ir.txt";
        final String errTarget = "error.txt";
        StringBuilder src = new StringBuilder();
        try (Reader reader = new FileReader(srcFile)) {
            int c;
            while ((c = reader.read()) != -1) {
                src.append((char)c);
            }
        }
        PrintStream out = new PrintStream(IRTarget);
        System.setOut(out);

        Scanner scanner = new Scanner(src.toString());
        List<Token> tokens = scanner.run();

        Parser parser = new Parser(tokens);
        TreeNode root = parser.run();
        IRGen irGen = new IRGen(root);
        CompUnit compUnit = irGen.run();
        if (ErrorHandler.getInstance().compileError()) {
            out = new PrintStream(errTarget);
            System.setOut(out);
            System.out.print(ErrorHandler.getInstance());
        } else {
            compUnit.fullMaintain();
            compUnit.setValueName();
            new PrecSucc().run(compUnit);
            new SimplifyG().run(compUnit);

            new PrecSucc().run(compUnit);
            compUnit.fullMaintain();
            new BBInfo().run(compUnit);
            new Mem2Reg().run(compUnit);

            compUnit.maintainUser();
            new SimpleCP().run(compUnit);

            compUnit.fullMaintain();
            new PrecSucc().run(compUnit);
            new BBInfo().run(compUnit);
            new GVNGCM().run(compUnit);

            compUnit.setValueName();
            System.out.print(compUnit);

            CodeGen codeGen = new CodeGen(compUnit);
            MCUnit mcUnit = codeGen.run();
            //BakaAllocator bakaAllocator = new BakaAllocator(mcUnit);
            //bakaAllocator.run();
            ColorAllocDriver.run(mcUnit);
            out = new PrintStream(MIPSTarget);
            System.setOut(out);
            System.out.print(mcUnit);
        }
    }
    public static void checkErr() throws IOException, LexErr, ParseErr, IRGenErr {
        final String srcFile = "testfile.txt";
        final String target = "ir.txt";
        final String errTarget = "error.txt";
        StringBuilder src = new StringBuilder();
        try (Reader reader = new FileReader(srcFile)) {
            int c;
            while ((c = reader.read()) != -1) {
                src.append((char)c);
            }
        }
        PrintStream out;
        Scanner scanner = new Scanner(src.toString());
        List<Token> tokens = scanner.run();

        Parser parser = new Parser(tokens);
        TreeNode root = parser.run();
        IRGen irGen = new IRGen(root);
        CompUnit compUnit = irGen.run();
        out = new PrintStream(errTarget);
        System.setOut(out);
        System.out.print(ErrorHandler.getInstance());

    }
    private static void emitIR() throws IOException, LexErr, ParseErr, IRGenErr {
        final String srcFile = "testfile.txt";
        final String target = "ir.txt";
        final String errTarget = "error.txt";
        StringBuilder src = new StringBuilder();
        try (Reader reader = new FileReader(srcFile)) {
            int c;
            while ((c = reader.read()) != -1) {
                src.append((char)c);
            }
        }
        PrintStream out = new PrintStream(target);
        System.setOut(out);

        Scanner scanner = new Scanner(src.toString());
        List<Token> tokens = scanner.run();

        Parser parser = new Parser(tokens);
        TreeNode root = parser.run();
        IRGen irGen = new IRGen(root);
        CompUnit compUnit = irGen.run();
        if (ErrorHandler.getInstance().compileError()) {
            out = new PrintStream(errTarget);
            System.setOut(out);
            System.out.print(ErrorHandler.getInstance());
        } else {
            compUnit.maintainUser();
            compUnit.maintainBBelong();
            compUnit.setValueName();
            new PrecSucc().run(compUnit);
            new SimplifyG().run(compUnit);
            new BBInfo().run(compUnit);
            new Mem2Reg().run(compUnit);
            compUnit.setValueName();
            System.out.print(compUnit);
        }
    }
    private static void runParser() throws IOException, LexErr, ParseErr {
        final String srcFile = "testfile.txt";
        final String target = "output.txt";
        StringBuilder src = new StringBuilder();
        try (Reader reader = new FileReader(srcFile)) {
            int c;
            while ((c = reader.read()) != -1) {
                src.append((char)c);
            }
        }
        PrintStream out = new PrintStream(target);
        System.setOut(out);

        Scanner scanner = new Scanner(src.toString());
        List<Token> tokens = scanner.run();

        Parser parser = new Parser(tokens);
        TreeNode root = parser.run();

        TreeNode.postOrderPrint(root);
    }
    private static void runLexer() throws IOException, LexErr {
        final String srcFile = "testfile.txt";
        final String target = "output.txt";
        StringBuilder src = new StringBuilder();
        try (Reader reader = new FileReader(srcFile)) {
            int c;
            while ((c = reader.read()) != -1) {
                src.append((char)c);
            }
        }
        PrintStream out = new PrintStream(target);
        System.setOut(out);

        Scanner scanner = new Scanner(src.toString());
        List<Token> tokens = scanner.run();
        for (Token token :
                tokens) {
            System.out.println(token);
        }
    }
}
