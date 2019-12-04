/**
 * Copyright (c) 2011-2015, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jfinal.ext.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import com.jfinal.core.Controller;
import com.jfinal.kit.StrKit;
import com.jfinal.render.Render;

public class CaptchaRender extends Render {
	
	private static final int WIDTH = 85, HEIGHT = 20;
	private static final String[] strArr = {"3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y"};
	
	private String randomCodeKey;
	
	public CaptchaRender(String randomCodeKey) {
		if (StrKit.isBlank(randomCodeKey))
			throw new IllegalArgumentException("randomCodeKey can not be blank");
		this.randomCodeKey = randomCodeKey;
	}
	
	public void render() {
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		String vCode = drawGraphic(image);
		vCode = encrypt(vCode);
		Cookie cookie = new Cookie(randomCodeKey, vCode);
		cookie.setMaxAge(-1);
		cookie.setPath("/");
		response.addCookie(cookie);
		response.setHeader("Pragma","no-cache");
        response.setHeader("Cache-Control","no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        
        ServletOutputStream sos = null;
        try {
			sos = response.getOutputStream();
			ImageIO.write(image, "jpeg",sos);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (sos != null)
				try {sos.close();} catch (IOException e) {e.printStackTrace();}
		}
	}

	private String drawGraphic(BufferedImage image){
		// 获�?�图形上下文
		Graphics g = image.createGraphics();
		// 生�?�?机类
		Random random = new Random();
		// 设定背景色
		g.setColor(getRandColor(200, 250));
		g.fillRect(0, 0, WIDTH, HEIGHT);
		// 设定字体
		g.setFont(new Font("Times New Roman", Font.PLAIN, 18));

		// �?机产生155�?�干扰线，使图象中的认�?�?�?易被其它程�?探测到
		g.setColor(getRandColor(160, 200));
		for (int i = 0; i < 155; i++) {
			int x = random.nextInt(WIDTH);
			int y = random.nextInt(HEIGHT);
			int xl = random.nextInt(12);
			int yl = random.nextInt(12);
			g.drawLine(x, y, x + xl, y + yl);
		}

		// �?��?机产生的认�?�?(4�?数字)
		String sRand = "";
		for (int i = 0; i < 4; i++) {
			String rand = String.valueOf(strArr[random.nextInt(strArr.length)]);
			sRand += rand;
			// 将认�?�?显示到图象中
			g.setColor(new Color(20 + random.nextInt(110), 20 + random.nextInt(110), 20 + random.nextInt(110)));
			// 调用函数出�?�的颜色相�?�，�?�能是因为�?�?太接近，所以�?�能直接生�?
			g.drawString(rand, 13 * i + 6, 16);
		}

		// 图象生效
		g.dispose();
		
		return sRand;
	}
	
	/*
	 * 给定范围获得�?机颜色
	 */
	private Color getRandColor(int fc, int bc) {
		Random random = new Random();
		if (fc > 255)
			fc = 255;
		if (bc > 255)
			bc = 255;
		int r = fc + random.nextInt(bc - fc);
		int g = fc + random.nextInt(bc - fc);
		int b = fc + random.nextInt(bc - fc);
		return new Color(r, g, b);
	}
	
	private static final String encrypt(String srcStr) {
		try {
			String result = "";
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] bytes = md.digest(srcStr.getBytes("utf-8"));
			for(byte b:bytes){
				String hex = Integer.toHexString(b&0xFF).toUpperCase();
				result += ((hex.length() ==1 ) ? "0" : "") + hex;
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
//	public static boolean validate(String inputRandomCode, String rightRandomCode){
//		if (StringKit.isBlank(inputRandomCode))
//			return false;
//		try {
//			inputRandomCode = encrypt(inputRandomCode);
//			return inputRandomCode.equals(rightRandomCode);
//		}catch(Exception e){
//			e.printStackTrace();
//			return false;
//		}
//	}
	
	// TODO 需�?改进
	public static boolean validate(Controller controller, String inputRandomCode, String randomCodeKey) {
		if (StrKit.isBlank(inputRandomCode))
			return false;
		try {
			inputRandomCode = encrypt(inputRandomCode);
			return inputRandomCode.equals(controller.getCookie(randomCodeKey));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}


