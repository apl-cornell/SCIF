package typecheck;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Shared utility for formatting compiler error messages with source context.
 * Used by syntax errors, regular type checking errors, and IFC errors.
 */
public class ErrorFormatter {

    /**
     * Format an error message with the source line and a caret marking the error location.
     *
     * @param fileName  the source file path (absolute or relative)
     * @param line      1-based line number
     * @param column    1-based column number
     * @param message   the error message
     * @return formatted error string with source context
     */
    public static String format(String fileName, int line, int column, String message) {
        String baseName = new File(fileName).getName();
        StringBuilder sb = new StringBuilder();
        sb.append(baseName).append(", line ").append(line).append(", column ").append(column)
          .append(": ").append(message);
        String context = sourceContext(fileName, line, column);
        if (context != null) {
            sb.append("\n").append(context);
        }
        return sb.toString();
    }

    /**
     * Format an error message using a CodeLocation.
     */
    public static String format(CodeLocation loc, String message) {
        if (loc == null || !loc.valid()) return message;
        return format(loc.fileName, loc.lineNo, loc.columnNo, message);
    }

    /**
     * Return the source line and caret string for the given location,
     * reading from the file on disk.
     *
     * @return the source line and caret, or null if the file can't be read
     */
    public static String sourceContext(String fileName, int line, int column) {
        try {
            List<String> lines = Files.readAllLines(Path.of(fileName));
            if (line >= 1 && line <= lines.size()) {
                return sourceContext(lines.get(line - 1), column);
            }
        } catch (IOException | RuntimeException e) {
            // file not readable; skip context
        }
        return null;
    }

    /**
     * Return the source line and caret for a given source line string and column.
     * Use this overload when the source code is already in memory.
     *
     * @param sourceLine  the text of the source line
     * @param column      1-based column number
     * @return the source line with a caret on the next line
     */
    public static String sourceContext(String sourceLine, int column) {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceLine);
        if (column >= 1) {
            sb.append("\n").append(" ".repeat(column - 1)).append("^");
        }
        return sb.toString();
    }
}
