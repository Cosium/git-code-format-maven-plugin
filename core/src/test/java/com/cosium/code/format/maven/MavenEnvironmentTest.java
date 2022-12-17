package com.cosium.code.format.maven;

import static org.assertj.core.api.Assertions.assertThat;

import com.cosium.code.format.TestingLog;
import com.cosium.code.format.executable.CommandRunException;
import com.cosium.code.format.executable.CommandRunner;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class MavenEnvironmentTest {

  private Map<String, String> systemProperties;
  private TestingCommandRunner commandRunner;
  private MavenEnvironment tested;

  @Before
  public void before() {
    systemProperties = new HashMap<>();
    commandRunner = new TestingCommandRunner();
    tested = new MavenEnvironment(TestingLog::new, systemProperties::get, commandRunner);
  }

  @Test
  public void testMavenHomeExecutable() {
    systemProperties.put("maven.home", "/opt/maven");
    Path expectedPath = Paths.get("/opt/maven/bin/mvn");
    commandRunner.validExecutables.add(expectedPath.toString());
    Path path = tested.getMavenExecutable(false);
    assertThat(path).isEqualTo(expectedPath);
  }

  @Test
  public void testMavenHomeDebugExecutable() {
    systemProperties.put("maven.home", "/opt/maven");
    Path expectedPath = Paths.get("/opt/maven/bin/mvnDebug");
    commandRunner.validExecutables.add(expectedPath.toString());
    Path path = tested.getMavenExecutable(true);
    assertThat(path).isEqualTo(expectedPath);
  }

  @Test
  public void testMavenPathExecutableFallback() {
    systemProperties.put("maven.home", "/opt/maven");
    commandRunner.validExecutables.add("mvn");
    Path path = tested.getMavenExecutable(false);
    assertThat(path).isEqualTo(Paths.get("mvn"));
  }

  @Test
  public void testMavenPathDebugExecutableFallback() {
    systemProperties.put("maven.home", "/opt/maven");
    commandRunner.validExecutables.add("mvnDebug");
    Path path = tested.getMavenExecutable(true);
    assertThat(path).isEqualTo(Paths.get("mvnDebug"));
  }

  private static class TestingCommandRunner implements CommandRunner {

    final Set<String> validExecutables = new HashSet<>();

    @Override
    public String run(Path workingDir, Map<String, String> environment, String... command) {
      if (validExecutables.contains(command[0])) {
        return null;
      }
      throw new CommandRunException(1, "");
    }
  }
}
