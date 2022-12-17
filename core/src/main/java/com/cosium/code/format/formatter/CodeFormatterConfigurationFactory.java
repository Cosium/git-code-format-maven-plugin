package com.cosium.code.format.formatter;

import com.cosium.code.format_spi.CodeFormatterConfiguration;
import java.util.Map;

/**
 * @author Réda Housni Alaoui
 */
public class CodeFormatterConfigurationFactory {

  private final Map<String, String> globalConfiguration;

  public CodeFormatterConfigurationFactory(Map<String, String> globalConfiguration) {
    this.globalConfiguration = globalConfiguration;
  }

  public CodeFormatterConfiguration build(String formatterConfigurationId) {
    return new SimpleCodeFormatterConfiguration(globalConfiguration, formatterConfigurationId);
  }
}
