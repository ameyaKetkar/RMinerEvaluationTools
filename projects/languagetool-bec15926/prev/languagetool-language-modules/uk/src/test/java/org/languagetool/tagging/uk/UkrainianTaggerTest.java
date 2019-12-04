/* LanguageTool, a natural language style checker 
 * Copyright (C) 2006 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.tagging.uk;

import java.io.IOException;

import junit.framework.TestCase;

import org.languagetool.TestTools;
import org.languagetool.language.Ukrainian;
import org.languagetool.tokenizers.uk.UkrainianWordTokenizer;

public class UkrainianTaggerTest extends TestCase {
    
  private UkrainianTagger tagger;
  private UkrainianWordTokenizer tokenizer;
      
  @Override
  public void setUp() {
    tagger = new UkrainianTagger();
    tokenizer = new UkrainianWordTokenizer();
  }

  public void testDictionary() throws IOException {
    TestTools.testDictionary(tagger, new Ukrainian());
  }
  
  public void testTagger() throws IOException {

    // one-way case sensitivity
    TestTools.myAssert("києві", "києві/[кий]noun:m:v_dav", tokenizer, tagger);
    TestTools.myAssert("Києві", "Києві/[Київ]noun:m:v_mis|Києві/[кий]noun:m:v_dav", tokenizer, tagger);
    TestTools.myAssert("віл", "віл/[віл]noun:m:v_naz:anim", tokenizer, tagger);
    TestTools.myAssert("Віл", "Віл/[віл]noun:m:v_naz:anim", tokenizer, tagger);
    TestTools.myAssert("ВІЛ", "ВІЛ/[ВІЛ]noun:m:v_dav:nv:np:abbr|ВІЛ/[ВІЛ]noun:m:v_mis:nv:np:abbr|ВІЛ/[ВІЛ]noun:m:v_naz:nv:np:abbr|ВІЛ/[ВІЛ]noun:m:v_oru:nv:np:abbr|ВІЛ/[ВІЛ]noun:m:v_rod:nv:np:abbr|ВІЛ/[ВІЛ]noun:m:v_zna:nv:np:abbr|ВІЛ/[віл]noun:m:v_naz:anim", tokenizer, tagger);
    TestTools.myAssert("далі", "далі/[далі]adv", tokenizer, tagger);
    TestTools.myAssert("Далі", "Далі/[Даль]noun:m:v_mis:anim:lname|Далі/[Далі]noun:m:v_dav:nv:np:anim:lname|Далі/[Далі]noun:m:v_mis:nv:np:anim:lname|Далі/[Далі]noun:m:v_naz:nv:np:anim:lname|Далі/[Далі]noun:m:v_oru:nv:np:anim:lname|Далі/[Далі]noun:m:v_rod:nv:np:anim:lname|Далі/[Далі]noun:m:v_zna:nv:np:anim:lname|Далі/[далі]adv", tokenizer, tagger);
    TestTools.myAssert("Бен", "Бен/[Бен]noun:m:v_naz:anim:fname|Бен/[бен]unknown", tokenizer, tagger);
    TestTools.myAssert("бен", "бен/[бен]unknown", tokenizer, tagger);


    TestTools.myAssert("Справу порушено �?удом", 
      "Справу/[�?права]noun:f:v_zna -- порушено/[порушити]verb:impers:perf -- �?удом/[�?уд]noun:m:v_oru|�?удом/[�?удома]noun:p:v_rod",
       tokenizer, tagger);
       
    String expected = 
      "Майже/[майже]adv -- два/[два]numr:m:v_naz|два/[два]numr:m:v_zna|два/[два]numr:n:v_naz|два/[два]numr:n:v_zna -- роки/[рік]noun:p:v_naz|роки/[рік]noun:p:v_zna"
    + " -- тому/[той]adj:m:v_dav:&pron:dem|тому/[той]adj:m:v_mis:&pron:dem|тому/[той]adj:n:v_dav:&pron:dem|тому/[той]adj:n:v_mis:&pron:dem|тому/[том]noun:m:v_dav|тому/[том]noun:m:v_mis|тому/[том]noun:m:v_rod|тому/[тому]adv|тому/[тому]conj:subord"
    + " -- Люба/[Люба]noun:f:v_naz:anim:fname|Люба/[любий]adj:f:v_naz -- разом/[раз]noun:m:v_oru|разом/[разом]adv -- із/[із]prep:rv_rod:rv_zna:rv_oru"
    + " -- чоловіком/[чоловік]noun:m:v_oru:anim -- Степаном/[Степан]noun:m:v_oru:anim:fname -- виїхали/[виїхати]verb:past:m:perf -- туди/[туди]adv:&pron:dem"
    + " -- на/[на]excl|на/[на]part|на/[на]prep:rv_zna:rv_mis -- "
    + "проживанн�?/[проживанн�?]noun:n:v_naz|проживанн�?/[проживанн�?]noun:n:v_rod|проживанн�?/[проживанн�?]noun:n:v_zna|проживанн�?/[проживанн�?]noun:p:v_naz|проживанн�?/[проживанн�?]noun:p:v_zna";
  
    TestTools.myAssert("Майже два роки тому Люба разом із чоловіком Степаном виїхали туди на проживанн�?.",
        expected, tokenizer, tagger);
  }

  public void testNumberTagging() throws IOException {
    TestTools.myAssert("101,234", "101,234/[101,234]number", tokenizer, tagger);
    TestTools.myAssert("3,5-5,6% 7° 7,4°С", "3,5-5,6%/[3,5-5,6%]number -- 7°/[7°]number -- 7,4°С/[7,4°С]number", tokenizer, tagger);
    TestTools.myAssert("XIX", "XIX/[XIX]number", tokenizer, tagger);

    TestTools.myAssert("14.07.2001", "14.07.2001/[14.07.2001]date", tokenizer, tagger);

    TestTools.myAssert("о 15.33", "о/[о]excl|о/[о]prep:rv_zna:rv_mis -- 15.33/[15.33]time", tokenizer, tagger);
    TestTools.myAssert("О 1:05", "О/[о]excl|О/[о]prep:rv_zna:rv_mis -- 1:05/[1:05]time", tokenizer, tagger);
  }
  
  public void testTaggingWithDots() throws IOException {
    TestTools.myAssert("300 р. до н. е.", 
      "300/[300]number -- р./[null]null -- до/[до]noun:n:v_dav:nv|до/[до]noun:n:v_mis:nv|до/[до]noun:n:v_naz:nv|до/[до]noun:n:v_oru:nv|до/[до]noun:n:v_rod:nv|до/[до]noun:n:v_zna:nv|до/[до]noun:p:v_dav:nv|до/[до]noun:p:v_mis:nv|до/[до]noun:p:v_naz:nv|до/[до]noun:p:v_oru:nv|до/[до]noun:p:v_rod:nv|до/[до]noun:p:v_zna:nv|до/[до]prep:rv_rod -- н./[null]null -- е/[е]excl",
       tokenizer, tagger);
  
//    TestTools.myAssert("Є.Бакуліна.",
//      "Є.Бакуліна[Бакулін]noun:m:v_rod:anim:lname|Є.Бакуліна[Бакулін]noun:m:v_zna:anim:lname",
//       tokenizer, tagger);
  }
  
  public void testDynamicTagging() throws IOException {
    TestTools.myAssert("г-г-г", "г-г-г/[null]null", tokenizer, tagger);
    
    TestTools.myAssert("100-річному", "100-річному/[100-річний]adj:m:v_dav|100-річному/[100-річний]adj:m:v_mis|100-річному/[100-річний]adj:n:v_dav|100-річному/[100-річний]adj:n:v_mis", tokenizer, tagger);
    TestTools.myAssert("100-й", "100-й/[100-й]adj:m:v_naz|100-й/[100-й]adj:m:v_zna", tokenizer, tagger);
    TestTools.myAssert("50-х", "50-х/[50-й]adj:p:v_rod|50-х/[50-й]adj:p:v_zna", tokenizer, tagger);

    TestTools.myAssert("по-�?вин�?чому", "по-�?вин�?чому/[по-�?вин�?чому]adv", tokenizer, tagger);
    TestTools.myAssert("по-�?ибір�?ьки", "по-�?ибір�?ьки/[по-�?ибір�?ьки]adv", tokenizer, tagger);

    TestTools.myAssert("давай-но", "давай-но/[давати]verb:impr:s:2:imperf", tokenizer, tagger);
    TestTools.myAssert("дивіть�?�?-но", "дивіть�?�?-но/[дивити�?�?]verb:rev:impr:p:2:imperf", tokenizer, tagger);
    TestTools.myAssert("той-таки", "той-таки/[той-таки]adj:m:v_naz:&pron:dem|той-таки/[той-таки]adj:m:v_zna:&pron:dem", tokenizer, tagger);
    TestTools.myAssert("буде-таки", "буде-таки/[бути]verb:futr:s:3:imperf", tokenizer, tagger);
    TestTools.myAssert("оцей-от", "оцей-от/[оцей]adj:m:v_naz:&pron:dem|оцей-от/[оцей]adj:m:v_zna:&pron:dem", tokenizer, tagger);
    TestTools.myAssert("оттакий-то", "оттакий-то/[оттакий]adj:m:v_naz:&pron:dem:rare|оттакий-то/[оттакий]adj:m:v_zna:&pron:dem:rare", tokenizer, tagger);
    TestTools.myAssert("геть-то", "геть-то/[геть]adv|геть-то/[геть]part", tokenizer, tagger);
    TestTools.myAssert("ану-бо", "ану-бо/[ану]excl|ану-бо/[ану]part", tokenizer, tagger);
    TestTools.myAssert("годі-бо", "годі-бо/[годі]predic", tokenizer, tagger);
    TestTools.myAssert("гей-но", "гей-но/[гей]excl", tokenizer, tagger);
    TestTools.myAssert("цить-но", "цить-но/[цить]excl", tokenizer, tagger);

    TestTools.myAssert("ек�?-партнер", "ек�?-партнер/[ек�?-партнер]noun:m:v_naz:anim", tokenizer, tagger);

    // TODO: �?тарий -> �?тарший
    TestTools.myAssert("�?лієва-�?таршого", "�?лієва-�?таршого/[�?лієв-�?тарий]noun:m:v_rod:anim:lname|�?лієва-�?таршого/[�?лієв-�?тарий]noun:m:v_zna:anim:lname", tokenizer, tagger);

//    TestTools.myAssert("греко-уні�?т�?ький", "", tokenizer, tagger);
    
    TestTools.myAssert("жило-було", "жило-було/[жити-бути]verb:past:n:imperf", tokenizer, tagger);
    TestTools.myAssert("учиш-учиш", "учиш-учиш/[учити-учити]verb:pres:s:2:imperf:v-u", tokenizer, tagger);

    TestTools.myAssert("вгору-вниз", "вгору-вниз/[вгору-вниз]adv:v-u", tokenizer, tagger);

    TestTools.myAssert("низенько-низенько", "низенько-низенько/[низенько-низенько]adv", tokenizer, tagger);
    TestTools.myAssert("такого-�?�?кого", "такого-�?�?кого/[такий-�?�?кий]adj:m:v_rod:&pron:def|такого-�?�?кого/[такий-�?�?кий]adj:m:v_zna:&pron:def|такого-�?�?кого/[такий-�?�?кий]adj:n:v_rod:&pron:def", tokenizer, tagger);
    TestTools.myAssert("великий-превеликий", "великий-превеликий/[великий-превеликий]adj:m:v_naz|великий-превеликий/[великий-превеликий]adj:m:v_zna", tokenizer, tagger);
    TestTools.myAssert("чорній-чорній", "чорній-чорній/[чорний-чорний]adj:f:v_dav|чорній-чорній/[чорний-чорний]adj:f:v_mis|чорній-чорній/[чорніти-чорніти]verb:impr:s:2:imperf", tokenizer, tagger);

    TestTools.myAssert("лікар-гомеопат", "лікар-гомеопат/[лікар-гомеопат]noun:m:v_naz:anim", tokenizer, tagger);
    TestTools.myAssert("лікар�?-гомеопата", "лікар�?-гомеопата/[лікар-гомеопат]noun:m:v_rod:anim|лікар�?-гомеопата/[лікар-гомеопат]noun:m:v_zna:anim", tokenizer, tagger);
    TestTools.myAssert("шмкр-гомеопат", "шмкр-гомеопат/[null]null", tokenizer, tagger);
    TestTools.myAssert("шмкр-ткр", "шмкр-ткр/[null]null", tokenizer, tagger);

    TestTools.myAssert("вчинок-приклад", "вчинок-приклад/[вчинок-приклад]noun:m:v_naz:v-u|вчинок-приклад/[вчинок-приклад]noun:m:v_zna:v-u", tokenizer, tagger);
    TestTools.myAssert("мі�?та-фортеці", "мі�?та-фортеці/[мі�?то-фортец�?]noun:n:v_rod|мі�?та-фортеці/[мі�?то-фортец�?]noun:p:v_naz|мі�?та-фортеці/[мі�?то-фортец�?]noun:p:v_zna", tokenizer, tagger);

    // inanim-anim
    TestTools.myAssert("вчених-новаторів", "вчених-новаторів/[вчений-новатор]noun:p:v_rod:anim:v-u|вчених-новаторів/[вчений-новатор]noun:p:v_zna:anim:v-u", tokenizer, tagger);
    TestTools.myAssert("країна-виробник", "країна-виробник/[країна-виробник]noun:f:v_naz", tokenizer, tagger);
    TestTools.myAssert("банк-виробник", "банк-виробник/[банк-виробник]noun:m:v_naz|банк-виробник/[банк-виробник]noun:m:v_zna", tokenizer, tagger);
    TestTools.myAssert("банки-агенти", "банки-агенти/[банк-агент]noun:p:v_naz|банки-агенти/[банк-агент]noun:p:v_zna|банки-агенти/[банка-агент]noun:p:v_naz|банки-агенти/[банка-агент]noun:p:v_zna", tokenizer, tagger);
    TestTools.myAssert("мі�?то-гігант", "мі�?то-гігант/[мі�?то-гігант]noun:n:v_naz|мі�?то-гігант/[мі�?то-гігант]noun:n:v_zna", tokenizer, tagger);
    TestTools.myAssert("країни-агре�?ори", "країни-агре�?ори/[країна-агре�?ор]noun:p:v_naz|країни-агре�?ори/[країна-агре�?ор]noun:p:v_zna", tokenizer, tagger);
    TestTools.myAssert("по�?еленн�?-гігант", "по�?еленн�?-гігант/[по�?еленн�?-гігант]noun:n:v_naz|по�?еленн�?-гігант/[по�?еленн�?-гігант]noun:n:v_zna", tokenizer, tagger);
    
    TestTools.myAssert("�?он�?х-кра�?ень", "�?он�?х-кра�?ень/[�?он�?х-кра�?ень]noun:m:v_naz|�?он�?х-кра�?ень/[�?он�?х-кра�?ень]noun:m:v_zna", tokenizer, tagger);
    TestTools.myAssert("кра�?ень-�?он�?х", "кра�?ень-�?он�?х/[кра�?ень-�?он�?х]noun:m:v_naz|кра�?ень-�?он�?х/[кра�?ень-�?он�?х]noun:m:v_zna", tokenizer, tagger);
    TestTools.myAssert("депутатів-привидів", "депутатів-привидів/[депутат-привид]noun:p:v_rod:anim|депутатів-привидів/[депутат-привид]noun:p:v_zna:anim", tokenizer, tagger);
    TestTools.myAssert("дівчата-зірочки", "дівчата-зірочки/[дівча-зірочка]noun:p:v_naz:anim", tokenizer, tagger);

    TestTools.myAssert("абзац-два", "абзац-два/[абзац-два]noun:m:v_naz|абзац-два/[абзац-два]noun:m:v_zna", tokenizer, tagger);
    TestTools.myAssert("�?отні-дві", "�?отні-дві/[�?отн�?-два]noun:p:v_naz|�?отні-дві/[�?отн�?-два]noun:p:v_zna", tokenizer, tagger);
    TestTools.myAssert("ти�?�?чею-трьома", "ти�?�?чею-трьома/[ти�?�?ча-три]noun:f:v_oru|ти�?�?чею-трьома/[ти�?�?ча-троє]noun:f:v_oru", tokenizer, tagger);

    TestTools.myAssert("одним-двома", "одним-двома/[один-два]numr:m:v_oru|одним-двома/[один-два]numr:n:v_oru|одним-двома/[один-двоє]numr:m:v_oru|одним-двома/[один-двоє]numr:n:v_oru", tokenizer, tagger);
    //TODO: бере іменник п’�?та
//    TestTools.myAssert("п'�?ти-ше�?ти", "п'�?ти-ше�?ти/[п'�?ть-ші�?ть]numr:v_dav|п'�?ти-ше�?ти/[п'�?ть-ші�?ть]numr:v_mis|п'�?ти-ше�?ти/[п'�?ть-ші�?ть]numr:v_rod", tokenizer, tagger);
    TestTools.myAssert("п'�?ти-ше�?ти", "п'�?ти-ше�?ти/[п'�?та-ші�?ть]noun:f:v_rod|п'�?ти-ше�?ти/[п'�?ть-ші�?ть]numr:p:v_dav|п'�?ти-ше�?ти/[п'�?ть-ші�?ть]numr:p:v_mis|п'�?ти-ше�?ти/[п'�?ть-ші�?ть]numr:p:v_rod", tokenizer, tagger);
    TestTools.myAssert("півтори-дві", "півтори-дві/[півтори-два]numr:f:v_naz|півтори-дві/[півтори-два]numr:f:v_zna", tokenizer, tagger);
    TestTools.myAssert("три-чотири", "три-чотири/[три-чотири]numr:p:v_naz|три-чотири/[три-чотири]numr:p:v_zna", tokenizer, tagger);
    TestTools.myAssert("два-чотири", "два-чотири/[два-чотири]numr:m:v_naz|два-чотири/[два-чотири]numr:m:v_zna|два-чотири/[два-чотири]numr:n:v_naz|два-чотири/[два-чотири]numr:n:v_zna", tokenizer, tagger);
    TestTools.myAssert("одному-двох", "одному-двох/[один-два]numr:m:v_mis|одному-двох/[один-два]numr:n:v_mis|одному-двох/[один-двоє]numr:m:v_mis|одному-двох/[один-двоє]numr:n:v_mis", tokenizer, tagger);
    // u2013
    TestTools.myAssert("три–чотири", "три–чотири/[три–чотири]numr:p:v_naz|три–чотири/[три–чотири]numr:p:v_zna", tokenizer, tagger);
    
//    "однією-єдиною"
//    TestTools.myAssert("капуджі-ага", "два-чотири/[два-чотири]numr:v_naz|два-чотири/[два-чотири]numr:v_naz", tokenizer, tagger);
//    TestTools.myAssert("Каладжі-бей", "два-чотири/[два-чотири]numr:v_naz|два-чотири/[два-чотири]numr:v_naz", tokenizer, tagger);
//    TestTools.myAssert("капудан-паша", "два-чотири/[два-чотири]numr:v_naz|два-чотири/[два-чотири]numr:v_naz", tokenizer, tagger);
//    TestTools.myAssert("кальфа-ефенді", "два-чотири/[два-чотири]numr:v_naz|два-чотири/[два-чотири]numr:v_naz", tokenizer, tagger);

    TestTools.myAssert("а-а", "а-а/[а-а]excl", tokenizer, tagger);

    TestTools.myAssert("Мо�?кви-ріки", "Мо�?кви-ріки/[Мо�?ква-ріка]noun:f:v_rod", tokenizer, tagger);
    
    TestTools.myAssert("пів-України", "пів-України/[пів-України]noun:f:v_dav|пів-України/[пів-України]noun:f:v_mis|пів-України/[пів-України]noun:f:v_naz|пів-України/[пів-України]noun:f:v_oru|пів-України/[пів-України]noun:f:v_rod|пів-України/[пів-України]noun:f:v_zna", tokenizer, tagger);

    TestTools.myAssert("кава-е�?пре�?о", "кава-е�?пре�?о/[кава-е�?пре�?о]noun:f:v_naz", tokenizer, tagger);
    TestTools.myAssert("кави-е�?пре�?о", "кави-е�?пре�?о/[кава-е�?пре�?о]noun:f:v_rod", tokenizer, tagger);
    TestTools.myAssert("е�?пре�?о-машина", "е�?пре�?о-машина/[е�?пре�?о-машина]noun:f:v_naz", tokenizer, tagger);
    TestTools.myAssert("програмою-мак�?имум", "програмою-мак�?имум/[програма-мак�?имум]noun:f:v_oru", tokenizer, tagger);

    TestTools.myAssert("Пен�?ильвані�?-авеню", "Пен�?ильвані�?-авеню/[Пен�?ильвані�?-авеню]noun:f:nv", tokenizer, tagger);

    TestTools.myAssert("патолого-анатомічний", "патолого-анатомічний/[патолого-анатомічний]adj:m:v_naz|патолого-анатомічний/[патолого-анатомічний]adj:m:v_zna", tokenizer, tagger);
    TestTools.myAssert("паталого-анатомічний", "паталого-анатомічний/[null]null", tokenizer, tagger);
    //TODO: fix this case (now works like братів-право�?лавних)
//    TestTools.myAssert("патолога-анатомічний", "патолога-анатомічний/[null]null", tokenizer, tagger);
    TestTools.myAssert("патолого-гмкнх", "патолого-гмкнх/[null]null", tokenizer, tagger);
    TestTools.myAssert("патолого-голова", "патолого-голова/[null]null", tokenizer, tagger);
    //TODO: remove :compb ?
    TestTools.myAssert("о�?вітньо-культурний", "о�?вітньо-культурний/[о�?вітньо-культурний]adj:m:v_naz:compb|о�?вітньо-культурний/[о�?вітньо-культурний]adj:m:v_zna:compb", tokenizer, tagger);
    TestTools.myAssert("бірмюково-блакитний", "бірмюково-блакитний/[null]null", tokenizer, tagger);
    TestTools.myAssert("�?ліпуче-�?�?кравого", "�?ліпуче-�?�?кравого/[�?ліпуче-�?�?кравий]adj:m:v_rod:compb|�?ліпуче-�?�?кравого/[�?ліпуче-�?�?кравий]adj:m:v_zna:compb|�?ліпуче-�?�?кравого/[�?ліпуче-�?�?кравий]adj:n:v_rod:compb", tokenizer, tagger);
    TestTools.myAssert("дво-триметровий", "дво-триметровий/[дво-триметровий]adj:m:v_naz|дво-триметровий/[дво-триметровий]adj:m:v_zna", tokenizer, tagger);
    TestTools.myAssert("україно-болгар�?ький", "україно-болгар�?ький/[україно-болгар�?ький]adj:m:v_naz|україно-болгар�?ький/[україно-болгар�?ький]adj:m:v_zna", tokenizer, tagger);

//    TestTools.myAssert("американо-блакитний", "бірмюково-блакитний/[бірмюково-блакитний]adj:m:v_naz|бірмюково-блакитний/[бірмюково-блакитний]adj:m:v_zna", tokenizer, tagger);

    TestTools.myAssert("Дівчинка-першокла�?ниц�?", "Дівчинка-першокла�?ниц�?/[дівчинка-першокла�?ниц�?]noun:f:v_naz:anim", tokenizer, tagger);

    // і�?тота-неі�?тота
    //TODO:
    // про мі�?�?ц�?-мі�?�?ченька
    // бабці-�?в�?трії
    // змаганн�? зі �?лалому-гіганту
    // голо�?увати за Тимошенко-прем’єра
  }

}
