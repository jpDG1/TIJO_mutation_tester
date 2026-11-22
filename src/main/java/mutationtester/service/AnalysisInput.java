package mutationtester.service;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import mutationtester.parser.JavaSourceParser;

import java.util.List;
import java.util.Map;

record AnalysisInput(
        String originalSource,
        CompilationUnit sourceTree,
        String sourceClassName,
        String testSource,
        String testClassName
) {

    static AnalysisInput parse(JavaSourceParser parser, String source, String tests) {
        CompilationUnit sourceTree = parseFile(parser, source, "source");
        CompilationUnit testTree = parseFile(parser, tests, "test");
        String sourceClassName = qualifiedName(sourceTree);
        String testClassName = qualifiedName(testTree);
        if (sourceClassName.equals(testClassName)) {
            throw new AnalysisRejectedException("The source and test classes must have different names.");
        }
        return new AnalysisInput(source, sourceTree, sourceClassName, tests, testClassName);
    }

    Map<String, String> compilationSources(String currentSource) {
        return Map.of(sourceClassName, currentSource, testClassName, testSource);
    }

    private static CompilationUnit parseFile(JavaSourceParser parser, String code, String fileRole) {
        try {
            CompilationUnit tree = parser.parse(code);
            if (tree.getTypes().size() != 1) {
                throw new AnalysisRejectedException("The " + fileRole + " file must declare one top-level type.");
            }
            return tree;
        } catch (ParseProblemException exception) {
            List<String> diagnostics = exception.getProblems().stream().map(Problem::getVerboseMessage).toList();
            throw new AnalysisRejectedException("Invalid Java syntax in the " + fileRole + " file.", diagnostics);
        }
    }

    private static String qualifiedName(CompilationUnit tree) {
        TypeDeclaration<?> type = tree.getType(0);
        return type.getFullyQualifiedName().orElseGet(type::getNameAsString);
    }
}
