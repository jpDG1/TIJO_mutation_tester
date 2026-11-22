package mutationtester.mutation;

import org.springframework.stereotype.Component;

import java.util.Map;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.GREATER;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.GREATER_EQUALS;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.LESS;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.LESS_EQUALS;

@Component
public final class ConditionalBoundaryMutator extends BinaryOperatorMutator {

    public ConditionalBoundaryMutator() {
        super(Map.of(
                GREATER, GREATER_EQUALS,
                GREATER_EQUALS, GREATER,
                LESS, LESS_EQUALS,
                LESS_EQUALS, LESS
        ));
    }
}
