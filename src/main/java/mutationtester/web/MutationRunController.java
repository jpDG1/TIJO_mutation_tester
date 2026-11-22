package mutationtester.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import mutationtester.service.MutationRun;
import mutationtester.service.MutationRunService;
import mutationtester.service.RunStatus;
import mutationtester.web.dto.ApiErrorResponse;
import mutationtester.web.dto.RunAcceptedResponse;
import mutationtester.web.dto.RunProgressResponse;
import mutationtester.web.dto.RunReportResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/runs", produces = MediaType.APPLICATION_JSON_VALUE)
public final class MutationRunController {

    private final MutationRunService service;

    public MutationRunController(MutationRunService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Start a mutation analysis with a Java class and a JUnit test class")
    @ApiResponse(responseCode = "202", description = "Analysis accepted",
            content = @Content(schema = @Schema(implementation = RunAcceptedResponse.class)))
    @ApiResponse(responseCode = "400", description = "Missing or invalid upload",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "The original code or tests could not be validated",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public RunAcceptedResponse startRun(@RequestPart("sourceFile") MultipartFile sourceFile,
                                        @RequestPart("testFile") MultipartFile testFile) {
        String source = readJavaFile(sourceFile, "sourceFile");
        String tests = readJavaFile(testFile, "testFile");
        return new RunAcceptedResponse(service.startRun(source, tests));
    }

    @GetMapping("/{runId}/progress")
    @Operation(summary = "Get the status and number of processed mutants")
    @ApiResponse(responseCode = "200", description = "Current run progress",
            content = @Content(schema = @Schema(implementation = RunProgressResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown run ID",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public RunProgressResponse progress(@PathVariable("runId") UUID runId) {
        return RunProgressResponse.from(findRun(runId));
    }

    @GetMapping("/{runId}/report")
    @Operation(summary = "Get the completed mutation report")
    @ApiResponse(responseCode = "200", description = "Completed report",
            content = @Content(schema = @Schema(implementation = RunReportResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown run ID",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The report is unavailable for the current run status",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public RunReportResponse report(@PathVariable("runId") UUID runId) {
        MutationRun run = findRun(runId);
        if (run.status() != RunStatus.COMPLETED) {
            String reason = run.error() == null ? "The report is not ready." : run.error();
            throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
        }
        return RunReportResponse.from(runId, run.report());
    }

    private MutationRun findRun(UUID runId) {
        return service.findRun(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown run ID."));
    }

    private static String readJavaFile(MultipartFile file, String partName) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".java")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, partName + " must be a .java file.");
        }

        String source;
        try {
            source = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(file.getBytes())).toString();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, partName + " must be readable UTF-8 text.",
                    exception);
        }

        if (source.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, partName + " must contain Java source code.");
        }
        return source;
    }
}
