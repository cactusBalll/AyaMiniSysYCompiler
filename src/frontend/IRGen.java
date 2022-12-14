package frontend;

import exceptions.IRGenErr;
import exceptions.ParseErr;
import frontend.ast.NonTerminator;
import frontend.ast.Terminator;
import frontend.ast.TreeNode;
import ir.instruction.AllocInstr;
import ir.instruction.ArrView;
import ir.instruction.BinaryOp;
import ir.instruction.CallInstr;
import ir.value.*;
import ty.*;

import java.util.*;

public class IRGen {
    private final TreeNode root;
    private final IRGenManager irGenManager = new IRGenManager();

    private final SymbolTable symbolTable = new SymbolTable();

    private final Evaluator evaluator = new Evaluator();

    private int loopDepth = 0; //循环嵌套深度

    private boolean hasReturn = false; //存在返回语句

    private boolean shouldReturn = false;

    public IRGen(TreeNode root) {
        this.root = root;
    }

    public CompUnit run() throws IRGenErr {
        symbolTable.pushScope(); // global var scope
        List<TreeNode> children = ((NonTerminator) root).getChildren();
        int i = 0;
        for (; i < children.size(); i++) {
            TreeNode node = children.get(i);
            NonTerminator nonTerminator = (NonTerminator) node;
            if (nonTerminator.getType() != NonTerminator.Type.ConstDecl &&
                    nonTerminator.getType() != NonTerminator.Type.VarDecl &&
                    nonTerminator.getType() != NonTerminator.Type.FuncDef) {
                break;
            }
            if (nonTerminator.getType() == NonTerminator.Type.ConstDecl ||
                    nonTerminator.getType() == NonTerminator.Type.VarDecl) {
                genDecl(nonTerminator, false);
            } else {
                genFunction(nonTerminator);
            }

        }
        /*for (; i < children.size(); i++) {
            TreeNode node = children.get(i);
            NonTerminator nonTerminator = (NonTerminator) node;
            if (nonTerminator.getType() != NonTerminator.Type.FuncDef) {
                break;
            }
            genFunction(nonTerminator);
        }*/

        TreeNode node = children.get(i);
        NonTerminator nonTerminator = (NonTerminator) node;
        genMain(nonTerminator);

        return irGenManager.getCompUnit();
    }

    private void genMain(NonTerminator func) throws IRGenErr {
        irGenManager.intoFunction("main", new ArrayList<>(),
                FuncTy.build(IntTy.build(false), new ArrayList<>()));
        shouldReturn = true;
        checkRetAtEnd((NonTerminator) func.getChild(func.getChildSize() - 1));
        NonTerminator block = (NonTerminator) func.getChild(func.getChildSize() - 1);
        genBlock(block);
        irGenManager.genJmp(irGenManager.getRetBB());
        irGenManager.outFunction();
    }
    private void genFunction(NonTerminator func) throws IRGenErr {
        Ty retTy;
        NonTerminator funcType = (NonTerminator) func.getChild(0);
        if (TreeNode.match(funcType, 0, Token.Type.VOIDTK) != null) {
            retTy = VoidTy.build();
            shouldReturn = false;
        } else {
            retTy = IntTy.build(false);
            shouldReturn = true;
            checkRetAtEnd((NonTerminator) func.getChild(func.getChildSize() - 1));
        }

        Terminator ident = (Terminator) func.getChild(1);
        if (symbolTable.containName(ident.getToken().getText())) {
            ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(ident.getToken().getLine()));
            return; //abort
        }
        symbolTable.pushScope(); // 进入函数作用域
        if (func.getChild(3) instanceof NonTerminator) {
            List<Param> params = genFuncFParams((NonTerminator) func.getChild(3));
            List<Ty> paramsTy = Param.extractType(params);
            Ty ty = FuncTy.build(retTy, paramsTy);
            Function function = irGenManager.intoFunction(ident.getToken().getText(), params, ty);
            Map<String, VarElem> scope = symbolTable.popScope(); // 先回到全局作用域
            symbolTable.putFunctionOrGenErr(ident.getToken().getText(), function, ident.getToken().getLine());
            symbolTable.pushScope(scope);
        } else { // 空参数列表
            Ty ty = FuncTy.build(retTy, new ArrayList<>());
            Function function = irGenManager.intoFunction(ident.getToken().getText(), new ArrayList<>(), ty);
            Map<String, VarElem> scope = symbolTable.popScope(); // 先回到全局作用域
            symbolTable.putFunctionOrGenErr(ident.getToken().getText(), function, ident.getToken().getLine());
            symbolTable.pushScope(scope);
        }

