import java.io.*;
import java_cup.runtime.*;
import java.util.*;

public class TypeChecker {
    public static void main(String[] args) {
        Node<String> root;
        try {
            Lexer lexer = new Lexer(new FileReader(args[0]));
            Parser p = new Parser(lexer);
            Symbol result = p.parse();
            root = (Node<String>) result.value;
            Parser.printTree(root, 0);
            System.out.println("Finish\n");
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
        HashMap<String, varInfo> varMap = new HashMap<String, varInfo>();
        HashMap<String, funcInfo> funcMap = new HashMap<String, funcInfo>();
        ArrayList<IFConstraint> cons = new ArrayList<>();
        collectGlobalDec("", root, varMap, funcMap);

        //generate constraint from gloabl dec
/*        Iterator it = varMap.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<String, varInfo> pair = (HashMap.Entry<String, varInfo>) it.next();
            varInfo v = (varInfo) pair.getValue();
            cons.add(Utils.genCons(v.name, v.lbl, v.x, v.y));
            cons.add(Utils.genCons(v.lbl, v.name, v.x, v.y));
        }*/
/*        it = funcMap.entrySet().iterator();
        while (it.hasNext()) {
            HashMap<String, funcInfo>.Entry pair = (HashMap<String, funcInfo>.Entry) it.next();
            funcInfo f = (funcInfo) pair.getValue();
            //cons.add(Utils.genCons(f.name + "..call", f.callLbl, f.x, f.y));
            //cons.add(Utils.genCons(f.name + "..return", f.rtLbl, f.x, f.y));
            Iterator iit = f.prmters.entrySet().iterator();
            while (iit.hasNext()) {
                HashMap<String. varInfo>.Entry pair = (HashMap<String, varInfo>.Entry) it.next();
                varInfo v = (varInfo) pair.getValue();
                cons.add(Utils.genCons(f.name + "." + v.name, v.lbl, v.x, v.y));
                cons.add(Utils.genCons(v.lbl, f.name + "." + v.name, v.x, v.y));
            }
        }*/

        IFLabel pc = IFLabel.bottom;
        LookupMaps varNameMap = new LookupMaps(varMap);

        generateConstraints("", root, varMap, funcMap, cons, varNameMap, pc);
    }

    public static Node<String> strip(Node<String> x) {
        if (x.childSize() == 0) return x;
        if (x.childSize() > 1) return null;
        return strip(x.getChilds().get(0));
    }

    public static IFLabel evaIFLabel(Node<String> x) { //x should be "test" node
        if (x.getData() != "test") {
            System.err.println("meet non-test node when evaIFLabel");
            return null;
        }

        List<Node<String>> kids = x.getChilds();
        String xd = x.getData();
        if (x.getChilds().size() == 0) {
            return new IFLabel(x.getData());
        } else if (x.getChilds().size() == 1) {
            return evaIFLabel(kids.get(0));
        } else if (xd.equals("comparison") || xd.equals("expr")) {
            return new IFLabel(kids.get(1).getData(), evaIFLabel(kids.get(0)), evaIFLabel(kids.get(1)));
        }
        //TODO: err rep
        return null;
    }

    public static boolean genIfVarDef(String ctxt, Node<String> x, ArrayList<varInfo> varArr) {
        if (x.getData() == "tfpdef") {
            List<Node<String>> kids = x.getChilds();
            String name = ctxt + ":" + kids.get(0).getData();
            int px = x.c, py = x.r;
            if (kids.size() == 1) {
                varArr.add(new varInfo(name, null, px, py));
            }
            else {
                varArr.add(new varInfo(name, evaIFLabel(kids.get(1)), px, py));
            }
            return true;
        }
        else if (x.getData() == "expr_stmt") {
            List<Node<String>> kids = x.getChilds();
            if (kids.size() == 1) return false;
            Node<String> ann = kids.get(1).getChilds().get(0);
            if (ann.getData() == "annassign") {
                if (kids.get(0).childSize() != 1) {
                    //TODO: left expr more than one test
                    return false;
                }
                Node<String> name = kids.get(0).getChilds().get(0);
                if (name.getData() != "test") return false;
                name = strip(name);
                int px = name.c, py = name.r;
                if (name != null && name.getData().charAt(0) == '%') {
                    varArr.add(new varInfo(name.getData(), evaIFLabel(ann.getChilds().get(0)), px, py));
                    return true;
                }
                return false;
            }
            else {
                return false;
            }

        }
        return false;
    }

    public static boolean genIfFuncDef(String ctxt, Node<String> x, HashMap<String, varInfo> varMap, HashMap<String, funcInfo> funcMap) {
        if (x.getData() != "funcdef") return false;
        String fname;
        IFLabel callLabel, returnLabel;
        List<Node<String>> y = x.getChilds();
        int px = x.c, py = x.r;


        if (y.get(0).getData() == "LabeledName") {
            fname = y.get(0).getChilds().get(0).getData();
            callLabel = evaIFLabel(y.get(0).getChilds().get(1));
        }
        else {
            fname = y.get(0).getData();
            callLabel = null;
        }
        ctxt = ctxt + "." + fname;
        ArrayList<varInfo> pmArr = new ArrayList<>();
        if (y.size() == 4) {
            List<Node<String>> pms = y.get(1).getChilds();
            if (pms.get(0).getData() == "nonstartypedargslist") {
                pms = pms.get(0).getChilds();
                for (Node<String> pm : pms) {
                    genIfVarDef(ctxt, pm.getChilds().get(0), pmArr);
                }
            }
            returnLabel = evaIFLabel(y.get(2));
        }
        else {
            returnLabel = null;
        }
        funcInfo f = new funcInfo(fname, callLabel, pmArr, returnLabel, px, py);
        funcMap.put(fname, f);
        return true;
    }

    public static void collectGlobalDec(String ctxt, Node<String> x, HashMap<String, varInfo> varMap, HashMap<String, funcInfo> funcMap) {
        if (x == null) return;
        String nctxt = new String(ctxt);
        ArrayList<varInfo> tmp = new ArrayList<>();
        if (genIfVarDef(ctxt, x, tmp)) {
            for (varInfo v : tmp) {
                varMap.put(v.name, v);
            }
            return;
        }
        if (genIfFuncDef(nctxt, x, varMap, funcMap)) {

        }
        else {
            switch (x.getData()) {
                case "if_stmt":
                    nctxt = nctxt + ".if." + x.getPos();
                    List<Node<String>> kids = x.getChilds();
                    collectGlobalDec(nctxt, kids.get(1), varMap, funcMap);
                    if (kids.size() > 2) {
                        if (kids.get(2).getData() == "elif_stmt") {
                            collectGlobalDec(ctxt, kids.get(2), varMap, funcMap);
                            if (kids.size() > 3) {
                                nctxt = ctxt + ".else." + kids.get(3).getPos();
                                collectGlobalDec(nctxt, kids.get(3), varMap, funcMap);
                            }
                        }
                        else {
                            nctxt = ctxt + ".else." + kids.get(2).getPos();
                            collectGlobalDec(nctxt, kids.get(2), varMap, funcMap);
                        }
                    }
                    break;
                case "elif_stmts":
                    kids = x.getChilds();
                    for (int i = 0; i < kids.size(); i += 2) {
                        nctxt = ctxt + ".elif." + kids.get(i).getPos();
                        collectGlobalDec(ctxt, kids.get(i), varMap, funcMap);
                        collectGlobalDec(nctxt, kids.get(i+1), varMap, funcMap);
                    }
                    break;
                case "while_stmt" :
                    nctxt = nctxt + ".while." + x.getPos();
                    break;
                /*case "for_stmt" :
                    nctxt = nctxt + ".for" + x.getPos();
                    break;*/
                default: break;
            }
        }
        for (Node<String> y : x.getChilds())
            collectGlobalDec(nctxt, y, varMap, funcMap);
    }

    public static IFLabel getExpIFLabel(String ctxt, Node<String> x, HashMap<String, varInfo> varMap, HashMap<String, funcInfo> funcMap, ArrayList<IFConstraint> cons, LookupMaps varNameMap, IFLabel pc) {
        List<Node<String>> kids = x.getChilds();
        if (kids.size() == 0) {
            if (x.getData().startsWith("%")) {
                String name = x.getData();
                if (varNameMap.exists(name)) {
                    if (varMap.containsKey(name))
                        return varMap.get(name).lbl;
                }
                return null;
            }
            return null;
        }
        if (kids.size() == 1) {
            return getExpIFLabel(ctxt, kids.get(0), varMap, funcMap, cons, varNameMap, pc);
        } else if (x.getData().equals("or_test") || x.getData().equals("and_test") || x.getData().equals("power")) {
            IFLabel result = getExpIFLabel(ctxt, kids.get(0), varMap, funcMap, cons, varNameMap, pc);
            for (int i = 1; i < kids.size(); ++i) {
                result = new IFLabel("meet", result, getExpIFLabel(ctxt, kids.get(i), varMap, funcMap, cons, varNameMap, pc));
            }
            return result;
        } else if (x.getData().equals("comparison") || x.getData().equals("expr") || x.getData().equals("factor")) {
            if (kids.size() == 2) {
                return getExpIFLabel(ctxt, kids.get(1), varMap, funcMap, cons, varNameMap, pc);
            }
            IFLabel result = getExpIFLabel(ctxt, kids.get(0), varMap, funcMap, cons, varNameMap, pc);
            for (int i = 2; i < kids.size(); i += 2) {
                result = new IFLabel("meet", result, getExpIFLabel(ctxt, kids.get(i), varMap, funcMap, cons, varNameMap, pc));
            }
            return result;
        } else if (x.getData().equals("atom_expr")) {
            //assume all funcs are global
            Node<String> lc = kids.get(0), rc = kids.get(1);
            if (lc.getData().startsWith("%")) {
                String name = lc.getData();
                if (!varNameMap.exists(name) && funcMap.containsKey(name)) {
                    if (!rc.getData().equals("trailer_()")) {
                        //TODO: report error
                        return null;
                    }
                    funcInfo f = funcMap.get(name);
                    ArrayList<varInfo> pmArr = funcMap.get(name).prmters;
                    ArrayList<IFLabel> augArr = new ArrayList<>();
                    for (Node<String> aug: rc.getChilds().get(0).getChilds()) {
                        augArr.add(getExpIFLabel(ctxt, aug, varMap, funcMap, cons, varNameMap, pc));
                    }
                    if (augArr.size() != pmArr.size()) {
                        //TODO: err rep
                        return null;
                    }
                    cons.add(Utils.genCons(f.callLbl, pc, x.r, x.c));
                    for (int i = 0; i < pmArr.size(); ++i)
                        cons.add(Utils.genCons(pmArr.get(i).lbl, augArr.get(i), x.r, x.c));
                    return f.rtLbl;
                }
            }
            IFLabel rlabel = getExpIFLabel(ctxt, rc, varMap, funcMap, cons, varNameMap, pc);
            IFLabel llabel = getExpIFLabel(ctxt, lc, varMap, funcMap, cons, varNameMap, pc);
            return new IFLabel("meet", llabel, rlabel);
        } else if (x.getData().startsWith("atom")) {
            if (kids.size() == 0)
                return IFLabel.bottom;
            return getExpIFLabel(ctxt, kids.get(0), varMap, funcMap, cons, varNameMap, pc);
        }
        //TODO: err rep
        return null;
    }

    public static void generateConstraints(String ctxt, Node<String> x, HashMap<String, varInfo> varMap, HashMap<String, funcInfo> funcMap, ArrayList<IFConstraint> cons, LookupMaps varNameMap, IFLabel pc) {
        if (x == null) return;
        List<Node<String>> kids = x.getChilds();
        if (x.getData() == "expr_stmt") {

            if (kids.size() == 1) {
                generateConstraints(ctxt, kids.get(0), varMap, funcMap, cons, varNameMap, pc);
            } else {
                String name = kids.get(0).getData();
                String fullName;
                List<Node<String>> kkids = kids.get(1).getChilds();
                if (kkids.get(1).getData() == "annassign") {
                    fullName = ctxt + '.' + name;
                    if (varNameMap.exists_curlevel(name)) {
                        //TODO: error rep
                        return;
                    }
                    varNameMap.add(name, fullName);
                    if (kkids.size() > 1) {
                        IFLabel result = new IFLabel("meet", getExpIFLabel(ctxt, kkids.get(1), varMap, funcMap, cons, varNameMap, pc), pc);
                        cons.add(Utils.genCons(varMap.get(fullName).lbl, result, x.c, x.r));
                    }
                } else if (kkids.get(1).getData() == "augassign") {
                    //TODO: augassign support
                } else {
                    if (!varNameMap.exists(name)) {
                        //TODO: error rep
                        return;
                    }
                    fullName = varNameMap.get(name);
                    IFLabel result = new IFLabel("meet", getExpIFLabel(ctxt, kkids.get(0), varMap, funcMap, cons, varNameMap, pc), pc);
                    cons.add(Utils.genCons(varMap.get(fullName).lbl, result, x.c, x.r));
                }
            }
            return;
        } else if (x.getData() == "if_stmt") {
            String nctxt = ctxt + ".if" + x.getPos();
            IFLabel npc = new IFLabel("meet", pc, getExpIFLabel(ctxt, kids.get(0), varMap, funcMap, cons, varNameMap, pc));
            varNameMap.incLayer();
            generateConstraints(nctxt, kids.get(1), varMap, funcMap, cons, varNameMap, npc);
            varNameMap.decLayer();
            if (kids.size() > 2) {
                if (kids.get(2).getData() == "elif_stmt") {
                    generateConstraints(nctxt, kids.get(2), varMap, funcMap, cons, varNameMap, npc);
                    if (kids.size() > 3) {
                        nctxt = ctxt + ".else." + kids.get(3).getPos();
                        varNameMap.incLayer();
                        generateConstraints(nctxt, kids.get(3), varMap, funcMap, cons, varNameMap, npc);
                        varNameMap.decLayer();
                    }
                }
                else {
                    nctxt = ctxt + ".else." + kids.get(2).getPos();
                    varNameMap.incLayer();
                    generateConstraints(nctxt, kids.get(2), varMap, funcMap, cons, varNameMap, npc);
                    varNameMap.decLayer();
                }
            }
        } else if (x.getData() == "while_stmt") {
            IFLabel npc = new IFLabel("meet", pc, getExpIFLabel(ctxt, kids.get(0), varMap, funcMap, cons, varNameMap, pc));
            varNameMap.incLayer();
            String nctxt = ctxt + ".while." + x.getPos();
            generateConstraints(nctxt, kids.get(1), varMap, funcMap, cons, varNameMap, npc);
            varNameMap.decLayer();
        } else if (x.getData() == "elif_stmt") {
            String nctxt;
            for (int i = 0; i < kids.size(); i += 2) {
                nctxt = ctxt + ".elif." + kids.get(i).getPos();
                IFLabel npc = new IFLabel("meet", pc, getExpIFLabel(ctxt, kids.get(i), varMap, funcMap, cons, varNameMap, pc));
                varNameMap.incLayer();
                generateConstraints(nctxt, kids.get(i+1), varMap, funcMap, cons, varNameMap, npc);
                varNameMap.decLayer();
            }
        }/* else if (x.getData() == "for_stmt") {
            String nctxt = ctxt + ".for" + x.getPos();

        }*/
        for (Node<String> kid : kids) {
            generateConstraints(ctxt, kid, varMap, funcMap, cons, varNameMap, pc);
        }
    }
}





