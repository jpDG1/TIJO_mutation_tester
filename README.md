# Mini Mutation Tester

## Nazwa kursu

Testowanie i Jakość Oprogramowania II, Akademia Tarnowska.

## Autor

jpDG1 (konto GitHub)

## Temat projektu

Mini Mutation Tester to aplikacja webowa oceniająca skuteczność testów JUnit 5 za pomocą testowania mutacyjnego.

## Opis projektu

1. Użytkownik wkleja klasę Java do zakładki **Source class**, a klasę testową do zakładki **JUnit tests**. Może też załadować jeden z gotowych przykładów.
2. Aplikacja parsuje oba pliki przy użyciu JavaParsera, kompiluje je w pamięci i uruchamia testy na oryginalnym kodzie (*baseline*). Błąd składni, błąd kompilacji, brak wykrytych testów lub nieudany przebieg bazowy przerywa analizę.
3. Dla każdego pasującego fragmentu kodu aplikacja tworzy mutanta z jedną zmianą. Każdy mutant jest kompilowany w pamięci i sprawdzany tym samym zestawem testów w osobnym procesie JVM. Na kompilację i wykonanie testów w tym procesie przypada łącznie 10 sekund.
4. Raport pokazuje wynik mutacyjny, liczniki statusów i tabelę mutantów z filtrami. Podświetlone linie kodu wskazują miejsca, w których mutanty przeżyły.

| Mutator | Zmiany |
| --- | --- |
| `ArithmeticMutator` | `+` ↔ `-`, `*` ↔ `/` |
| `ConditionalBoundaryMutator` | `>` ↔ `>=`, `<` ↔ `<=` |
| `BooleanReturnMutator` | `return true` ↔ `return false` |

| Status | Znaczenie |
| --- | --- |
| `KILLED` | Co najmniej jeden test wykrył zmianę. |
| `TIMED_OUT` | Wykonanie testów przekroczyło limit czasu. Wynik zalicza mutanta jako wykrytego. |
| `SURVIVED` | Wszystkie testy przeszły mimo zmiany. |
| `INVALID` | Kompilacja mutanta zakończyła się błędem. Obliczenie wyniku pomija tego mutanta. |

```text
mutation score = 100% × (KILLED + TIMED_OUT) / (KILLED + TIMED_OUT + SURVIVED)
```

Przy braku poprawnie skompilowanych mutantów wynik wynosi 0%. Jedno uruchomienie przyjmuje jedną klasę źródłową korzystającą ze standardowej biblioteki JDK i jedną klasę testową JUnit 5. Aplikacja działa lokalnie i wykonuje kod dostarczony przez użytkownika. Używaj własnego lub zaufanego kodu.

### Architektura

| Pakiet | Odpowiedzialność |
| --- | --- |
| `web`, `service` | REST API, walidacja zgłoszenia i koordynacja analizy |
| `parser`, `mutation` | Parsowanie kodu i generowanie mutantów |
| `compile`, `execution` | Kompilacja w pamięci, ładowanie klas i uruchamianie testów w JVM |
| `report`, `progress` | Wynik mutacyjny, raport i aktualizacja postępu |
| `verification` | Odczyt raportów Maven Surefire |

Interfejs `Mutator` realizuje wzorzec **Strategy**. `MutatorFactory` udostępnia zestaw implementacji wstrzykniętych przez Springa (**Factory**). Mutatory odwiedzają węzły AST za pomocą **Visitor**, a `ProgressTracker` powiadamia słuchaczy o zmianach (**Observer**). Aby dodać mutator, wystarczy napisać implementację `Mutator` i zarejestrować ją jako komponent Springa.

## Uruchomienie projektu

Wymagania to JDK 17 i Maven. W katalogu zawierającym [pom.xml](pom.xml) wykonaj polecenie.

```sh
mvn spring-boot:run
```

