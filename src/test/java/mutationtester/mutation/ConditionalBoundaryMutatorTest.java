package mutationtester.mutation;

import com.github.javaparser.ast.CompilationUnit;
import mutationtester.parser.JavaSourceParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionalBoundaryMutatorTest {

    private final JavaSourceParser parser = new JavaSourceParser();
    private final Mutator mutator = new ConditionalBoundaryMutator();

    @Test
    void generatesIndependentMutantsForAllFourBoundaryOperators() {
        // Arrange
        String source = """
                class RangeCheck {
                    boolean covers(int value, int lower, int upper) {
                        return value > lower && value >= lower && value < upper && value <= upper;
                    }
                }
                """;
        CompilationUnit compilationUnit = parser.parse(source);
        Set<String> expectedSources = Set.of(
                source.replace("value > lower", "value >= lower"),
                source.replace("value >= lower", "value > lower"),
                source.replace("value < upper", "value <= upper"),
                source.replace("value <= upper", "value < upper")
        );

        // Act
        List<Mutant> mutants = mutator.generateMutants(compilationUnit);

        // Assert
        assertEquals(4, mutants.size());
        assertEquals(expectedSources, mutants.stream().map(Mutant::sourceCode).collect(Collectors.toSet()));
    }

    @Test
    void reportsTheCompoundOperatorLineInSourceWithCrLf() {
        // Arrange
        String source = """
                class RangeCheck {
                    boolean admits(int value, int lower) {
                        return value
                                /* Include the lower bound. */ >= lower;
                    }
                }
                """.replace("\n", "\r\n");
        CompilationUnit compilationUnit = parser.parse(source);
        String expectedSource = source.replace(">= lower", "> lower");

        // Act
        List<Mutant> mutants = mutator.generateMutants(compilationUnit);

        // Assert
        assertEquals(List.of(new Mutant("ConditionalBoundaryMutator", 4, ">=", ">", expectedSource)), mutants);
    }

    @Test
    void ignoresTypeArgumentsEqualityAndShiftOperators() {
        // Arrange
        String source = """
                class Comparison<T extends Comparable<T>> {
                    boolean matches(int left, int right) {
                        return (left >> 1) == right || left != (right << 1);
                    }
                }
                """;
        CompilationUnit compilationUnit = parser.parse(source);

        // Act
        List<Mutant> mutants = mutator.generateMutants(compilationUnit);

        // Assert
        assertTrue(mutants.isEmpty());
    }
}
