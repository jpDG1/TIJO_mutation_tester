package mutationtester.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Problem;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceParserTest {

    private final JavaSourceParser parser = new JavaSourceParser();

    @Test
    void parsesSourceAndPreservesLineNumbers() {
        // Arrange
        String source = """
                package sample;

                public class Calculator {
                    int add(int left, int right) {
                        return left + right;
                    }
                }
                """;

        // Act
        CompilationUnit compilationUnit = parser.parse(source);

        // Assert
        assertEquals("sample", compilationUnit.getPackageDeclaration().orElseThrow().getNameAsString());
        assertTrue(compilationUnit.getClassByName("Calculator").isPresent());
        BinaryExpr addition = compilationUnit.findFirst(BinaryExpr.class).orElseThrow();
        assertEquals(5, addition.getBegin().orElseThrow().line);
    }

    @Test
    void parsesJava17SealedClass() {
        // Arrange
        String source = """
                sealed class Shape permits Shape.Circle {
                    static final class Circle extends Shape {
                    }
                }
                """;

        // Act
        CompilationUnit compilationUnit = parser.parse(source);

        // Assert
        ClassOrInterfaceDeclaration shape = compilationUnit.getClassByName("Shape").orElseThrow();
        assertTrue(shape.hasModifier(Modifier.Keyword.SEALED));
    }

    @Test
    void reportsSyntaxProblemAtSourceLine() {
        // Arrange
        String source = """
                class Broken {
                    int value = ;
                }
                """;

        // Act
        ParseProblemException exception = assertThrows(ParseProblemException.class, () -> parser.parse(source));

        // Assert
        Problem problem = exception.getProblems().get(0);
        Range location = problem.getLocation().flatMap(TokenRange::toRange).orElseThrow();
        assertEquals(2, location.begin.line);
    }
}
