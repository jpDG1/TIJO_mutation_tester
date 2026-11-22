package mutationtester.mutation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MutatorFactoryTest {

    @Test
    void retainsRegisteredStrategiesInAnImmutableList() {
        // Arrange
        Mutator arithmetic = new ArithmeticMutator();
        Mutator extension = source -> List.of();
        List<Mutator> strategies = new ArrayList<>(List.of(arithmetic, extension));
        MutatorFactory factory = new MutatorFactory(strategies);
        strategies.clear();

        // Act
        List<Mutator> mutators = factory.createMutators();

        // Assert
        assertEquals(List.of(arithmetic, extension), mutators);
        assertThrows(UnsupportedOperationException.class, mutators::clear);
    }
}
