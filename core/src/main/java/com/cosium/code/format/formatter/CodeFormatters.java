package com.cosium.code.format.formatter;

import static java.util.Objects.requireNonNull;

import com.cosium.code.format.FileExtension;
import java.util.List;
import java.util.stream.Collectors;

/** @author Réda Housni Alaoui */
public class CodeFormatters {

  private final List<CodeFormatter> formatters;

  public CodeFormatters(List<CodeFormatter> formatters) {
    this.formatters = requireNonNull(formatters);
  }

  public List<CodeFormatter> forFileExtension(FileExtension fileExtension) {
    return formatters.stream()
        .filter(formatter -> formatter.fileExtension().equals(fileExtension))
        .collect(Collectors.toList());
  }
}
