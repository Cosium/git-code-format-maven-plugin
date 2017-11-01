package com.cosium.code.format;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created on 01/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public abstract class AbstractMavenGitCodeFormatMojo extends AbstractMojo {

  @Parameter(readonly = true, defaultValue = "${project}")
  private MavenProject currentProject;

  /** @return The git api allowing to intereact with the current git repository */
  protected Git git() {
    Repository gitRepository;
    try {
      gitRepository = new FileRepositoryBuilder().findGitDir(currentProject.getBasedir()).build();
    } catch (IOException e) {
      throw new RuntimeException(
          "Could not find the git repository. Run 'git init' if you did not.", e);
    }
    return Git.wrap(gitRepository);
  }

  protected Path baseDir() {
    return currentProject.getBasedir().toPath();
  }

  protected String artifactId() {
    return currentProject.getArtifactId();
  }
}
