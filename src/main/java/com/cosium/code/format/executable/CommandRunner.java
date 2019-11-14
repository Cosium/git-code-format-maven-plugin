package com.cosium.code.format.executable;

import java.nio.file.Path;

/** @author Réda Housni Alaoui */
public interface CommandRunner {
  String run(Path workingDir, String... command);
}
