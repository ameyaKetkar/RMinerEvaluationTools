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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.languagetool.AnalyzedSentence;
import org.languagetool.JLanguageTool;
import org.languagetool.TestTools;
import org.languagetool.language.Ukrainian;
import org.languagetool.rules.RuleMatch;

public class TokenAgreementRuleTest {

  private JLanguageTool langTool;
  private TokenAgreementRule rule;

  @Before
  public void setUp() throws IOException {
    rule = new TokenAgreementRule(TestTools.getMessages("uk"));
    langTool = new JLanguageTool(new Ukrainian());
  }
  
  @Test
  public void testRule() throws IOException {

    // correct sentences:
    assertEmptyMatch("без повного");
    assertEmptyMatch("без неба");

    assertEmptyMatch("по авеню");

    assertEmptyMatch("що за ганебна непо�?лідовні�?ть?");

    assertEmptyMatch("щодо вла�?не людини");
    assertEmptyMatch("у загалом �?импатичній пові�?тині");

    assertEmptyMatch("понад половина людей");
    assertEmptyMatch("з понад �?та людей");

    assertEmptyMatch("по нервах");
    assertEmptyMatch("з о�?обливою увагою");

    assertEmptyMatch("щодо бодай гіпотетичної здатно�?ті");
    assertEmptyMatch("хто їде на заробітки за кордон");

    assertEmptyMatch("піти в президенти");
    assertEmptyMatch("піти межі люди");

    assertEmptyMatch("що то була за людина");
    assertEmptyMatch("що за людина");
    assertEmptyMatch("що балотував�?�? за цім округом");

    assertEmptyMatch("на дому");

    assertEmptyMatch("окрім �?к українці");
    assertEmptyMatch("за дві�?ті метрів");
    assertEmptyMatch("переходить у Фрідріх Штра�?�?е");
    assertEmptyMatch("від міну�? 1 до плю�? 1");
    assertEmptyMatch("до міну�? �?орока град");
    assertEmptyMatch("до міну�? ші�?тде�?�?ти");
    assertEmptyMatch("через років 10");
    assertEmptyMatch("на хвилин 9-10");
    assertEmptyMatch("�?півпрацювати із �?обі подібними");
    assertEmptyMatch("через у�?ім відомі причини");
    assertEmptyMatch("через нікому не відомі причини");
    assertEmptyMatch("прийшли до В�?Т «Кривий Ріг цемент»");
    assertEmptyMatch("від �? до Я");
    assertEmptyMatch("до та пі�?л�?");
    assertEmptyMatch("до �?хід �?онц�?");
    assertEmptyMatch("з рана до вечора, від рана до ночі");
    assertEmptyMatch("до �?�?К «�?адра України»");
    assertEmptyMatch("призвів до значною мірою демократичного �?ереднього кла�?у");
    assertEmptyMatch("Вони замі�?ть �?ндрій вибрали Юрій");
    assertEmptyMatch("на мохом �?теленому дні");
    assertEmptyMatch("ча�? від ча�?у нам доводило�?ь");
    assertEmptyMatch("�?кий до речі вони при�?�?гали�?�?");
    assertEmptyMatch("ні до чого доброго �?илові дії не призведуть");
//    assertEmptyMatch("Імена від �?ндрій до Юрій");  // називний між від і до рідко зу�?трічаєть�?�? але такий вин�?ток ховає багато помилок 

    assertEquals(1, rule.match(langTool.getAnalyzedSentence("призвів до значною мірою демократичному �?ередньому кла�?у")).length);

//    assertEmptyMatch("�?к у Конана Дойла")).length); //TODO
//    assertEmptyMatch("�?к у Конану Дойла")).length);
//    assertEmptyMatch("�?к у Конан Дойла")).length);
    
    //incorrect sentences:

    RuleMatch[] matches = rule.match(langTool.getAnalyzedSentence("без небу"));
    // check match positions:
    assertEquals(1, matches.length);
    assertEquals(Arrays.asList("неба"), matches[0].getSuggestedReplacements());

    matches = rule.match(langTool.getAnalyzedSentence("не в о�?танню чергу через    корупцією, міжрелігійну ворожнечу"));
    assertEquals(1, matches.length);

    matches = rule.match(langTool.getAnalyzedSentence("по нервам"));
    // check match positions:
    assertEquals(1, matches.length);
    assertEquals(3, matches[0].getFromPos());
    assertEquals(9, matches[0].getToPos());
    assertEquals(Arrays.asList("нервах", "нерви"), matches[0].getSuggestedReplacements());
    
    assertEquals(1, rule.match(langTool.getAnalyzedSentence("в п'�?тьом люд�?м")).length);
    assertEquals(1, rule.match(langTool.getAnalyzedSentence("в понад п'�?тьом люд�?м")).length);

    AnalyzedSentence analyzedSentence = langTool.getAnalyzedSentence("завд�?ки їх вдалим трюкам");
    RuleMatch[] match = rule.match(analyzedSentence);
    assertEquals(1, match.length);
    List<String> suggestedReplacements = match[0].getSuggestedReplacements();
    assertTrue("Did not find «їхній»: " + suggestedReplacements, suggestedReplacements.contains("їхнім"));

    analyzedSentence = langTool.getAnalyzedSentence("О дівчина!");
    match = rule.match(analyzedSentence);
    assertEquals(1, match.length);
    suggestedReplacements = match[0].getSuggestedReplacements();
    assertTrue("Did not find кличний «дівчино»: " + suggestedReplacements, suggestedReplacements.contains("дівчино"));

    matches = rule.match(langTool.getAnalyzedSentence("по церковним канонам"));
    // check match positions:
    assertEquals(1, matches.length);

    // �?в�?та
    assertEmptyMatch("на Купала");
    assertEmptyMatch("на Явдохи");
    // вулиці
    assertEmptyMatch("на Мазепи");
    assertEmptyMatch("на Кульчицької");
    assertEmptyMatch("на Правди");
    assertEmptyMatch("на Ломоно�?ова");
    // invert
    assertEmptyMatch("�?к на Кучми іменини");

    assertEmptyMatch("�?пирало�?�? на мі�?�?чної давнини рішенн�?");
    assertEmptyMatch("�?а �?ередньої довжини шубу");

    matches = rule.match(langTool.getAnalyzedSentence("�?пирало�?�? на мі�?�?чної давнини рішенн�?м"));
    assertEquals(1, matches.length);

    matches = rule.match(langTool.getAnalyzedSentence("Від �?т�?гу �?татюрка до пірат�?ького прапору"));
    assertEquals(1, matches.length);

    matches = rule.match(langTool.getAnalyzedSentence("згідно з документа"));
    assertEquals(1, matches.length);

//    matches = rule.match(langTool.getAnalyzedSentence("колега з Мін�?ьку"));
//    System.out.println(langTool.getAnalyzedSentence("колега з Мін�?ьку"));
//    // check match positions:
//    assertEquals(1, matches.length);

  }

