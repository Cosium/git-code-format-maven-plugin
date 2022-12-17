package com.cosium.code.format_spi;

/**
 * @author Réda Housni Alaoui
 */
public interface CodeFormatterFactory {

  /**
   * The prefix that will be used by the plugin user to configure formatters created by this
   * factory.
   *
   * <p>e.g. If the prefix was 'fooBar', a configuration attribute named 'baz' would have to be
   * declared as 'fooBar.baz' in the plugin configuration.
   */
  String configurationId();

  /**
   * @param configuration This code formatter factory configuration
   * @param sourceEncoding The files source encoding
   * @return The formatter to use
   */
  CodeFormatter build(CodeFormatterConfiguration configuration, String sourceEncoding);
}
