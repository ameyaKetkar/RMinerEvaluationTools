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

import junit.framework.TestCase;
import org.languagetool.TestTools;
import org.languagetool.language.Ukrainian;
import org.languagetool.tokenizers.SRXSentenceTokenizer;

public class UkrainianSRXSentenceTokenizerTest extends TestCase {

  private final SRXSentenceTokenizer stokenizer = new SRXSentenceTokenizer(new Ukrainian());

  public final void testTokenize() {
    testSplit("Це про�?те реченн�?.");
    testSplit("Вони приїхали в Париж. ", "�?ле там їм геть не �?подобало�?�?.");
    testSplit("Панк-рок — напр�?м у рок-музиці, що виник у �?ередині 1970-х рр. у СШ�? і Великобританії.");
    testSplit("Разом із втечами, вже у XV �?т. поча�?тішали збройні ви�?тупи �?ел�?н.");
    testSplit("�?а початок 1994 р. державний борг України �?тановив 4,8 млрд. дол.");
    testSplit("Київ, вул. Сагайдачного, буд. 43, кв. 4.");
    testSplit("�?аша зу�?тріч з �?. Марчуком і Г. В. Трі�?кою відбула�?�? в грудні минулого року.");
    testSplit("�?аша зу�?тріч з �?.Марчуком і М.В.Хвилею відбула�?�? в грудні минулого року.");
    testSplit("Комендант преподобний С.\u00A0Мокітімі");
    testSplit("Комендант преподобний С.\u00A0С.\u00A0Мокітімі 1.");
    testSplit("Комендант преподобний С.\u00A0С. Мокітімі 2.");
    testSplit("Склад: акад. Вернад�?ький, проф. Харченко, доц. Семен�?к.");
    testSplit("Опергрупа приїхала в �?. Лі�?ове.");
    testSplit("300 р. до н. е.");
    testSplit("Пролі�?ок (ро�?. проле�?ок) — маленька квітка.");
    testSplit("Квітка Ці�?ик (англ. Kvitka Cisyk також Kacey Cisyk від ініціалів К.С.); 4 квітн�? 1953р., Квінз, �?ью-Йорк — 29 березн�? 1998 р., Мангеттен, �?ью-Йорк) — американ�?ька �?півачка україн�?ького походженн�?.");
    testSplit("До Ін�?титуту ім. Глієра під'їжджає чорне авто."); 
    testSplit("До табору «�?ртек».");
  }

  private void testSplit(final String... sentences) {
    TestTools.testSplit(sentences, stokenizer);
  }

}
