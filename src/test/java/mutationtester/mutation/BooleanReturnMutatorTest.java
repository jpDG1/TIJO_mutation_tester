package mutationtester.mutation;

import com.github.javaparser.ast.CompilationUnit;
import mutationtester.parser.JavaSourceParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BooleanReturnMutatorTest {

    private final JavaSourceParser parser = new JavaSourceParser();
    private final Mutator mutator = new BooleanReturnMutator();

    @Test
    void flipsBothBooleanLiteralsInIndependentMutants() {
        // Arrange
        String source = """
                class Flags {
                    boolean enabled() {
                        return true;
                    }
                    boolean disabled() {
                        return false;
                    }
                }
                """;
        CompilationUnit compilationUnit = parser.parse(source);
        Set<Mutant> expectedMutants = Set.of(
                new Mutant("BooleanReturnMutator", 3, "true", "false", source.replace("return true;", "return false;")),
                new Mutant("BooleanReturnMutator", 6, "false", "true", source.replace("return false;", "return true;"))
        );

        // Act
        List<Mutant> mutants = mutator.generateMutants(compilationUnit);

        // Assert
        assertEquals(2, mutants.size());
        assertEquals(expectedMutants, Set.copyOf(mutants));
    }

    @Test
    void reportsTheLiteralLineInAMultilineReturn() {
        // Arrange
        String source = """
                class Flags {
                    boolean enabled() {
                        return
                                /* Enable the flag. */ true;
                    }
                }
                """;
        CompilationUnit compilationUnit = parser.parse(source);
        String expectedSource = source.replace("*/ true;", "*/ false;");

        // Act
        List<Mutant> mutants = mutator.generateMutants(compilationUnit);

        // Assert
        assertEquals(List.of(new Mutant("BooleanReturnMutator", 4, "true", "false", expectedSource)), mutants);
    }

    @Test
    void ignoresReturnsWithoutDirectBooleanLiterals() {
        // Arrange
        String source = """
                class Flags {
                    private boolean active = true;

                    boolean isActive() {
                        return active && true;
                    }

                    int count() {
                        return 0;
                    }

                    void reset() {
                        active = false;
                        return;
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
