import exceptions.IRGenErr;
import exceptions.LexErr;
import exceptions.ParseErr;
import frontend.*;
import frontend.ast.TreeNode;
import ir.value.CompUnit;

import java.io.*;
import java.util.List;

public class Compiler {
    public static void main(String[] argv) throws Exception {
        emitIR();
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
