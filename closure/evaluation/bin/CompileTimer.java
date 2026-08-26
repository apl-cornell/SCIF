import ast.SourceFile;
import java.io.File;
import java.util.*;

/**
 * Times the SCIF front end the way the SCIF paper's Table 1 does: the
 * whole checking pipeline for one program, in one process.
 *
 * The SCIF CLI silently fails when invoked from a directory other than
 * the repository root (regular typecheck returns false with no
 * diagnostic), so this harness drives the compiler API directly, the
 * same way the JUnit suite does.
 *
 * Usage: CompileTimer <reps> <file.scif> ...
 *   reps = 1  -> cold numbers (JIT not warmed): what a user experiences
 *   reps > 1  -> median of reps, all in the same JVM (steady state)
 *
 * Prints TSV: file, regular_ms, ifc_ms, total_ms, ok
 */
public class CompileTimer {
    public static void main(String[] args) throws Exception {
        int reps = Integer.parseInt(args[0]);
        System.out.println("file\tregular_ms\tifc_ms\ttotal_ms\tok");
        for (int a = 1; a < args.length; a++) {
            File f = new File(args[a]);
            List<Double> reg = new ArrayList<>(), ifc = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < reps; i++) {
                ArrayList<File> files = new ArrayList<>();
                files.add(f);
                long t0 = System.nanoTime();
                List<SourceFile> roots = Preprocessor.preprocess(files);
                boolean r1 = TypeChecker.regularTypecheck(roots, false);
                long t1 = System.nanoTime();
                boolean r2 = TypeChecker.ifcTypecheck(roots, false);
                long t2 = System.nanoTime();
                ok = r1 && r2;
                reg.add((t1 - t0) / 1e6);
                ifc.add((t2 - t1) / 1e6);
            }
            Collections.sort(reg);
            Collections.sort(ifc);
            double r = reg.get(reg.size() / 2), i2 = ifc.get(ifc.size() / 2);
            System.out.printf("%s\t%.1f\t%.1f\t%.1f\t%s%n", f.getName(), r, i2, r + i2, ok);
        }
    }
}