        NonTerminator block = (NonTerminator) func.getChild(func.getChildSize() - 1);
        genBlockNoScope(block);
        irGenManager.genJmp(irGenManager.getRetBB());
        symbolTable.popScope();
        irGenManager.outFunction();
    }

    private void checkRetAtEnd(NonTerminator block) {
        TreeNode last = block.getChild(block.getChildSize()-2);
        if (last instanceof NonTerminator) {
            NonTerminator nonTerminator = (NonTerminator) last;
            if (nonTerminator.getType() == NonTerminator.Type.Stmt &&
                    nonTerminator.getInnerType() == NonTerminator.InnerType.ReturnStmt) {
                return;
            }
        }
        ErrorHandler.getInstance().addError(
                RequiredErr.buildMissingRet(((Terminator)block.getChildAtLast()).getToken().getLine()) // }的行号
        );
    }

    private List<Param> genFuncFParams(NonTerminator fParams) throws IRGenErr {
        List<Param> ret = new ArrayList<>();
        for (int i = 0; i < fParams.getChildSize(); i += 2) {
            Param p = genFuncFParam((NonTerminator) fParams.getChild(i));
            ret.add(p);
        }
        return ret;
    }

    private Param genFuncFParam(NonTerminator fParam) throws IRGenErr {
        Terminator ident = (Terminator) fParam.getChild(1);
        Param p = new Param(IntTy.build(false));
        if (fParam.getChildSize() == 2) {
            p = new Param(IntTy.build(false));

        } else if (fParam.getChildSize() == 4) {
            p = new Param(IntArrTy.build(IntArrTy.ANY_DIM, false));
        } else if (fParam.getChildSize() == 7) {
            int dim2 = evaluator.evaluate(fParam.getChild(5));
            p = new Param(IntArrTy.build(IntArrTy.ANY_DIM, dim2, false));
        }
        symbolTable.putParamOrGenErr(ident.getToken().getText(), p, ident.getToken().getLine());
        return p;
    }
    private void genBlockNoScope(NonTerminator block) throws IRGenErr {
        for (int i = 1; i < block.getChildSize() - 1; i++) {
            NonTerminator declOrStmt = (NonTerminator) block.getChild(i);
            if (declOrStmt.getType() == NonTerminator.Type.VarDecl ||
                    declOrStmt.getType() == NonTerminator.Type.ConstDecl) {
                genDecl(declOrStmt, true);
            } else if (declOrStmt.getType() == NonTerminator.Type.Stmt) {
                boolean unreachable = genStmt(declOrStmt); // return 之后的语句不可能被执行
                if (unreachable) {
                    break;
                }
            }
        }
    }
    private void genBlock(NonTerminator block) throws IRGenErr {
        symbolTable.pushScope();
        genBlockNoScope(block);
        symbolTable.popScope();
    }

    private boolean genStmt(NonTerminator stmt) throws IRGenErr {
        if (stmt.getInnerType() == NonTerminator.InnerType.AssignStmt) {
            Value v = genExp((NonTerminator) stmt.getChild(2));
            genLLVal((NonTerminator) stmt.getChild(0), v);
        } else if (stmt.getInnerType() == NonTerminator.InnerType.ExpStmt) {
            genExp((NonTerminator) stmt.getChild(0));
        } else if (stmt.getInnerType() == NonTerminator.InnerType.BlockStmt) {
            genBlock((NonTerminator) stmt.getChild(0));

        } else if (stmt.getInnerType() == NonTerminator.InnerType.InputStmt) {
            Value v = irGenManager.genInput();
            genLLVal((NonTerminator) stmt.getChild(0), v);
        } else if (stmt.getInnerType() == NonTerminator.InnerType.PrintfStmt) {
            if (((Terminator) stmt.getChild(2)).getToken().isWrongFormat()) {
                return false; // format 格式已经错了，跳过这条语句
            }
            List<Value> params = new ArrayList<>();
            for (int i = 4; i < stmt.getChildSize() - 2; i+=2) {
                params.add(genExp((NonTerminator) stmt.getChild(i)));
            }
            String t = ((Terminator)stmt.getChild(2)).getToken().getText();
            genPrintf(
                    t.substring(1, t.length() - 1), //去掉前后引号
                    params,
                    ((Terminator) stmt.getChild(2)).getToken().getLine()
            );
        } else if (stmt.getInnerType() == NonTerminator.InnerType.IfStmt) {
            NonTerminator cond = (NonTerminator) stmt.getChild(2);
            NonTerminator stmt1 = (NonTerminator) stmt.getChild(4);

            if (stmt.getChildSize() == 5) { // 没有else
                BasicBlock stmt1BB = irGenManager.buildBB();
                BasicBlock exitBB = irGenManager.buildBB();
                BasicBlock nwBB = irGenManager.getNwBlock();
                BasicBlock bb = genLor((NonTerminator) cond.getChild(0), stmt1BB, exitBB);
                irGenManager.intoBB(nwBB);
                irGenManager.genJmp(bb);

                irGenManager.addBB(stmt1BB);
                genStmt(stmt1);
                irGenManager.genJmp(exitBB);

                irGenManager.addBB(exitBB); // 现在在出口块，继续生成
            } else {
                NonTerminator stmt2 = (NonTerminator) stmt.getChild(6);
                BasicBlock stmt1BB = irGenManager.buildBB();
                BasicBlock stmt2BB = irGenManager.buildBB();
                BasicBlock exitBB = irGenManager.buildBB();

                BasicBlock nwBB = irGenManager.getNwBlock();
                BasicBlock bb = genLor((NonTerminator) cond.getChild(0), stmt1BB, stmt2BB);
                irGenManager.intoBB(nwBB);
                irGenManager.genJmp(bb);

                irGenManager.addBB(stmt1BB);
                genStmt(stmt1);
                irGenManager.genJmp(exitBB);

                irGenManager.addBB(stmt2BB);
                genStmt(stmt2);
                irGenManager.genJmp(exitBB);

                irGenManager.addBB(exitBB); // 现在在出口块，继续生成
            }
        } else if (stmt.getInnerType() == NonTerminator.InnerType.WhileStmt) {
            NonTerminator cond = (NonTerminator) stmt.getChild(2);
            NonTerminator stmt1 = (NonTerminator) stmt.getChild(4);
            BasicBlock stmt1BB = new BasicBlock();
            BasicBlock exitBB = new BasicBlock();
            BasicBlock nwBB = irGenManager.getNwBlock();

            loopDepth++;

            BasicBlock bb = genLor((NonTerminator) cond.getChild(0), stmt1BB, exitBB);
            bb.loopDepth = loopDepth;
            irGenManager.intoLoop(bb, exitBB);

            irGenManager.intoBB(nwBB);
            irGenManager.genJmp(bb);

            irGenManager.addBB(stmt1BB);
            stmt1BB.loopDepth = loopDepth;
            genStmt(stmt1);
            irGenManager.genJmpAndReorderBB(bb);

            irGenManager.addBB(exitBB);

            irGenManager.outLoop();
            loopDepth--;
        } else if (stmt.getInnerType() == NonTerminator.InnerType.BreakStmt) {
            if (loopDepth > 0) {
                irGenManager.genJmp(irGenManager.getExitBB());
                irGenManager.getBB();
                return true;
            } else {
                ErrorHandler.getInstance().addError(
                        RequiredErr.buildCtrlOutOfLoop(
                                ((Terminator) stmt.getChild(0)).getToken().getLine()
                        )
                );
            }
        } else if (stmt.getInnerType() == NonTerminator.InnerType.ContinueStmt) {
            if (loopDepth > 0) {
                irGenManager.genJmp(irGenManager.getCondBB());
                irGenManager.getBB();
                return true;
            } else {
                ErrorHandler.getInstance().addError(
                        RequiredErr.buildCtrlOutOfLoop(
                                ((Terminator) stmt.getChild(0)).getToken().getLine()
                        )
                );
            }
        } else if (stmt.getInnerType() == NonTerminator.InnerType.ReturnStmt) {
            if (stmt.getChildSize() == 3) {
                if (!shouldReturn) {
                    ErrorHandler.getInstance().addError(
                            RequiredErr.buildShouldRetVoid(
                                    ((Terminator)stmt.getChild(0)).getToken().getLine()
                            )
                    );
                    return false;
                } else {
                    Value v = genExp((NonTerminator) stmt.getChild(1));
                    irGenManager.genReturn(v);
                }
            } else {
                irGenManager.genReturn(null);
            }
            return true;
        }
        return false;
    }

    private BasicBlock genLor(NonTerminator lor, BasicBlock succ, BasicBlock fail) throws IRGenErr {
        if (lor.getChildSize() == 1) {
            return genLAnd((NonTerminator) lor.getChild(0), succ, fail);
        } else {
            NonTerminator lor2 = (NonTerminator) lor.getChild(0);
            NonTerminator lAnd = (NonTerminator) lor.getChild(2);
            BasicBlock lAndFirst = genLAnd(lAnd, succ, fail);
            return genLor(lor2, succ, lAndFirst);
        }
    }

    private BasicBlock genLAnd(NonTerminator lAnd, BasicBlock succ, BasicBlock fail) throws IRGenErr {
        if (lAnd.getChildSize() == 1) {
            BasicBlock eqFirst = irGenManager.getBB();
            Value v = genEq((NonTerminator) lAnd.getChild(0));
            irGenManager.genBranch(v, succ, fail);
            return eqFirst;
        } else {
            NonTerminator lAnd2 = (NonTerminator) lAnd.getChild(0);
            NonTerminator eq = (NonTerminator) lAnd.getChild(2);
            BasicBlock eqFirst = irGenManager.getBB();
            Value v = genEq(eq);
            irGenManager.genBranch(v, succ, fail);
            return genLAnd(lAnd2, eqFirst, fail);
        }
    }

    private Value genEq(NonTerminator eq) throws IRGenErr {
        if (eq.getChildSize() == 1) {
            return genRel((NonTerminator) eq.getChild(0));
        } else {
            NonTerminator eq2 = (NonTerminator) eq.getChild(0);
            NonTerminator rel = (NonTerminator) eq.getChild(2);
            Value v1 = genEq(eq2);
            Value v2 = genRel(rel);
            if (v1 instanceof InitVal && v2 instanceof InitVal) {
                if (TreeNode.match(eq, 1, Token.Type.EQL) != null) {
                    int t = ((InitVal) v1).getValue() == ((InitVal) v2).getValue()?1:0;
                    return InitVal.buildInitVal(t);
                } else {
                    int t = ((InitVal) v1).getValue() != ((InitVal) v2).getValue()?1:0;
                    return InitVal.buildInitVal(t);
                }
            }
            if (TreeNode.match(eq, 1, Token.Type.EQL) != null) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Seq, v1, v2);
            } else {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Sne, v1, v2);
            }
        }
    }

    private Value genRel(NonTerminator rel) throws IRGenErr {
        if (rel.getChildSize() == 1) {
            return genAdd((NonTerminator) rel.getChild(0));
        } else {
            NonTerminator rel2 = (NonTerminator) rel.getChild(0);
            NonTerminator add = (NonTerminator) rel.getChild(2);
            Value v1 = genRel(rel2);
            Value v2 = genAdd(add);
            if (v1 instanceof InitVal && v2 instanceof InitVal) {
                if (TreeNode.match(rel, 1, Token.Type.LSS) != null) {
                    int t = ((InitVal) v1).getValue() < ((InitVal) v2).getValue()?1:0;
                    return InitVal.buildInitVal(t);
                } else if (TreeNode.match(rel, 1, Token.Type.LEQ) != null) {
                    int t = ((InitVal) v1).getValue() <= ((InitVal) v2).getValue()?1:0;
                    return InitVal.buildInitVal(t);
                } else if (TreeNode.match(rel, 1, Token.Type.GEQ) != null) {
                    int t = ((InitVal) v1).getValue() >= ((InitVal) v2).getValue()?1:0;
                    return InitVal.buildInitVal(t);
                } else {
                    int t = ((InitVal) v1).getValue() > ((InitVal) v2).getValue()?1:0;
                    return InitVal.buildInitVal(t);
                }
            }
            if (TreeNode.match(rel, 1, Token.Type.LSS) != null) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Slt, v1, v2);
            } else if (TreeNode.match(rel, 1, Token.Type.LEQ) != null) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Sle, v1, v2);
            } else if (TreeNode.match(rel, 1, Token.Type.GRE) != null) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Sgt, v1, v2);
            } else {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Sge, v1, v2);
            }
        }
    }

    private void genPrintf(String format, List<Value> params, int line) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) == '%') {
                i++; // skip d
                if (sb.length() > 0) {
                    irGenManager.genPutStr(sb.toString());
                }
                sb = new StringBuilder();
                if (idx >= params.size()) {
                    ErrorHandler.getInstance().addError(RequiredErr.buildPrintfParamNotMatch(line));
                    return;
                }
                irGenManager.genPutInt(params.get(idx));
                idx++;
            } else {
                sb.append(format.charAt(i));
            }
        }
        if (idx < params.size()) {
            ErrorHandler.getInstance().addError(RequiredErr.buildPrintfParamNotMatch(line));
        }
        if (sb.length() > 0) {
            irGenManager.genPutStr(sb.toString());
        }
    }

    private void genDecl(NonTerminator decl, boolean onStack) throws IRGenErr {
        if (TreeNode.match(decl, 0, Token.Type.CONSTTK) != null) {
            for (int i = 2; i < decl.getChildSize(); i += 2) {
                assert decl.getChild(i) instanceof NonTerminator &&
                        ((NonTerminator) decl.getChild(i)).getType() == NonTerminator.Type.ConstDef;
                genDef((NonTerminator) decl.getChild(i), onStack, true);
            }
        } else {
            for (int i = 1; i < decl.getChildSize(); i += 2) {
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
        Terminator t = (Terminator) def.getChild(0);
        int idx0 = -2, idx1 = -2;
        Ty ty = IntTy.build(isConst);
        if (symbolTable.containName(t.getToken().getText())) { // 名字重定义
            ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(t.getToken().getLine()));
            return; // 放弃这个定义
        }
        if (eqlIdx >= 4) {
            idx0 = evaluator.evaluate(def.getChild(2));
            ty = IntArrTy.build(idx0, isConst);
        }
        if (eqlIdx == 7) {
            idx1 = evaluator.evaluate(def.getChild(5));
            ty = IntArrTy.build(idx0, idx1, isConst);
        }
        // 没有初始化
        if (eqlIdx == def.getChildSize()) {
            if (isConst) {
                // 常量未初始化
                throw new IRGenErr(t.getToken().getLine());
            } else {
                if (ty instanceof IntTy) {
                    if (onStack) {
                        AllocInstr allocInstr = irGenManager.genStackData(ty, InitVal.buildInitVal(0));
                        symbolTable.putVarOrGenErr(t.getToken().getText(), allocInstr, t.getToken().getLine());
                    } else {
                       AllocInstr allocInstr = irGenManager.genStaticData(
                                ty, new Constant(ty, 0), t.getToken().getText());
                       symbolTable.putVarOrGenErr(t.getToken().getText(), allocInstr, t.getToken().getLine());
                    }
                } else {
                    AllocInstr allocInstr;
                    if (onStack) {
                        allocInstr = irGenManager.genStackDataNoInit(ty);
                    } else {
                        allocInstr = irGenManager.genStaticData(ty, new Constant(ty), t.getToken().getText());
                    }
                    symbolTable.putVarOrGenErr(t.getToken().getText(), allocInstr, t.getToken().getLine());
                }
            }
        } else {
            if (isConst) {
                List<Integer> constants = new ArrayList<>();
                genConstInitVal((NonTerminator) def.getChild(eqlIdx + 1), constants);
                if (ty instanceof IntTy) {
                    symbolTable.putConstOrGenErr(
                            t.getToken().getText(), new Constant(ty, constants.get(0)), t.getToken().getLine());
                } else {
                    AllocInstr allocInstr = irGenManager.genStaticData(
                            ty, new Constant(ty, constants), t.getToken().getText());
                    symbolTable.putVarOrGenErr(t.getToken().getText(), allocInstr, t.getToken().getLine());
                }
            } else if (!onStack) {
                List<Integer> constants = new ArrayList<>();
                genConstInitVal((NonTerminator) def.getChild(eqlIdx + 1), constants);
                AllocInstr allocInstr;
                if (ty instanceof IntTy) {
                    allocInstr = irGenManager.genStaticData(
                            ty, new Constant(ty, constants.get(0)), t.getToken().getText());
                } else {
                    allocInstr = irGenManager.genStaticData(ty, new Constant(ty, constants), t.getToken().getText());
                }
                symbolTable.putVarOrGenErr(t.getToken().getText(), allocInstr, t.getToken().getLine());
            } else { // onStack && !isConst
                List<Value> values = new ArrayList<>();
                genInitVal((NonTerminator) def.getChild(eqlIdx + 1), values);
                AllocInstr allocInstr;
                if (ty instanceof IntTy) {
                    allocInstr = irGenManager.genStackData(ty, values.get(0));
                } else {
                    allocInstr = irGenManager.genStackData(ty, values);
                }
                symbolTable.putVarOrGenErr(t.getToken().getText(), allocInstr, t.getToken().getLine());
            }
        }
    }

    /**
     * 处理initVal,会展平使用类型来标识。
     *
     * @param init
     * @param values
     * @throws IRGenErr
     */
    private void genInitVal(NonTerminator init, List<Value> values) throws IRGenErr {
        // 不能是{}，语义约束
        if (init.getChildSize() == 1) {
            values.add(genExp((NonTerminator) init.getChild(0)));
        } else {
            for (int i = 1; i < init.getChildSize() - 1; i += 2) {
                genInitVal((NonTerminator) init.getChild(i), values);
            }
        }
    }

    private void genConstInitVal(NonTerminator init, List<Integer> constants) throws IRGenErr { // 编译期会全部求值
        if (init.getChildSize() == 1) {
            constants.add(evaluator.evaluate(init.getChild(0)));
        } else {
            for (int i = 1; i < init.getChildSize() - 1; i += 2) {
                genConstInitVal((NonTerminator) init.getChild(i), constants);
            }
        }
    }

    /**
     * 生成表达式，相当于Evaluator的变量版本
     *
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

            if (left instanceof InitVal && right instanceof InitVal) {
                if (op.getToken().getType() == Token.Type.PLUS) {
                    int t = ((InitVal) left).getValue() + ((InitVal) right).getValue();
                    return InitVal.buildInitVal(t);
                } else if (op.getToken().getType() == Token.Type.MINU) {
                    int t = ((InitVal) left).getValue() - ((InitVal) right).getValue();
                    return InitVal.buildInitVal(t);
                }
            }
            if (op.getToken().getType() == Token.Type.PLUS) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Add, left, right);
            } else if (op.getToken().getType() == Token.Type.MINU) {
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
            if (left instanceof InitVal && right instanceof InitVal) {
                if (op.getToken().getType() == Token.Type.MULT) {
                    int t = ((InitVal) left).getValue() * ((InitVal) right).getValue();
                    return InitVal.buildInitVal(t);
                } else if (op.getToken().getType() == Token.Type.DIV) {
                    int t = ((InitVal) left).getValue() / ((InitVal) right).getValue();
                    return InitVal.buildInitVal(t);
                } else if (op.getToken().getType() == Token.Type.MOD) {
                    int t = ((InitVal) left).getValue() % ((InitVal) right).getValue();
                    return InitVal.buildInitVal(t);
                }
            }
            if (op.getToken().getType() == Token.Type.MULT) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Mul, left, right);
            } else if (op.getToken().getType() == Token.Type.DIV) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Div, left, right);
            } else if (op.getToken().getType() == Token.Type.MOD) {
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
                return irGenManager.genBinaryOp(BinaryOp.OpType.Not,
                        InitVal.buildInitVal(0),
                        genUnary((NonTerminator) exp.getChild(1)));
            } else if (op.getToken().getType() == Token.Type.MINU) {
                return irGenManager.genBinaryOp(BinaryOp.OpType.Sub,
                        InitVal.buildInitVal(0),
                        genUnary((NonTerminator) exp.getChild(1)));
            } else if (op.getToken().getType() == Token.Type.PLUS) {
                return genUnary((NonTerminator) exp.getChild(1));
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
                    checkFunCallTy(func.getFunction(), new ArrayList<>(), t.getToken().getLine());
                    return irGenManager.genCallInstr(func.getFunction(), new ArrayList<>());
                } else {
                    List<Value> params = rParams((NonTerminator) exp.getChild(2));
                    checkFunCallTy(func.getFunction(), params, t.getToken().getLine());
                    return irGenManager.genCallInstr(func.getFunction(), params);
                }
            }
        } else {
            // primary
            return genPrimaryExp((NonTerminator) exp.getChild(0));
        }
        return InitVal.buildInitVal(0);
    }

    private boolean checkFunCallTy(Function function, List<Value> params, int line) {
        FuncTy funcTy = (FuncTy) function.getType();
        if (funcTy.getParams().size() != params.size()) {
            ErrorHandler.getInstance().addError(
                    RequiredErr.buildBadParamNum(line)
            );
            return false;
        }
        List<Ty> tyList = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            if (params.get(i) instanceof ArrView) { // 传部分数组
                tyList.add(IntArrTy.build(IntArrTy.ANY_DIM, false));
            } else if (params.get(i) instanceof AllocInstr) { // 传数组
                tyList.add(((AllocInstr) params.get(i)).getAllocTy());
            } else if (params.get(i) instanceof CallInstr) { // 传函数调用，可能是void
                Function targetFunc = ((CallInstr) params.get(i)).getFunction();
                tyList.add(((FuncTy)targetFunc.getType()).getRet());
            } else if (params.get(i) instanceof Param) {
                tyList.add(params.get(i).getType());
            }else {
                tyList.add(IntTy.build(false));
            }
        }

        if (funcTy.isMatchedCall(tyList)) {
            return true;
        } else {
            ErrorHandler
                    .getInstance().addError(
                            RequiredErr.buildBadParamTy(line)
                    );
            return false;
        }
    }
    private Value genPrimaryExp(NonTerminator exp) throws IRGenErr {
        if (exp.getChild(0) instanceof Terminator) {
            Terminator op = (Terminator) exp.getChildren().get(0);
            if (op.getToken().getType() == Token.Type.LPARENT) { // (Exp)
                return genAdd((NonTerminator) ((NonTerminator) exp.getChildren().get(1)).getChildren().get(0));
            }
        } else if (exp.getChild(0) instanceof NonTerminator) {
            if (((NonTerminator) exp.getChild(0)).getType() == NonTerminator.Type.LVal) {
                return genLVal((NonTerminator) exp.getChildren().get(0));
            } else if (((NonTerminator) exp.getChild(0)).getType() == NonTerminator.Type.Number){
                Terminator intCon = (Terminator) ((NonTerminator) exp.getChild(0)).getChild(0);
                return InitVal.buildInitVal(Integer.parseInt(intCon.getToken().getText()));
            }

        }
        return InitVal.buildInitVal(0);
    }

    /**
     * 产生“左边的左值”，这时对于LVal应该生成store
     *
     * @param exp
     * @param target
     * @throws IRGenErr
     */
    private void genLLVal(NonTerminator exp, Value target) throws IRGenErr {
        Token token = ((Terminator) exp.getChildren().get(0)).getToken();
        String name = token.getText();
        VarElem varElem = symbolTable.getVarOrGenErr(name, token.getLine());
        if (varElem != null) {
            if (exp.getChildren().size() == 1) {
                if (varElem.getEn() == VarElem.En.Const) {
                    ErrorHandler.getInstance().addError(RequiredErr.buildConstModified(token.getLine()));
                } else if (varElem.getEn() == VarElem.En.Alloc) {
                    if (varElem.getAlloc().getAllocTy().isConst) {
                        ErrorHandler.getInstance().addError(RequiredErr.buildConstModified(token.getLine()));
                    } else {
                        irGenManager.genStoreInstr(varElem.getAlloc(), InitVal.buildInitVal(0), target);
                    }
                } else if (varElem.getEn() == VarElem.En.Param) {
                    irGenManager.genStoreInstr(varElem.getParam(), InitVal.buildInitVal(0), target);
                }
            } else if (exp.getChildren().size() == 4) { // ID[exp]
                Value idx0 = genExp((NonTerminator) exp.getChild(2));
                if (varElem.getEn() == VarElem.En.Alloc) {
                    if (varElem.getAlloc().getAllocTy().isConst) {
                        ErrorHandler.getInstance().addError(RequiredErr.buildConstModified(token.getLine()));
                    } else {
                        AllocInstr allocInstr = varElem.getAlloc();
                        irGenManager.genStoreInstr(allocInstr, idx0, target);
                    }
                } else {
                    irGenManager.genStoreInstr(varElem.getParam(), idx0, target);
                }

            } else if (exp.getChildren().size() == 7) { // ID[exp][exp]
                Value idx0 = genExp((NonTerminator) exp.getChild(2));
                Value idx1 = genExp((NonTerminator) exp.getChild(5));
                if (varElem.getEn() == VarElem.En.Alloc) {
                    if (varElem.getAlloc().getAllocTy().isConst) {
                        ErrorHandler.getInstance().addError(RequiredErr.buildConstModified(token.getLine()));
                    } else {
                        Value idx = irGenManager.genIndex(varElem.getAlloc(), idx0, idx1);
                        irGenManager.genStoreInstr(varElem.getAlloc(), idx, target);
                    }
                } else {
                    Value idx = irGenManager.genIndex(varElem.getParam(), idx0, idx1);
                    irGenManager.genStoreInstr(varElem.getParam(), idx, target);
                }
            }
        }
    }

    private Value genLVal(NonTerminator exp) throws IRGenErr {
        Token token = ((Terminator) exp.getChildren().get(0)).getToken();
        String name = token.getText();
        VarElem varElem = symbolTable.getVarOrGenErr(name, token.getLine());
        if (varElem != null) {
            if (exp.getChildren().size() == 1) {
                if (varElem.getEn() == VarElem.En.Const) {
                    return InitVal.buildInitVal(varElem.getConstant().getValue());
                } else if (varElem.getEn() == VarElem.En.Alloc) {
                    if (varElem.getAlloc().getAllocTy() instanceof IntArrTy) {
                        return varElem.getAlloc();
                    } else {
                        return irGenManager.genLoadInstr(varElem.getAlloc(), InitVal.buildInitVal(0));
                    }
                } else if (varElem.getEn() == VarElem.En.Param) {
                    if (varElem.getParam().getType() instanceof  IntArrTy) {
                        return varElem.getParam();
                    } else {
                        return irGenManager.genLoadInstr(varElem.getParam(), InitVal.buildInitVal(0));
                    }
                } else {
                    return InitVal.buildInitVal(0);
                }
            } else if (exp.getChildren().size() == 4) { // ID[exp]
                Value idx0 = genExp((NonTerminator) exp.getChild(2));
                if (varElem.getEn() == VarElem.En.Alloc) {
                    AllocInstr allocInstr = varElem.getAlloc();
                    IntArrTy ty = (IntArrTy) allocInstr.getAllocTy();
                    if (ty.getDims().size() == 2) {
                        //取不到元素
                        Value idx = irGenManager.genIndex(allocInstr, idx0, InitVal.buildInitVal(0));
                        return irGenManager.genArrView(allocInstr, idx);
                    } else {
                        return irGenManager.genLoadInstr(allocInstr, idx0);
                    }
                } else {
                    Param param = varElem.getParam();
                    IntArrTy ty = (IntArrTy) param.getType();
                    if (ty.getDims().size() == 2) {
                        //取不到元素
                        Value idx = irGenManager.genIndex(param, idx0, InitVal.buildInitVal(0));
                        return irGenManager.genArrView(param, idx);
                    } else {
                        return irGenManager.genLoadInstr(varElem.getParam(), idx0);
                    }
                }

            } else if (exp.getChildren().size() == 7) { // ID[exp][exp]
                Value idx0 = genExp((NonTerminator) exp.getChild(2));
                Value idx1 = genExp((NonTerminator) exp.getChild(5));
                if (varElem.getEn() == VarElem.En.Alloc) {
                    Value idx = irGenManager.genIndex(varElem.getAlloc(), idx0, idx1);
                    return irGenManager.genLoadInstr(varElem.getAlloc(), idx);
                } else {
                    Value idx = irGenManager.genIndex(varElem.getParam(), idx0, idx1);
                    return irGenManager.genLoadInstr(varElem.getParam(), idx);
                }
            }
        }
        return InitVal.buildInitVal(0);
    }

    private List<Value> rParams(NonTerminator params) throws IRGenErr {
        assert params.getType() == NonTerminator.Type.FuncRParams;
        List<Value> ret = new ArrayList<>();
        for (int i = 0; i < params.getChildSize(); i += 2) {
            ret.add(genExp((NonTerminator) params.getChild(i)));
        }
        return ret;
    }

    class Evaluator {
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
                } else if (op.getToken().getType() == Token.Type.MINU) {
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
                } else if (op.getToken().getType() == Token.Type.DIV) {
                    return left /
                            right;
                } else if (op.getToken().getType() == Token.Type.MOD) {
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
                    return evalUnary((NonTerminator) exp.getChildren().get(1)) != 0 ? 0 : 1;
                } else if (op.getToken().getType() == Token.Type.MINU) {
                    return -evalUnary((NonTerminator) exp.getChildren().get(1));
                } else if (op.getToken().getType() == Token.Type.PLUS) {
                    return evalUnary((NonTerminator) exp.getChildren().get(1));
                }
            } else if (exp.getChildren().get(0) instanceof NonTerminator) {
                return evalPrimary((NonTerminator) exp.getChildren().get(0));
            }
            return 0;
        }

        private int evalPrimary(NonTerminator exp) throws IRGenErr {
            if (exp.getChildren().get(0) instanceof Terminator) {
                Terminator op = (Terminator) exp.getChildren().get(0);
                if (op.getToken().getType() == Token.Type.LPARENT) { // (Exp)
                    return evalAdd((NonTerminator) ((NonTerminator) exp.getChildren().get(1)).getChildren().get(0));
                }
            } else if (exp.getChild(0) instanceof NonTerminator) {
                if (((NonTerminator) exp.getChild(0)).getType() == NonTerminator.Type.LVal) {
                    return evalLVal((NonTerminator) exp.getChildren().get(0));
                } else if (((NonTerminator) exp.getChild(0)).getType() == NonTerminator.Type.Number){
                    Terminator intCon = (Terminator) ((NonTerminator) exp.getChild(0)).getChild(0);
                    return Integer.parseInt(intCon.getToken().getText());
                }

            }
            return 0;
        }

        private int evalLVal(NonTerminator exp) throws IRGenErr {
            Token token = ((Terminator) exp.getChildren().get(0)).getToken();
            String name = token.getText();
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
                    int idx = evalAdd((NonTerminator) ((NonTerminator) exp.getChildren().get(2)).getChildren().get(0));
                    if (!(constant.getTy() instanceof IntArrTy)) {
                        throw new IRGenErr(token.getLine());
                    }
                    return constant.getValue(idx, 0);
                } else if (exp.getChildren().size() == 7) { // ID[exp][exp]
                    if (varElem.getEn() != VarElem.En.Alloc) {
                        throw new IRGenErr(token.getLine());
                    }

                    AllocInstr allocInstr = varElem.getAlloc();
                    Constant constant = allocInstr.getInitVal();
                    int idx = evalAdd((NonTerminator) ((NonTerminator) exp.getChildren().get(2)).getChildren().get(0));
                    int idx2 = evalAdd((NonTerminator) ((NonTerminator) exp.getChildren().get(5)).getChildren().get(0));
                    if (!(constant.getTy() instanceof IntArrTy)) {
                        throw new IRGenErr(token.getLine());
                    }
                    return constant.getValue(idx, idx2);
                }

            }
            return 0;
        }
    }

    static class SymbolTable {
        List<Map<String, VarElem>> varTable = new ArrayList<>();

        public void putFunctionOrGenErr(String name, Function function, int line) {
            Map<String, VarElem> scope = varTable.get(varTable.size() - 1);
            if (scope.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {
                scope.put(name, new VarElem(function));
            }
        }

        public void putVarOrGenErr(String name, AllocInstr allocInstr, int line) {
            Map<String, VarElem> scope = varTable.get(varTable.size() - 1);
            if (scope.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {

                scope.put(name, new VarElem(allocInstr));
            }
        }

        public void putConstOrGenErr(String name, Constant constant, int line) {
            Map<String, VarElem> scope = varTable.get(varTable.size() - 1);
            if (scope.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {
                scope.put(name, new VarElem(constant));
            }
        }

        public void putParamOrGenErr(String name, Param param, int line) {
            Map<String, VarElem> scope = varTable.get(varTable.size() - 1);
            if (scope.containsKey(name)) {
                ErrorHandler.getInstance().addError(RequiredErr.buildRedefinedName(line));
            } else {
                scope.put(name, new VarElem(param));
            }
        }

        public void pushScope() {
            varTable.add(new HashMap<>());
        }

        public void pushScope(Map<String, VarElem> scope) {
            varTable.add(scope);
        }

        public Map<String, VarElem> popScope() {
            return varTable.remove(varTable.size() - 1);
        }



        public VarElem getVarOrGenErr(String name, int line) {
            int index = varTable.size() - 1;
            Map<String, VarElem> scope;
            while (index >= 0) {
                scope = varTable.get(index);
                if (scope.containsKey(name)) {
                    return scope.get(name);
                }
                index--;

            }
            ErrorHandler.getInstance().addError(RequiredErr.buildUndefinedName(line));
            return null;
        }

        public boolean containName(String name) {
            int index = varTable.size() - 1;
            Map<String, VarElem> scope = varTable.get(varTable.size() - 1);
            return scope.containsKey(name);
        }

    }

    // 符号表项
    private static class VarElem {

        public enum En {
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
