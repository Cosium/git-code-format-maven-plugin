package com.cosium.code.format;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * Created on 16/01/18.
 *
 * @author Reda.Housni-Alaoui
 */
@MavenVersions({"3.5.0"})
@RunWith(MavenJUnitTestRunner.class)
public abstract class AbstractTest {
  protected static final String GROUP_ID = "com.cosium.code";
  protected static final String ARTIFACT_ID = "maven-git-code-format";
  @Rule public final TestResources resources;

  private final MavenRuntime maven;
  private final String projectRootDirectoryName;
  private Path projectDestination;
  private Path projectRoot;
  private Git git;

  public AbstractTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder, String projectRootDirectoryName)
      throws Exception {
    this.resources =
        new TestResources(
            "src/test/projects",
            Files.createTempDirectory("maven-git-code-format-test").toString());
    this.maven = mavenBuilder.withCliOptions("-B", "-U").build();
    this.projectRootDirectoryName = projectRootDirectoryName;
  }

  @Before
  public final void before() throws Exception {
    projectRoot = resources.getBasedir(projectRootDirectoryName).toPath();

    git = Git.init().setDirectory(projectRoot.toFile()).call();
    git.add().addFilepattern(".").call();
    git.commit().setAll(true).setMessage("First commit").call();

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

  protected final Git getGit() {
    return git;
  }

  protected void touch(Path sourceName) throws IOException {
    Path sourceFile = resolveRelativelyToProjectRoot(sourceName);
    String content;
    try (InputStream inputStream = Files.newInputStream(sourceFile)) {
      content = IOUtils.toString(inputStream) + "\n//Hello world";
    }
    try (OutputStream outputStream = Files.newOutputStream(sourceFile)) {
      IOUtils.write(content, outputStream);
    }
  }

  protected Path resolveRelativelyToProjectRoot(Path sourceName) {
    return projectRoot.resolve(sourceName);
  }
  
  protected Path projectRoot(){
    return projectRoot;
  }
  
  @After
  public final void moveFiles() throws Exception {
    Files.move(projectRoot, projectDestination);
  }
}
