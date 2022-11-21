package backend.coloralloc;

import backend.MCBlock;
import backend.MCFunction;
import backend.instr.*;
import backend.pass.CalcuLiveInfo;
import backend.regs.PReg;
import backend.regs.Reg;
import backend.regs.VReg;
import exceptions.BackEndErr;
import ir.instruction.Instr;
import util.MyNode;
import util.SetUtil;

import java.util.*;

/**
 * 图染色寄存器分配
 */
public class ColorAllocator {

    private final MCFunction function;

    public ColorAllocator(MCFunction function) {
        this.function = function;
        //初始化initial里的临时寄存器
        function.list.forEach(bbNode -> {
            MCBlock bb = bbNode.getValue();
            bb.list.forEach(
                    instrNode -> {
                        if (!instrNode.getValue().getDef().isEmpty()) {
                            Reg reg = instrNode.getValue().getDef().iterator().next();
                            if (reg instanceof VReg) {
                                IGNode t = new IGNode(reg);
                                initial.add(t);
                                iGraph.nodes.add(t);
                            }
                        }
                    }
            );
        });
    }

    private final Map<Reg, Double> spillPRT = new HashMap<>(); // 溢出优先级

    private void calculateSpillPRT() {
        spillPRT.clear();
        for (MyNode<MCBlock> bbNode : function.list) {
            MCBlock bb = bbNode.getValue();
            for (MyNode<MCInstr> instrNode : bb.list) {
                MCInstr instr = instrNode.getValue();
                for (Reg reg : instr.getDef()) {
                    spillPRT.compute(reg,
                            (reg1, aDouble) ->
                                    (aDouble == null ?
                                            Math.pow(10, bb.loopDepth) :
                                            aDouble + Math.pow(10, bb.loopDepth)));
                }
                for (Reg reg : instr.getUse()) {
                    spillPRT.compute(reg,
                            (reg1, aDouble) ->
                                    (aDouble == null ?
                                            Math.pow(10, bb.loopDepth) :
                                            aDouble + Math.pow(10, bb.loopDepth)));
                }

            }
        }
    }

    private void clear() {
        simplifyWorkList.clear();
        freezeWorkList.clear();
        spillWorkList.clear();
        spilledNodes.clear();
        coalescedNodes.clear();
        coloredNodes.clear();
        selectStack.clear();
        selectSet.clear();
        coalescesMoves.clear();
        constrainedMoves.clear();
        frozenMoves.clear();
        worklistMoves.clear();
        activeMoves.clear();
        //iGraph.adjSet.clear();
        iGraph.nodes.clear();
        iGraph.nodes.addAll(initial);
        iGraph.nodes.forEach(n -> {
            if (n.reg instanceof VReg) {
                n.color = null;
                n.degree = 0;
            } else {
                n.degree = Integer.MAX_VALUE;
            }
            n.edges.clear();
            n.alias = null;
            n.moveList.clear();
        });
    }

    public void run() {
        while (true) {
            new CalcuLiveInfo().runOnFunctionOpt(function);
            calculateSpillPRT();
            clear();
            build();
            makeWorkList();
            do {
                if (!simplifyWorkList.isEmpty()) {
                    simplify();
                } else if (!worklistMoves.isEmpty()) {
                    coalesce();
                } else if (!freezeWorkList.isEmpty()) {
                    freeze();
                } else if (!spillWorkList.isEmpty()) {
                    selectSpill();
                }
            } while (!(simplifyWorkList.isEmpty() && worklistMoves.isEmpty() &&
                    freezeWorkList.isEmpty() && spillWorkList.isEmpty()));
            assignColors();
            if (!spilledNodes.isEmpty()) {
                rewriteProgram();
            } else {
                break;
            }
        }
        finish();
    }

    private void finish() {
        function.stackSlot += spilledVarCnt;
        function.pop.offset = function.stackSlot * 4;
        function.push.offset = -function.stackSlot * 4;
        Map<VReg, PReg> allocMap = new HashMap<>();
        for (IGNode n :
                iGraph.nodes) {
            if (n.color != null) {
                allocMap.put((VReg) n.reg, n.color);
            }
        }
        for (MyNode<MCBlock> bbNode : function.list) {
            for (MCInstr instr : bbNode.getValue().list.toList()) {
                instr.getDef().forEach(reg -> {
                    if (reg instanceof VReg) {
                        instr.allocate(reg, allocMap.get((VReg) reg));
                    }
                });
                instr.getUse().forEach(reg -> {
                    if (reg instanceof VReg) {
                        instr.allocate(reg, allocMap.get((VReg) reg));
                    }
                });
                if (instr instanceof MCMove) {
                    MCMove move = (MCMove) instr;
                    if (move.d == move.s) {
                        move.getNode().removeMe();
                    }
                }
                if (instr instanceof MCLw && ((MCLw) instr).isLoadArg) {
                    ((MCLw) instr).numOffset += function.stackSlot * 4;
                }
            }
        }

    }

