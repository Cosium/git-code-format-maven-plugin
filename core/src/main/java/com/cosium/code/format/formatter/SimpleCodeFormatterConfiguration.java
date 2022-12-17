package com.cosium.code.format.formatter;

import com.cosium.code.format_spi.CodeFormatterConfiguration;
import java.util.Map;
import java.util.Optional;

/**
 * @author Réda Housni Alaoui
 */
class SimpleCodeFormatterConfiguration implements CodeFormatterConfiguration {

  private final Map<String, String> globalConfiguration;
  private final String formatterConfigurationId;

  public SimpleCodeFormatterConfiguration(
      Map<String, String> globalConfiguration, String formatterConfigurationId) {
    this.globalConfiguration = globalConfiguration;
    this.formatterConfigurationId = formatterConfigurationId;
  }

  @Override
  public Optional<String> getValue(String key) {
    return Optional.ofNullable(
        globalConfiguration.get(String.format("%s.%s", formatterConfigurationId, key)));
  }
}
