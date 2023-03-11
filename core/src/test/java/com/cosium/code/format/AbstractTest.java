package com.cosium.code.format;

import static org.assertj.core.api.Assertions.assertThat;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

/**
 * @author Réda Housni Alaoui
 */
@MavenVersions({"3.5.0"})
@RunWith(MavenJUnitTestRunner.class)
public abstract class AbstractTest {
  private static final String GROUP_ID = "com.cosium.code";
  private static final String ARTIFACT_ID = "git-code-format-maven-plugin";
  private static final PersonIdent gitIdentity =
      new PersonIdent("John Doe", "john.doe@example.org");
  @Rule public final TestResources resources;

  private final MavenRuntime maven;
  private final String projectRootDirectoryName;
  private Path projectDestination;
  private Path projectRoot;
  private Git jGit;

  public AbstractTest(
      MavenRuntime.MavenRuntimeBuilder mavenBuilder, String projectRootDirectoryName)
      throws Exception {
    this.resources =
        new TestResources(
            "src/test/projects", Files.createTempDirectory(ARTIFACT_ID + "-test").toString());
    this.maven = mavenBuilder.withCliOptions("-B", "-U").build();
    this.projectRootDirectoryName = projectRootDirectoryName;
  }

  @Before
  public final void before() throws Exception {
    projectRoot = resources.getBasedir(projectRootDirectoryName).toPath();

    jGit = Git.init().setDirectory(projectRoot.toFile()).call();
    jGit.add().addFilepattern(".").call();
    jGit.commit()
        .setCommitter(gitIdentity)
        .setAuthor(gitIdentity)
        .setAll(true)
        .setMessage("First commit")
        .call();

    projectDestination =
        Files.createDirectories(Paths.get("target/test-projects"))
            .resolve(projectRoot.getParent().relativize(projectRoot));

    if (Files.notExists(projectDestination)) {
      return;
    }

    Files.walk(projectDestination, FileVisitOption.FOLLOW_LINKS)
        .sorted(Comparator.reverseOrder())
        .forEach(
            path -> {
              try {
                Files.deleteIfExists(path);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    Files.deleteIfExists(projectDestination);
  }

  protected final MavenExecution buildMavenExecution(Path mavenProjectPath) {
    return maven.forProject(mavenProjectPath.toFile());
  }

  protected final Git jGit() {
    return jGit;
  }

  protected final PersonIdent gitIdentity() {
    return gitIdentity;
  }

  protected void touch(String sourceName) throws IOException {
    Path sourceFile = resolveRelativelyToProjectRoot(sourceName);
    String content;
    try (InputStream inputStream = Files.newInputStream(sourceFile)) {
      content = IOUtils.toString(inputStream, StandardCharsets.UTF_8) + "\n";
    }
    try (OutputStream outputStream = Files.newOutputStream(sourceFile)) {
      IOUtils.write(content, outputStream, StandardCharsets.UTF_8);
    }
  }

  protected Path resolveRelativelyToProjectRoot(String sourceName) {
    return projectRoot.resolve(sourceName);
  }

  protected String sha1(String sourceName) {
    try (InputStream inputStream =
        Files.newInputStream(resolveRelativelyToProjectRoot(sourceName))) {
      return DigestUtils.sha1Hex(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void assertMatchExpected(String sourceName) {
    try (InputStream actual = Files.newInputStream(resolveRelativelyToProjectRoot(sourceName));
        InputStream expected =
            getClass()
                .getResourceAsStream("/expected/" + projectRootDirectoryName + "/" + sourceName)) {
      assertThat(IOUtils.toString(actual, StandardCharsets.UTF_8))
          .isEqualTo(IOUtils.toString(expected, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected Path projectRoot() {
    return projectRoot;
  }

  protected String goalCliOption(String goal) {
    return GROUP_ID + ":" + ARTIFACT_ID + ":" + goal;
  }

  @After
  public final void moveFiles() throws Exception {
    Files.move(projectRoot, projectDestination);
  }
}