  private void assertEmptyMatch(String text) throws IOException {
    assertEquals(Collections.<RuleMatch>emptyList(), Arrays.asList(rule.match(langTool.getAnalyzedSentence(text))));
  }
  
  @Test
  public void testSpecialChars() throws IOException {
    TokenAgreementRule rule = new TokenAgreementRule(TestTools.getMessages("uk"));

    JLanguageTool langTool = new JLanguageTool(new Ukrainian());

    RuleMatch[] matches = rule.match(langTool.getAnalyzedSentence("по не�?рвам, по мо\u00AD�?там, по воротам"));
    // check match positions:
    assertEquals(3, matches.length);

    assertEmptyMatch("до їм поді\u00ADбних");

    assertEquals(3, matches[0].getFromPos());
    assertEquals(10, matches[0].getToPos());
    assertEquals(Arrays.asList("нервах", "нерви"), matches[0].getSuggestedReplacements());
//    assertEquals(3, matches[1].getFromPos());

    assertEquals(15, matches[1].getFromPos());
    assertEquals(Arrays.asList("мо�?тах", "мо�?ти"), matches[1].getSuggestedReplacements());
//    assertEquals(1, matches[1].getFromPos());

    assertEquals(27, matches[2].getFromPos());
    assertEquals(Arrays.asList("воротах", "ворота"), matches[2].getSuggestedReplacements());
  }

}
