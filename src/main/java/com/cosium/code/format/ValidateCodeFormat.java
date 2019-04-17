package com.cosium.code.format;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.nio.file.Path;

/**
 * Created on 16/01/18.
 *
 * @author Reda.Housni-Alaoui
 */
@Mojo(name = "validate-code-format", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ValidateCodeFormat extends AbstractFormatMojo {
  @Override
  protected void process(Path path) throws MojoFailureException {
    if (codeFormatter().validate(path)) {
      return;
    }

    throw new MojoFailureException(path + " is not correctly formatted !");
  }
}
