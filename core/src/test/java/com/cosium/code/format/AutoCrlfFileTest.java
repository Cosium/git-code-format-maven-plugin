package com.cosium.code.format;

import static org.assertj.core.api.Assertions.assertThat;

import io.takari.maven.testing.executor.MavenRuntime;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Test;

/**
 * Covers <a href="https://github.com/Cosium/git-code-format-maven-plugin/issues/227">issue 227</a>.
 *
 * <p>Git caches the stat data of the working tree file in the index entry. The formatting has to
 * leave that stat data consistent with the file on disk, otherwise the freshly committed file is
 * reported as modified. It takes a blob whose size differs from the size of the file to surface,
 * which is what {@code core.autocrlf} produces: git stores text with LF and checks it out with
 * CRLF.
 *
 * @author Réda Housni Alaoui
 */
public class AutoCrlfFileTest extends AbstractTest {

  private static final String BAD_FORMAT_JAVA = "src/main/java/BadFormat.java";

  private static final String BASELINE =
      "public class BadFormat {\n" + "\n" + "  void a() {}\n" + "}\n";

  public AutoCrlfFileTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "single-module");
  }

  @Test
  public void GIVEN_auto_crlf_WHEN_committing_a_badly_formatted_file_THEN_it_is_not_left_modified()
      throws Exception {
    enableAutoCrlf();
    commitBaseline();

    installHooks();

    write(BAD_FORMAT_JAVA, crlf("public class BadFormat {\n" + "\n" + "  void a(  ){}\n" + "}\n"));
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();

    commit("Committing a badly formatted file");

    // The formatted content is stored with the LF endings git normalizes text to.
    assertThat(readCommitted(BAD_FORMAT_JAVA)).isEqualTo(BASELINE);
    // The formatting reached the working tree with the CRLF endings a check-out produces.
    assertThat(read(BAD_FORMAT_JAVA)).isEqualTo(crlf(BASELINE));
    // Used to be reported as modified right after the commit: the index kept the size of the
    // formatted blob, which is smaller than the CRLF file on disk, and that size is trusted.
    assertThat(jGit().status().call().getModified()).doesNotContain(BAD_FORMAT_JAVA);
  }

  private void enableAutoCrlf() throws IOException {
    StoredConfig config = jGit().getRepository().getConfig();
    config.setString("core", null, "autocrlf", "true");
    config.save();
  }

  private void commitBaseline() throws Exception {
    write(BAD_FORMAT_JAVA, crlf(BASELINE));
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();
    commit("Baseline");
  }

  private void installHooks() throws Exception {
    buildMavenExecution(projectRoot()).execute("initialize").assertErrorFreeLog();
  }

  private void commit(String message) throws Exception {
    jGit().commit().setCommitter(gitIdentity()).setAuthor(gitIdentity()).setMessage(message).call();
  }

  private String crlf(String content) {
    return content.replace("\n", "\r\n");
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
