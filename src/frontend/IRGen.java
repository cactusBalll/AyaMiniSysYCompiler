package frontend;

import frontend.ast.NonTerminator;
import frontend.ast.Terminator;
import frontend.ast.TreeNode;
import ir.instruction.AllocInstr;
import ir.value.CompUnit;
import ir.value.Constant;
import ir.value.Function;
import ir.value.Value;
import ty.FuncTy;
import ty.Ty;

import java.util.*;

public class IRGen {
    private final TreeNode root;
    private final CompUnit compUnit = new CompUnit();

    private final SymbolTable symbolTable = new SymbolTable();

    private final Evaluator evaluator = new Evaluator();

    private int loopDepth = 0; //循环嵌套深度

    private boolean hasReturn = false; //存在返回语句

    public IRGen(TreeNode root) {
        this.root = root;
    }

    public CompUnit run() {
        symbolTable.pushScope(); // global var scope

        return compUnit;
    }


    static class Evaluator{
        // todo: 编译时常量求值器
        public int evaluate(TreeNode expr) {
            assert expr instanceof NonTerminator;
            assert ((NonTerminator) expr).getType() == NonTerminator.Type.ConstExp;
            return evalAdd((NonTerminator) ((NonTerminator) expr).getChildren().get(0));
        }

        private int evalAdd(NonTerminator exp) {
            if (exp.getChildren().size() == 1) {
                return evalMul((NonTerminator) exp.getChildren().get(0));
            } else {
                Terminator op = (Terminator) exp.getChildren().get(1);
                if (op.getToken().getType() == Token.Type.PLUS) {
                    return evalAdd((NonTerminator) exp.getChildren().get(0)) +
                            evalMul((NonTerminator) exp.getChildren().get(2));
                } else if(op.getToken().getType() == Token.Type.MINU){
                    return evalAdd((NonTerminator) exp.getChildren().get(0)) -
                            evalMul((NonTerminator) exp.getChildren().get(2));
                }
            }
            return 0;
        }

        private int evalMul(NonTerminator exp) {
            if (exp.getChildren().size() == 1) {
                return evalUnary((NonTerminator) exp.getChildren().get(0));
            } else {
                Terminator op = (Terminator) exp.getChildren().get(1);
                if (op.getToken().getType() == Token.Type.MULT) {
                    return evalMul((NonTerminator) exp.getChildren().get(0)) *
                            evalUnary((NonTerminator) exp.getChildren().get(2));
                } else if(op.getToken().getType() == Token.Type.DIV){
                    return evalMul((NonTerminator) exp.getChildren().get(0)) /
                            evalUnary((NonTerminator) exp.getChildren().get(2));
                } else if(op.getToken().getType() == Token.Type.MOD){
                    return evalMul((NonTerminator) exp.getChildren().get(0)) %
                            evalUnary((NonTerminator) exp.getChildren().get(2));
                }
            }
            return 0;
        }

        private int evalUnary(NonTerminator exp) {
            return 0;
        }
        private int evalPrimary(NonTerminator exp) {
            return 0;
        }
        private int evalLVal(NonTerminator exp) {
            return 0;
        }
    }

    static class SymbolTable{
        List<Map<String, VarElem>> varTable = new ArrayList<>();
        Map<String, FuncElem> funcTable = new HashMap<>();

        public void putFunctionOrGenErr(String name, Function function, int line) {
            if (funcTable.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {
                funcTable.put(name, new FuncElem((FuncTy) function.getType(), function));
            }
        }

        public void putVarOrGenErr(String name, AllocInstr allocInstr, int line) {
            Map<String, VarElem> scope = varTable.get(varTable.size()-1);
            if (scope.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {

                scope.put(name, new VarElem(allocInstr, allocInstr.getAllocTy()));
            }
        }

        public void putConstOrGenErr(String name, Constant constant, int line) {
            Map<String, VarElem> scope = varTable.get(varTable.size()-1);
            if (scope.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {

                scope.put(name, new VarElem(constant, constant.getTy()));
            }
        }

        public void pushScope() {
            varTable.add(new HashMap<>());
        }

        public void popScope() {
            varTable.remove(varTable.size() - 1);
        }

        public Function getFuncOrGenErr(String name, int line) {
            if (!funcTable.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildUndefinedName(line));
                return null;
            } else {
                return funcTable.get(name).getFunction();
            }
        }

        public VarElem getVarOrGenErr(String name, int line) {
            int index = varTable.size() - 1;
            Map<String, VarElem> scope = varTable.get(varTable.size()-1);
            while (index >= 0) {
                if (scope.containsKey(name)) {
                    return scope.get(name);
                }
                index--;
                scope = varTable.get(index);
            }
            ErrorHandler.getInstance().addError(RequiredErr.buildUndefinedName(line));
            return null;
        }

    }
    // 符号表项
    private static class VarElem{
        private final AllocInstr alloc; // 分配的位置

        private final Constant constant; // 常量
        private final Ty ty;

        public VarElem(AllocInstr alloc, Ty ty) {
            this.alloc = alloc;
            this.ty = ty;
            constant = null;
        }

        public VarElem(Constant constant, Ty ty) {
            this.constant = constant;
            this.ty = ty;
            this.alloc = null;
        }
        public AllocInstr getAlloc() {
            return alloc;
        }

        public Ty getType() {
            return ty;
        }

        public boolean isConst() {
            return constant != null;
        }
    }

    private static class FuncElem{
        private final FuncTy ty;
        private final Function function;
        public FuncTy getTy() {
            return ty;
        }

        public FuncElem(FuncTy ty, Function function) {
            this.ty = ty;
            this.function = function;
        }

        public Function getFunction() {
            return function;
        }
    }
}
