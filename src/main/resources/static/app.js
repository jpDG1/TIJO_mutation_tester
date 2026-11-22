"use strict";

const byId = (id) => document.getElementById(id);
const form = byId("analysis-form");
const runButton = byId("run-button");
const weakSampleButton = byId("weak-sample");
const strongSampleButton = byId("strong-sample");
const clearButton = byId("clear-button");
const progressBar = byId("progress-bar");
const progressLabel = byId("progress-label");
const progressCount = byId("progress-count");
const log = byId("log");
const logEmpty = byId("log-empty");
const errorCard = byId("error-card");
const errorMessage = byId("error-message");
const errorDetails = byId("error-details");
const refreshVerificationButton = byId("refresh-verification");
const verificationNote = byId("verification-note");
const result = byId("result");
const scoreGauge = byId("score-gauge");
const score = byId("score");
const mutantRows = byId("mutant-rows");
const mutantsEmpty = byId("mutants-empty");
const reportSource = byId("report-source-lines");
const filterButtons = [...byId("status-filters").querySelectorAll("button[data-status]")];
const counterFields = {
    killed: byId("killed-count"),
    timedOut: byId("timed-out-count"),
    survived: byId("survived-count"),
    invalid: byId("invalid-count")
};
const editors = [
    { name: "Source class", field: byId("source-editor"), gutter: byId("source-gutter"),
        error: byId("source-error"), tab: byId("source-tab"), panel: byId("source-panel") },
    { name: "JUnit tests", field: byId("tests-editor"), gutter: byId("tests-gutter"),
        error: byId("tests-error"), tab: byId("tests-tab"), panel: byId("tests-panel") }
];
const [sourceEditor, testsEditor] = editors;
const actionButtons = [runButton, weakSampleButton, strongSampleButton, clearButton];
const statusBadges = new Map([
    ["KILLED", { icon: "✓", label: "Killed" }],
    ["TIMED_OUT", { icon: "◷", label: "Timed out" }],
    ["SURVIVED", { icon: "!", label: "Survived" }],
    ["INVALID", { icon: "–", label: "Invalid" }]
]);
const logIcons = { INFO: "›", SUCCESS: "✓", ERROR: "!" };
const timeFormat = new Intl.DateTimeFormat("en-GB", { timeStyle: "medium", hour12: false });
const scoreFormat = new Intl.NumberFormat("en", { style: "percent", maximumFractionDigits: 2 });
const utf8 = new TextEncoder();
const editorLimitKiB = 128;
const editorLimitBytes = editorLimitKiB * 1024;
const pollIntervalMs = 500;
const requestTimeoutMs = 15_000;
const startTimeoutMs = 30_000;
const inactivityTimeoutSeconds = 60;
const inactivityTimeoutMs = inactivityTimeoutSeconds * 1000;
let reportMutants = [];

const sampleSource = `package samples;

public class PriceCalculator {

    public int total(int price, int quantity) {
        return price * quantity;
    }

    public int applyDiscount(int total, int discount) {
        return total - discount;
    }

    public boolean qualifiesForDiscount(int total) {
        if (total >= 100) {
            return true;
        }
        return false;
    }
}
`;

const sampleTotalTest = `    @Test
    void returnsTotalForQuantity() {
        // Arrange
        PriceCalculator calculator = new PriceCalculator();

        // Act
        int total = calculator.total(12, 3);

        // Assert
        assertEquals(36, total);
    }`;

const weakSampleTests = `package samples;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceCalculatorWeakTest {

${sampleTotalTest}
}
`;

const strongSampleTests = `package samples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceCalculatorStrongTest {

${sampleTotalTest}

    @Test
    void subtractsDiscountFromTotal() {
        // Arrange
        PriceCalculator calculator = new PriceCalculator();

        // Act
        int discountedTotal = calculator.applyDiscount(120, 15);

        // Assert
        assertEquals(105, discountedTotal);
    }

    @ParameterizedTest
    @CsvSource({"99, false", "100, true"})
    void checksDiscountAtBoundary(int total, boolean expected) {
        // Arrange
        PriceCalculator calculator = new PriceCalculator();

        // Act
        boolean qualifies = calculator.qualifiesForDiscount(total);

        // Assert
        assertEquals(expected, qualifies);
    }
}
`;

class AnalysisRequestError extends Error {
    constructor(message, { status, diagnostics } = {}) {
        super(message);
        this.status = status;
        this.diagnostics = Array.isArray(diagnostics) ? diagnostics : [];
    }
}

