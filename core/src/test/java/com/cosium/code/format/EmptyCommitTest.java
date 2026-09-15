package com.cosium.code.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenPluginTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Covers <a href="https://github.com/Cosium/git-code-format-maven-plugin/issues/96">issue 96</a>.
 *
 * <p>Git refuses to create a commit holding no change, but it performs that check before running
 * the pre-commit hook and never performs it again. When the formatting reverts every staged change,
 * there is nothing left to commit and git records an empty commit. Failing the commit is all a hook
 * can do about it, hence the opt-in option.
 *
 * @author Réda Housni Alaoui
 */
@MavenVersions({"3.5.0"})
public class EmptyCommitTest extends AbstractTest {

  private static final boolean FAIL_ON_EMPTY_COMMIT = true;
  private static final boolean DO_NOT_FAIL_ON_EMPTY_COMMIT = false;

  private static final String BAD_FORMAT_JAVA = "src/main/java/BadFormat.java";

  private static final String OTHER_JAVA = "src/main/java/Other.java";

  private static final String BASELINE =
      "public class BadFormat {\n" + "\n" + "  void a() {}\n" + "}\n";

  private static final String MAIN_CHANGE =
      "public class BadFormat {\n" + "\n" + "  void main() {}\n" + "}\n";

  private static final String OTHER_BASELINE =
      "public class Other {\n" + "\n" + "  void a() {}\n" + "}\n";

