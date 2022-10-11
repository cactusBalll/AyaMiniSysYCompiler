package frontend;

import exceptions.IRGenErr;
import exceptions.ParseErr;
import frontend.ast.NonTerminator;
import frontend.ast.Terminator;
import frontend.ast.TreeNode;
import ir.instruction.AllocInstr;
import ir.instruction.BinaryOp;
import ir.value.*;
import ty.FuncTy;
import ty.IntArrTy;
import ty.IntTy;
import ty.Ty;

import java.util.*;

public class IRGen {
    private final TreeNode root;
    private final IRGenManager irGenManager = new IRGenManager();

    private final SymbolTable symbolTable = new SymbolTable();

    private final Evaluator evaluator = new Evaluator();

    private int loopDepth = 0; //循环嵌套深度

    private boolean hasReturn = false; //存在返回语句

    public IRGen(TreeNode root) {
        this.root = root;
    }

    public CompUnit run() throws IRGenErr{
        symbolTable.pushScope(); // global var scope

        return irGenManager.getCompUnit();
    }

    private void genDecl(NonTerminator decl, boolean onStack) throws IRGenErr {
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

    private void genDef(NonTerminator def, boolean onStack, boolean isConst) throws IRGenErr {
        // 定位到等号
        int eqlIdx = 0;
        while (eqlIdx < def.getChildSize() &&
                TreeNode.match(def, eqlIdx, Token.Type.ASSIGN) == null) {
            eqlIdx++;
        }
        // 没有初始化
        if (eqlIdx == def.getChildSize()) {
            if (eqlIdx == 1) {
                Terminator t = (Terminator) def.getChild(0);
                if (symbolTable.containName(t.getToken().getText())) { // 名字重定义
                    ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(t.getToken().getLine()));
                } else {
                    if (isConst) {
                        throw new IRGenErr(t.getToken().getLine()); // const value not initialized
                    } else {
                        if (onStack) {
                            irGenManager.genStackData(IntTy.build(false), 0);
                        } else {
                            irGenManager.genStaticData(IntTy.build(false),
                                    new Constant(IntTy.build(false),0));
                        }
                    }
                }
            } else if (eqlIdx == 4) {

            }
        }
    }

    private void genInitVal(List<Value> values) {

    }

    private void genConstInitVal(List<Integer> constants) { // 编译期会全部求值

    }

    /**
     * 生成表达式，相当于Evaluator的变量版本
     * @param exp
     */
    private Value genExp(NonTerminator exp) throws IRGenErr {
        return genAdd((NonTerminator) exp.getChild(0));
    }

