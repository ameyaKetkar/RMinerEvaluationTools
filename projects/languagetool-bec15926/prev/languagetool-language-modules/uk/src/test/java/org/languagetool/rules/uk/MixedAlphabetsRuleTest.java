/* LanguageTool, a natural language style checker
 * Copyright (C) 2013 Andriy Rysin
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

import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.TestTools;
import org.languagetool.language.Ukrainian;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class MixedAlphabetsRuleTest {

  @Test
  public void testRule() throws IOException {
    final MixedAlphabetsRule rule = new MixedAlphabetsRule(TestTools.getMessages("uk"));
    final JLanguageTool langTool = new JLanguageTool(new Ukrainian());

    // correct sentences:
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("�?мітт�?")).length);
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("not mixed")).length);
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("123454")).length);

    //incorrect sentences:

    RuleMatch[] matches = rule.match(langTool.getAnalyzedSentence("�?мiтт�?"));  //latin i
    // check match positions:
    assertEquals(1, matches.length);
    assertEquals(Arrays.asList("�?мітт�?"), matches[0].getSuggestedReplacements());

    matches = rule.match(langTool.getAnalyzedSentence("mіхed"));  // cyrillic i and x

    assertEquals(1, matches.length);
    assertEquals(Arrays.asList("mixed"), matches[0].getSuggestedReplacements());
    
    matches = rule.match(langTool.getAnalyzedSentence("XІ")); // cyrillic І and latin X

    assertEquals(1, matches.length);
    assertEquals(Arrays.asList("XI"), matches[0].getSuggestedReplacements());

    matches = rule.match(langTool.getAnalyzedSentence("ХI")); // cyrillic X and latin I

    assertEquals(1, matches.length);
    assertEquals(Arrays.asList("XI"), matches[0].getSuggestedReplacements());

    matches = rule.match(langTool.getAnalyzedSentence("ХІ")); // cyrillic both X and I used for latin number

    assertEquals(1, matches.length);
    assertEquals(Arrays.asList("XI"), matches[0].getSuggestedReplacements());
  }

}
