package org.ansj.app.crf;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.ansj.app.crf.pojo.Element;
import org.ansj.app.crf.pojo.Template;
import org.ansj.util.MatrixUtil;
import org.ansj.util.WordAlert;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * 分�?
 * 
 * @author ansj
 * 
 */
public class SplitWord {

	private final Model model;

	private final int[] tagConver;

	private final int[] revTagConver;

    private final int modelEnd1;

    private final int modelEnd2;

	/**
	 * 这个对象比较�?. 支�?多线程, 请尽�?�?�?使用
	 * 
	 * @param model
	 */
	public SplitWord(final Model model) {
		this.tagConver = new int[model.template.tagNum];
		this.revTagConver = new int[model.template.tagNum];

		// case 0:'S';case 1:'B';case 2:'M';3:'E';
        model.template.statusMap.forEach((statKey, statVal) -> {
            switch (statKey) {
                case "S":
                    this.tagConver[statVal] = 0;
                    this.revTagConver[0] = statVal;
                    break;
                case "B":
                    this.tagConver[statVal] = 1;
                    this.revTagConver[1] = statVal;
                    break;
                case "M":
                    this.tagConver[statVal] = 2;
                    this.revTagConver[2] = statVal;
                    break;
                case "E":
                    this.tagConver[statVal] = 3;
                    this.revTagConver[3] = statVal;
                    break;
                default:
                    break;
            }
        });

        this.model = model;
		this.modelEnd1 = model.template.statusMap.get("S");
		this.modelEnd2 = model.template.statusMap.get("E");
	}

	public List<String> cut(final char[] chars) {
        return cut(new String(chars));
	}

	public List<String> cut(final String line) {
		if (isBlank(line)) {
			return Collections.emptyList();
		}

		final List<Element> elements = vterbi(line);
		final List<String> result = new LinkedList<>();
		int begin = 0;
		int end = 0;
		for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
			switch (fixTag(element.getTag())) {
			case 0:
				end += element.len;
				result.add(line.substring(begin, end));
				begin = end;
				break;
			case 1:
				end += element.len;
				while (fixTag((element = elements.get(++i)).getTag()) != 3) {
					end += element.len;
				}
				end += element.len;
				result.add(line.substring(begin, end));
				begin = end;
			default:
				break;
			}
		}
		return result;
	}

	private List<Element> vterbi(String line) {
		List<Element> elements = WordAlert.str2Elements(line);

		int length = elements.size();
		if (length == 0) { // �?��?空list，下�?�get(0)�?作越界
			return elements;
		}
		if (length == 1) {
			elements.get(0).updateTag(revTagConver[0]);
			return elements;
		}

		/**
		 * 填充图
		 */
		for (int i = 0; i < length; i++) {
			computeTagScore(elements, i);
		}

		// 如果是开始�?�?�能从 m，e开始 ，所以将它设为一个很�?的值
		elements.get(0).tagScore[revTagConver[2]] = -1000;
		elements.get(0).tagScore[revTagConver[3]] = -1000;
		for (int i = 1; i < length; i++) {
			elements.get(i).maxFrom(this.model, elements.get(i - 1));
		}

		// 末�?置�?�能从S,E开始
		Element next = elements.get(elements.size() - 1);
		Element self = null;
		int maxStatus = next.tagScore[this.modelEnd1] > next.tagScore[this.modelEnd2] ?
				this.modelEnd1 :
				this.modelEnd2;
		next.updateTag(maxStatus);
		maxStatus = next.from[maxStatus];
		// 逆�?寻找
		for (int i = elements.size() - 2; i > 0; i--) {
			self = elements.get(i);
			self.updateTag(maxStatus);
			maxStatus = self.from[self.getTag()];
			next = self;
		}
		elements.get(0).updateTag(maxStatus);
		return elements;

	}

	private void computeTagScore(final List<Element> elements, final int index) {
        final Template tmpl = this.model.template;

		final double[] tagScore = new double[tmpl.tagNum];
		for (int i = 0; i < tmpl.ft.length; i++) {
			final char[] chars = new char[tmpl.ft[i].length];
			for (int j = 0; j < chars.length; j++) {
				chars[j] = getElement(elements, index + tmpl.ft[i][j]).name;
			}
			MatrixUtil.dot(tagScore, this.model.getFeature(i, chars));
		}
		elements.get(index).tagScore = tagScore;
	}

	private Element getElement(final List<Element> elements, final int i) {
		if (i < 0) {
			return new Element((char) ('B' + i));
		} else if (i >= elements.size()) {
			return new Element((char) ('B' + i - elements.size() + 1));
		} else {
			return elements.get(i);
		}
	}

	public int fixTag(final int tag) {
        return this.tagConver[tag];
	}

	/**
	 * �?便给一个�?. 计算这个�?的内�?�分值, �?�以�?�解为计算这个�?的�?�信度
	 * 
	 * @param word word
	 */
	public double cohesion(final String word) {
		if (word.length() == 0) {
			return Integer.MIN_VALUE;
		}

		final List<Element> elements = WordAlert.str2Elements(word);
		for (int i = 0; i < elements.size(); i++) {
			computeTagScore(elements, i);
		}

        final int len = elements.size() - 1;

		double value = elements.get(0).tagScore[revTagConver[1]];
		for (int i = 1; i < len; i++) {
			value += elements.get(i).tagScore[revTagConver[2]];
		}
		value += elements.get(len).tagScore[revTagConver[3]];

        return value < 0 ? 1 : value + 1;
	}
}
