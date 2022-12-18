package com.cosium.code.format_gjf;

import static com.google.googlejavaformat.java.JavaFormatterOptions.Style.AOSP;
import static com.google.googlejavaformat.java.JavaFormatterOptions.Style.GOOGLE;

import com.cosium.code.format_spi.CodeFormatterConfiguration;
import com.google.googlejavaformat.java.JavaFormatterOptions;

/**
 * @author Réda Housni Alaoui
 */
class GoogleJavaFormatterOptions {

  private final JavaFormatterOptions.Style style;
  private final boolean formatJavadoc;
  private final boolean fixImportsOnly;
  private final boolean skipSortingImports;
  private final boolean skipRemovingUnusedImports;

  public GoogleJavaFormatterOptions(CodeFormatterConfiguration configuration) {
    boolean aosp = configuration.getValue("aosp").map(Boolean::parseBoolean).orElse(false);
    if (aosp) {
      style = AOSP;
    } else {
      style = GOOGLE;
    }
    formatJavadoc = configuration.getValue("formatJavadoc").map(Boolean::parseBoolean).orElse(true);
    fixImportsOnly =
        configuration.getValue("fixImportsOnly").map(Boolean::parseBoolean).orElse(false);
    skipSortingImports =
        configuration.getValue("skipSortingImports").map(Boolean::parseBoolean).orElse(false);
    skipRemovingUnusedImports =
        configuration
            .getValue("skipRemovingUnusedImports")
            .map(Boolean::parseBoolean)
            .orElse(false);
  }

  public JavaFormatterOptions javaFormatterOptions() {
    return JavaFormatterOptions.builder().style(style).formatJavadoc(formatJavadoc).build();
  }

  public boolean isFixImportsOnly() {
    return fixImportsOnly;
  }

  public boolean isSkipSortingImports() {
    return skipSortingImports;
  }

  public boolean isSkipRemovingUnusedImports() {
    return skipRemovingUnusedImports;
  }

  @Override
  public String toString() {
    return "GoogleJavaFormatterOptions{"
        + "style="
        + style
        + ", fixImportsOnly="
        + fixImportsOnly
        + ", skipSortingImports="
        + skipSortingImports
        + ", skipRemovingUnusedImports="
        + skipRemovingUnusedImports
        + '}';
  }
}
