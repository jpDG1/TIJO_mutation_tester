package mutationtester.web;

import io.swagger.v3.oas.annotations.Operation;
import mutationtester.mutation.MutatorFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public final class MutatorController {

    private final MutatorFactory factory;

    public MutatorController(MutatorFactory factory) {
        this.factory = factory;
    }

    @GetMapping(value = "/api/mutators", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List the registered mutators")
    public List<String> listMutators() {
        return factory.createMutators().stream()
                .map(mutator -> mutator.getClass().getSimpleName())
                .sorted()
                .toList();
    }
}
