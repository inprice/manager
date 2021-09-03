package io.inprice.manager.helpers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.steadystate.css.parser.CSSOMParser;

import io.inprice.common.meta.EmailTemplate;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;

/**
 * https://github.com/lodenrogue/CSS-Inliner/blob/master/src/com/lodenrogue/cssinliner/CssInliner.java
 */
public class CssInliner {

  private static final Logger logger = LoggerFactory.getLogger(CssInliner.class);

  private static CSSStyleSheet styleSheet;
  private static String header;
  private static String footer;
  private static Map<EmailTemplate, String> inlinedTemplatesMap = new HashMap<>(EmailTemplate.values().length);

  private CssInliner() {
  }

  public static String inlinedEmailTemplate(EmailTemplate template) {
    String inlinedTemplate = inlinedTemplatesMap.get(template);
    if (inlinedTemplate != null) return inlinedTemplate;

    String content = checkFragments(template.getFileName());
    if (content == null) return null;

    final Document document = Jsoup.parse(content);
    inlinedTemplate = inlineCss(styleSheet, document);
    inlinedTemplatesMap.put(template, inlinedTemplate);

    return inlinedTemplate;
  }

  private static String checkFragments(String bodyFileName) {
    if (styleSheet == null) {
      try {
        CSSOMParser parser = new CSSOMParser();
        InputSource baseCssSource = new InputSource(new FileReader(CssInliner.class.getClassLoader().getResource("templates/fragment/base.css").getFile()));
        styleSheet = parser.parseStyleSheet(baseCssSource, null, null);
      } catch (IOException e) {
        logger.error("Failed to load base.css", e);
      }
    }

    if (header == null) {
      try {
        header = FileUtils.readFileToString(new File(CssInliner.class.getClassLoader().getResource("templates/fragment/header.html").getFile()), StandardCharsets.UTF_8);
      } catch (IOException e) {
        logger.error("Failed to load header.html", e);
      }
    }

    if (footer == null) {
      try {
        footer = FileUtils.readFileToString(new File(CssInliner.class.getClassLoader().getResource("templates/fragment/footer.html").getFile()), StandardCharsets.UTF_8);
      } catch (IOException e) {
        logger.error("Failed to load footer.html", e);
      }
    }

    String content = null;
    try {
      content = FileUtils.readFileToString(new File(CssInliner.class.getClassLoader().getResource("templates/" + bodyFileName).getFile()), StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("Failed to load content", e);
    }

    if (styleSheet == null || styleSheet.getCssRules() == null || styleSheet.getCssRules().getLength() == 0) {
      return null;
    }

    if (header == null || content == null || footer == null) {
      return null;
    }

    return header + content + footer;
  }

  private static String inlineCss(CSSStyleSheet styleSheet, Document document) {
    final CSSRuleList rules = styleSheet.getCssRules();
    final Map<Element, Map<String, String>> elementStyles = new HashMap<>();

    /*
     * For each rule in the style sheet, find all HTML elements that match based on
     * its selector and store the style attributes in the map with the selected
     * element as the key.
     */
    for (int i = 0; i < rules.getLength(); i++) {
      final CSSRule rule = rules.item(i);
      if (rule instanceof CSSStyleRule) {
        final CSSStyleRule styleRule = (CSSStyleRule) rule;
        final String selector = styleRule.getSelectorText();

        // Ignore pseudo classes, as JSoup's selector cannot handle them.
        if (!selector.contains(":")) {
          final Elements selectedElements = document.select(selector);
          for (final Element selected : selectedElements) {
            if (!elementStyles.containsKey(selected)) {
              elementStyles.put(selected, new LinkedHashMap<String, String>());
            }

            final CSSStyleDeclaration styleDeclaration = styleRule.getStyle();

            for (int j = 0; j < styleDeclaration.getLength(); j++) {
              final String propertyName = styleDeclaration.item(j);
              final String propertyValue = styleDeclaration.getPropertyValue(propertyName);
              final Map<String, String> elementStyle = elementStyles.get(selected);
              elementStyle.put(propertyName, propertyValue);
            }

          }
        }
      }
    }

    /*
     * Apply the style attributes to each element and remove the "class" attribute.
     */
    for (final Map.Entry<Element, Map<String, String>> elementEntry : elementStyles.entrySet()) {
      final Element element = elementEntry.getKey();
      final StringBuilder builder = new StringBuilder();
      for (final Map.Entry<String, String> styleEntry : elementEntry.getValue().entrySet()) {
        builder.append(styleEntry.getKey()).append(":").append(styleEntry.getValue()).append(";");
      }
      builder.append(element.attr("style"));
      element.attr("style", builder.toString());
      element.removeAttr("class");
    }
    return document.html();
  }

}