    private final Map<Reg, IGNode> reg2NodeMap = new HashMap<>();

    private IGNode reg2Node(Reg reg) {
        /*if (reg instanceof PReg) {
            for (IGNode n : precolored) {
                if (n.reg == reg) {
                    return n;
                }
            }
        } else {
            for (IGNode n : initial) {
                if (n.reg == reg) {
                    return n;
                }
            }
        }
        return null;*/
        return reg2NodeMap.get(reg);
    }

    private void build() {
        reg2NodeMap.clear();
        for (IGNode n : precolored) {
            reg2NodeMap.put(n.reg, n);
        }
        for (IGNode n : initial) {
            reg2NodeMap.put(n.reg, n);
        }


        for (MyNode<MCBlock> bNode :
                function.list) {
            MCBlock b = bNode.getValue();
            Set<Reg> live = new HashSet<>(b.liveOut);
            live.removeAll(PReg.getSpRegs());
            List<MCInstr> instrList = b.list.toList();
            for (int i = instrList.size() - 1; i >= 0; i--) {
                MCInstr instr = instrList.get(i);
                if (instr instanceof MCMove) {
                    MCMove move = (MCMove) instr;
                    if (!(move.s instanceof PReg && ((PReg) move.s).id == 2 ||
                            move.d instanceof PReg && ((PReg) move.d).id == 2)) {
                        live.removeAll(instr.getUse());
                        IGMove igMove = new IGMove(reg2Node(move.s), reg2Node(move.d));
                        for (Reg reg1 : instr.getUse()) {
                            reg2Node(reg1).moveList.add(igMove);
                        }

                        for (Reg reg : instr.getDef()) {
                            reg2Node(reg).moveList.add(igMove);
                        }

                        worklistMoves.add(igMove);
                    }
                }
                live.addAll(instr.getDef());
                live.removeAll(PReg.getSpRegs());
                for (Reg r :
                        instr.getDef()) {
                    if (r instanceof PReg && PReg.getSpRegs().contains((PReg) r)) {
                        continue;
                    }
                    for (Reg l :
                            live) {
                        iGraph.addEdge(reg2Node(r), reg2Node(l));
                    }
                }
                live.removeAll(instr.getDef());
                live.addAll(instr.getUse());
                live.removeAll(PReg.getSpRegs());
            }
        }
        /*Set<PReg> sp = PReg.getSpRegs();
        for (IGEdge e :
                iGraph.adjSet) {
            if (e.u.reg instanceof PReg && sp.contains((PReg) e.u.reg) || e.v.reg instanceof PReg && sp.contains((PReg) e.v.reg)) {
                System.out.println("error occured");
                throw new RuntimeException();
            }
        }*/
    }

    // 可用于着色的寄存器
    private final Set<PReg> colors = new HashSet<PReg>() {{
        addAll(PReg.getTRegs());
    }};
    private final Set<IGNode> precolored = new HashSet<IGNode>() {{
        PReg.getTRegs().forEach(reg -> {
            IGNode t = new IGNode(reg);
            t.degree = Integer.MAX_VALUE; //预着色节点度无限大
            add(t);
        });
        forEach(n -> n.color = (PReg) n.reg);
    }};
    private final Set<IGNode> initial = new HashSet<>();
    private final List<IGNode> simplifyWorkList = new ArrayList<>(); // 改成List优化性能
    private final Set<IGNode> freezeWorkList = new HashSet<>();
    private final Set<IGNode> spillWorkList = new HashSet<>();
    private final Set<IGNode> spilledNodes = new HashSet<>();
    private final Set<IGNode> coalescedNodes = new HashSet<>();
    private final Set<IGNode> coloredNodes = new HashSet<>();
    private final Deque<IGNode> selectStack = new ArrayDeque<>();
    private final Set<IGNode> selectSet = new HashSet<>(); // 优化contain判断

    private final Set<IGMove> coalescesMoves = new HashSet<>();
    private final Set<IGMove> constrainedMoves = new HashSet<>();
    private final Set<IGMove> frozenMoves = new HashSet<>();
    private final Set<IGMove> worklistMoves = new HashSet<>();
    private final Set<IGMove> activeMoves = new HashSet<>();

    private final IGraph iGraph = new IGraph();

    public class IGraph {
        public Set<IGNode> nodes = new HashSet<>();
        public Set<IGEdge> adjSet = new HashSet<>();