    private Value genAdd(NonTerminator exp) throws IRGenErr {
        if (exp.getChildren().size() == 1) {
            return genMul((NonTerminator) exp.getChildren().get(0));
        } else {
            Terminator op = (Terminator) exp.getChildren().get(1);
            Value left = genAdd((NonTerminator) exp.getChildren().get(0));
            Value right = genMul((NonTerminator) exp.getChildren().get(2));
            if (op.getToken().getType() == Token.Type.PLUS) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Add, left, right);
            } else if(op.getToken().getType() == Token.Type.MINU){
                return irGenManager.genBinaryOp(BinaryOp.OpType.Sub, left, right);
            }
        }
        return InitVal.buildInitVal(0);
    }

    private Value genMul(NonTerminator exp) throws IRGenErr {
        if (exp.getChildren().size() == 1) {
            return genUnary((NonTerminator) exp.getChildren().get(0));
        } else {
            Terminator op = (Terminator) exp.getChildren().get(1);
            Value left = genMul((NonTerminator) exp.getChildren().get(0));
            Value right = genUnary((NonTerminator) exp.getChildren().get(2));
            if (op.getToken().getType() == Token.Type.MULT) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Mul, left, right);
            } else if(op.getToken().getType() == Token.Type.DIV){
                return irGenManager.genBinaryOp(BinaryOp.OpType.Div, left, right);
            } else if(op.getToken().getType() == Token.Type.MOD){
                return irGenManager.genBinaryOp(BinaryOp.OpType.Mod, left, right);
            }
        }
        return InitVal.buildInitVal(0);
    }

    private Value genUnary(NonTerminator exp) throws IRGenErr {
        if (exp.getChild(0) instanceof NonTerminator &&
                ((NonTerminator) exp.getChild(0)).getType() == NonTerminator.Type.UnaryOp) {
            Terminator op = (Terminator) ((NonTerminator) exp.getChild(0)).getChild(0);
            if (op.getToken().getType() == Token.Type.NOT) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Nor,
                        InitVal.buildInitVal(0),
                        genLVal((NonTerminator) exp.getChild(1)));
            } else if (op.getToken().getType() == Token.Type.MINU) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Sub,
                        InitVal.buildInitVal(0),
                        genLVal((NonTerminator) exp.getChild(1)));
            } else if (op.getToken().getType() == Token.Type.PLUS) {
                return genLVal((NonTerminator) exp.getChild(1));
            }
        } else if (exp.getChild(0) instanceof Terminator) {
            Terminator t = (Terminator) exp.getChild(0);
             if (t.getToken().getType() == Token.Type.IDENFR &&
                    exp.getChild(1) instanceof Terminator &&
                    ((Terminator) exp.getChild(1)).getToken().getType() == Token.Type.LPARENT) {
                //func()
                VarElem func = symbolTable.getVarOrGenErr(t.getToken().getText(), t.getToken().getLine());
                if (func == null) {
                    return InitVal.buildInitVal(0);
                }
                if (exp.getChildSize() == 3) { // 无参调用
                    return irGenManager.genCallInstr(func.getFunction(), new ArrayList<>());
                } else {
                    List<Value> params = rParams((NonTerminator) exp.getChild(2));
                    return irGenManager.genCallInstr(func.getFunction(), params);
                }
            }
        } else {
            // primary
            return genPrimaryExp((NonTerminator) exp.getChild(0));
        }
        return InitVal.buildInitVal(0);
    }

    private Value genPrimaryExp(NonTerminator exp) throws IRGenErr {
        if (TreeNode.match(exp, 0, Token.Type.LPARENT) != null) {
            // (exp)
            return genExp((NonTerminator) exp.getChild(1));
        } else if (TreeNode.match(exp, 0, Token.Type.INTCON) != null) {
            String s = TreeNode.match(exp, 0, Token.Type.INTCON);
            return InitVal.buildInitVal(Integer.parseInt(s));
        } else {
            return genLVal((NonTerminator) exp.getChild(0));
        }
    }

    private Value genLVal(NonTerminator exp) throws IRGenErr {
        Token token = ((Terminator) exp.getChildren().get(0)).getToken();
        String name = token.toString();
        VarElem varElem = symbolTable.getVarOrGenErr(name, token.getLine());
        if (varElem != null) {
            if (exp.getChildren().size() == 1) {
                if (varElem.getEn() == VarElem.En.Const) {
                    return InitVal.buildInitVal(varElem.getConstant().getValue());
                } else if (varElem.getEn() == VarElem.En.Alloc) {
                    return irGenManager.genLoadInstr(varElem.getAlloc(), new ArrayList<>());
                } else if (varElem.getEn() == VarElem.En.Param) {
                    return varElem.getParam();
                } else {
                    return InitVal.buildInitVal(0);
                }
            } else if (exp.getChildren().size() == 4) { // ID[exp]
                AllocInstr allocInstr = varElem.getAlloc();
                Value idx = genExp((NonTerminator) exp.getChild(2));
                List<Value> idxx = new ArrayList<>();
                idxx.add(idx);
                return irGenManager.genLoadInstr(allocInstr, idxx);
            } else if (exp.getChildren().size() == 7) { // ID[exp][exp]
                AllocInstr allocInstr = varElem.getAlloc();
                Value idx0 = genExp((NonTerminator) exp.getChild(2));
                Value idx1 = genExp((NonTerminator) exp.getChild(5));
                List<Value> idxx = new ArrayList<>();
                idxx.add(idx0);
                idxx.add(idx1);
                return irGenManager.genLoadInstr(allocInstr, idxx);
            }
        }
        return InitVal.buildInitVal(0);
    }

    private List<Value> rParams(NonTerminator params) {
        assert params.getType() == NonTerminator.Type.FuncRParams;
        //todo: 函数实参
        return new ArrayList<>();
    }
    class Evaluator{
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
                int left = evalAdd((NonTerminator) exp.getChildren().get(0));
                int right = evalMul((NonTerminator) exp.getChildren().get(2));
                if (op.getToken().getType() == Token.Type.PLUS) {
                    return left + right;
                } else if(op.getToken().getType() == Token.Type.MINU){
                    return left - right;
                }
            }
            return 0;
        }

        private int evalMul(NonTerminator exp) throws IRGenErr {
            if (exp.getChildren().size() == 1) {
                return evalUnary((NonTerminator) exp.getChildren().get(0));
            } else {
                Terminator op = (Terminator) exp.getChildren().get(1);
                int left = evalMul((NonTerminator) exp.getChildren().get(0));
                int right = evalUnary((NonTerminator) exp.getChildren().get(2));
                if (op.getToken().getType() == Token.Type.MULT) {
                    return left *
                            right;
                } else if(op.getToken().getType() == Token.Type.DIV){
                    return left /
                            right;
                } else if(op.getToken().getType() == Token.Type.MOD){
                    return left %
                            right;
                }
            }
            return 0;
        }

        private int evalUnary(NonTerminator exp) throws IRGenErr {
            if (exp.getChild(0) instanceof NonTerminator &&
                    ((NonTerminator) exp.getChild(0)).getType() == NonTerminator.Type.UnaryOp) {
                Terminator op = (Terminator) ((NonTerminator) exp.getChild(0)).getChild(0);
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

        public boolean containName(String name) {
            int index = varTable.size() - 1;
            Map<String, VarElem> scope = varTable.get(varTable.size()-1);
            return scope.containsKey(name);
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


}
