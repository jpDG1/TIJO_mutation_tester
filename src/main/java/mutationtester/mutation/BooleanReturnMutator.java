package mutationtester.mutation;

import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

public final class BooleanReturnMutator implements Mutator {

    @Override
    public List<Mutant> generateMutants(CompilationUnit source) {
        List<Mutant> mutants = new ArrayList<>();
        String mutatorName = getClass().getSimpleName();

        source.accept(new VoidVisitorAdapter<List<Mutant>>() {
            @Override
            public void visit(ReturnStmt statement, List<Mutant> result) {
                statement.getExpression()
                        .filter(Expression::isBooleanLiteralExpr)
                        .map(Expression::asBooleanLiteralExpr)
                        .ifPresent(literal -> {
                            JavaToken token = literal.getTokenRange().orElseThrow().getBegin();
                            String replacement = Boolean.toString(!literal.getValue());
                            result.add(Mutant.replaceToken(mutatorName, token, replacement));
                        });
                super.visit(statement, result);
            }
        }, mutants);

        return List.copyOf(mutants);
    }
}
