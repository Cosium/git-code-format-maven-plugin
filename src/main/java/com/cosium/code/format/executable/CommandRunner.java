package com.cosium.code.format.executable;

import java.nio.file.Path;
import java.util.Map;

/**
 * @author Réda Housni Alaoui
 */
public interface CommandRunner {
  String run(Path workingDir, Map<String, String> environment, String... command);
}
