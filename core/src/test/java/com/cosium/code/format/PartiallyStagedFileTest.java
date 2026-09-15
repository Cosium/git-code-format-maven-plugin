package com.cosium.code.format;

import static org.assertj.core.api.Assertions.assertThat;

import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenPluginTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Covers <a href="https://github.com/Cosium/git-code-format-maven-plugin/issues/49">issue 49</a>,
 * which is neither Windows nor CRLF specific.
 *
 * <p>The formatting is performed on the git index, then reported to the working tree. A partially
 * staged file has a working tree content which differs from the staged one, so the formatting has
 * to be merged into it instead of being applied as a plain patch.
 *
 * @author Réda Housni Alaoui
 */
@MavenVersions({"3.5.0"})
public class PartiallyStagedFileTest extends AbstractTest {

  private static final String BAD_FORMAT_JAVA = "src/main/java/BadFormat.java";

  public PartiallyStagedFileTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "single-module");
  }

  @MavenPluginTest
  public void
      GIVEN_partially_staged_file_WHEN_committing_THEN_staged_lines_are_formatted_and_unstaged_changes_survive()
          throws Exception {
    String baseline =
        "public class BadFormat {\n"
            + "\n"
            + "  void a() {}\n"
            + "\n"
            + "  void filler1() {}\n"
            + "\n"
            + "  void filler2() {}\n"
            + "\n"
            + "  void filler3() {}\n"
            + "\n"
            + "  void z() {}\n"
            + "}\n";
    commitBaseline(baseline);

    installHooks();

    // Staged: a badly formatted change at the bottom of the file.
    write(BAD_FORMAT_JAVA, baseline.replace("  void z() {}", "  void z(  ){}"));
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();

    // Unstaged: a further change, far away from the staged one.
    write(
        BAD_FORMAT_JAVA,
        baseline
            .replace("  void z() {}", "  void z(  ){}")
            .replace("  void a() {}\n", "  void a() {}\n\n  void unstagedTop(  ){}\n"));

    commit("Committing a partially staged badly formatted file");

    // The staged change is formatted, so the commit restores the baseline.
    assertThat(readCommitted(BAD_FORMAT_JAVA)).isEqualTo(baseline);

    String workingTree = read(BAD_FORMAT_JAVA);
    // The formatting reached the working tree too.
    assertThat(workingTree).contains("  void z() {}").doesNotContain("void z(  ){}");
    // The unstaged change survived, and was left unformatted since it was not staged.
    assertThat(workingTree).contains("  void unstagedTop(  ){}");
  }

  @MavenPluginTest
  public void
      GIVEN_formatting_conflicting_with_unstaged_changes_WHEN_committing_THEN_the_commit_still_succeeds()
          throws Exception {
    String baseline = "public class BadFormat {\n" + "\n" + "  void a() {}\n" + "}\n";
    commitBaseline(baseline);

    installHooks();

    // Staged: a badly formatted method, with a trailing blank line the formatter will remove.
    write(
        BAD_FORMAT_JAVA,
        "public class BadFormat {\n"
            + "\n"
            + "  void a() {}\n"
            + "\n"
            + "  void b(  ){}\n"
            + "\n"
            + "}\n");
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();

    // Unstaged: a further change right next to the staged one, so that the formatting of the
    // staged content cannot be merged into the working tree.
    String unstaged =
        "public class BadFormat {\n"
            + "\n"
            + "  void a() {}\n"
            + "\n"
            + "  void b(  ){}\n"
            + "\n"
            + "  void c(  ){}\n"
            + "\n"
            + "}\n";
    write(BAD_FORMAT_JAVA, unstaged);

    // Used to fail with 'PatchApplyException: Cannot apply: HunkHeader[...]'.
    commit("Committing a partially staged badly formatted file");

    assertThat(readCommitted(BAD_FORMAT_JAVA))
        .isEqualTo(
            "public class BadFormat {\n" + "\n" + "  void a() {}\n" + "\n" + "  void b() {}\n}\n");

    // The working tree could not be updated, but it was left intact rather than corrupted.
    assertThat(read(BAD_FORMAT_JAVA)).isEqualTo(unstaged);
  }

  private void commitBaseline(String baseline) throws Exception {
    write(BAD_FORMAT_JAVA, baseline);
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();
    commit("Baseline");
  }

  private void installHooks() throws Exception {
    buildMavenExecution(projectRoot()).execute("initialize").assertErrorFreeLog();
  }

  private void commit(String message) throws Exception {
    jGit()
        .commit()
        .setCommitter(gitIdentity())
        .setAuthor(gitIdentity())
        .setMessage(message)
        .call();
  }

  private void write(String sourceName, String content) throws IOException {
    Files.write(
        resolveRelativelyToProjectRoot(sourceName), content.getBytes(StandardCharsets.UTF_8));
  }

  private String read(String sourceName) throws IOException {
    return new String(
        Files.readAllBytes(resolveRelativelyToProjectRoot(sourceName)), StandardCharsets.UTF_8);
  }

  /** Reads the content actually stored in HEAD, bypassing the working tree. */
  private String readCommitted(String sourceName) throws IOException {
    Repository repository = jGit().getRepository();
    try (RevWalk revWalk = new RevWalk(repository)) {
      ObjectId headTree = revWalk.parseCommit(repository.resolve("HEAD")).getTree().getId();
      try (TreeWalk treeWalk = TreeWalk.forPath(repository, sourceName, headTree)) {
        assertThat(treeWalk).as("'%s' must exist in HEAD", sourceName).isNotNull();
        return new String(
            repository.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8);
      }
    }
  }
}
