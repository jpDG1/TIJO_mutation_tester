package mutationtester.mutation;

import org.springframework.stereotype.Component;

import java.util.Map;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.DIVIDE;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.MINUS;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.MULTIPLY;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.PLUS;

@Component
public final class ArithmeticMutator extends BinaryOperatorMutator {

    public ArithmeticMutator() {
        super(Map.of(PLUS, MINUS, MINUS, PLUS, MULTIPLY, DIVIDE, DIVIDE, MULTIPLY));
    }
}
