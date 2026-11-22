package mutationtester.mutation;

import java.util.Map;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.DIVIDE;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.MINUS;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.MULTIPLY;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.PLUS;

public final class ArithmeticMutator extends BinaryOperatorMutator {

    public ArithmeticMutator() {
        super(Map.of(PLUS, MINUS, MINUS, PLUS, MULTIPLY, DIVIDE, DIVIDE, MULTIPLY));
    }
}
