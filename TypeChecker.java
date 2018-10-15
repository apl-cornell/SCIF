import java.io.*;
import java_cup.runtime.*;
import java.util.*;

public class TypeChecker {
    public static void main(String[] args) {
        try {
            Lexer lexer = new Lexer(new FileReader(args[0]));
            Parser p = new Parser(lexer);
            Symbol result = p.parse();
            Node<String> root = (Node<String>) result.value;
            Parser.printTree(root, 0);
            System.out.println("Finish\n");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

public class varInfo {
    public String name;
    public IFLabel lbl;
}

public class funcInfo {
    public String name;
    public IFLabel callLbl;
    public Arraylist<IFLable> prmters;
    public IFLabel rtLbl;
}

public class IFLabel {

}
