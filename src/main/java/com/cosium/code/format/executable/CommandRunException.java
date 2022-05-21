package com.cosium.code.format.executable;

import org.apache.commons.lang3.StringUtils;

/**
 * Created on 20/02/18.
 *
 * @author Reda.Housni-Alaoui
 */
public class CommandRunException extends RuntimeException {

  private final int exitCode;

  public CommandRunException(int exitCode, String output, String... command) {
    super(
        String.format(
            "'%s' failed with code %s: %n%n %s",
            StringUtils.join(command, StringUtils.SPACE), exitCode, output));
    this.exitCode = exitCode;
  }

  public int getExitCode() {
    return exitCode;
  }
}
