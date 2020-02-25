package com.cosium.code.format;

import static java.util.Optional.ofNullable;

import com.cosium.code.format.executable.Executable;
import com.cosium.code.format.executable.ExecutableManager;
import com.cosium.code.format.maven.MavenEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Installs git hooks on each initialization. Hooks are always overriden in case changes in:
 *
 * <ul>
 *   <li>maven installation
 *   <li>plugin structure
 * </ul>
 */
@Mojo(name = "install-hooks", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class InstallHooksMojo extends AbstractMavenGitCodeFormatMojo {

  private static final String BASE_PLUGIN_PRE_COMMIT_HOOK = "maven-git-code-format.pre-commit.sh";
  private static final String PRE_COMMIT_HOOK_BASE_SCRIPT = "pre-commit";

  private final ExecutableManager executableManager = new ExecutableManager(this::getLog);
  private final MavenEnvironment mavenEnvironment = new MavenEnvironment(this::getLog);

  /**
   * True to truncate hooks base scripts before each install. <br>
   * Do not use this option if any other system or human manipulate the hooks
   */
  @Parameter(property = "truncateHooksBaseScripts", defaultValue = "false")
  private boolean truncateHooksBaseScripts;

  /** The list of properties to propagate to the hooks */
  @Parameter(property = "propertiesToPropagate")
  private String[] propertiesToPropagate;

  /** The list of properties to add to the hooks */
  @Parameter(property = "propertiesToAdd")
  private String[] propertiesToAdd;

  @Parameter(property = "debug", defaultValue = "false")
  private boolean debug;

  /** Make the pre-commit hook quiet */
  @Parameter(property = "preCommitHookPipeline", defaultValue = "")
  private String preCommitHookPipeline;

  public void execute() throws MojoExecutionException {
    if (!isExecutionRoot()) {
      getLog().debug("Not in execution root. Do not execute.");
      return;
    }

    try {
      getLog().info("Installing git hooks");
      doExecute();
      getLog().info("Installed git hooks");
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void doExecute() throws IOException {
    Path hooksDirectory = prepareHooksDirectory();

    writePluginHooks(hooksDirectory);

    configureHookBaseScripts(hooksDirectory);
  }

  private void writePluginHooks(Path hooksDirectory) throws IOException {
    getLog().debug("Writing plugin pre commit hook file");
    executableManager
        .getOrCreateExecutableScript(hooksDirectory.resolve(pluginPreCommitHookFileName()))
        .truncateWithTemplate(
            () -> getClass().getResourceAsStream(BASE_PLUGIN_PRE_COMMIT_HOOK),
            StandardCharsets.UTF_8.toString(),
            mavenEnvironment.getMavenExecutable(debug).toAbsolutePath(),
            pomFile().toAbsolutePath(),
            mavenCliArguments());
    getLog().debug("Written plugin pre commit hook file");
  }

  private void configureHookBaseScripts(Path hooksDirectory) throws IOException {
    Executable basePreCommitHook =
        executableManager.getOrCreateExecutableScript(
            hooksDirectory.resolve(PRE_COMMIT_HOOK_BASE_SCRIPT));
    getLog().debug("Configuring '" + basePreCommitHook + "'");
    if (truncateHooksBaseScripts) {
      basePreCommitHook.truncate();
    }
    basePreCommitHook.appendCommandCall(preCommitHookBaseScriptCall());
  }

  private String mavenCliArguments() {
    Stream<String> propagatedProperties =
        ofNullable(propertiesToPropagate).map(Arrays::asList).orElse(Collections.emptyList())
            .stream()
            .filter(prop -> System.getProperty(prop) != null)
            .map(prop -> "-D" + prop + "=" + System.getProperty(prop));

    Stream<String> properties = Stream.concat(propagatedProperties, Stream.of(propertiesToAdd));
    if (preCommitHookPipeline != null && !preCommitHookPipeline.isEmpty()) {
      properties = Stream.concat(properties, Stream.of(preCommitHookPipeline));
    }
    return properties.collect(Collectors.joining(" "));
  }

  private Path prepareHooksDirectory() {
    getLog().debug("Preparing git hook directory");
    Path hooksDirectory;
    hooksDirectory = getOrCreateHooksDirectory();
    getLog().debug("Prepared git hook directory");
    return hooksDirectory;
  }

  private String preCommitHookBaseScriptCall() {
    return "./"
        + gitBaseDir().relativize(getOrCreateHooksDirectory())
        + "/"
        + pluginPreCommitHookFileName();
  }

  private String pluginPreCommitHookFileName() {
    return artifactId() + "." + BASE_PLUGIN_PRE_COMMIT_HOOK;
  }
}
