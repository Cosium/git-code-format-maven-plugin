package com.cosium.code.format;

/**
 * @author Réda Housni Alaoui
 */
public class MavenGitCodeFormatException extends RuntimeException {

  public MavenGitCodeFormatException(Throwable cause) {
    super(cause);
  }

  public MavenGitCodeFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  public MavenGitCodeFormatException(String message) {
    super(message);
  }
}
