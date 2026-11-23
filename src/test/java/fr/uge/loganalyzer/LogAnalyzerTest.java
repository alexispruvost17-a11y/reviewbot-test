package fr.uge.loganalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import module java.base;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class LogAnalyzerTest {
  record ProcessResult(int exitCode, String stdout, String stderr) {}

  private static ProcessResult run(String... arguments) throws IOException, InterruptedException {
    var javaHome = System.getProperty("java.home");
    var javaBin = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
    var executable = Path.of(javaHome, "bin", javaBin);
    var classpath = System.getProperty("java.class.path");

    var command = new ArrayList<String>();
    command.add(executable.toString());
    command.add("-classpath");
    command.add(classpath);
    command.add("fr.uge.loganalyzer.LogAnalyzer");
    command.addAll(List.of(arguments));

    try (var process = new ProcessBuilder(command).redirectErrorStream(false).start()) {
      var stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      var stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
      stdout = stdout.replace("\r\n", "\n");
      stderr = stderr.replace("\r\n", "\n");
      var exitCode = process.waitFor();
      return new ProcessResult(exitCode, stdout, stderr);
    }
  }

  private static Path createLog(Path directory) throws IOException {
    var file = directory.resolve("log.txt");
    Files.writeString(file, """
        ordinary message
        [ERROR] first error
        [WARN] first warning
        USER_LOGIN alice
        AUTH_SUCCESS bob
        USER_AUTH combined
        [ERROR] second error
        [WARN] second warning
        [INFO] information
        """);
    return file;
  }


  @Nested
  public class Q2 {
    @Test
    @DisplayName("main with no arguments displays usage and exits with code 1")
    public void mainWithNoArgumentsDisplaysUsageAndExitsWithOne() throws Exception {
      var result = run();

      assertEquals(1, result.exitCode());
      assertTrue(result.stdout().contains("Usage:"));
    }

    @Test
    @DisplayName("main with only a file displays usage and exits with code 1")
    public void mainWithOnlyAFileDisplaysUsageAndExitsWithOne(@TempDir Path directory) throws Exception {
      var file = directory.resolve("log.txt");
      Files.writeString(file, "[ERROR] error\n");

      var result = run(file.toString());

      assertEquals(1, result.exitCode());
      assertTrue(result.stdout().contains("Usage:"));
    }

    @Test
    @DisplayName("main with --errors prints only error lines")
    public void mainWithErrorsPrintsOnlyErrorLines(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors");

      var expected = new ProcessResult(0, """
          [ERROR] first error
          [ERROR] second error
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --warnings prints only warning lines")
    public void mainWithWarningsPrintsOnlyWarningLines(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--warnings");

      var expected = new ProcessResult(0, """
          [WARN] first warning
          [WARN] second warning
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --auth prints only authentication lines")
    public void mainWithAuthPrintsOnlyAuthenticationLines(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--auth");

      var expected = new ProcessResult(0, """
          USER_LOGIN alice
          AUTH_SUCCESS bob
          USER_AUTH combined
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --auth does not print when USER_ prefix is in the middle of the line")
    public void mainWithAuthDoNotPrintIfUserLoginInTheMiddleOfTheLine(@TempDir Path directory) throws Exception {
      var file = directory.resolve("log.txt");
      Files.writeString(file, """
          ordinary message
          USER_LOGIN alice
          THIS IS A USER_ERROR
          """);

      var result = run(file.toString(), "--auth");

      var expected = new ProcessResult(0, """
          USER_LOGIN alice
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --auth does not print when AUTH_ prefix is in the middle of the line")
    public void mainWithAuthDoNotPrintIfAuthAtTheEndOfTheLine(@TempDir Path directory) throws Exception {
      var file = directory.resolve("log.txt");
      Files.writeString(file, """
          ordinary message
          NO AUTH_GRANTED
          AUTH_SUCCESS bob
          """);

      var result = run(file.toString(), "--auth");

      var expected = new ProcessResult(0, """
          AUTH_SUCCESS bob
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --all prints every line")
    public void mainDoesPrintAllLines(@TempDir Path directory) throws Exception {
      var file = directory.resolve("log.txt");
      Files.writeString(file, """
          ordinary message
          [INFO] information
          DEBUG something
          """);

      var result = run(file.toString(), "--all");

      var expected = new ProcessResult(0, """
          ordinary message
          [INFO] information
          DEBUG something
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with a repeated flag does not duplicate matching lines")
    public void mainWithRepeatedFlagsDoesNotDuplicateLines(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors", "--errors");

      var expected = new ProcessResult(0, """
          [ERROR] first error
          [ERROR] second error
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main accepts filter flags case-insensitively")
    public void mainAcceptsCaseInsensitiveFlags(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--ErRoRs");

      var expected = new ProcessResult(0, """
          [ERROR] first error
          [ERROR] second error
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with only an unknown flag exits with code 1 and reports the flag")
    public void mainWithOnlyUnknownFlagsExit(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--unknown");

      assertEquals(1, result.exitCode());
      assertEquals("", result.stdout());
      assertTrue(result.stderr().contains("--unknown"));
    }

    @Test
    @DisplayName("main with a valid flag followed by an unknown flag exits with code 1")
    public void mainWitUnknownFlagsExit(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors", "--unknown");

      assertEquals(1, result.exitCode());
      assertEquals("", result.stdout());
      assertTrue(result.stderr().contains("--unknown"));
    }

    @Test
    @DisplayName("main with a missing file exits with code 1 and reports the file path")
    public void mainWithMissingFileExitAndPrintTheFile(@TempDir Path directory) throws Exception {
      var file = directory.resolve("missing.log");

      var result = run(file.toString(), "--all");

      assertEquals(1, result.exitCode());
      assertEquals("", result.stdout());
      assertTrue(result.stderr().contains(file.toString()));
    }

    @Test
    @DisplayName("LogAnalyzer class is public and final")
    public void logAnalyzerClass() {
      assertTrue(LogAnalyzer.class.accessFlags().contains(AccessFlag.PUBLIC));
      assertTrue(LogAnalyzer.class.accessFlags().contains(AccessFlag.FINAL));
    }

    @Test
    @DisplayName("LogAnalyzer class has no public constructor")
    public void logAnalyzerClassHasNoPublicConstructor() {
      assertFalse(Arrays.stream(LogAnalyzer.class.getConstructors())
          .anyMatch(constructor -> constructor.accessFlags().contains(AccessFlag.PUBLIC)));
    }
  }


  @Nested
  public class Q3 {
    @Test
    @DisplayName("main with --errors and --warnings prints both types via OR logic")
    public void mainWithErrorsAndWarningsPrintsBothTypes(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors", "--warnings");

      var expected = new ProcessResult(0, """
          [ERROR] first error
          [WARN] first warning
          [ERROR] second error
          [WARN] second warning
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --errors and --auth prints both types via OR logic")
    public void mainWithErrorsAndAuthPrintsBothTypes(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors", "--auth");

      var expected = new ProcessResult(0, """
          [ERROR] first error
          USER_LOGIN alice
          AUTH_SUCCESS bob
          USER_AUTH combined
          [ERROR] second error
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --warnings and --auth prints both types via OR logic")
    public void mainWithWarningsAndAuthPrintsBothTypes(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--warnings", "--auth");

      var expected = new ProcessResult(0, """
          [WARN] first warning
          USER_LOGIN alice
          AUTH_SUCCESS bob
          USER_AUTH combined
          [WARN] second warning
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --all combined with other flags still prints every line")
    public void mainWithAllAndOtherFlagsPrintsAllMatchingLines(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors", "--all", "--auth");

      var expected = new ProcessResult(0, """
          ordinary message
          [ERROR] first error
          [WARN] first warning
          USER_LOGIN alice
          AUTH_SUCCESS bob
          USER_AUTH combined
          [ERROR] second error
          [WARN] second warning
          [INFO] information
          """, "");
      assertEquals(expected, result);
    }
  }


  @Nested
  public class Q7 {
    @Test
    @DisplayName("main with --all and stats groups every level")
    public void mainStatsAll(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--all", "stats");

      var expected = new ProcessResult(0, """
          {ERROR=2, INFO=1, OTHER=4, WARN=2}
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --errors and stats groups only error lines")
    public void mainStatsErrorsOnly(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors", "stats");

      var expected = new ProcessResult(0, """
          {ERROR=2}
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --errors, --warnings and stats groups both levels via OR logic")
    public void mainStatsErrorsAndWarnings(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors", "--warnings", "stats");

      var expected = new ProcessResult(0, """
          {ERROR=2, WARN=2}
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --errors and print outputs only error lines")
    public void mainPrintErrorsOnly(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors", "print");

      var expected = new ProcessResult(0, """
          [ERROR] first error
          [ERROR] second error
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with multiple filters and print combines matches via OR logic")
    public void mainPrintMultipleFiltersOrLogic(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors", "--auth", "print");

      var expected = new ProcessResult(0, """
          [ERROR] first error
          USER_LOGIN alice
          AUTH_SUCCESS bob
          USER_AUTH combined
          [ERROR] second error
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with an unknown action exits with code 1 and reports the action")
    public void mainUnknownActionExitCodeOne(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--all", "invalid_action");

      assertEquals(1, result.exitCode());
      assertTrue(result.stderr().contains("Unknown action: invalid_action"));
    }

    @Test
    @DisplayName("main with a missing file and an action exits with code 1")
    public void mainFileNotFoundExitCodeOne(@TempDir Path directory) throws Exception {
      var nonExistentFile = directory.resolve("missing.log").toString();

      var result = run(nonExistentFile, "--all", "print");

      assertEquals(1, result.exitCode());
      assertFalse(result.stderr().isEmpty());
    }

    @Test
    @DisplayName("main with more than one action specified exits with an error")
    public void mainWithMoreThanOneActionsExitsWithError(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--all", "print", "stats");

      assertEquals(1, result.exitCode());
      assertEquals("", result.stdout());
      assertFalse(result.stderr().isEmpty());
    }

    @Test
    @DisplayName("main with only stats action and no filters prints an empty map")
    public void mainWithOnlyStatsActionAndNoFiltersPrintsEmptyMap(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "stats");

      var expected = new ProcessResult(0, "{}\n", "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with only print action and no filters prints an empty line")
    public void mainWithOnlyPrintActionAndNoFiltersPrintsEmptyLine(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "print");

      var expected = new ProcessResult(0, "\n", "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main accepts the action case-insensitively")
    public void mainAcceptsCaseInsensitiveAction(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--all", "STATS");

      var expected = new ProcessResult(0, """
          {ERROR=2, INFO=1, OTHER=4, WARN=2}
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main accepts the action before the filter flags")
    public void mainAcceptsActionBeforeFilters(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "stats", "--errors");

      var expected = new ProcessResult(0, """
          {ERROR=2}
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --auth and stats groups all matches under OTHER")
    public void mainStatsWithAuthOnlyGroupsUnderOther(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--auth", "stats");

      var expected = new ProcessResult(0, """
          {OTHER=3}
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with print and no matching lines prints an empty line")
    public void mainPrintWithNoMatchingLinesPrintsEmptyLine(@TempDir Path directory) throws Exception {
      var file = directory.resolve("log.txt");
      Files.writeString(file, "ordinary message\n");

      var result = run(file.toString(), "--errors", "print");

      var expected = new ProcessResult(0, "\n", "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with stats and no matching lines prints an empty map")
    public void mainStatsWithNoMatchingLinesPrintsEmptyMap(@TempDir Path directory) throws Exception {
      var file = directory.resolve("log.txt");
      Files.writeString(file, "ordinary message\n");

      var result = run(file.toString(), "--errors", "stats");

      var expected = new ProcessResult(0, "{}\n", "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with stats correctly extracts the level when a line has multiple bracket pairs")
    public void mainStatsCorrectlyMatchWhenLineHasMultipleBracketPairs(@TempDir Path directory) throws Exception {
      var file = directory.resolve("log.txt");
      Files.writeString(file, "[ERROR] something [ignored]\n");

      var result = run(file.toString(), "--all", "stats");

      var expected = new ProcessResult(0, """
          {ERROR=1}
          """, "");
      assertEquals(expected, result);
    }

    @Test
    @DisplayName("main with --errors and print outputs exactly the error lines")
    public void mainPrintErrorsOnlyExactOutput(@TempDir Path directory) throws Exception {
      var file = createLog(directory);

      var result = run(file.toString(), "--errors", "print");

      var expected = new ProcessResult(0, """
          [ERROR] first error
          [ERROR] second error
          """, "");
      assertEquals(expected, result);
    }
  }
}