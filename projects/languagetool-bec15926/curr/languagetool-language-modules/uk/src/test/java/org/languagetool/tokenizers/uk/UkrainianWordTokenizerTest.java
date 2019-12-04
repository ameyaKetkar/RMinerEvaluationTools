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

package org.languagetool.tokenizers.uk;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class UkrainianWordTokenizerTest extends TestCase {
  private final UkrainianWordTokenizer w = new UkrainianWordTokenizer();

  public void testTokenizeUrl() {
    String url = "http://youtube.com:80/herewego?start=11&quality=high%3F";
    List<String> testList = w.tokenize(url);
    assertEquals(Arrays.asList(url), testList);
  }
  
  public void testNumbers() {
    List<String> testList = w.tokenize("300 грн на балан�?і");
    assertEquals(Arrays.asList("300", " ", "грн", " ", "на", " ", "балан�?і"), testList);

    testList = w.tokenize("надійшло 2,2 мільйона");
    assertEquals(Arrays.asList("надійшло", " ", "2,2", " ", "мільйона"), testList);

    testList = w.tokenize("надійшло 84,46 мільйона");
    assertEquals(Arrays.asList("надійшло", " ", "84,46", " ", "мільйона"), testList);

    //TODO:
//    testList = w.tokenize("в 1996,1997,1998");
//    assertEquals(Arrays.asList("в", " ", "1996,1997,1998"), testList);

//    testList = w.tokenize("надійшло 2 000 тон");
//    assertEquals(Arrays.asList("надійшло", " ", "2 000", " ", "тон"), testList);

    testList = w.tokenize("�?тало�?�? 14.07.2001 вночі");
    assertEquals(Arrays.asList("�?тало�?�?", " ", "14.07.2001", " ", "вночі"), testList);

    testList = w.tokenize("вчора о 7.30 ранку");
    assertEquals(Arrays.asList("вчора", " ", "о", " ", "7.30", " ", "ранку"), testList);
  }
  
  public void testTokenize() {
    List<String> testList = w.tokenize("Вони прийшли додому.");
    assertEquals(Arrays.asList("Вони", " ", "прийшли", " ", "додому", "."), testList);

    testList = w.tokenize("Вони прийшли пʼ�?тими зів’�?лими.");
    assertEquals(Arrays.asList("Вони", " ", "прийшли", " ", "п'�?тими", " ", "зів'�?лими", "."), testList);

//    testList = w.tokenize("Вони\u0301 при\u00ADйшли пʼ�?\u0301тими зів’�?\u00ADлими.");
//    assertEquals(Arrays.asList("Вони", " ", "прийшли", " ", "п'�?тими", " ", "зів'�?лими", "."), testList);

    testList = w.tokenize("�? українець(�?мієть�?�?");
    assertEquals(Arrays.asList("�?", " ", "українець", "(", "�?мієть�?�?"), testList);
        
    testList = w.tokenize("ОУ�?(б) та КП(б)У");
    assertEquals(Arrays.asList("ОУ�?(б)", " ", "та", " ", "КП(б)У"), testList);

    testList = w.tokenize("�?егода є... за�?тупником");
    assertEquals(Arrays.asList("�?егода", " ", "є", "...", " ", "за�?тупником"), testList);

    testList = w.tokenize("Запагубили!.. також");
    assertEquals(Arrays.asList("Запагубили", "!..", " ", "також"), testList);

    testList = w.tokenize("Цей графин.");
    assertEquals(Arrays.asList("Цей", " ", "графин", "."), testList);

    testList = w.tokenize("— Гм.");
    assertEquals(Arrays.asList("—", " ", "Гм", "."), testList);
  }
  
  public void testAbbreviations() {
    List<String> testList = w.tokenize("За�?ідав І.Єрмолюк.");
    assertEquals(Arrays.asList("За�?ідав", " ", "І", ".", "Єрмолюк", "."), testList);

    testList = w.tokenize("За�?ідав І.П.Єрмолюк.");
    assertEquals(Arrays.asList("За�?ідав", " ", "І", ".", "П", ".", "Єрмолюк", "."), testList);

    testList = w.tokenize("І.\u00A0Єрмолюк.");
    assertEquals(Arrays.asList("І", ".", "\u00A0", "Єрмолюк", "."), testList);

    // �?короченн�?
    
    testList = w.tokenize("140 ти�?. працівників");
    assertEquals(Arrays.asList("140", " ", "ти�?.", " ", "працівників"), testList);

    testList = w.tokenize("проф. �?ртюхов");
    assertEquals(Arrays.asList("проф.", " ", "�?ртюхов"), testList);

    testList = w.tokenize("проф.\u00A0�?ртюхов");
    assertEquals(Arrays.asList("проф.", "\u00A0", "�?ртюхов"), testList);

    testList = w.tokenize("до н. е.");
    assertEquals(Arrays.asList("до", " ", "н.", " ", "е."), testList);
 
    testList = w.tokenize("до н.е.");
    assertEquals(Arrays.asList("до", " ", "н.", "е."), testList);

    testList = w.tokenize("1998 р.н.");
    assertEquals(Arrays.asList("1998", " ", "р.", "н."), testList);

    testList = w.tokenize("18-19 �?т.�?т. були");
    assertEquals(Arrays.asList("18-19", " ", "�?т.", "�?т.", " ", "були"), testList);
    
    testList = w.tokenize("І �?т. 11");
    assertEquals(Arrays.asList("І", " ", "�?т.", " ", "11"), testList);

    testList = w.tokenize("У �?. Вижва");
    assertEquals(Arrays.asList("У", " ", "�?.", " ", "Вижва"), testList);

    testList = w.tokenize("Довжиною 30 �?. з гаком.");
    assertEquals(Arrays.asList("Довжиною", " ", "30", " ", "�?", ".", " ", "з", " ", "гаком", "."), testList);

    testList = w.tokenize("Довжиною 30 �?. Поїхали.");
    assertEquals(Arrays.asList("Довжиною", " ", "30", " ", "�?", ".", " ", "Поїхали", "."), testList);

    testList = w.tokenize("100 м. дороги.");
    assertEquals(Arrays.asList("100", " ", "м", ".", " ", "дороги", "."), testList);

    testList = w.tokenize("�?а ви�?оті 4000 м...");
    assertEquals(Arrays.asList("�?а", " ", "ви�?оті", " ", "4000", " ", "м", "..."), testList);

    testList = w.tokenize("№47 (м. Слов'�?н�?ьк)");
    assertEquals(Arrays.asList("№47", " ", "(", "м.", " ", "Слов'�?н�?ьк", ")"), testList);

    testList = w.tokenize("�?.-г.");
    assertEquals(Arrays.asList("�?.-г."), testList);

    testList = w.tokenize("100 грн. в банк");
    assertEquals(Arrays.asList("100", " ", "грн", ".", " ", "в", " ", "банк"), testList);
    
    testList = w.tokenize("таке та ін.");
    assertEquals(Arrays.asList("таке", " ", "та", " ", "ін."), testList);

    testList = w.tokenize("і т. ін.");
    assertEquals(Arrays.asList("і", " ", "т.", " ", "ін."), testList);

    testList = w.tokenize("Ін�?титут ім. акад. Вернад�?ького.");
    assertEquals(Arrays.asList("Ін�?титут", " ", "ім.", " ", "акад.", " ", "Вернад�?ького", "."), testList);

    testList = w.tokenize("Палац ім. гетьмана Скоропад�?ького.");
    assertEquals(Arrays.asList("Палац", " ", "ім.", " ", "гетьмана", " ", "Скоропад�?ького", "."), testList);

    testList = w.tokenize("від лат. momento");
    assertEquals(Arrays.asList("від", " ", "лат.", " ", "momento"), testList);

    testList = w.tokenize("на 1-кімн. кв. в центрі");
    assertEquals(Arrays.asList("на", " " , "1-кімн.", " ", "кв.", " ", "в", " ", "центрі"), testList);
  }

}
