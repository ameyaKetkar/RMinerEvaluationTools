/* LanguageTool, a natural language style checker
 * Copyright (C) 2012 Marcin Miłkowski
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.TestTools;
import org.languagetool.language.Ukrainian;
import org.languagetool.rules.RuleMatch;

public class MorfologikUkrainianSpellerRuleTest {

  @Test
  public void testMorfologikSpeller() throws IOException {
    MorfologikUkrainianSpellerRule rule = new MorfologikUkrainianSpellerRule (TestTools.getMessages("uk"), new Ukrainian());

    JLanguageTool langTool = new JLanguageTool(new Ukrainian());

    // correct sentences:
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("До ва�? прийде заввідділу!")).length);
    assertEquals(0, rule.match(langTool.getAnalyzedSentence(",")).length);
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("123454")).length);

    assertEquals(0, rule.match(langTool.getAnalyzedSentence("До на�? приїде The Beatles!")).length);

    // soft hyphen
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("пі�?\u00ADні")).length);
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("пі�?\u00ADні пі�?\u00ADні")).length);
    
    
    //incorrect sentences:

    RuleMatch[] matches = rule.match(langTool.getAnalyzedSentence("атакуючий"));
    // check match positions:
    assertEquals(1, matches.length);

    matches = rule.match(langTool.getAnalyzedSentence("шкл�?ний"));

    assertEquals(1, matches.length);
    assertEquals("�?кл�?ний", matches[0].getSuggestedReplacements().get(0));

    assertEquals(0, rule.match(langTool.getAnalyzedSentence("а")).length);

    // mix alphabets
    matches = rule.match(langTool.getAnalyzedSentence("прийдешнiй"));   // latin 'i'

    assertEquals(1, matches.length);
    assertEquals("прийдешній", matches[0].getSuggestedReplacements().get(0));

    // compounding
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("Жакет був �?иньо-жовтого кольору")).length);

    assertEquals(0, rule.match(langTool.getAnalyzedSentence("Він багато �?идів на інтернет-форумах")).length);

    assertEquals(1, rule.match(langTool.getAnalyzedSentence("Він багато �?идів на інтермет-форумах")).length);

    
    // dynamic tagging
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("ек�?-креветка")).length);

    assertEquals(1, rule.match(langTool.getAnalyzedSentence("банд-формуванн�?.")).length);


    // abbreviations

    RuleMatch[] match = rule.match(langTool.getAnalyzedSentence("Читанн�? віршів Т.Г.Шевченко і Г.Тютюнника"));
    assertEquals(new ArrayList<RuleMatch>(), Arrays.asList(match));

    match = rule.match(langTool.getAnalyzedSentence("Читанн�? віршів Т. Г. Шевченко і Г. Тютюнника"));
    assertEquals(new ArrayList<RuleMatch>(), Arrays.asList(match));

    match = rule.match(langTool.getAnalyzedSentence("�?нглі�?й�?ька мова (англ. English language, English) належить до герман�?ької групи"));
    assertEquals(new ArrayList<RuleMatch>(), Arrays.asList(match));

    match = rule.match(langTool.getAnalyzedSentence("�?нглі�?й�?ька мова (англ English language, English) належить до герман�?ької групи"));
    assertEquals(1, match.length);

  
    match = rule.match(langTool.getAnalyzedSentence("100 ти�?. гривень"));
    assertEquals(new ArrayList<RuleMatch>(), Arrays.asList(match));

    match = rule.match(langTool.getAnalyzedSentence("100 кв. м"));
    assertEquals(new ArrayList<RuleMatch>(), Arrays.asList(match));

    match = rule.match(langTool.getAnalyzedSentence("100 кв м"));
    assertEquals(1, Arrays.asList(match).size());
  }

}
