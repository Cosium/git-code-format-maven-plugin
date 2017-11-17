package com.cosium.code.format;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created on 17/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public abstract class AbstractModulMavenGitCodeFormatMojo extends AbstractMavenGitCodeFormatMojo {

  @Parameter(property = "includedModules")
  private List<String> includedModules;

  @Parameter(property = "excludedModules")
  private List<String> excludedModules;

  /** @return True if the goal is enabled for the current module */
  private boolean isEnabled() {
    List<String> excludedModules =
        Optional.ofNullable(this.excludedModules).orElse(Collections.emptyList());
    if (excludedModules.contains(artifactId())) {
      getLog().info(artifactId() + " is part of the excluded modules. Goal disabled.");
      return false;
    }

    List<String> includedModules =
        Optional.ofNullable(this.includedModules).orElse(Collections.emptyList());
    if (!includedModules.isEmpty() && !includedModules.contains(artifactId())) {
      getLog().info(artifactId() + " is not part of defined included modules. Goal disabled.");
      return false;
    }

    if ((!includedModules.isEmpty() || !excludedModules.isEmpty())
        && isExecutionRoot()) {
      getLog()
          .info(
              "Explicit included or excluded modules defined and the current module the execution root. Goal disabled.");
      return false;
    }

    getLog().debug("Goal enabled");
    return true;
  }

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (!isEnabled()) {
      return;
    }
    doExecute();
  }

  protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;
}
