package com.cosium.code.format.formatter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Created on 07/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public class CompositeCodeFormatter implements CodeFormatter {

  private final Collection<CodeFormatter> formatters;

  public CompositeCodeFormatter(CodeFormatter... formatters) {
    this.formatters = Collections.unmodifiableCollection(Arrays.asList(formatters));
  }

  @Override
  public void format(Path file) {
    formatters.forEach(formatter -> formatter.format(file));
  }

  @Override
  public boolean validate(Path file) {
    return formatters.stream()
        .map(codeFormatter -> codeFormatter.validate(file))
        .filter(valid -> !valid)
        .findFirst()
        .orElse(true);
  }
}