function selectEditor(activeEditor) {
    for (const editor of editors) {
        const selected = editor === activeEditor;
        editor.tab.setAttribute("aria-selected", String(selected));
        editor.tab.tabIndex = selected ? 0 : -1;
        editor.panel.hidden = !selected;
    }
    syncGutter(activeEditor);
}

function handleTabNavigation(event, index) {
    const tabPositions = {
        ArrowLeft: (index + editors.length - 1) % editors.length,
        ArrowRight: (index + 1) % editors.length,
        Home: 0,
        End: editors.length - 1
    };
    if (!Object.hasOwn(tabPositions, event.key)) {
        return;
    }
    event.preventDefault();
    const activeEditor = editors[tabPositions[event.key]];
    selectEditor(activeEditor);
    activeEditor.tab.focus();
}

function updateGutter(editor) {
    const lineCount = editor.field.value.split(/\r\n?|\n/).length;
    if (editor.lineCount !== lineCount) {
        editor.gutter.textContent = Array.from({ length: lineCount }, (_, index) => index + 1).join("\n");
        editor.lineCount = lineCount;
    }
    syncGutter(editor);
}

function syncGutter(editor) {
    editor.gutter.scrollTop = editor.field.scrollTop;
}

function showValidation(editor, message = "") {
    editor.error.textContent = message;
    editor.error.hidden = !message;
    editor.field.setAttribute("aria-invalid", String(Boolean(message)));
}

function validateEditor(editor) {
    const code = editor.field.value;
    let message = "";
    if (!code.trim()) {
        message = `${editor.name} is required.`;
    } else if (code.length > editorLimitBytes || utf8.encode(code).length > editorLimitBytes) {
        message = `${editor.name} must be ${editorLimitKiB} KiB or smaller.`;
    }
    showValidation(editor, message);
    return !message;
}

function setupEditor(editor, index) {
    const { field, tab } = editor;
    let escapeTab = false;
    tab.addEventListener("click", () => selectEditor(editor));
    tab.addEventListener("keydown", (event) => handleTabNavigation(event, index));
    field.addEventListener("input", () => {
        updateGutter(editor);
        if (!editor.error.hidden) {
            validateEditor(editor);
        }
    });
    field.addEventListener("scroll", () => syncGutter(editor));
    field.addEventListener("blur", () => { escapeTab = false; });
    field.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            escapeTab = true;
            return;
        }
        const shouldIndent = event.key === "Tab" && !escapeTab && !event.shiftKey && !event.altKey
            && !event.ctrlKey && !event.metaKey && !event.isComposing && !field.readOnly;
        escapeTab = false;
        if (shouldIndent) {
            event.preventDefault();
            field.setRangeText("    ", field.selectionStart, field.selectionEnd, "end");
            field.dispatchEvent(new Event("input", { bubbles: true }));
        }
    });
    updateGutter(editor);
}

function replaceInput(source, tests) {
    sourceEditor.field.value = source;
    testsEditor.field.value = tests;
    for (const editor of editors) {
        editor.field.setSelectionRange(0, 0);
        editor.field.scrollTop = 0;
        editor.field.scrollLeft = 0;
        updateGutter(editor);
        showValidation(editor);
    }
    selectEditor(sourceEditor);
    resetOutput();
}

function setProgressLabel(state, message) {
    progressLabel.dataset.state = state;
    if (progressLabel.textContent !== message) {
        progressLabel.textContent = message;
    }
}

function setRunning(running) {
    for (const button of actionButtons) {
        button.disabled = running;
    }
    for (const editor of editors) {
        editor.field.readOnly = running;
    }
    runButton.textContent = running ? "Analyzing…" : "Run analysis";
}

function resetOutput() {
    reportMutants = [];
    result.hidden = true;
    errorCard.hidden = true;
    errorDetails.replaceChildren();
    errorDetails.hidden = true;
    errorMessage.textContent = "";
    log.replaceChildren();
    logEmpty.hidden = false;
    setProgressLabel("idle", "Ready to analyze");
    progressBar.max = 1;
    progressBar.value = 0;
    progressCount.textContent = "";
}

function parseJson(text) {
    try {
        return JSON.parse(text);
    } catch {
        return null;
    }
}

async function requestJson(url, { timeoutMs = requestTimeoutMs, ...options } = {}) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
        const response = await fetch(url, { ...options, signal: controller.signal });
        const data = parseJson(await response.text());
        if (!response.ok) {
            const message = data?.message || data?.detail || `Server returned an error (HTTP ${response.status}).`;
            throw new AnalysisRequestError(message, { status: response.status, diagnostics: data?.diagnostics });
        }
        if (!data || typeof data !== "object") {
            throw new AnalysisRequestError("The server returned an invalid response.");
        }
        return data;
    } catch (error) {
        if (controller.signal.aborted) {
            throw new AnalysisRequestError("The server did not respond in time. Please try again.");
        }
        if (error instanceof AnalysisRequestError) {
            throw error;
        }
        throw new AnalysisRequestError("Could not reach the server. Please try again.");
    } finally {
        clearTimeout(timeout);
    }
}