Otwórz [interfejs aplikacji](http://127.0.0.1:8080/). Kliknij **Load sample (weak tests)** i **Run analysis**, aby zobaczyć wynik 20%. Powtórz z **Load sample (strong tests)**, aby uzyskać 100%.

Pliki przykładowe znajdują się w [src/test/resources/samples](src/test/resources/samples). Można skopiować ich zawartość do obu edytorów.

## Testy

Zestaw projektu obejmuje **22 testy jednostkowe**, **26 testów integracyjnych** i **11 scenariuszy manualnych**. Testy automatyczne używają konwencji AAA z komentarzami `// Arrange`, `// Act` oraz `// Assert`. Pliki w katalogu `samples` są danymi wejściowymi do analizy.

### Testy jednostkowe

| Zakres | Pliki |
| --- | --- |
| Parsowanie poprawnego i błędnego kodu | [JavaSourceParserTest](src/test/java/mutationtester/parser/JavaSourceParserTest.java) |
| Generowanie zmian i rejestracja mutatorów | [ArithmeticMutatorTest](src/test/java/mutationtester/mutation/ArithmeticMutatorTest.java), [ConditionalBoundaryMutatorTest](src/test/java/mutationtester/mutation/ConditionalBoundaryMutatorTest.java), [BooleanReturnMutatorTest](src/test/java/mutationtester/mutation/BooleanReturnMutatorTest.java), [MutatorFactoryTest](src/test/java/mutationtester/mutation/MutatorFactoryTest.java) |
| Punktacja, raport i powiadomienia o postępie | [ScoreCalculatorTest](src/test/java/mutationtester/report/ScoreCalculatorTest.java), [MutationReportTest](src/test/java/mutationtester/report/MutationReportTest.java), [ProgressTrackerTest](src/test/java/mutationtester/progress/ProgressTrackerTest.java) |
| Odczyt i zliczanie wyników Maven | [SurefireReportReaderTest](src/test/java/mutationtester/verification/SurefireReportReaderTest.java) |

### Testy integracyjne

| Zakres | Pliki |
| --- | --- |
| Kompilacja w pamięci, uruchamianie JUnit i limit czasu | [InMemoryCompilerIntegrationTest](src/test/java/mutationtester/integration/InMemoryCompilerIntegrationTest.java), [TestRunnerIntegrationTest](src/test/java/mutationtester/integration/TestRunnerIntegrationTest.java), [MutantExecutorIntegrationTest](src/test/java/mutationtester/integration/MutantExecutorIntegrationTest.java) |
| Generowanie mutantów i pełna analiza z raportem | [MutationGenerationIntegrationTest](src/test/java/mutationtester/integration/MutationGenerationIntegrationTest.java), [MutationRunServiceIntegrationTest](src/test/java/mutationtester/integration/MutationRunServiceIntegrationTest.java) |
| Kontrakt REST, walidacja wejścia i kody odpowiedzi | [MutationRunControllerIntegrationTest](src/test/java/mutationtester/integration/MutationRunControllerIntegrationTest.java) |

### Polecenia Maven

| Cel | Polecenie |
| --- | --- |
| Wszystkie testy automatyczne | `mvn test` |
| Testy jednostkowe | `mvn "-Dtest=*Test,!*IntegrationTest" test` |
| Testy integracyjne | `mvn "-Dtest=*IntegrationTest" test` |
| Wszystkie testy i raport pokrycia JaCoCo | `mvn verify` |

Raporty testów są zapisywane w `target/surefire-reports/`, a raport pokrycia w `target/site/jacoco/index.html`. Aby odświeżyć wyniki obu grup w UI, uruchom `mvn clean verify`, a następnie uruchom aplikację i kliknij **Refresh results** w panelu **Project verification**. Panel odczytuje wyniki z raportów Maven i pokazuje liczbę przygotowanych scenariuszy manualnych.

## Dokumentacja API

[Swagger UI](http://127.0.0.1:8080/swagger-ui.html) i [specyfikacja OpenAPI w formacie JSON](http://127.0.0.1:8080/v3/api-docs) są dostępne po uruchomieniu aplikacji.

| Metoda i ścieżka | Dane i odpowiedź |
| --- | --- |
| `POST /api/runs` | Przyjmuje `multipart/form-data` z plikami `sourceFile` i `testFile` w formacie `.java` zapisanymi w UTF-8. Zwraca `202` z obiektem zawierającym `runId`, `400` przy błędnych plikach lub `422` przy błędzie kodu Java albo odrzuceniu przebiegu bazowego. |
| `GET /api/runs/{runId}/progress` | Zwraca `200` z polami `status`, `processedMutants`, `totalMutants` i `error`. Status to `RUNNING`, `COMPLETED` lub `FAILED`. Nieznane ID daje `404`. |
| `GET /api/runs/{runId}/report` | Zwraca `200` z wynikiem, licznikami, listą mutantów, kodem źródłowym i numerami podświetlonych linii. Przy niedostępnym raporcie zwraca `409`, a przy nieznanym ID `404`. |
| `GET /api/mutators` | Zwraca `200` z listą nazw dostępnych mutatorów. |
| `GET /api/project-verification` | Zwraca `200` z podsumowaniem dostępnych raportów Maven pod kluczami `unit` i `integration`. |

Interfejs odpytuje endpoint postępu co 500 ms podczas działania analizy. Po otrzymaniu statusu `COMPLETED` pobiera raport dla tego samego `runId`.

## Przypadki testowe (TestCase)

Każdy scenariusz zakłada uruchomioną aplikację i otwarty [interfejs użytkownika](http://127.0.0.1:8080/). Wynik w ostatniej kolumnie jest kryterium zaliczenia scenariusza.

| ID | Tytuł | Warunki początkowe | Kroki testowe | Oczekiwany rezultat |
| --- | --- | --- | --- | --- |
| TC001 | Analiza ze słabymi testami | Interfejs jest gotowy do nowej analizy. | 1. Kliknij **Load sample (weak tests)**. 2. Kliknij **Run analysis**. | Wynik wynosi 20%. Jeden mutant ma status **Killed**, cztery mają status **Survived**. |
| TC002 | Analiza z silnymi testami | Interfejs jest gotowy do nowej analizy. | 1. Kliknij **Load sample (strong tests)**. 2. Kliknij **Run analysis**. | Wynik wynosi 100%. Pięć mutantów ma status **Killed**. Licznik **Survived** wynosi 0. |
| TC003 | Pusty edytor klasy źródłowej | Interfejs jest gotowy do nowej analizy. | 1. Kliknij **Clear**. 2. Kliknij **Run analysis**. | Pod edytorem pojawia się komunikat o wymaganej klasie źródłowej. Analiza się nie rozpoczyna. |
| TC004 | Pusty edytor testów | Załadowany jest przykład **Load sample (weak tests)**. | 1. Przejdź do **JUnit tests** i usuń jego zawartość. 2. Kliknij **Run analysis**. | Pod edytorem pojawia się komunikat o wymaganych testach JUnit. Analiza się nie rozpoczyna. |
| TC005 | Przekroczenie limitu rozmiaru | Załadowany jest dowolny przykład. Przygotowany jest tekst dłuższy niż 128 KiB. | 1. Wklej przygotowany tekst do **Source class**. 2. Kliknij **Run analysis**. | Pod edytorem pojawia się komunikat o przekroczeniu limitu 128 KiB. |
| TC006 | Błąd składni Java | Załadowany jest przykład **Load sample (weak tests)**. | 1. Zastąp zawartość **Source class** tekstem `class Broken {`. 2. Kliknij **Run analysis**. | Karta błędu wyświetla diagnostykę składni otrzymaną z API. |
| TC007 | Nieudany przebieg bazowy | Załadowany jest przykład **Load sample (weak tests)**. | 1. W **JUnit tests** zmień oczekiwaną wartość w asercji z `36` na `99`. 2. Kliknij **Run analysis**. | Karta błędu informuje o teście, który nie przeszedł na oryginalnym kodzie. Przycisk **Run analysis** staje się ponownie aktywny. |
| TC008 | Przekroczenie limitu czasu | Dostępne są [InfiniteLoopSource.java](src/test/resources/samples/InfiniteLoopSource.java) i [InfiniteLoopTest.java](src/test/resources/samples/InfiniteLoopTest.java). | 1. Wklej zawartość plików odpowiednio do **Source class** i **JUnit tests**. 2. Kliknij **Run analysis** i poczekaj na raport. | Mutant zmieniający `>` na `>=` otrzymuje status **Timed out**. Analiza kończy się raportem. |
| TC009 | Filtr i podświetlenie mutantów | Widoczny jest raport z TC001. | 1. Wybierz filtr **Survived** nad tabelą. 2. Najedź na podświetloną linię kodu. | Tabela pokazuje cztery wiersze. Cztery odpowiadające im linie są podświetlone, a wskazana linia pokazuje opis mutacji. |
| TC010 | Aktualizacja postępu analizy | Załadowany jest przykład **Load sample (strong tests)**. | 1. Kliknij **Run analysis**. 2. Obserwuj **Analysis log** i pasek postępu. | Dziennik pokazuje etapy analizy. Po przetworzeniu `5 / 5` mutantów pasek osiąga 100% i pojawia się raport. |
| TC011 | Ponowna analiza po błędzie | Widoczna jest karta błędu z TC007. | 1. Kliknij **Load sample (strong tests)**. 2. Kliknij **Run analysis**. | Karta błędu znika. Aplikacja wyświetla raport z wynikiem 100%. |

## Technologie użyte w projekcie

- Java 17 i `javax.tools.JavaCompiler` z własnym `ClassLoader`
- Spring Boot 3 i Spring Web
- Maven, Surefire i JaCoCo
- JavaParser
- JUnit 5, JUnit Platform Launcher, Spring Boot Test i MockMvc
- springdoc-openapi i Swagger UI
- HTML, CSS i JavaScript
- Git
