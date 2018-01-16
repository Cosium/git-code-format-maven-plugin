package com.cosium.code.format;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.nio.file.Path;

/**
 * Created on 07/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
@Mojo(name = "format-code", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class FormatCodeMojo extends AbstractFormatMojo {

  @Override
  protected void process(Path path) {
    codeFormatter().format(path);
  }
}