async function startRun(input) {
    try {
        return await requestJson("/api/runs", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(input),
            timeoutMs: startTimeoutMs
        });
    } catch (error) {
        if (error.status !== 415) {
            throw error;
        }
    }
    // Older controllers accept only multipart Java files.
    const files = new FormData();
    files.append("sourceFile", new Blob([input.sourceCode], { type: "text/plain" }), "Source.java");
    files.append("testFile", new Blob([input.testCode], { type: "text/plain" }), "Tests.java");
    return requestJson("/api/runs", { method: "POST", body: files, timeoutMs: startTimeoutMs });
}

function showProjectTestCounts(suite, { passed, failed, skipped }) {
    if (![passed, failed, skipped].every(Number.isInteger)) {
        throw new AnalysisRequestError("The server returned invalid Maven test results.");
    }
    byId(`project-${suite}-count`).textContent = passed + failed;
    byId(`project-${suite}-detail`).textContent = `${passed} passed · ${failed} failed · ${skipped} skipped`;
}

async function refreshVerification() {
    refreshVerificationButton.disabled = true;
    verificationNote.textContent = "Loading Maven results…";
    try {
        const { unit, integration } = await requestJson("/api/project-verification", { cache: "no-store" });
        showProjectTestCounts("unit", unit);
        showProjectTestCounts("integration", integration);
        const hasReports = Object.values(unit).some(Boolean) || Object.values(integration).some(Boolean);
        verificationNote.textContent = hasReports
            ? "Latest available Maven reports. Run mvn verify and refresh to update."
            : "Run mvn verify in the project directory to generate test results.";
    } catch (error) {
        verificationNote.textContent = `Could not load project test results. ${error.message}`;
    } finally {
        refreshVerificationButton.disabled = false;
    }
}

function appendLog(message, level = "INFO", time = new Date()) {
    const entry = document.createElement("li");
    entry.className = "log-entry";
    entry.dataset.level = level;

    const icon = document.createElement("span");
    icon.className = "log-icon";
    icon.setAttribute("aria-hidden", "true");
    icon.textContent = logIcons[level];

    const timestamp = document.createElement("time");
    timestamp.className = "log-time";
    const date = new Date(time);
    if (Number.isFinite(date.valueOf())) {
        timestamp.dateTime = date.toISOString();
        timestamp.textContent = timeFormat.format(date);
    } else {
        timestamp.textContent = String(time);
    }

    const text = document.createElement("span");
    text.className = "log-message";
    text.textContent = message;
    entry.append(icon, timestamp, text);
    const wasAtBottom = log.scrollHeight - log.clientHeight - log.scrollTop <= 8;
    log.append(entry);
    logEmpty.hidden = true;
    if (wasAtBottom) {
        log.scrollTop = log.scrollHeight;
    }
}

function appendServerLogs(entries, tracking) {
    if (entries.length > 0 && !tracking.usesApiLogs) {
        tracking.usesApiLogs = true;
        log.replaceChildren();
    }
    const snapshotCounts = new Map();
    for (const entry of entries) {
        if (typeof entry?.message !== "string") {
            continue;
        }
        const { message, time, level } = entry;
        const key = JSON.stringify([time, level, message]);
        const count = (snapshotCounts.get(key) || 0) + 1;
        snapshotCounts.set(key, count);
        if (count > (tracking.logCounts.get(key) || 0)) {
            appendLog(message, level === "ERROR" ? "ERROR" : "INFO", time);
            tracking.logCounts.set(key, count);
            tracking.lastChange = performance.now();
        }
    }
}

function appendProgressLogs(progress, tracking) {
    const { logEntries, totalMutants, processedMutants, status } = progress;
    if (Array.isArray(logEntries) && logEntries.length > 0) {
        appendServerLogs(logEntries, tracking);
        return;
    }
    if (tracking.usesApiLogs) {
        return;
    }
    if (tracking.lastProgress === null) {
        appendLog("Parsing source: completed.", "SUCCESS");
        appendLog("Running baseline tests: passed.", "SUCCESS");
    }
    if (!tracking.loggedGeneration && (totalMutants > 0 || status === "COMPLETED")) {
        appendLog(`Generating mutants (${totalMutants} found).`);
        tracking.loggedGeneration = true;
    }
    const last = tracking.lastProgress;
    if (totalMutants > 0 && (totalMutants !== last?.totalMutants || processedMutants !== last?.processedMutants)) {
        const message = processedMutants < totalMutants
            ? `Running mutant ${processedMutants + 1}/${totalMutants}…`
            : `Finished running mutants (${processedMutants}/${totalMutants}).`;
        appendLog(message);
    }
}

