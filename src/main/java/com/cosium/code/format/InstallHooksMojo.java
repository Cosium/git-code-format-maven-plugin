package com.cosium.code.format;

import static java.util.Optional.ofNullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import com.cosium.code.format.executable.Executable;
import com.cosium.code.format.executable.ExecutableManager;
import com.cosium.code.format.maven.MavenEnvironment;

/**
 * Installs git hooks on each initialization. Hooks are always overriden in case of changes in:
 *
 * <ul>
 *   <li>maven installation
 *   <li>plugin structure
 * </ul>
 */
@Mojo(name = "install-hooks", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class InstallHooksMojo extends AbstractMavenGitCodeFormatMojo {

  /** Name of 1.x plugin pre-commit hook */
  private static final String LEGACY_BASE_PLUGIN_PRE_COMMIT_HOOK =
      "maven-git-code-format.pre-commit.sh";

  private static final String BASE_PLUGIN_PRE_COMMIT_HOOK = "git-code-format.pre-commit.sh";
  private static final String PRE_COMMIT_HOOK_BASE_SCRIPT = "pre-commit";

  private final ExecutableManager executableManager = new ExecutableManager(this::getLog);
  private final MavenEnvironment mavenEnvironment = new MavenEnvironment(this::getLog);

  /** Skip execution of this goal */
  @Parameter(property = "gcf.skip", defaultValue = "false")
  private boolean skip;

  /** Skip execution of this specific goal */
  @Parameter(property = "gcf.skipInstallHooks", defaultValue = "false")
  private boolean skipInstallHooks;

  /**
   * True to truncate hooks base scripts before each install. <br>
   * Do not use this option if any other system or human manipulate the hooks
   */
  @Parameter(property = "gcf.truncateHooksBaseScripts", defaultValue = "false")
  private boolean truncateHooksBaseScripts;

  /** The list of properties to propagate to the hooks */
  @Parameter(property = "gcf.propertiesToPropagate")
  private String[] propertiesToPropagate;

  /** The list of properties to add to the hooks */
  @Parameter(property = "gcf.propertiesToAdd")
  private String[] propertiesToAdd;

  @Parameter(property = "gcf.debug", defaultValue = "false")
  private boolean debug;

  /**
   * Add pipeline to process the results of the pre-commit hook. Exit non-zero to prevent the commit
   */
  @Parameter(property = "gcf.preCommitHookPipeline", defaultValue = "")
  private String preCommitHookPipeline;

  @Parameter(property = "gcf.hookScriptName")
  private String hookScriptName;

  @Override
  public void execute() throws MojoExecutionException {
    if (!isExecutionRoot()) {
      getLog().debug("Not in execution root. Do not execute.");
      return;
    }
    if (this.skip || this.skipInstallHooks) {
      final Log log = getLog();
      if (log.isInfoEnabled()) {
        log.info("skipped");
      }
      return;
    }

    try {
      getLog().info("Installing git hooks");
      doExecute();
      getLog().info("Installed git hooks");
    } catch (final Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void doExecute() throws IOException {
    final Path hooksDirectory = prepareHooksDirectory();

    writePluginHooks(hooksDirectory);

    configureHookBaseScripts(hooksDirectory);
  }

  private void writePluginHooks(final Path hooksDirectory) throws IOException {
    getLog().debug("Removing legacy pre commit hook file");
    Files.deleteIfExists(hooksDirectory.resolve(legacyPluginPreCommitHookFileName()));
    getLog().debug("Rmeoved legacy pre commit hook file");

    getLog().debug("Writing plugin pre commit hook file");
    this.executableManager
    .getOrCreateExecutableScript(hooksDirectory.resolve(pluginPreCommitHookFileName()))
    .truncateWithTemplate(
        () -> getClass().getResourceAsStream(BASE_PLUGIN_PRE_COMMIT_HOOK),
        StandardCharsets.UTF_8.toString(),
        this.mavenEnvironment.getMavenExecutable(this.debug).toAbsolutePath(),
        pomFile().toAbsolutePath(),
        mavenCliArguments());
    getLog().debug("Written plugin pre commit hook file");
  }

  private void configureHookBaseScripts(final Path hooksDirectory) throws IOException {
    final Executable basePreCommitHook =
        this.executableManager.getOrCreateExecutableScript(
            hooksDirectory.resolve(PRE_COMMIT_HOOK_BASE_SCRIPT));
    getLog().debug("Configuring '" + basePreCommitHook + "'");
    if (this.truncateHooksBaseScripts) {
      basePreCommitHook.truncate();
    } else {
      legacyPreCommitHookBaseScriptCalls().forEach(basePreCommitHook::removeCommandCall);
    }
    basePreCommitHook.appendCommandCall(preCommitHookBaseScriptCall());
  }

  private String mavenCliArguments() {
    final Stream<String> propagatedProperties =
        ofNullable(this.propertiesToPropagate).map(Arrays::asList).orElse(Collections.emptyList())
        .stream()
        .filter(prop -> System.getProperty(prop) != null)
        .map(prop -> "-D" + prop + "=" + System.getProperty(prop));

    Stream<String> properties = Stream.concat(propagatedProperties, Stream.of(this.propertiesToAdd));
    if (this.preCommitHookPipeline != null && !this.preCommitHookPipeline.isEmpty()) {
      properties = Stream.concat(properties, Stream.of(this.preCommitHookPipeline));
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
    return "$(git rev-parse --git-dir)/" + HOOKS_DIR + "/" + pluginPreCommitHookFileName();
  }

  private List<String> legacyPreCommitHookBaseScriptCalls() {
    final List<String> calls = new ArrayList<>();
    calls.add(
        "./"
            + gitBaseDir().relativize(getOrCreateHooksDirectory())
            + "/"
            + legacyPluginPreCommitHookFileName());
    calls.add(
        "./"
            + gitBaseDir().relativize(getOrCreateHooksDirectory())
            + "/"
            + pluginPreCommitHookFileName());
    return calls;
  }

  private String getHookScriptName() {
    return Optional.ofNullable(this.hookScriptName).orElseGet(this::artifactId);
  }

  private String pluginPreCommitHookFileName() {
    return getHookScriptName() + "." + BASE_PLUGIN_PRE_COMMIT_HOOK;
  }

  private String legacyPluginPreCommitHookFileName() {
    return getHookScriptName() + "." + LEGACY_BASE_PLUGIN_PRE_COMMIT_HOOK;
  }
}
