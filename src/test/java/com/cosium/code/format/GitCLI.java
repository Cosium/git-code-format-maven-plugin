package com.cosium.code.format;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/** @author Réda Housni Alaoui */
public class GitCLI {

  private final Path repositoryDirectory;
  private final CommandRunner commandRunner;

  GitCLI(Path repositoryDirectory) {
    this.repositoryDirectory = requireNonNull(repositoryDirectory);
    this.commandRunner = new CommandRunner();
  }

  public String commit(boolean all, String message) {
    List<String> commands = new ArrayList<>();
    commands.add("commit");
    if (all) {
      commands.add("-a");
    }
    commands.add("-m");
    commands.add(message);
    return git(commands.toArray(new String[0]));
  }

  private String git(String... args) {
    return commandRunner.run(repositoryDirectory, ArrayUtils.insert(0, args, "git"));
  }
}
