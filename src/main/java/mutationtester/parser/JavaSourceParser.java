package mutationtester.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_17;

public final class JavaSourceParser {

    /**
     * Parses Java 17 source text into an abstract syntax tree with source positions.
     *
     * @throws com.github.javaparser.ParseProblemException if the source cannot be parsed
     */
    public CompilationUnit parse(String source) {
        ParserConfiguration configuration = new ParserConfiguration().setLanguageLevel(JAVA_17);
        JavaParser parser = new JavaParser(configuration);
        return JavaParserAdapter.of(parser).parse(source);
    }
}
