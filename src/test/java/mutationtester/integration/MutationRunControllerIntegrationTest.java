package mutationtester.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static mutationtester.integration.PriceCalculatorSamples.SOURCE_CLASS_NAME;
import static mutationtester.integration.PriceCalculatorSamples.TEST_CLASS_NAME;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MutationRunControllerIntegrationTest {

    private final MockMvc mvc;
    private final ObjectMapper mapper;

    @Autowired
    MutationRunControllerIntegrationTest(MockMvc mvc, ObjectMapper mapper) {
        this.mvc = mvc;
        this.mapper = mapper;
    }

    @Test
    void uploadsFilesAndReturnsProgressAndReport() throws Exception {
        // Arrange
        Map<String, String> sources = PriceCalculatorSamples.sources();
        String source = sources.get(SOURCE_CLASS_NAME);

        // Act
        MvcResult accepted = mvc.perform(upload("PriceCalculator.java", source, sources.get(TEST_CLASS_NAME)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.runId").isNotEmpty())
                .andReturn();
        String runId = mapper.readTree(accepted.getResponse().getContentAsByteArray()).path("runId").asText();
        await().atMost(Duration.ofSeconds(30)).until(() -> !"RUNNING".equals(readRunStatus(runId)));
        ResultActions progress = mvc.perform(get("/api/runs/{runId}/progress", runId));
        ResultActions report = mvc.perform(get("/api/runs/{runId}/report", runId));

        // Assert
        progress.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.processedMutants").value(5))
                .andExpect(jsonPath("$.totalMutants").value(5));
        report.andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.sourceCode").value(source))
                .andExpect(jsonPath("$.mutationScore").value(100.0))
                .andExpect(jsonPath("$.killed").value(5))
                .andExpect(jsonPath("$.mutants.length()").value(5))
                .andExpect(jsonPath("$.survivingLines").isEmpty());
    }

    @Test
    void rejectsMissingTestFile() throws Exception {
        // Arrange
        String source = SampleSources.read("PriceCalculator.java");
        MockMultipartFile sourceFile = file("sourceFile", "PriceCalculator.java", source);

        // Act
        ResultActions response = mvc.perform(multipart("/api/runs").file(sourceFile));

        // Assert
        response.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @ParameterizedTest
    @MethodSource("invalidSourceFiles")
    void rejectsInvalidSourceFiles(String filename, String source) throws Exception {
        // Arrange
        String tests = SampleSources.read("PriceCalculatorStrongTest.java");

        // Act
        ResultActions response = mvc.perform(upload(filename, source, tests));

        // Assert
        response.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void rejectsMalformedJava() throws Exception {
        // Arrange
        String tests = SampleSources.read("PriceCalculatorStrongTest.java");

        // Act
        ResultActions response = mvc.perform(upload("PriceCalculator.java", "class PriceCalculator {", tests));

        // Assert
        response.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.diagnostics[0]").isString());
    }

    @Test
    void rejectsSourceThatCannotCompile() throws Exception {
        // Arrange
        Map<String, String> sources = PriceCalculatorSamples.sources();
        String invalidSource = sources.get(SOURCE_CLASS_NAME).replace("price * quantity", "price * missingQuantity");

        // Act
        ResultActions response = mvc.perform(upload("PriceCalculator.java", invalidSource, sources.get(TEST_CLASS_NAME)));

        // Assert
        response.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.diagnostics[0]").isString());
    }

    @Test
    void rejectsAnalysisWhenAnOriginalTestFails() throws Exception {
        // Arrange
        Map<String, String> sources = PriceCalculatorSamples.sources();
        String failingTests = sources.get(TEST_CLASS_NAME).replace("assertEquals(36, total)", "assertEquals(99, total)");

        // Act
        ResultActions response = mvc.perform(upload("PriceCalculator.java", sources.get(SOURCE_CLASS_NAME), failingTests));

        // Assert
        response.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.diagnostics[0]").isString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"progress", "report"})
    void returnsNotFoundForUnknownRun(String endpoint) throws Exception {
        // Arrange
        UUID runId = UUID.randomUUID();

        // Act
        ResultActions response = mvc.perform(get("/api/runs/{runId}/{endpoint}", runId, endpoint));

        // Assert
        response.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void listsRegisteredMutators() throws Exception {
        // Arrange
        String endpoint = "/api/mutators";

        // Act
        ResultActions response = mvc.perform(get(endpoint));

        // Assert
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$", containsInAnyOrder(
                        "ArithmeticMutator", "BooleanReturnMutator", "ConditionalBoundaryMutator")));
    }

    @Test
    void documentsTheUploadAndReportEndpoints() throws Exception {
        // Arrange
        String endpoint = "/v3/api-docs";

        // Act
        ResultActions response = mvc.perform(get(endpoint));

        // Assert
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/runs'].post.requestBody.content['multipart/form-data']").exists())
                .andExpect(jsonPath("$.paths['/api/runs'].post.responses['202']").exists())
                .andExpect(jsonPath("$.paths['/api/runs/{runId}/report'].get.responses['409']").exists());
    }

    private String readRunStatus(String runId) throws Exception {
        MvcResult result = mvc.perform(get("/api/runs/{runId}/progress", runId))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsByteArray()).path("status").asText();
    }

    private static Stream<Arguments> invalidSourceFiles() {
        return Stream.of(
                Arguments.of("PriceCalculator.txt", "class PriceCalculator {}"),
                Arguments.of("PriceCalculator.java", ""),
                Arguments.of("PriceCalculator.java", " \n\t")
        );
    }

    private static MockMultipartHttpServletRequestBuilder upload(String sourceName, String source, String tests) {
        return multipart("/api/runs")
                .file(file("sourceFile", sourceName, source))
                .file(file("testFile", "PriceCalculatorStrongTest.java", tests));
    }

    private static MockMultipartFile file(String field, String filename, String content) {
        return new MockMultipartFile(field, filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }
}