        public void addEdge(IGNode u, IGNode v) {
            if (u != v /*&& !adjSet.contains(new IGEdge(u, v))*/) {
                //adjSet.add(new IGEdge(u, v));
                //adjSet.add(new IGEdge(v, u));
                if (!precolored.contains(u)) {
                    u.edges.add(v);
                    u.degree += 1;
                }
                if (!precolored.contains(v)) {
                    v.edges.add(u);
                    v.degree += 1;
                }
            }
        }
    }

    public void makeWorkList() {
        for (IGNode node :
                initial) {
            if (node.degree >= colors.size()) {
                spillWorkList.add(node);
            } else if (moveRelated(node)) {
                freezeWorkList.add(node);
            } else {
                simplifyWorkList.add(node);
            }
        }
    }

    private Set<IGNode> adjacent(IGNode node) {
        Set<IGNode> ret = new HashSet<>(node.edges);
        ret.removeAll(selectSet);
        ret.removeAll(coalescedNodes);
        return ret;
    }

    private Set<IGMove> nodeMoves(IGNode node) {
        Set<IGMove> ret = new HashSet<>();
        for (IGMove move :
                node.moveList) {
            if (activeMoves.contains(move) || worklistMoves.contains(move)) {
                ret.add(move);
            }
        }
        return ret;
    }

    private boolean moveRelated(IGNode node) {
        return !nodeMoves(node).isEmpty();
    }

    private void simplify() {
        IGNode node = simplifyWorkList.get(simplifyWorkList.size() - 1);
        if (!(node.reg instanceof VReg)) {
            throw new RuntimeException();
        }

        simplifyWorkList.remove(simplifyWorkList.size() - 1);
        selectStack.push(node);
        selectSet.add(node);
        for (IGNode igNode : node.edges) {
            if (!selectSet.contains(igNode) && !coalescedNodes.contains(igNode)) {
                decrementDegree(igNode);
            }
        }
    }

    private void decrementDegree(IGNode node) {
        int d = node.degree;
        node.degree = d - 1;
        if (d == colors.size()) {
            Set<IGNode> t = adjacent(node);
            t.add(node);
            enableMoves(t);
            spillWorkList.remove(node);
            if (moveRelated(node)) {
                freezeWorkList.add(node);
            } else {
                simplifyWorkList.add(node);
            }
        }
    }

    private void enableMoves(Set<IGNode> nodes) {
        nodes.forEach(
                node -> nodeMoves(node).forEach(
                        node1 -> {
                            if (activeMoves.contains(node1)) {
                                activeMoves.remove(node1);
                                worklistMoves.remove(node1);
                            }
                        }
                )
        );

    }

    private void coalesce() {
        IGMove m = worklistMoves.iterator().next();
        IGNode x = getAlias(m.to);
        IGNode y = getAlias(m.from);
        IGNode u, v;
        if (precolored.contains(y)) {
            u = y;
            v = x;
        } else {
            u = x;
            v = y;
        }
        worklistMoves.remove(m);
        if (u == v) {
            coalescesMoves.add(m);
            addWorkList(u);
        } else if (precolored.contains(v) || /*iGraph.adjSet.contains(new IGEdge(u, v))*/
                u.edges.contains(v)||v.edges.contains(u)) {
            constrainedMoves.add(m);
            addWorkList(u);
            addWorkList(v);
        } else if (precolored.contains(u) && adjacent(v).stream().allMatch(t -> ok(t, u)) ||
                !precolored.contains(u) && conservative(SetUtil.ofIGNode().union(adjacent(u), adjacent(v)))) {
            coalescesMoves.add(m);
            combine(u, v);
            addWorkList(u);
        } else {
            activeMoves.add(m);
        }

    }

    private void addWorkList(IGNode u) {
        if (!precolored.contains(u) && !moveRelated(u) && u.degree < colors.size()) {
            freezeWorkList.remove(u);
            simplifyWorkList.add(u);
        }
    }

    private boolean ok(IGNode t, IGNode r) {
        return t.degree < colors.size() || precolored.contains(t)
                || /*iGraph.adjSet.contains(new IGEdge(t, r))*/t.edges.contains(r) || r.edges.contains(t);
    }

    private boolean conservative(Set<IGNode> nodes) {
        int k = 0;
        for (IGNode n :
                nodes) {
            if (n.degree >= colors.size()) {
                k = k + 1;
            }
        }
        return k < colors.size();

    }

    private IGNode getAlias(IGNode n) {
        if (coalescedNodes.contains(n)) {
            return getAlias(n.alias);
        } else {
            return n;
        }
    }

