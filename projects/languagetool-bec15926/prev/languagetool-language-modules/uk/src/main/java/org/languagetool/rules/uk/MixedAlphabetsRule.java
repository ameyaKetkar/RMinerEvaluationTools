/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules.uk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.rules.Category;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;

/**
 * A rule that matches words Latin and Cyrillic characters in them
 * 
 * @author Andriy Rysin
 */
public class MixedAlphabetsRule extends Rule {

  private static final Pattern LIKELY_LATIN_NUMBER = Pattern.compile("[XVIХІ]{2,8}");
  private static final Pattern LATIN_NUMBER_WITH_CYRILLICS = Pattern.compile("Х{1,3}І{1,3}|І{1,3}Х{1,3}|Х{2,3}|І{2,3}");
  private static final Pattern MIXED_ALPHABETS = Pattern.compile(".*([a-zA-Z]'?[а-�?іїєґ�?-ЯІЇЄ�?]|[а-�?іїєґ�?-ЯІЇЄ�?]'?[a-zA-Z]).*");
  private static final Pattern CYRILLIC_ONLY = Pattern.compile(".*[бвгґдєжзйїлнпфцчшщью�?БГ�?ДЄЖЗИЙЇЛПФЦЧШЩЬЮЯ].*");
  private static final Pattern LATIN_ONLY = Pattern.compile(".*[bdfghjlqrsvzDFGLNQRSUVZ].*");

  public MixedAlphabetsRule(final ResourceBundle messages) throws IOException {
    super.setCategory(new Category(messages.getString("category_misc")));
  }

  @Override
  public final String getId() {
    return "UK_MIXED_ALPHABETS";
  }

  @Override
  public String getDescription() {
    return "Змішуванн�? кирилиці й латиниці";
  }

  public String getShort() {
    return "Мішанина розкладок";
  }

  public String getSuggestion(String word) {
    String highlighted = word.replaceAll("([a-zA-Z])([а-�?іїєґ�?-ЯІЇЄ�?])", "$1/$2");
    highlighted = highlighted.replaceAll("([а-�?іїєґ�?-ЯІЇЄ�?])([a-zA-Z])", "$1/$2");
    return " мі�?тить �?уміш кирилиці та латиниці: «"+ highlighted +"», виправленн�?: ";
  }

  /**
   * Indicates if the rule is case-sensitive. 
   * @return true if the rule is case-sensitive, false otherwise.
   */
  public boolean isCaseSensitive() {
    return true;  
  }

  @Override
  public final RuleMatch[] match(final AnalyzedSentence sentence) {
    List<RuleMatch> ruleMatches = new ArrayList<>();
    AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();

    for (AnalyzedTokenReadings tokenReadings: tokens) {
      String tokenString = tokenReadings.getToken();

      if( MIXED_ALPHABETS.matcher(tokenString).matches() ) {
      
        List<String> replacements = new ArrayList<>();

        if(!LATIN_ONLY.matcher(tokenString).matches() && ! LIKELY_LATIN_NUMBER.matcher(tokenString).matches()) {
          replacements.add( toCyrillic(tokenString) );
        }
        if(!CYRILLIC_ONLY.matcher(tokenString).matches() || LIKELY_LATIN_NUMBER.matcher(tokenString).matches()) {
          replacements.add( toLatin(tokenString) );
        }

        if (replacements.size() > 0) {
          RuleMatch potentialRuleMatch = createRuleMatch(tokenReadings, replacements);
          ruleMatches.add(potentialRuleMatch);
        }
      }
      else if(LATIN_NUMBER_WITH_CYRILLICS.matcher(tokenString).matches()) {
        List<String> replacements = new ArrayList<>();
        replacements.add( toLatin(tokenString) );

        RuleMatch potentialRuleMatch = createRuleMatch(tokenReadings, replacements);
        ruleMatches.add(potentialRuleMatch);
      }
    }
    return toRuleMatchArray(ruleMatches);
  }

  private RuleMatch createRuleMatch(AnalyzedTokenReadings readings, List<String> replacements) {
    String tokenString = readings.getToken();
    String msg = tokenString + getSuggestion(tokenString) + StringUtils.join(replacements, ", ");

    RuleMatch potentialRuleMatch = new RuleMatch(this, readings.getStartPos(), readings.getEndPos(), msg, getShort());
    potentialRuleMatch.setSuggestedReplacements(replacements);

    return potentialRuleMatch;
  }

  @Override
  public void reset() {
  }

  private static final Map<Character, Character> toLatMap = new HashMap<>();
  private static final Map<Character, Character> toCyrMap = new HashMap<>();
  private static final String cyrChars = "аеікмор�?тух�?ВЕІКМ�?ОРСТУХ";
  private static final String latChars = "aeikmopctyxABEIKMHOPCTYX";

  static {
    for (int i = 0; i < cyrChars.length(); i++) {
      toLatMap.put(cyrChars.charAt(i), latChars.charAt(i));
      toCyrMap.put(latChars.charAt(i), cyrChars.charAt(i));
    }
  }

  private static String toCyrillic(String word) {
    for (Map.Entry<Character, Character> entry : toCyrMap.entrySet()) {
      word = word.replace(entry.getKey(), entry.getValue());
    }
    return word;
  }

  private static String toLatin(String word) {
    for (Map.Entry<Character, Character> entry : toLatMap.entrySet()) {
      word = word.replace(entry.getKey(), entry.getValue());
    }
    return word;
  }

}
