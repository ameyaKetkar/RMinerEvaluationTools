package org.ansj.app.crf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.ansj.app.crf.pojo.Element;
import org.ansj.app.crf.pojo.Feature;
import org.ansj.app.crf.pojo.Template;
import org.nlpcn.commons.lang.tire.domain.SmartForest;

public abstract class Model {

//	public enum MODEL_TYPE {
//		CRF, EMM
//	}

	protected Template template = null;

	protected double[][] status = null;

	protected Map<String, Feature> myGrad;

	protected SmartForest<double[][]> smartForest = null;

	public int allFeatureCount = 0;

	private List<Element> leftList = null;

	private List<Element> rightList = null;

	public int end1;

	public int end2;

	/**
	 * 根�?�模�?�文件解�?特�?
	 * 
	 * @param left l
	 * @param right r
	 * @throws IOException
	 */
	private void makeSide(int left, int right) throws IOException {
		// TODO Auto-generated method stub

		leftList = new ArrayList<>(Math.abs(left));
		for (int i = left; i < 0; i++) {
			leftList.add(new Element((char) ('B' + i)));
		}

		rightList = new ArrayList<>(right);
		for (int i = 1; i < right + 1; i++) {
			rightList.add(new Element((char) ('B' + i)));
		}
	}

	/**
	 * 将模型写入
	 * 
	 * @param path path
	 * @throws IOException
	 */
	public void writeModel(String path) throws IOException {
		// TODO Auto-generated method stub

		System.out.println("compute ok now to save model!");
		// 写模型
		ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(path))));

		// �?置模�?�
		oos.writeObject(template);
		// 特�?转移率
		oos.writeObject(status);
		// 总共的特�?数
		oos.writeInt(myGrad.size());
		double[] ds;
		for (Entry<String, Feature> entry : myGrad.entrySet()) {
			oos.writeUTF(entry.getKey());
			for (int i = 0; i < template.ft.length; i++) {
				ds = entry.getValue().w[i];
				for (int j = 0; j < ds.length; j++) {
					oos.writeByte(j);
					oos.writeFloat((float) ds[j]);
				}
				oos.writeByte(-1);
			}
		}

		oos.flush();
		oos.close();
	}

	/**
	 * 模型读�?�
	 * 
	 * @param modelPath modelPath
	 * @return model
	 * @throws Exception
	 */
	public static Model loadModel(String modelPath) throws Exception {
		return loadModel(new FileInputStream(modelPath));

	}

	public static Model loadModel(InputStream modelStream) throws Exception {
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(modelStream)));

			Model model = new Model() {

				@Override
				public void writeModel(String path) throws IOException {
					throw new RuntimeException("you can not to calculate ,this model only use by cut ");
				}

			};

			model.template = (Template) ois.readObject();

			model.makeSide(model.template.left, model.template.right);

			int tagNum = model.template.tagNum;

			int featureNum = model.template.ft.length;

			model.smartForest = new SmartForest<>(0.8);

			model.status = (double[][]) ois.readObject();

			// 总共的特�?数
			double[][] w;
			String key;
			int b = 0;
			int featureCount = ois.readInt();
			for (int i = 0; i < featureCount; i++) {
				key = ois.readUTF();
				w = new double[featureNum][0];
				for (int j = 0; j < featureNum; j++) {
					while ((b = ois.readByte()) != -1) {
						if (w[j].length == 0) {
							w[j] = new double[tagNum];
						}
						w[j][b] = ois.readFloat();
					}
				}
				model.smartForest.addBranch(key, w);
			}

			return model;
		} finally {
			if (ois != null) {
				ois.close();
			}
		}
	}

	public double[] getFeature(int featureIndex, char... chars) {
		// TODO Auto-generated method stub
		SmartForest<double[][]> sf = smartForest;
		sf = sf.getBranch(chars);
		if (sf == null || sf.getParam() == null) {
			return null;
		}
		return sf.getParam()[featureIndex];
	}

	/**
	 * @param s1 s1
	 * @param s2 s2
	 * @return tag转移率
	 */
	public double tagRate(int s1, int s2) {
		return status[s1][s2];
	}
}