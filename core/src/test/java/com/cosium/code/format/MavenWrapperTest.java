package com.cosium.code.format;

import static org.assertj.core.api.Assertions.assertThat;

import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenPluginTest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Covers <a href="https://github.com/Cosium/git-code-format-maven-plugin/issues/90">issue 90</a>.
 *
 * <p>A repository holding a maven wrapper pins the maven version every build has to run with. The
 * hook is a build like any other, so it has to run with that same version instead of whichever
 * installation happened to install the hook.
 *
 * @author Réda Housni Alaoui
 */
@MavenVersions({"3.5.0"})
public class MavenWrapperTest extends AbstractTest {

  private static final String MAVEN_WRAPPER = "mvnw";
  private static final String BAD_FORMAT_JAVA = "src/main/java/BadFormat.java";
  private static final Path MAVEN_WRAPPER_PROPERTIES =
      Paths.get(".mvn/wrapper/maven-wrapper.properties");

  public MavenWrapperTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "single-module");
  }

  @MavenPluginTest
  public void GIVEN_a_maven_wrapper_WHEN_installing_the_hooks_THEN_the_hook_runs_the_wrapper()
      throws Exception {
    Path wrapper = installMavenWrapper(projectRoot());

    installHooks();

    assertThat(readHookScript()).contains(wrapper.toAbsolutePath().toString());
  }

  @MavenPluginTest
  public void
      GIVEN_a_maven_wrapper_in_a_parent_directory_WHEN_installing_the_hooks_THEN_the_hook_runs_the_wrapper()
          throws Exception {
    // The build is run from the module, while the wrapper sits at the root of the repository.
    Path wrapper = installMavenWrapper(projectRoot());
    Path module = Files.createDirectories(projectRoot().resolve("module"));
    Files.copy(projectRoot().resolve("pom.xml"), module.resolve("pom.xml"));

    buildMavenExecution(module).execute("initialize").assertErrorFreeLog();

    assertThat(readHookScript()).contains(wrapper.toAbsolutePath().toString());
  }

  @MavenPluginTest
  public void
      GIVEN_no_maven_wrapper_WHEN_installing_the_hooks_THEN_the_hook_runs_the_maven_installation()
          throws Exception {
    installHooks();

    assertThat(readHookScript()).doesNotContain(MAVEN_WRAPPER);
  }

  @MavenPluginTest
  public void
      GIVEN_the_maven_wrapper_is_not_preferred_WHEN_installing_the_hooks_THEN_the_hook_runs_the_maven_installation()
          throws Exception {
    installMavenWrapper(projectRoot());

    buildMavenExecution(projectRoot())
        .withCliOption("-Dgcf.preferMavenWrapper=false")
        .execute("initialize")
        .assertErrorFreeLog();

    assertThat(readHookScript()).doesNotContain(MAVEN_WRAPPER);
  }

  /**
   * The hook holding the path of the wrapper only tells that it was selected. This one tells that
   * the selected wrapper is something git can actually run.
   */
  @MavenPluginTest
  public void GIVEN_a_maven_wrapper_WHEN_committing_THEN_the_wrapper_formats_the_staged_file()
      throws Exception {
    installMavenWrapper(projectRoot());

    installHooks();

    write(BAD_FORMAT_JAVA, "public class BadFormat {\n" + "\n" + "  void a(  ){}\n" + "}\n");
    jGit().add().addFilepattern(BAD_FORMAT_JAVA).call();

    jGit()
        .commit()
        .setCommitter(gitIdentity())
        .setAuthor(gitIdentity())
        .setMessage("Committing a badly formatted file")
        .call();

    assertThat(read(BAD_FORMAT_JAVA))
        .isEqualTo("public class BadFormat {\n" + "\n" + "  void a() {}\n" + "}\n");
  }

  @MavenPluginTest
  public void
      GIVEN_a_maven_wrapper_WHEN_the_validation_fails_THEN_the_wrapper_is_the_command_to_run()
          throws Exception {
    installMavenWrapper(projectRoot());

    buildMavenExecution(projectRoot())
        .withCliOptions(goalCliOption("validate-code-format"))
        .execute()
        .assertLogText(
            "Run '."
                + File.separator
                + "mvnw git-code-format:format-code' to format all the files.");
  }

  private void write(String sourceName, String content) throws IOException {
    Files.write(
        resolveRelativelyToProjectRoot(sourceName), content.getBytes(StandardCharsets.UTF_8));
  }

  private String read(String sourceName) throws IOException {
    return new String(
        Files.readAllBytes(resolveRelativelyToProjectRoot(sourceName)), StandardCharsets.UTF_8);
  }

  private void installHooks() throws Exception {
    buildMavenExecution(projectRoot()).execute("initialize").assertErrorFreeLog();
  }

  /**
   * Installs the very maven wrapper this repository holds, so that the hook runs a real one. It
   * pins the maven version the build already runs with, hence no distribution left to download. The
   * executable bit has to be set back, as a plain copy drops it.
   */
  private Path installMavenWrapper(Path directory) throws IOException {
    Path repositoryRoot = Paths.get("..");

    Path wrapper = directory.resolve(MAVEN_WRAPPER);
    Files.copy(repositoryRoot.resolve(MAVEN_WRAPPER), wrapper);
    assertThat(wrapper.toFile().setExecutable(true)).isTrue();

    Path properties = directory.resolve(MAVEN_WRAPPER_PROPERTIES);
    Files.createDirectories(properties.getParent());
    Files.copy(repositoryRoot.resolve(MAVEN_WRAPPER_PROPERTIES), properties);

    return wrapper;
  }

  private String readHookScript() throws IOException {
    Path hook =
        jGit()
            .getRepository()
            .getDirectory()
            .toPath()
            .resolve("hooks")
            .resolve("single-module.git-code-format.pre-commit.sh");
    assertThat(hook).exists();
    return new String(Files.readAllBytes(hook), StandardCharsets.UTF_8);
  }
}