  public EmptyCommitTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "single-module");
  }

  @MavenPluginTest
  public void
      GIVEN_default_options_WHEN_the_formatting_reverts_the_staged_change_THEN_git_creates_an_empty_commit()
          throws Exception {
    commitBaseline();

    installHooks(DO_NOT_FAIL_ON_EMPTY_COMMIT);

    stageFormattingNoise();

    commit("Committing a change which the formatting reverts");

    assertThat(headTree()).isEqualTo(parentTree());
  }

  @MavenPluginTest
  public void
      GIVEN_fail_on_empty_commit_WHEN_the_formatting_reverts_the_staged_change_THEN_the_commit_is_refused()
          throws Exception {
    commitBaseline();

    installHooks(FAIL_ON_EMPTY_COMMIT);

    stageFormattingNoise();

    ObjectId headBeforeCommit = head();
    assertThatThrownBy(() -> commit("Committing a change which the formatting reverts"))
        .isInstanceOf(AbortedByHookException.class);
    assertThat(head()).as("No commit must have been created").isEqualTo(headBeforeCommit);
    // The formatting still took place, so there is nothing left to commit.
    assertThat(read(BAD_FORMAT_JAVA)).isEqualTo(BASELINE);
  }

  @MavenPluginTest
  public void
      GIVEN_fail_on_empty_commit_WHEN_the_staged_change_survives_the_formatting_THEN_the_commit_succeeds()
          throws Exception {
    commitBaseline();

    installHooks(FAIL_ON_EMPTY_COMMIT);

    write(
        BAD_FORMAT_JAVA,
        "public class BadFormat {\n"
            + "\n"
            + "  void a() {}\n"
            + "\n"
            + "  void b(  ){}\n"
            + "}\n");
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();

    ObjectId headBeforeCommit = head();

    commit("Committing a badly formatted change");

    assertThat(head()).isNotEqualTo(headBeforeCommit);
    assertThat(read(BAD_FORMAT_JAVA))
        .isEqualTo(
            "public class BadFormat {\n" + "\n" + "  void a() {}\n" + "\n  void b() {}\n}\n");
  }

  @MavenPluginTest
  public void
      GIVEN_fail_on_empty_commit_WHEN_only_a_deletion_survives_the_formatting_THEN_the_commit_succeeds()
          throws Exception {
    write(OTHER_JAVA, OTHER_BASELINE);
    jGit().add().addFilepattern(OTHER_JAVA).call();
    commitBaseline();

    installHooks(FAIL_ON_EMPTY_COMMIT);

    // The formatting reverts the only staged content change, but the staged deletion remains.
    stageFormattingNoise();
    jGit().rm().addFilepattern(OTHER_JAVA).call();

    ObjectId headBeforeCommit = head();

    commit("Committing a deletion along a change which the formatting reverts");

    assertThat(head()).isNotEqualTo(headBeforeCommit);
    assertThat(jGit().getRepository().resolve("HEAD:" + OTHER_JAVA)).isNull();
  }

  @MavenPluginTest
  public void
      GIVEN_fail_on_empty_commit_WHEN_the_formatting_reverts_the_merge_resolution_THEN_the_merge_commit_is_created()
          throws Exception {
    commitBaseline();
    String mainBranch = jGit().getRepository().getBranch();

    jGit().checkout().setCreateBranch(true).setName("side").call();
    write(BAD_FORMAT_JAVA, "public class BadFormat {\n" + "\n" + "  void side() {}\n" + "}\n");
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();
    commit("Side change");

    jGit().checkout().setName(mainBranch).call();
    write(BAD_FORMAT_JAVA, MAIN_CHANGE);
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();
    commit("Main change");

    installHooks(FAIL_ON_EMPTY_COMMIT);

    MergeResult mergeResult =
        jGit().merge().include(jGit().getRepository().exactRef("refs/heads/side")).call();
    assertThat(mergeResult.getMergeStatus()).isEqualTo(MergeStatus.CONFLICTING);

    // Resolved by keeping what the branch already holds, badly formatted: once formatted, the
    // merge commit holds nothing more than its first parent.
    write(BAD_FORMAT_JAVA, "public class BadFormat {\n" + "\n" + "  void main(  ){}\n" + "}\n");
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();

    ObjectId headBeforeCommit = head();

    commit("Merging side");

    assertThat(head()).isNotEqualTo(headBeforeCommit);
    assertThat(jGit().getRepository().resolve("HEAD^2"))
        .as("A merge must have been recorded")
        .isNotNull();
    // Git records a merge commit holding no change, as the merge itself is the information.
    assertThat(headTree()).isEqualTo(parentTree());
    assertThat(read(BAD_FORMAT_JAVA)).isEqualTo(MAIN_CHANGE);
  }

  /** Stages a change holding nothing but formatting the formatter undoes. */
  private void stageFormattingNoise() throws Exception {
    write(BAD_FORMAT_JAVA, "public class BadFormat {\n" + "\n" + "  void a(  ){}\n" + "}\n");
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();
  }

  private ObjectId head() throws IOException {
    return jGit().getRepository().resolve("HEAD");
  }

  private ObjectId headTree() throws IOException {
    return jGit().getRepository().resolve("HEAD^{tree}");
  }

  private ObjectId parentTree() throws IOException {
    return jGit().getRepository().resolve("HEAD~1^{tree}");
  }

  private void commitBaseline() throws Exception {
    write(BAD_FORMAT_JAVA, BASELINE);
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();
    commit("Baseline");
  }

  private void installHooks(boolean failOnEmptyCommit) throws Exception {
    buildMavenExecution(projectRoot())
        .withCliOption("-Dgcf.failOnEmptyCommit=" + failOnEmptyCommit)
        .execute("initialize")
        .assertErrorFreeLog();
  }

  private void commit(String message) throws Exception {
    jGit().commit().setCommitter(gitIdentity()).setAuthor(gitIdentity()).setMessage(message).call();
  }

  private void write(String sourceName, String content) throws IOException {
    Files.write(
        resolveRelativelyToProjectRoot(sourceName), content.getBytes(StandardCharsets.UTF_8));
  }

  private String read(String sourceName) throws IOException {
    return new String(
        Files.readAllBytes(resolveRelativelyToProjectRoot(sourceName)), StandardCharsets.UTF_8);
  }
}
