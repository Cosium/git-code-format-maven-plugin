package com.cosium.code.format;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Created on 01/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
@Mojo(name = "on-pre-commit", defaultPhase = LifecyclePhase.NONE)
public class OnPreCommitMojo extends AbstractMavenGitCodeFormatMojo {

  private static final String COMMA = ",";

  /** The comma separated staged files list. i.e. "src/main/java/Foo.java,src/main/java/Bar.java" */
  @Parameter(property = "stagedFiles", required = true)
  private String stagedFiles;

  public void execute() throws MojoExecutionException {
    try {
      getLog().info("Executing pre-commit hooks");
      doExecute();
      getLog().info("Executed pre-commit hooks");
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void doExecute() throws GitAPIException {
    getLog().debug("Staged files are '" + stagedFiles + "'");

    Pattern.compile(COMMA)
        .splitAsStream(stagedFiles)
        .map(StringUtils::trim)
        .filter(StringUtils::isNotBlank)
        .map(this::toPath)
        .filter(this::isEligible)
        .forEach(file -> codeFormatter().format(file));
  }

  private Path toPath(String diffPath) {
    Path workTree = gitRepository().getWorkTree().toPath();
    return workTree.resolve(diffPath);
  }

  private boolean isEligible(Path file) {
    if (!file.toAbsolutePath().toString().contains(baseDir().toAbsolutePath().toString())) {
      getLog().debug(file + " does not belong to the current project");
      return false;
    }
    return true;
  }
}
