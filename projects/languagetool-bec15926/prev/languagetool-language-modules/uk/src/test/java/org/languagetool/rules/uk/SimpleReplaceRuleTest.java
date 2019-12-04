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

import junit.framework.TestCase;
import org.languagetool.JLanguageTool;
import org.languagetool.TestTools;
import org.languagetool.language.Ukrainian;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.Arrays;


public class SimpleReplaceRuleTest extends TestCase {

  public void testRule() throws IOException {
    SimpleReplaceRule rule = new SimpleReplaceRule(TestTools.getEnglishMessages());

    RuleMatch[] matches;
    JLanguageTool langTool = new JLanguageTool(new Ukrainian());

    // correct sentences:
    matches = rule.match(langTool.getAnalyzedSentence("Ці р�?дки повинні збігати�?�?."));
    assertEquals(0, matches.length);

    // incorrect sentences:
    matches = rule.match(langTool.getAnalyzedSentence("Ці р�?дки повинні �?півпадати."));
    assertEquals(1, matches.length);
    assertEquals(2, matches[0].getSuggestedReplacements().size());
    assertEquals(Arrays.asList("збігати�?�?", "�?ходити�?�?"), matches[0].getSuggestedReplacements());

    matches = rule.match(langTool.getAnalyzedSentence("�?ападаючий"));
    assertEquals(1, matches.length);
    assertEquals(Arrays.asList("�?ападник", "�?ападальний", "�?ападний"), matches[0].getSuggestedReplacements());

    matches = rule.match(langTool.getAnalyzedSentence("�?ападаючого"));
    assertEquals(1, matches.length);
    assertEquals(Arrays.asList("�?ападник", "�?ападальний", "�?ападний"), matches[0].getSuggestedReplacements());

    // test ignoreTagged
    matches = rule.match(langTool.getAnalyzedSentence("щедрота"));
    assertEquals(1, matches.length);
    assertEquals(Arrays.asList("щедрі�?ть", "гойні�?ть", "щедрин�?"), matches[0].getSuggestedReplacements());

    matches = rule.match(langTool.getAnalyzedSentence("щедроти"));
    assertEquals(0, matches.length);
  }
}
