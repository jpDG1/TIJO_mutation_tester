package mutationtester.mutation;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;

public record Mutant(
        String mutatorName,
        int lineNumber,
        String originalToken,
        String replacementToken,
        String sourceCode
) {

    static Mutant replaceToken(String mutatorName, JavaToken original, String replacement) {
        // A separator keeps an adjacent unary operator or comment from merging with the replacement.
        boolean needsSeparator = original.getNextToken()
                .map(JavaToken::getText)
                .filter(nextText -> nextText.startsWith(replacement))
                .isPresent();
        String replacementText = needsSeparator ? replacement + " " : replacement;

        TokenRange sourceTokens = new TokenRange(original.findFirstToken(), original.findLastToken());
        StringBuilder sourceCode = new StringBuilder();
        for (JavaToken token : sourceTokens) {
            sourceCode.append(token == original ? replacementText : token.getText());
        }

        int lineNumber = original.getRange().orElseThrow().begin.line;
        return new Mutant(mutatorName, lineNumber, original.getText(), replacement, sourceCode.toString());
    }
}
