package com.cosium.code.format.executable;

import com.cosium.code.format.MavenGitCodeFormatException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

/** @author Réda Housni Alaoui */
public class DefaultCommandRunner implements CommandRunner {
  private final Supplier<Log> log;

  public DefaultCommandRunner(Supplier<Log> log) {
    this.log = log;
  }

  @Override
  public String run(Path workingDir, String... command) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      if (workingDir != null) {
        processBuilder.directory(workingDir.toFile());
      }
      processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);

      log.get().debug("Executing '" + StringUtils.join(command, StringUtils.SPACE) + "'");
      Process process = processBuilder.start();

      String output =
          IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim()
              + IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8).trim();

      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new CommandRunException(exitCode, output, command);
      }

      log.get().debug(output);
      return StringUtils.defaultIfBlank(output, null);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MavenGitCodeFormatException(e);
    } catch (IOException e) {
      throw new MavenGitCodeFormatException(e);
    }
  }
}
