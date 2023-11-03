package compile.ast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public interface SolNode {

    abstract List<String> toSolCode(int indentLevel);
}
