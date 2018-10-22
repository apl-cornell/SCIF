import java.io.*;
import java_cup.runtime.*;
import org.omg.CORBA.INV_FLAG;

import javax.imageio.metadata.IIOInvalidTreeException;
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
        }
        HashMap<String, varInfo> varMap = new HashMap<String, varInfo>;
        HashMap<String, funcInfo> funcMap = new HashMap<String, funcInfo>;
        ArrayList<IFConstraint> cons = new ArrayList<>();
        collectGlobalDec("", root, varMap, funcMap);

        //generate constraint from gloabl dec
        Iterator it = varMap.entrySet().iterator();
        while (it.hasNext()) {
            HashMap<String, varInfo>.Entry pair = (HashMap<String, varInfo>.Entry) it.next();
            varInfo v = (varInfo) pair.getValue();
            cons.add(Utils.genCons(v.name, v.lbl, v.x, v.y));
            cons.add(Utils.genCons(v.lbl, v.name, v.x, v.y));
        }
        it = funcMap.entrySet().iterator();
        while (it.hasNext()) {
            HashMap<String, funcInfo>.Entry pair = (HashMap<String, funcInfo>.Entry) it.next();
            funcInfo f = (funcInfo) pair.getValue();
            cons.add(Utils.genCons(f.name + "..call", f.callLbl, f.x, f.y));
            cons.add(Utils.genCons(f.name + "..return", f.rtLbl, f.x, f.y));
            Iterator iit = f.prmters.entrySet().iterator();
            while (iit.hasNext()) {
                HashMap<String. varInfo>.Entry pair = (HashMap<String, varInfo>.Entry) it.next();
                varInfo v = (varInfo) pair.getValue();
                cons.add(Utils.genCons(f.name + "." + v.name, v.lbl, v.x, v.y));
                cons.add(Utils.genCons(v.lbl, f.name + "." + v.name, v.x, v.y));
            }
        }

        generateConstraints("", root, varMap, funcMap, cons);
    }

    public Node<String> strip(Node<String> x) {
        if (x.childSize() == 0) return x;
        if (x.childSize() > 1) return null;
        return strip(x.getChilds().get(0));
    }

    public static IFLabel evaIFLabel(Node<String> x) { //x should be "test" node
        if (x.getData() != "test") {
            System.err.println("meet non-test node when evaIFLabel");
            return null;
        }
        //TODO: eva a test node to a IFLabel
    }

    public static boolean genIfVarDef(String ctxt, Node<String> x, HashMap<String, varInfo> varMap) {
        if (x.getData() == "tfpdef") {
            List<Node<String>> kids = x.getChilds();
            String name = ctxt + ":" + kids.get(0).getData();
            int px = x.c, py = x.r;
            if (kids.size() == 1) {
                varMap.put(name, new varInfo(name, null), px, py);
            }
            else {
                varMap.put(name, new varInfo(name, evaIFLabel(kids.get(1)), px, py));
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
                    varMap.put(ctxt + name.getData(), evaIFLabel(ann.getChilds().get(0)), px, py);
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
        HashMap<String, varInfo> pmMap = new HashMap<>();
        if (y.size() == 4) {
            List<Node<String>> pms = y.get(1).getChilds();
            if (pms.get(0).getData() == "nonstartypedargslist") {
                pms = pms.get(0).getChilds();
                for (Node<String> pm : pms) {
                    genIfVarDef(ctxt, pm.getChilds().get(0), pmMap);
                }
            }
            returnLabel = evaIFLabel(y.get(2));
        }
        else {
            returnLabel = null;
        }
        funcInfo f = new funcInfo(fname, callLabel, pmMap, returnLabel, px, py);
        funcMap.put(fname, f);
        return true;
    }

    public static void collectGlobalDec(String ctxt, Node<String> x, HashMap<String, varInfo> varMap, HashMap<String, funcInfo> funcMap) {
        if (x == null) return;
        String nctxt = new String(ctxt);
        if (genIfVarDef(ctxt, x, varMap)) {
            return;
        }
        if (genIfFuncDef(nctxt, x, varMap, funcMap)) {

        }
        else {
            switch (x.getData()) {
                case "if_stmt":
                    nctxt = nctxt + ".if" + x.getPos();
                    break;
                case "elif_stmt":
                    nctxt = nctxt + ".elif" + x.getPos();
                    break;
                case "while_stmt" :
                    nctxt = nctxt + ".while" + x.getPos();
                    break;
                case "for_stmt" :
                    nctxt = nctxt + ".for" + x.getPos();
                    break;
                default: break;
            }
        }
        for (Node<String> y : x.getChilds())
            collectGlobalDec(nctxt, y, varMap, funcMap);
    }

    public static void generateConstraints(Node<String> x, HashMap<String, varInfo> varMap, HashMap<String, funcInfo> funcMap, ArrayList<IFConstraint> cons) {
        if (x == null) return;

    }
}

public class varInfo {
    public String name;
    public IFLabel lbl;
    public int x, y;
    public varInfo(String name, IFLabel lbl, int x, int y) {
        this.name = name;
        this.lbl = lbl;
        this.x = x;
        this.y = y;
    }
}

public class funcInfo {
    public String name;
    public IFLabel callLbl;
    public HashMap<String, varInfo> prmters;
    public IFLabel rtLbl;
    public int x, y;
    public funcInfo(String name, IFLabel callLbl, HashMap<String, varInfo> prmters, IFLabel rtLbl, int x, int y) {
        this.name = name;
        this.callLbl = callLbl;
        this.prmters = prmters;
        this.rtLbl = rtLbl;
        this.x = x;
        this.y = y;
    }
}

public class IFLabel {
    public String value;
    public IFLabel left, right;
}

public class IFConstraint {
    public String op;
    public IFLabel left, right;
}

public class lookupMaps {
    ArrayList<HashMap<String, String>> maps;
}