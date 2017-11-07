package com.cosium.code.format.formatter;

import java.nio.file.Path;

/**
 * Created on 07/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public interface CodeFormatter {

  void format(Path file);
}
