package com.cosium.code.format;

import com.cosium.code.format.executable.Executable;
import com.cosium.code.format.executable.ExecutableManager;
import com.cosium.code.format.utils.MavenUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

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
  private final MavenUtils mavenUtils = new MavenUtils();

  /**
   * True to truncate hooks base scripts before each install. <br>
   * Do not use this option if any other system or human manipulate the hooks
   */
  @Parameter(property = "truncateHooksBaseScripts", defaultValue = "false")
  private boolean truncateHooksBaseScripts;

  /** The list of properties to propagate to the hooks */
  @Parameter(property = "propertiesToPropagate")
  private String[] propertiesToPropagate;

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

    writePluginPreCommitHook(hooksDirectory);

    configurePreCommitHookBaseScript(hooksDirectory);
  }

  private void configurePreCommitHookBaseScript(Path hooksDirectory) throws IOException {
    Executable basePreCommitHook =
        executableManager.getOrCreateExecutableScript(
            hooksDirectory.resolve(PRE_COMMIT_HOOK_BASE_SCRIPT));
    getLog().debug("Configuring '" + basePreCommitHook + "'");
    if (truncateHooksBaseScripts) {
      basePreCommitHook.truncate();
    }
    basePreCommitHook.appendCommandCall(preCommitHookBaseScriptCall());
  }

  private void writePluginPreCommitHook(Path hooksDirectory) throws IOException {
    getLog().debug("Writing plugin pre commit hook file");
    executableManager
        .getOrCreateExecutableScript(hooksDirectory.resolve(pluginPreCommitHookFileName()))
        .truncateWithTemplate(
            () -> getClass().getResourceAsStream(BASE_PLUGIN_PRE_COMMIT_HOOK),
            mavenUtils.getMavenExecutable().toAbsolutePath(),
            mavenCliArguments());
    getLog().debug("Written plugin pre commit hook file");
  }

  private String mavenCliArguments() {


    return ofNullable(propertiesToPropagate)
        .map(Arrays::asList)
        .orElse(Collections.emptyList())
        .stream()
        .filter(prop -> System.getProperty(prop) != null)
        .map(prop -> "-D" + prop + "=" + System.getProperty(prop))
        .collect(Collectors.joining(" "));
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
        + baseDir().relativize(getOrCreateHooksDirectory())
        + "/"
        + pluginPreCommitHookFileName();
  }

  private String pluginPreCommitHookFileName() {
    return artifactId() + "." + BASE_PLUGIN_PRE_COMMIT_HOOK;
  }
}