function updateProgress({ status, processedMutants, totalMutants }) {
    if (totalMutants > 0) {
        progressBar.max = totalMutants;
        progressBar.value = processedMutants;
        progressCount.textContent = `${processedMutants} / ${totalMutants}`;
        setProgressLabel("running", "Running mutants");
    } else if (status === "COMPLETED") {
        progressBar.max = 1;
        progressBar.value = 1;
        progressCount.textContent = "0 / 0";
    }
}

async function waitForCompletion(runUrl) {
    const tracking = {
        lastProgress: null, lastChange: performance.now(), logCounts: new Map(),
        usesApiLogs: false, loggedGeneration: false
    };
    while (true) {
        const progress = await requestJson(`${runUrl}/progress`, { cache: "no-store" });
        const { status, totalMutants, processedMutants } = progress;
        if (status === "FAILED") {
            appendProgressLogs(progress, tracking);
            const loggedError = Array.isArray(progress.logEntries)
                ? progress.logEntries.findLast((entry) => entry?.level === "ERROR")?.message : "";
            throw new AnalysisRequestError(progress.error || progress.message || loggedError || "The analysis failed.", {
                diagnostics: progress.diagnostics
            });
        }
        if (status !== "RUNNING" && status !== "COMPLETED") {
            throw new AnalysisRequestError("The server returned an unknown run status.");
        }
        if (!Number.isInteger(totalMutants) || !Number.isInteger(processedMutants)
            || processedMutants < 0 || totalMutants < processedMutants) {
            throw new AnalysisRequestError("The server returned invalid progress counts.");
        }
        appendProgressLogs(progress, tracking);
        updateProgress(progress);
        if (status === "COMPLETED") {
            if (!tracking.usesApiLogs) {
                appendLog("Building report…");
            }
            return;
        }
        const previous = tracking.lastProgress;
        if (totalMutants !== previous?.totalMutants || processedMutants !== previous?.processedMutants) {
            tracking.lastChange = performance.now();
        }
        if (performance.now() - tracking.lastChange >= inactivityTimeoutMs) {
            throw new AnalysisRequestError(`No analysis progress for ${inactivityTimeoutSeconds} seconds. Please try again.`);
        }
        tracking.lastProgress = progress;
        await new Promise((resolve) => setTimeout(resolve, pollIntervalMs));
    }
}

function showError(error) {
    const message = error.message || "The analysis failed.";
    const lastEntry = log.lastElementChild;
    if (lastEntry?.dataset.level !== "ERROR" || lastEntry.lastElementChild?.textContent !== message) {
        appendLog(message, "ERROR");
    }
    if (!progressBar.hasAttribute("value")) {
        progressBar.value = 0;
    }
    setProgressLabel("failed", "Analysis failed");
    errorMessage.textContent = message;
    const diagnostics = (error.diagnostics || []).map((diagnostic) => {
        const item = document.createElement("li");
        item.textContent = diagnostic;
        return item;
    });
    errorDetails.replaceChildren(...diagnostics);
    errorDetails.hidden = diagnostics.length === 0;
    errorCard.hidden = false;
}

function createStatusBadge(status) {
    const badge = document.createElement("span");
    badge.className = "status-badge";
    badge.dataset.status = status;
    const { icon, label } = statusBadges.get(status) || { icon: "?", label: "Unknown" };
    const symbol = document.createElement("span");
    symbol.setAttribute("aria-hidden", "true");
    symbol.textContent = icon;
    badge.append(symbol, label);
    return badge;
}

function createMutantRow(mutant) {
    const row = document.createElement("tr");
    row.insertCell().textContent = mutant.id;
    row.insertCell().textContent = mutant.mutator;

    const line = document.createElement("a");
    line.href = `#source-line-${mutant.line}`;
    line.textContent = mutant.line;
    row.insertCell().append(line);
    row.insertCell().textContent = mutant.description;
    row.insertCell().append(createStatusBadge(mutant.status));
    return row;
}

