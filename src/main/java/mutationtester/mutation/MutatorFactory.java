package mutationtester.mutation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class MutatorFactory {

    private final List<Mutator> mutators;

    public MutatorFactory(List<Mutator> mutators) {
        this.mutators = List.copyOf(mutators);
    }

    public List<Mutator> createMutators() {
        return mutators;
    }
}
