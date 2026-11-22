package mutationtester.mutation;

import com.github.javaparser.ast.CompilationUnit;

import java.util.List;

/** Generates single-change mutants from a parsed Java source file. */
public interface Mutator {

    List<Mutant> generateMutants(CompilationUnit source);
}