function filterMutants(status) {
    for (const button of filterButtons) {
        button.setAttribute("aria-pressed", String(button.dataset.status === status));
    }
    const shown = status === "ALL" ? reportMutants : reportMutants.filter((mutant) => mutant.status === status);
    const rows = document.createDocumentFragment();
    for (const mutant of shown) {
        rows.append(createMutantRow(mutant));
    }
    mutantRows.replaceChildren(rows);
    mutantsEmpty.textContent = reportMutants.length === 0 ? "No mutants were generated." : "No mutants match this filter.";
    mutantsEmpty.hidden = shown.length > 0;
}

function survivingMutantsByLine(mutants) {
    const descriptions = new Map();
    for (const { status, id, line, mutator, description } of mutants) {
        if (status === "SURVIVED") {
            const onLine = descriptions.get(line) || [];
            onLine.push(`Mutant ${id} · ${mutator}: ${description}`);
            descriptions.set(line, onLine);
        }
    }
    return descriptions;
}

function markSurvivingLine(item, descriptions) {
    item.classList.add("is-surviving");
    item.title = descriptions.join("\n");
    item.setAttribute("aria-description", descriptions.join(". "));
    item.tabIndex = 0;
    const marker = document.createElement("span");
    marker.className = "survivor-marker";
    marker.setAttribute("aria-hidden", "true");
    marker.textContent = "◆";
    item.append(marker);
}

function renderSource(source, mutants) {
    const survivors = survivingMutantsByLine(mutants);
    const lines = document.createDocumentFragment();
    source.split(/\r\n?|\n/).forEach((line, index) => {
        const lineNumber = index + 1;
        const item = document.createElement("li");
        item.id = `source-line-${lineNumber}`;
        if (survivors.has(lineNumber)) {
            markSurvivingLine(item, survivors.get(lineNumber));
        }
        const code = document.createElement("code");
        code.textContent = line;
        item.append(code);
        lines.append(item);
    });
    reportSource.replaceChildren(lines);
}

function renderReport(report, sourceCode) {
    if (!Number.isFinite(report.mutationScore) || !Array.isArray(report.mutants)
        || Object.keys(counterFields).some((field) => !Number.isInteger(report[field]))) {
        throw new AnalysisRequestError("The server returned an invalid report.");
    }
    const percentage = Math.max(0, Math.min(100, report.mutationScore));
    const formattedScore = scoreFormat.format(percentage / 100);
    score.textContent = formattedScore;
    scoreGauge.style.setProperty("--score", `${percentage}%`);
    scoreGauge.setAttribute("aria-valuenow", String(percentage));
    scoreGauge.setAttribute("aria-valuetext", formattedScore);
    for (const [field, element] of Object.entries(counterFields)) {
        element.textContent = report[field];
    }
    reportMutants = report.mutants;
    filterMutants("ALL");
    renderSource(typeof report.sourceCode === "string" ? report.sourceCode : sourceCode, reportMutants);
    result.hidden = false;
}

async function analyze(event) {
    event.preventDefault();
    if (runButton.disabled) {
        return;
    }
    const invalidEditors = editors.filter((editor) => !validateEditor(editor));
    if (invalidEditors.length > 0) {
        selectEditor(invalidEditors[0]);
        invalidEditors[0].field.focus();
        return;
    }

    const input = { sourceCode: sourceEditor.field.value, testCode: testsEditor.field.value };
    resetOutput();
    setRunning(true);
    progressBar.removeAttribute("value");
    setProgressLabel("running", "Checking input");
    appendLog("Parsing source and running baseline tests…");
    try {
        const { runId } = await startRun(input);
        if (typeof runId !== "string" || !runId.trim()) {
            throw new AnalysisRequestError("The server did not return a run ID.");
        }
        const runUrl = `/api/runs/${encodeURIComponent(runId)}`;
        setProgressLabel("running", "Generating mutants");
        await waitForCompletion(runUrl);
        setProgressLabel("running", "Building report");
        renderReport(await requestJson(`${runUrl}/report`, { cache: "no-store" }), input.sourceCode);
        appendLog("Analysis complete.", "SUCCESS");
        setProgressLabel("completed", "Analysis complete");
    } catch (error) {
        showError(error);
    } finally {
        setRunning(false);
    }
}

editors.forEach(setupEditor);
selectEditor(sourceEditor);
resetOutput();
form.addEventListener("submit", analyze);
weakSampleButton.addEventListener("click", () => replaceInput(sampleSource, weakSampleTests));
strongSampleButton.addEventListener("click", () => replaceInput(sampleSource, strongSampleTests));
clearButton.addEventListener("click", () => replaceInput("", ""));
byId("project-manual-count").textContent = byId("manual-test-cases").childElementCount;
refreshVerificationButton.addEventListener("click", refreshVerification);
refreshVerification();
for (const button of filterButtons) {
    button.addEventListener("click", () => filterMutants(button.dataset.status));
}
