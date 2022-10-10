package frontend;

import exceptions.IRGenErr;
import frontend.ast.NonTerminator;
import frontend.ast.Terminator;
import frontend.ast.TreeNode;
import ir.instruction.AllocInstr;
import ir.value.*;
import ty.FuncTy;
import ty.IntArrTy;
import ty.IntTy;
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

    public CompUnit run() throws IRGenErr{
        symbolTable.pushScope(); // global var scope

        return compUnit;
    }

    public void genDecl(NonTerminator decl, boolean onStack) throws IRGenErr {
        if (TreeNode.match(decl, 0, Token.Type.CONSTTK) != null) {
            for (int i = 2; i < decl.getChildSize(); i+=2) {
                assert decl.getChild(i) instanceof NonTerminator &&
                        ((NonTerminator) decl.getChild(i)).getType() == NonTerminator.Type.ConstDef;
                genDef((NonTerminator) decl.getChild(i), onStack, true);
            }
        } else {
            for (int i = 1; i < decl.getChildSize(); i+=2) {
                assert decl.getChild(i) instanceof NonTerminator &&
                        ((NonTerminator) decl.getChild(i)).getType() == NonTerminator.Type.VarDef;
                genDef((NonTerminator) decl.getChild(i), onStack, false);
            }
        }
    }

    public void genDef(NonTerminator def, boolean onStack, boolean isConst) throws IRGenErr {
        
    }



    class Evaluator{
        // todo: 编译时常量求值器
        public int evaluate(TreeNode expr) throws IRGenErr {
            assert expr instanceof NonTerminator;
            assert ((NonTerminator) expr).getType() == NonTerminator.Type.ConstExp;
            return evalAdd((NonTerminator) ((NonTerminator) expr).getChildren().get(0));
        }

        private int evalAdd(NonTerminator exp) throws IRGenErr {
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

        private int evalMul(NonTerminator exp) throws IRGenErr {
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

        private int evalUnary(NonTerminator exp) throws IRGenErr {
            if (exp.getChildren().get(0) instanceof Terminator) {
                Terminator op = (Terminator) exp.getChildren().get(0);
                if (op.getToken().getType() == Token.Type.NOT) {
                    return evalUnary((NonTerminator) exp.getChildren().get(1)) != 0 ? 0:1;
                } else if (op.getToken().getType() == Token.Type.MINU) {
                    return -evalUnary((NonTerminator) exp.getChildren().get(1));
                } else if (op.getToken().getType() == Token.Type.PLUS) {
                    return evalUnary((NonTerminator) exp.getChildren().get(1));
                }
            } else if(exp.getChildren().get(0) instanceof NonTerminator) {
                return evalPrimary((NonTerminator) exp.getChildren().get(0));
            }
            return 0;
        }
        private int evalPrimary(NonTerminator exp) throws IRGenErr {
            if (exp.getChildren().get(0) instanceof Terminator) {
                Terminator op = (Terminator) exp.getChildren().get(0);
                if (op.getToken().getType() == Token.Type.LPARENT) { // (Exp)
                    return evalAdd((NonTerminator) ((NonTerminator)exp.getChildren().get(1)).getChildren().get(0));
                } else if (op.getToken().getType() == Token.Type.INTCON) {
                    return Integer.parseInt(op.getToken().getText());
                }
            } else if(exp.getChildren().get(0) instanceof NonTerminator) {
                return evalLVal((NonTerminator) exp.getChildren().get(0));
            }
            return 0;
        }
        private int evalLVal(NonTerminator exp) throws IRGenErr {
            Token token = ((Terminator) exp.getChildren().get(0)).getToken();
            String name = token.toString();
            VarElem varElem = symbolTable.getVarOrGenErr(name, token.getLine());
            if (varElem != null) {

                if (exp.getChildren().size() == 1) {
                    if (varElem.getEn() != VarElem.En.Const) {
                        throw new IRGenErr(token.getLine());
                    }
                    Constant constant = varElem.getConstant();
                    return constant.getValue();
                } else if (exp.getChildren().size() == 4) { // ID[exp]
                    if (varElem.getEn() != VarElem.En.Alloc) {
                        throw new IRGenErr(token.getLine());
                    }

                    AllocInstr allocInstr = varElem.getAlloc();
                    Constant constant = allocInstr.getInitVal();
                    int idx = evalAdd((NonTerminator) ((NonTerminator)exp.getChildren().get(2)).getChildren().get(0));
                    if (!(constant.getTy() instanceof IntArrTy)) {
                        throw new IRGenErr(token.getLine());
                    }
                    return constant.getListValue().get(idx);
                } else if (exp.getChildren().size() == 7) { // ID[exp][exp]
                    if (varElem.getEn() != VarElem.En.Alloc) {
                        throw new IRGenErr(token.getLine());
                    }

                    AllocInstr allocInstr = varElem.getAlloc();
                    Constant constant = allocInstr.getInitVal();
                    int idx = evalAdd((NonTerminator) ((NonTerminator)exp.getChildren().get(2)).getChildren().get(0));
                    int idx2 = evalAdd((NonTerminator) ((NonTerminator)exp.getChildren().get(5)).getChildren().get(0));
                    if (!(constant.getTy() instanceof IntArrTy)) {
                        throw new IRGenErr(token.getLine());
                    }
                    return constant.getValue(idx,idx2);
                }

            }
            return 0;
        }
    }

    static class SymbolTable{
        List<Map<String, VarElem>> varTable = new ArrayList<>();

        public void putFunctionOrGenErr(String name, Function function, int line) {
            Map<String, VarElem> scope = varTable.get(varTable.size()-1);
            if (scope.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {
                scope.put(name, new VarElem(function));
            }
        }

        public void putVarOrGenErr(String name, AllocInstr allocInstr, int line) {
            Map<String, VarElem> scope = varTable.get(varTable.size()-1);
            if (scope.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {

                scope.put(name, new VarElem(allocInstr));
            }
        }

        public void putConstOrGenErr(String name, Constant constant, int line) {
            Map<String, VarElem> scope = varTable.get(varTable.size()-1);
            if (scope.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {
                scope.put(name, new VarElem(constant));
            }
        }

        public void putParamOrGenErr(String name, Param param, int line) {
            Map<String, VarElem> scope = varTable.get(varTable.size()-1);
            if (scope.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {
                scope.put(name, new VarElem(param));
            }
        }

        public void pushScope() {
            varTable.add(new HashMap<>());
        }

        public void popScope() {
            varTable.remove(varTable.size() - 1);
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

        public enum En{
            Alloc, //对于栈上变量
            Param, //参数
            Const, //Int常量在编译时会被全部替换
            Func
        }
        private final Object obj;
        private final En en;

        public VarElem(AllocInstr alloc) {
            obj = alloc;
            en = En.Alloc;
        }

        public VarElem(Param param) {
            obj = param;
            en = En.Param;
        }

        public VarElem(Constant constant) {
            obj = constant;
            en = En.Const;
        }

        public VarElem(Function function) {
            obj = function;
            en = En.Func;
        }

        public En getEn() {
            return en;
        }

        public AllocInstr getAlloc() {
            return (AllocInstr) obj;
        }

        public Param getParam() {
            return (Param) obj;
        }

        public Function getFunction() {
            return (Function) obj;
        }

        public Constant getConstant() {
            return (Constant) obj;
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
