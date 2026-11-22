package mutationtester.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class RunRepository {

    private final Map<UUID, MutationRun> runs = new ConcurrentHashMap<>();

    void save(MutationRun run) {
        runs.put(run.runId(), run);
    }

    Optional<MutationRun> find(UUID runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    void remove(UUID runId) {
        runs.remove(runId);
    }
}
