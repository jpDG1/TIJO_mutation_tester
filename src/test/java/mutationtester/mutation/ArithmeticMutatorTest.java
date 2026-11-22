package mutationtester.mutation;

import com.github.javaparser.ast.CompilationUnit;
import mutationtester.parser.JavaSourceParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArithmeticMutatorTest {

    private final JavaSourceParser parser = new JavaSourceParser();
    private final Mutator mutator = new ArithmeticMutator();

    @Test
    void generatesIndependentMutantsForAllFourArithmeticOperators() {
        // Arrange
        String source = """
                class Calculator {
                    int calculate(int left, int right, int divisor) {
                        return (left + right) - (left * right / divisor);
                    }
                }
                """;
        CompilationUnit compilationUnit = parser.parse(source);
        CompilationUnit original = compilationUnit.clone();
        Set<String> expectedSources = Set.of(
                source.replace("left + right", "left - right"),
                source.replace(") - (", ") + ("),
                source.replace("left * right", "left / right"),
                source.replace("right / divisor", "right * divisor")
        );

        // Act
        List<Mutant> mutants = mutator.generateMutants(compilationUnit);

        // Assert
        assertEquals(4, mutants.size());
        assertEquals(expectedSources, mutants.stream().map(Mutant::sourceCode).collect(Collectors.toSet()));
        assertEquals(original, compilationUnit);
    }

    @Test
    void reportsTheOperatorLineAndPreservesCommentsInMultilineSource() {
        // Arrange
        String source = "\n" + """
                class Calculator {
                    int add(int left, int right) {
                        return left
                                /* Add the second operand. */ + right;
                    }
                }
                """;
        CompilationUnit compilationUnit = parser.parse(source);
        String expectedSource = source.replace(" + right", " - right");

        // Act
        List<Mutant> mutants = mutator.generateMutants(compilationUnit);

        // Assert
        assertEquals(List.of(new Mutant("ArithmeticMutator", 5, "+", "-", expectedSource)), mutants);
    }

    @Test
    void treatsAdjacentUnaryOperatorsAndCommentsAsSeparateTokens() {
        // Arrange
        String source = """
                class Calculator {
                    int calculate(int left, int right) {
                        return (left+-right) % (left*/* factor */right);
                    }
                }
                """;
        CompilationUnit compilationUnit = parser.parse(source);
        Set<String> expectedSources = Set.of(
                source.replace("left+-right", "left- -right"),
                source.replace("left*/* factor */right", "left/ /* factor */right")
        );

        // Act
        List<Mutant> mutants = mutator.generateMutants(compilationUnit);

        // Assert
        assertEquals(2, mutants.size());
        assertEquals(expectedSources, mutants.stream().map(Mutant::sourceCode).collect(Collectors.toSet()));
    }

    @Test
    void leavesUnaryAndUnsupportedBinaryOperatorsAlone() {
        // Arrange
        String source = """
                class Calculator {
                    boolean compare(int left, int right) {
                        return -left % 2 < right && +right != 0;
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
