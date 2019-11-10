package com.cosium.code.format;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** @author Réda Housni Alaoui */
public class CommandRunner {
  private static final Logger LOG = LoggerFactory.getLogger(CommandRunner.class);

  public String run(Path workingDir, String... command) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.directory(workingDir.toFile()).redirectInput(ProcessBuilder.Redirect.INHERIT);

      LOG.debug("Executing '{}'", StringUtils.join(command, StringUtils.SPACE));
      Process process = processBuilder.start();

      String output =
          IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim()
              + IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8).trim();

      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new CommandRunException(exitCode, output, command);
      }

      LOG.debug(output);
      return StringUtils.defaultIfBlank(output, null);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
