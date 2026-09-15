package com.cosium.code.format;

import static org.assertj.core.api.Assertions.assertThat;

import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenPluginTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

  public MavenWrapperTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "single-module");
  }

  @MavenPluginTest
  public void GIVEN_a_maven_wrapper_WHEN_installing_the_hooks_THEN_the_hook_runs_the_wrapper()
      throws Exception {
    Path wrapper = createMavenWrapper(projectRoot());

    installHooks();

    assertThat(readHookScript()).contains(wrapper.toAbsolutePath().toString());
  }

  @MavenPluginTest
  public void
      GIVEN_a_maven_wrapper_in_a_parent_directory_WHEN_installing_the_hooks_THEN_the_hook_runs_the_wrapper()
          throws Exception {
    // The build is run from the module, while the wrapper sits at the root of the repository.
    Path wrapper = createMavenWrapper(projectRoot());
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
    createMavenWrapper(projectRoot());

    buildMavenExecution(projectRoot())
        .withCliOption("-Dgcf.preferMavenWrapper=false")
        .execute("initialize")
        .assertErrorFreeLog();

    assertThat(readHookScript()).doesNotContain(MAVEN_WRAPPER);
  }

  private void installHooks() throws Exception {
    buildMavenExecution(projectRoot()).execute("initialize").assertErrorFreeLog();
  }

  /**
   * The wrapper is never run by these tests, so its content does not matter. Only its presence and
   * its executable bit do.
   */
  private Path createMavenWrapper(Path directory) throws IOException {
    Path wrapper = directory.resolve(MAVEN_WRAPPER);
    Files.write(wrapper, "#!/bin/bash\n".getBytes(StandardCharsets.UTF_8));
    assertThat(wrapper.toFile().setExecutable(true)).isTrue();
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
