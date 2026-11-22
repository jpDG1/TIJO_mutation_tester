package mutationtester.mutation;

import com.github.javaparser.ast.CompilationUnit;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class MutationGenerator {

    private final MutatorFactory factory;

    public MutationGenerator(MutatorFactory factory) {
        this.factory = factory;
    }

    public List<Mutant> generateMutants(CompilationUnit source) {
        return factory.createMutators().stream()
                .flatMap(mutator -> mutator.generateMutants(source).stream())
                .toList();
    }
}
