/*
* Copyright (c) 2025, The beep-projects contributors
* this file originated from https://github.com/beep-projects
* Do not remove the lines above.
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see https://www.gnu.org/licenses/
*
*/
package de.freaklamarsch.systarest;

import java.awt.Color;
import java.util.*;

public class ColorNamer {

	// Map of basic colors and their names
	// scraped from https://notioncolors.com/color-names/
	private static final Map<String, Color> knownColors = new HashMap<>();
	static {
		knownColors.put("aliceblue", new Color(240, 248, 255));
		knownColors.put("antiquewhite", new Color(250, 235, 215));
		knownColors.put("aqua", new Color(0, 255, 255));
		knownColors.put("aquamarine", new Color(127, 255, 212));
		knownColors.put("azure", new Color(240, 255, 255));
		knownColors.put("beige", new Color(245, 245, 220));
		knownColors.put("bisque", new Color(255, 228, 196));
		knownColors.put("black", new Color(0, 0, 0));
		knownColors.put("blanchedalmond", new Color(255, 235, 205));
		knownColors.put("blue", new Color(0, 0, 255));
		knownColors.put("blueviolet", new Color(138, 43, 226));
		knownColors.put("brown", new Color(165, 42, 42));
		knownColors.put("burlywood", new Color(222, 184, 135));
		knownColors.put("cadetblue", new Color(95, 158, 160));
		knownColors.put("chartreuse", new Color(127, 255, 0));
		knownColors.put("chocolate", new Color(210, 105, 30));
		knownColors.put("coral", new Color(255, 127, 80));
		knownColors.put("cornflowerblue", new Color(100, 149, 237));
		knownColors.put("cornsilk", new Color(255, 248, 220));
		knownColors.put("crimson", new Color(220, 20, 60));
		knownColors.put("cyan", new Color(0, 255, 255));
		knownColors.put("darkblue", new Color(0, 0, 139));
		knownColors.put("darkcyan", new Color(0, 139, 139));
		knownColors.put("darkgoldenrod", new Color(184, 134, 11));
		knownColors.put("darkgray", new Color(169, 169, 169));
		knownColors.put("darkgreen", new Color(0, 100, 0));
		knownColors.put("darkkhaki", new Color(189, 183, 107));
		knownColors.put("darkmagenta", new Color(139, 0, 139));
		knownColors.put("darkolivegreen", new Color(85, 107, 47));
		knownColors.put("darkorange", new Color(255, 140, 0));
		knownColors.put("darkorchid", new Color(153, 50, 204));
		knownColors.put("darkred", new Color(139, 0, 0));
		knownColors.put("darksalmon", new Color(233, 150, 122));
		knownColors.put("darkseagreen", new Color(143, 188, 143));
		knownColors.put("darkslateblue", new Color(72, 61, 139));
		knownColors.put("darkslategray", new Color(47, 79, 79));
		knownColors.put("darkturquoise", new Color(0, 206, 209));
		knownColors.put("darkviolet", new Color(148, 0, 211));
		knownColors.put("deeppink", new Color(255, 20, 147));
		knownColors.put("deepskyblue", new Color(0, 191, 255));
		knownColors.put("dimgray", new Color(105, 105, 105));
		knownColors.put("dodgerblue", new Color(30, 144, 255));
		knownColors.put("firebrick", new Color(178, 34, 34));
		knownColors.put("floralwhite", new Color(255, 250, 240));
		knownColors.put("forestgreen", new Color(34, 139, 34));
		knownColors.put("fuchsia", new Color(255, 0, 255));
		knownColors.put("gainsboro", new Color(220, 220, 220));
		knownColors.put("ghostwhite", new Color(248, 248, 255));
		knownColors.put("gold", new Color(255, 215, 0));
		knownColors.put("goldenrod", new Color(218, 165, 32));
		knownColors.put("gray", new Color(128, 128, 128));
		knownColors.put("green", new Color(0, 128, 0));
		knownColors.put("greenyellow", new Color(173, 255, 47));
		knownColors.put("honeydew", new Color(240, 255, 240));
		knownColors.put("hotpink", new Color(255, 105, 180));
		knownColors.put("indianred", new Color(205, 92, 92));
		knownColors.put("indigo", new Color(75, 0, 130));
		knownColors.put("ivory", new Color(255, 255, 240));
		knownColors.put("khaki", new Color(240, 230, 140));
		knownColors.put("lavender", new Color(230, 230, 250));
		knownColors.put("lavenderblush", new Color(255, 240, 245));
		knownColors.put("lawngreen", new Color(124, 252, 0));
		knownColors.put("lemonchiffon", new Color(255, 250, 205));
		knownColors.put("lightblue", new Color(173, 216, 230));
		knownColors.put("lightcoral", new Color(240, 128, 128));
		knownColors.put("lightcyan", new Color(224, 255, 255));
		knownColors.put("lightgoldenrodyellow", new Color(250, 250, 210));
		knownColors.put("lightgray", new Color(211, 211, 211));
		knownColors.put("lightgreen", new Color(144, 238, 144));
		knownColors.put("lightpink", new Color(255, 182, 193));
		knownColors.put("lightsalmon", new Color(255, 160, 122));
		knownColors.put("lightseagreen", new Color(32, 178, 170));
		knownColors.put("lightskyblue", new Color(135, 206, 250));
		knownColors.put("lightslategray", new Color(119, 136, 153));
		knownColors.put("lightsteelblue", new Color(176, 196, 222));
		knownColors.put("lightyellow", new Color(255, 255, 224));
		knownColors.put("lime", new Color(0, 255, 0));
		knownColors.put("limegreen", new Color(50, 205, 50));
		knownColors.put("linen", new Color(250, 240, 230));
		knownColors.put("magenta", new Color(255, 0, 255));
		knownColors.put("maroon", new Color(128, 0, 0));
		knownColors.put("mediumaquamarine", new Color(102, 205, 170));
		knownColors.put("mediumblue", new Color(0, 0, 205));
		knownColors.put("mediumorchid", new Color(186, 85, 211));
		knownColors.put("mediumpurple", new Color(147, 112, 219));
		knownColors.put("mediumseagreen", new Color(60, 179, 113));
		knownColors.put("mediumslateblue", new Color(123, 104, 238));
		knownColors.put("mediumspringgreen", new Color(0, 250, 154));
		knownColors.put("mediumturquoise", new Color(72, 209, 204));
		knownColors.put("mediumvioletred", new Color(199, 21, 133));
		knownColors.put("midnightblue", new Color(25, 25, 112));
		knownColors.put("mintcream", new Color(245, 255, 250));
		knownColors.put("mistyrose", new Color(255, 228, 225));
		knownColors.put("moccasin", new Color(255, 228, 181));
		knownColors.put("navajowhite", new Color(255, 222, 173));
		knownColors.put("navy", new Color(0, 0, 128));
		knownColors.put("oldlace", new Color(253, 245, 230));
		knownColors.put("olive", new Color(128, 128, 0));
		knownColors.put("olivedrab", new Color(107, 142, 35));
		knownColors.put("orange", new Color(255, 165, 0));
		knownColors.put("orangered", new Color(255, 69, 0));
		knownColors.put("orchid", new Color(218, 112, 214));
		knownColors.put("palegoldenrod", new Color(238, 232, 170));
		knownColors.put("palegreen", new Color(152, 251, 152));
		knownColors.put("paleturquoise", new Color(175, 238, 238));
		knownColors.put("palevioletred", new Color(219, 112, 147));
		knownColors.put("papayawhip", new Color(255, 239, 213));
		knownColors.put("peachpuff", new Color(255, 218, 185));
		knownColors.put("peru", new Color(205, 133, 63));
		knownColors.put("pink", new Color(255, 192, 203));
		knownColors.put("plum", new Color(221, 160, 221));
		knownColors.put("powderblue", new Color(176, 224, 230));
		knownColors.put("purple", new Color(128, 0, 128));
		knownColors.put("rebeccapurple", new Color(102, 51, 153));
		knownColors.put("red", new Color(255, 0, 0));
		knownColors.put("rosybrown", new Color(188, 143, 143));
		knownColors.put("royalblue", new Color(65, 105, 225));
		knownColors.put("saddlebrown", new Color(139, 69, 19));
		knownColors.put("salmon", new Color(250, 128, 114));
		knownColors.put("sandybrown", new Color(244, 164, 96));
		knownColors.put("seagreen", new Color(46, 139, 87));
		knownColors.put("seashell", new Color(255, 245, 238));
		knownColors.put("sienna", new Color(160, 82, 45));
		knownColors.put("silver", new Color(192, 192, 192));
		knownColors.put("skyblue", new Color(135, 206, 235));
		knownColors.put("slateblue", new Color(106, 90, 205));
		knownColors.put("slategray", new Color(112, 128, 144));
		knownColors.put("snow", new Color(255, 250, 250));
		knownColors.put("springgreen", new Color(0, 255, 127));
		knownColors.put("steelblue", new Color(70, 130, 180));
		knownColors.put("tan", new Color(210, 180, 140));
		knownColors.put("teal", new Color(0, 128, 128));
		knownColors.put("thistle", new Color(216, 191, 216));
		knownColors.put("tomato", new Color(255, 99, 71));
		knownColors.put("turquoise", new Color(64, 224, 208));
		knownColors.put("violet", new Color(238, 130, 238));
		knownColors.put("wheat", new Color(245, 222, 179));
		knownColors.put("white", new Color(255, 255, 255));
		knownColors.put("whitesmoke", new Color(245, 245, 245));
		knownColors.put("yellow", new Color(255, 255, 0));
		knownColors.put("yellowgreen", new Color(154, 205, 50));
	}

	public static String getColorName(Color inputColor) {
		String closestName = null;
		double minDistance = Double.MAX_VALUE;

		for (Map.Entry<String, Color> entry : knownColors.entrySet()) {
			Color known = entry.getValue();
			double distance = colorDistance(inputColor, known);

			if (distance < minDistance) {
				minDistance = distance;
				closestName = entry.getKey();
			}
		}

		return closestName != null ? closestName : "unknown";
	}

	private static double colorDistance(Color c1, Color c2) {
		int rDiff = c1.getRed() - c2.getRed();
		int gDiff = c1.getGreen() - c2.getGreen();
		int bDiff = c1.getBlue() - c2.getBlue();
		return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
	}
}
