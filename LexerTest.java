import java.io.*;
import java.util.HashMap;
import java_cup.runtime.*;

public class LexerTest implements sym {
    public static void main(String[] args) {
        HashMap<Integer, String> tokenNames = new HashMap<Integer, String>();
        for (int i = 0; i < terminalNames.length; ++i)
            tokenNames.put(i, terminalNames[i]);
        Symbol sym;
        try {
            Lexer lexer = new Lexer(new FileReader(args[0]));

            for (sym = lexer.next_token(); sym.sym != 0; sym = lexer.next_token()) {
                System.out.println("<" + tokenNames.get(sym.sym) + (sym.value == null ? "" : "," + sym.value)
                        + "> at line " + sym.left + ", column " + sym.right);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
