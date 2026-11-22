package mutationtester.integration;

import com.github.javaparser.ast.CompilationUnit;
import mutationtester.mutation.Mutant;
import mutationtester.mutation.MutationGenerator;
import mutationtester.parser.JavaSourceParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MutationGenerationIntegrationTest {

    private final MutationGenerator generator;

    @Autowired
    MutationGenerationIntegrationTest(MutationGenerator generator) {
        this.generator = generator;
    }

    @Test
    void generatesMutantsFromAllThreeSpringStrategies() {
        // Arrange
        String source = """
                class RangeCheck {
                    boolean exceeds(int value, int increment, int limit) {
                        if (value + increment > limit) {
                            return true;
                        }
                        return false;
                    }
                }
                """;
        CompilationUnit compilationUnit = new JavaSourceParser().parse(source);
        Set<String> expectedSources = Set.of(
                source.replace("value + increment", "value - increment"),
                source.replace("> limit", ">= limit"),
                source.replace("return true;", "return false;"),
                source.replace("return false;", "return true;")
        );

        // Act
        List<Mutant> mutants = generator.generateMutants(compilationUnit);

        // Assert
        assertEquals(4, mutants.size());
        assertEquals(expectedSources, mutants.stream().map(Mutant::sourceCode).collect(Collectors.toSet()));
        assertEquals(Set.of("ArithmeticMutator", "BooleanReturnMutator", "ConditionalBoundaryMutator"),
                mutants.stream().map(Mutant::mutatorName).collect(Collectors.toSet()));
    }
}
