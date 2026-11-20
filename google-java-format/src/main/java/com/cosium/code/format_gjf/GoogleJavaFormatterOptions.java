package com.cosium.code.format_gjf;

import static com.google.googlejavaformat.java.JavaFormatterOptions.Style.AOSP;
import static com.google.googlejavaformat.java.JavaFormatterOptions.Style.GOOGLE;

import com.cosium.code.format_spi.CodeFormatterConfiguration;
import com.google.googlejavaformat.java.JavaFormatterOptions;

/**
 * @author Réda Housni Alaoui
 */
class GoogleJavaFormatterOptions {

  /** Sentinel value meaning "use the maximum line length of the selected style". */
  private static final int STYLE_DEFAULT_MAX_LINE_LENGTH = 0;

  /**
   * Upper bound accepted by google-java-format: {@code Doc.MAX_LINE_WIDTH - 1}. Layout widths
   * saturate at {@code Doc.MAX_LINE_WIDTH}, which is also how forced breaks are represented.
   */
  private static final int MAX_SUPPORTED_MAX_LINE_LENGTH = 999;

  private final JavaFormatterOptions.Style style;
  private final int maxLineLength;
  private final boolean formatJavadoc;
  private final boolean fixImportsOnly;
  private final boolean skipSortingImports;
  private final boolean skipRemovingUnusedImports;

  public GoogleJavaFormatterOptions(CodeFormatterConfiguration configuration) {
    boolean aosp = configuration.getValue("aosp").map(Boolean::parseBoolean).orElse(false);
    maxLineLength =
        configuration
            .getValue("maxLineLength")
            .map(GoogleJavaFormatterOptions::parseMaxLineLength)
            .orElse(STYLE_DEFAULT_MAX_LINE_LENGTH);
    if (maxLineLength == STYLE_DEFAULT_MAX_LINE_LENGTH) {
      style = aosp ? AOSP : GOOGLE;
    } else {
      style =
          (aosp
                  ? JavaFormatterOptions.Style.builder().aosp()
                  : JavaFormatterOptions.Style.builder().google())
              .maxLineLength(maxLineLength)
              .build();
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

  private static int parseMaxLineLength(String value) {
    final int parsed;
    try {
      parsed = Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "googleJavaFormat.maxLineLength must be an integer, but was '" + value + "'", e);
    }
    if (parsed < 1 || parsed > MAX_SUPPORTED_MAX_LINE_LENGTH) {
      throw new IllegalArgumentException(
          "googleJavaFormat.maxLineLength must be between 1 and "
              + MAX_SUPPORTED_MAX_LINE_LENGTH
              + ", but was "
              + parsed);
    }
    return parsed;
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
        + ", maxLineLength="
        + maxLineLength
        + ", fixImportsOnly="
        + fixImportsOnly
        + ", skipSortingImports="
        + skipSortingImports
        + ", skipRemovingUnusedImports="
        + skipRemovingUnusedImports
        + '}';
  }
}
