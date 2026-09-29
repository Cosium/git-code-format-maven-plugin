package com.cosium.code.format;

import static org.assertj.core.api.Assertions.assertThat;

import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenPluginTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

/**
 * Covers <a href="https://github.com/Cosium/git-code-format-maven-plugin/issues/390">issue 390</a>.
 *
 * <p>JGit cannot add a linked worktree, hence the git command line.
 *
 * @author Réda Housni Alaoui
 */
@MavenVersions({"3.5.0"})
public class WorktreeTest extends AbstractTest {

  private static final String BAD_FORMAT_JAVA = "src/main/java/BadFormat.java";
  private static final String FORMATTED_CONTENT =
      "public class BadFormat {\n" + "\n" + "  void a() {}\n" + "}\n";

  private Path worktree;

  public WorktreeTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "single-module");
  }

  @BeforeEach
  void addWorktree() throws Exception {
    worktree = Files.createTempDirectory("git-code-format-maven-plugin-worktree").resolve("wt");
    runGit(projectRoot(), "worktree", "add", "-b", "wt", worktree.toString());
  }

  @MavenPluginTest
  @DisplayName(
      "GIVEN hooks installed from the main checkout WHEN committing a bad formatted file from a"
          + " linked worktree THEN the file of the worktree has the correct format")
  public void test1() throws Exception {
    buildMavenExecution(projectRoot()).execute("initialize").assertErrorFreeLog();

    commitBadFormattedFileFromWorktree();

    assertThat(worktree.resolve(BAD_FORMAT_JAVA))
        .content(StandardCharsets.UTF_8)
        .isEqualTo(FORMATTED_CONTENT);
  }

  @MavenPluginTest
  @DisplayName(
      "GIVEN hooks installed from a linked worktree WHEN committing a bad formatted file from the"
          + " worktree THEN the file of the worktree has the correct format")
  public void test2() throws Exception {
    buildMavenExecution(worktree).execute("initialize").assertErrorFreeLog();

    commitBadFormattedFileFromWorktree();

    assertThat(worktree.resolve(BAD_FORMAT_JAVA))
        .content(StandardCharsets.UTF_8)
        .isEqualTo(FORMATTED_CONTENT);
  }

  private void commitBadFormattedFileFromWorktree() throws Exception {
    Files.write(
        worktree.resolve(BAD_FORMAT_JAVA),
        ("public class BadFormat {\n" + "\n" + "  void a(  ){}\n" + "}\n")
            .getBytes(StandardCharsets.UTF_8));
    runGit(worktree, "add", BAD_FORMAT_JAVA);
    runGit(
        worktree,
        "-c",
        "user.name=" + gitIdentity().getName(),
        "-c",
        "user.email=" + gitIdentity().getEmailAddress(),
        "commit",
        "-m",
        "Committing a badly formatted file");
  }

  private void runGit(Path directory, String... arguments)
      throws IOException, InterruptedException {
    List<String> command = new ArrayList<>();
    command.add("git");
    command.addAll(Arrays.asList(arguments));
    Process process =
        new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
    String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
    assertThat(process.waitFor()).as(output).isZero();
  }
}
