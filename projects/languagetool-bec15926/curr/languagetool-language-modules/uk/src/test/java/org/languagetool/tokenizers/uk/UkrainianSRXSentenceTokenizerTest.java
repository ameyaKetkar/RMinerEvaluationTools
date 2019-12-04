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
    testSplit("З 300 р. до н.е., і по цей день.");
    testSplit("Пролі�?ок (ро�?. проле�?ок) — маленька квітка.");
    testSplit("Квітка Ці�?ик (англ. Kvitka Cisyk також Kacey Cisyk від ініціалів К.С.); 4 квітн�? 1953р., Квінз, �?ью-Йорк — 29 березн�? 1998 р., Мангеттен, �?ью-Йорк) — американ�?ька �?півачка україн�?ького походженн�?.");
    testSplit("До Ін�?титуту ім. Глієра під'їжджає чорне авто."); 
    testSplit("До Ін�?титуту ім. акад. Вернад�?ького."); 
    testSplit("До вулиці гетьмана Скоропад�?ького під'їжджає чорне авто."); 
    testSplit("До табору «�?ртек».");
    testSplit("Спільні пральні й т. д.");
    testSplit("Спільні пральні й т. д. й т. п.");
    testSplit("див. �?тор. 24.");
    testSplit("Є.Бакуліна");
    testSplit("Від англ.\n  File.");
    testSplit("Від фр.  \nparachute.");
    testSplit("В цих �?вітлих про�?торих апартаментах...  м’�?кі крі�?ла, килими, дорогі �?татуетки");
    testSplit("(вони �?амі це визнали. - Ред.)");
    testSplit("В�?ього 33 ти�?. 356 о�?оби");
    testSplit("В�?ього 33 ти�?. (за �?ловами прораба)");
    testSplit("з �?ких приблизно   1,2 ти�?. – чоловіки.");
    testSplit("У �?. Вижва");
    testSplit("Книжка (�?. 200)");
    testSplit("позначені: «�?. Вижва»");
    testSplit("Микола Ва�?юк (�?. Корнієнки, Полтав�?ька обл.)");
    testSplit("U.S. Marine");
    testSplit("B.B. King");
    testSplit("Церква Св. Духа і церква �?в. Духа");
  }
  
  public void testTokenizeWithSplit() {
    testSplit("В�?ього 33 ти�?.", "�? можей й більше");
    testSplit("Їх було 7,5 млн.", "В кожного була �?орочка.");
    testSplit("Довжиною 30 �?. ", "Поїхали.");
    testSplit("Швидкі�?тю 30 м/�?. ", "Поїхали.");
    testSplit("О�?танні 100 м. ", "І тут в�?е пропало.");
    testSplit("Кори�?на площа 67 ти�?. кв.  м. ", "У 1954 році над Держпромом...");
    testSplit("�?а 0,6°C. ", "�?ле ми в�?е маємо."); //лат С 
    testSplit("�?а 0,6°С. ", "�?ле ми в�?е маємо."); //укр С
    testSplit("�?а 0,6 °C. ", "�?ле ми в�?е маємо."); //лат С 
    testSplit("�?а 0,6 °С. ", "�?ле ми в�?е маємо."); //укр С
    testSplit("Приїхав у СШ�?. ", "Проте на другий рік.");
  }

  private void testSplit(final String... sentences) {
    TestTools.testSplit(sentences, stokenizer);
  }

}
