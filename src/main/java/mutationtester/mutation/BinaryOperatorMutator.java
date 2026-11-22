package mutationtester.mutation;

import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class BinaryOperatorMutator implements Mutator {

    private final Map<Operator, Operator> replacements;

    protected BinaryOperatorMutator(Map<Operator, Operator> replacements) {
        this.replacements = Map.copyOf(replacements);
    }

    @Override
    public final List<Mutant> generateMutants(CompilationUnit source) {
        List<Mutant> mutants = new ArrayList<>();
        String mutatorName = getClass().getSimpleName();

        source.accept(new VoidVisitorAdapter<List<Mutant>>() {
            @Override
            public void visit(BinaryExpr expression, List<Mutant> result) {
                Operator replacement = replacements.get(expression.getOperator());
                if (replacement != null) {
                    JavaToken operator = findOperatorToken(expression);
                    result.add(Mutant.replaceToken(mutatorName, operator, replacement.asString()));
                }
                super.visit(expression, result);
            }
        }, mutants);

        return List.copyOf(mutants);
    }

    private static JavaToken findOperatorToken(BinaryExpr expression) {
        JavaToken token = expression.getLeft().getTokenRange().orElseThrow().getEnd();
        do {
            token = token.getNextToken().orElseThrow();
        } while (token.getCategory().isWhitespaceOrComment());
        return token;
    }
}