    private void combine(IGNode u, IGNode v) {
        if (freezeWorkList.contains(v)) {
            freezeWorkList.remove(v);
        } else {
            spillWorkList.remove(v);
        }
        coalescedNodes.add(v);
        v.alias = u;
        u.moveList.addAll(v.moveList);
        Set<IGNode> t1 = new HashSet<>();
        t1.add(v);
        enableMoves(t1);
        for (IGNode t :
                adjacent(v)) {
            iGraph.addEdge(t, u);
            decrementDegree(t);
        }
        if (u.degree >= colors.size() && freezeWorkList.contains(u)) {
            freezeWorkList.remove(u);
            spillWorkList.add(u);
        }
    }

    void freeze() {
        IGNode u = freezeWorkList.iterator().next();
        freezeWorkList.remove(u);
        simplifyWorkList.add(u);
        freezeMoves(u);
    }

    private void freezeMoves(IGNode u) {
        for (IGMove m :
                nodeMoves(u)) {
            IGNode x = m.to;
            IGNode y = m.from;
            IGNode v;
            if (getAlias(y) == getAlias(u)) {
                v = getAlias(x);
            } else {
                v = getAlias(y);
            }
            activeMoves.remove(m);
            frozenMoves.add(m);
            if (nodeMoves(v).isEmpty() && v.degree < colors.size()) {
                freezeWorkList.remove(v);
                simplifyWorkList.add(v);
            }
        }
    }

    private void selectSpill() {
        //Optional<IGNode> t = spillWorkList.stream().filter(n -> !spillCreated.contains(n)).findAny(); // 应该用启发式算法取
        //IGNode m = t.get();
        IGNode m = spillWorkList.stream().min((node1, node2) -> {
            double t = spillPRT.get(node1.reg) / node1.degree - spillPRT.get(node2.reg) / node2.degree;
            if (t < 0) {
                return -1;
            } else if (t == 0) {
                return 0;
            } else {
                return 1;
            }
        }).get();
        spillWorkList.remove(m);
        simplifyWorkList.add(m);
        freezeMoves(m);
    }

    private void assignColors() {
        while (!selectStack.isEmpty()) {
            IGNode n = selectStack.pop();
            Set<PReg> okColors = new HashSet<>(colors);
            for (IGNode w :
                    n.edges) {
                // new SetUtil<IGNode>().union(coloredNodes, precolored).contains(getAlias(w))
                IGNode alias = getAlias(w);
                if (coloredNodes.contains(alias) || precolored.contains(alias)) {
                    okColors.remove(alias.color);
                }
            }
            if (okColors.isEmpty()) {
                spilledNodes.add(n);
            } else {
                coloredNodes.add(n);
                PReg c = okColors.iterator().next();
                n.color = c;
            }
        }
        for (IGNode n :
                coalescedNodes) {
            n.color = getAlias(n).color;
        }
    }

    private int spilledVarCnt = 0;

    private Set<IGNode> spillCreated = new HashSet<>();

    private void rewriteProgram() {
        Set<IGNode> newTemp = new HashSet<>();
        for (IGNode n :
                spilledNodes) {
            function.stackSlot++;
            spilledVarCnt++;
            Reg r = n.reg;
            for (MyNode<MCBlock> bbNode : function.list) {
                for (MCInstr instr : bbNode.getValue().list.toList()) {
                    VReg t = new VReg(233);
                    List<Reg> use = instr.getUse();
                    List<Reg> def = instr.getDef();
                    if (use.contains(r) || def.contains(r)) {
                        IGNode tnode = new IGNode(t);
                        newTemp.add(tnode);
                        spillCreated.add(tnode);
                        //iGraph.nodes.add(tnode);
                    }
                    if (use.contains(r)) {
                        try {
                            MCLw lw = new MCLw(PReg.getRegByName("sp"),
                                    t,
                                    spilledVarCnt * 4 + function.stackTop * 4
                            );
                            instr.getNode().insertBefore(lw.getNode());
                        } catch (BackEndErr e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (def.contains(r)) {
                        try {
                            MCSw sw = new MCSw(PReg.getRegByName("sp"),
                                    t,
                                    spilledVarCnt * 4 + function.stackTop * 4
                            );
                            instr.getNode().insertAfter(sw.getNode());

                        } catch (BackEndErr e) {
                            throw new RuntimeException(e);
                        }
                    }
                    instr.allocate(r, t);
                }
            }

        }
        spilledNodes.clear();
        initial.clear();
        initial.addAll(coloredNodes);
        initial.addAll(coalescedNodes);
        initial.addAll(newTemp);

        coloredNodes.clear();
        coalescedNodes.clear();
    }
}