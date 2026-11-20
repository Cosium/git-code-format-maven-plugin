package com.cosium.code.format_gjf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cosium.code.format_spi.CodeFormatterConfiguration;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GoogleJavaFormatterOptionsTest {

  @Test
  void shouldDefaultToGoogleStyleWithTheStyleDefaultMaxLineLength() {
    GoogleJavaFormatterOptions options = new GoogleJavaFormatterOptions(configuration(Map.of()));

    JavaFormatterOptions javaFormatterOptions = options.javaFormatterOptions();

    assertThat(javaFormatterOptions.style()).isEqualTo(JavaFormatterOptions.Style.GOOGLE);
    assertThat(javaFormatterOptions.maxLineLength()).isEqualTo(100);
  }

  @Test
  void shouldUseAospStyle() {
    GoogleJavaFormatterOptions options =
        new GoogleJavaFormatterOptions(configuration(Map.of("aosp", "true")));

    assertThat(options.javaFormatterOptions().style()).isEqualTo(JavaFormatterOptions.Style.AOSP);
  }

  @Test
  void shouldApplyMaxLineLengthToTheSelectedStyle() {
    GoogleJavaFormatterOptions options =
        new GoogleJavaFormatterOptions(
            configuration(Map.of("aosp", "true", "maxLineLength", "120")));

    JavaFormatterOptions javaFormatterOptions = options.javaFormatterOptions();

    assertThat(javaFormatterOptions.maxLineLength()).isEqualTo(120);
    assertThat(javaFormatterOptions.style().isAosp()).isTrue();
    assertThat(javaFormatterOptions.indentationMultiplier()).isEqualTo(2);
  }

  @Test
  void shouldRejectNonPositiveMaxLineLength() {
    assertThatThrownBy(
            () -> new GoogleJavaFormatterOptions(configuration(Map.of("maxLineLength", "0"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("googleJavaFormat.maxLineLength");
  }

  @Test
  void shouldRejectMaxLineLengthAboveTheSupportedLimit() {
    assertThatThrownBy(
            () -> new GoogleJavaFormatterOptions(configuration(Map.of("maxLineLength", "1000"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("googleJavaFormat.maxLineLength");
  }

  @Test
  void shouldRejectNonNumericMaxLineLength() {
    assertThatThrownBy(
            () -> new GoogleJavaFormatterOptions(configuration(Map.of("maxLineLength", "wide"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("googleJavaFormat.maxLineLength");
  }

  private static CodeFormatterConfiguration configuration(Map<String, String> values) {
    return key -> Optional.ofNullable(values.get(key));
  }
}
