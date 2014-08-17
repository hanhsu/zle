import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** SyntaxHelper.java.

 Purpose:

 Description:

 History:
 3:10:24 PM May 17, 2013, Created by jumperchen

 Copyright (C) 2013 Potix Corporation. All Rights Reserved.
 */

/**
 * @author jumperchen
 * 
 */
public class SyntaxHelper {
	private static String FAKE_PROPERTY = "__faker___:";

	private static String fixSyntaxIssue(String source) {
		try {
			source = Comments.removeComment(source);
		} catch (IllegalStateException ex) {
			log("clear comment failed:" + ex.getMessage() + ":skip clear comment step");
		}
		StringBuffer sb = new StringBuffer(1024);
		Scanner scan = new Scanner(source);
		boolean hasProp = false;
		while (scan.hasNext()) {
			String line = scan.nextLine().trim();
			int start = line.indexOf(":");
			int end = line.indexOf("${");

			if (start > 0 && end >= 0 && start < end && line.endsWith(","))
				hasProp = true;

			// syntax issue with less but works in zk EL function
			if (end >= 0 && start >= -1 && start >= end) {
				if (hasProp && (line.endsWith(",") || line.endsWith(";"))) {
					sb.append(line).append("\n");
					continue;
				}
				if (!line.endsWith(";")) {
					if (!line.endsWith(","))
						log(line
								+ "\n\tmissing ',' or ';' signature at the end of line");
					continue;
				}
				sb.append(FAKE_PROPERTY);
			}
			if (line.endsWith(";"))
				hasProp = false; // reset
			sb.append(line).append("\n");
		}
		scan.close();
		return sb.toString();
	}
	
	public static String encodeDspNew(String source, String importRoot, String theme) {
		source = fixSyntaxIssue(source);
		StringBuffer sb = new StringBuffer();

		/* 1. Resolve import starts with ~./ */
		Matcher m = Pattern.compile("@import[\\s]{1,}[\"']{1}~./[\\w/.]{1,}[\"']{1}[;]{1}").matcher(source);
		int end = 0, i = 0;
		while (m.find()) {
			String s = m.group();
			int quoteIndex = s.indexOf("~");
			String quote = s.substring(quoteIndex - 1, quoteIndex);
			// s = s.replaceAll("[\"']{1}~./", quote + "classpath:web/");
			if (s.contains("zkmax")) {
				importRoot = importRoot.replace("zk/zul", "zkcml/zkmax");
			}
			s = s.replaceAll("[\"']{1}~./", quote + importRoot);
			
			m.appendReplacement(sb, s);
			// end = m.end() + 11 * (++i); // 11 = from "~." to "classpath:web" 
			end = m.end() + 4 * (++i);
		}
		m.appendTail(sb);
		if (!"".equals(theme)) {
			sb.insert(end, "\n@import \"classpath:web/" + theme + "/zul/less/_zkvariables.less\";\n");
		}
		source = sb.toString();
		sb.delete(0, sb.length());

		/* 2. Escape like @{variable} */
		m = Pattern.compile("@\\{([^\\}]+)\\}").matcher(source);
		while (m.find()) {
			String s = m.group();
			s = s.replaceAll("@\\{", "__LESSOPEN__");
			s = s.replaceAll("\\}", "__LESSEND__");
			m.appendReplacement(sb, s);
		}
		m.appendTail(sb);
		source = sb.toString();
		sb.delete(0, sb.length());

		/* 3. Resolve EL function in url() like url(${c:encodeThemeURL}) */
		m = Pattern.compile("url\\(\\$\\{([^\\}]+)\\}\\)").matcher(source);
		while (m.find()) {
			String s = m.group();
			s = s.replaceAll("\\$\\{", "__EL__");
			s = s.replaceAll(":", "__ELSP__");
			s = s.replaceAll("\\}", "__ELEND__");
			s = "~\"" + s + "\"";
			m.appendReplacement(sb, s);
		}
		m.appendTail(sb);
		source = sb.toString();
		sb.delete(0, sb.length());

		/* 4. Resolve EL function like ${t:applyCSS3} */
		m = Pattern.compile("\\$\\{([^\\}]+)\\}").matcher(source);
		while (m.find()) {
			String s = m.group();
			s = s.replaceAll("\\$\\{", "__EL__");
			s = s.replaceAll(":", "__ELSP__");
			s = s.replaceAll("\\}", "__ELEND__");
			m.appendReplacement(sb, s);
		}
		m.appendTail(sb);
		source = sb.toString();
		sb.delete(0, sb.length());

		/* 5. Resolve DSP declaration like <@taglib @> */
		m = Pattern.compile("<(.*)>").matcher(source);
		while (m.find()) {
			String s = m.group();
			s = "/*__TAGLIB " + s + " TAGLIB__*/";
			m.appendReplacement(sb, s);
		}
		m.appendTail(sb);

		return sb.toString()
				.replaceAll("__LESSOPEN__", "@\\{")
				.replaceAll("__LESSEND__", "\\}");
	}
	
	
	
	

	/**
	 * Encode LESS file that contains DSP function
	 * 
	 * @param source
	 *            the LESS file source.
	 * @param theme
	 *            specify the theme name to resolve LESS import path.
	 * @return encoded LESS source
	 */
	public static String encodeDsp(String source, String theme) {
		source = fixSyntaxIssue(source);
		StringBuffer sb = new StringBuffer();

		/* 1. Resolve import starts with ~./ */
		Matcher m = Pattern.compile("@import[\\s]{1,}[\"']{1}~./[\\w/.]{1,}[\"']{1}[;]{1}").matcher(source);
		int end = 0, i = 0;
		while (m.find()) {
			String s = m.group();
			int quoteIndex = s.indexOf("~");
			String quote = s.substring(quoteIndex - 1, quoteIndex);
			s = s.replaceAll("[\"']{1}~./", quote + "classpath:web/");
			m.appendReplacement(sb, s);
			end = m.end() + 11 * (++i); // 11 = from "~." to "classpath:web"
		}
		m.appendTail(sb);
		if (!"".equals(theme)) {
			sb.insert(end, "\n@import \"classpath:web/" + theme + "/zul/less/_zkvariables.less\";\n");
		}
		source = sb.toString();
		sb.delete(0, sb.length());

		/* 2. Escape like @{variable} */
		m = Pattern.compile("@\\{([^\\}]+)\\}").matcher(source);
		while (m.find()) {
			String s = m.group();
			s = s.replaceAll("@\\{", "__LESSOPEN__");
			s = s.replaceAll("\\}", "__LESSEND__");
			m.appendReplacement(sb, s);
		}
		m.appendTail(sb);
		source = sb.toString();
		sb.delete(0, sb.length());

		/* 3. Resolve EL function in url() like url(${c:encodeThemeURL}) */
		m = Pattern.compile("url\\(\\$\\{([^\\}]+)\\}\\)").matcher(source);
		while (m.find()) {
			String s = m.group();
			s = s.replaceAll("\\$\\{", "__EL__");
			s = s.replaceAll(":", "__ELSP__");
			s = s.replaceAll("\\}", "__ELEND__");
			s = "~\"" + s + "\"";
			m.appendReplacement(sb, s);
		}
		m.appendTail(sb);
		source = sb.toString();
		sb.delete(0, sb.length());

		/* 4. Resolve EL function like ${t:applyCSS3} */
		m = Pattern.compile("\\$\\{([^\\}]+)\\}").matcher(source);
		while (m.find()) {
			String s = m.group();
			s = s.replaceAll("\\$\\{", "__EL__");
			s = s.replaceAll(":", "__ELSP__");
			s = s.replaceAll("\\}", "__ELEND__");
			m.appendReplacement(sb, s);
		}
		m.appendTail(sb);
		source = sb.toString();
		sb.delete(0, sb.length());

		/* 5. Resolve DSP declaration like <@taglib @> */
		m = Pattern.compile("<(.*)>").matcher(source);
		while (m.find()) {
			String s = m.group();
			s = "/*__TAGLIB " + s + " TAGLIB__*/";
			m.appendReplacement(sb, s);
		}
		m.appendTail(sb);

		return sb.toString()
				.replaceAll("__LESSOPEN__", "@\\{")
				.replaceAll("__LESSEND__", "\\}");
	}

	/**
	 * Decode DSP function
	 * 
	 * @param source
	 *            the DSP file source.
	 * @return decoded DSP source
	 */
	public static String decodeDsp(String source) {
		return source
				/* 1. Restore DSP declaration like <@taglib @> */
				.replaceAll("\\/\\*__TAGLIB ", "")
				.replaceAll(" TAGLIB__\\*\\/", "")
				/* 2. Restore EL function like ${c:encodeThemeURL} */
				.replaceAll("__EL__", "\\$\\{").replaceAll("__ELSP__", ":")
				.replaceAll("__ELEND__", "\\}").replaceAll(FAKE_PROPERTY, "");
	}

	private static void log(Object... os) {
		for (Object o : os) {
			System.out.print(o + (os[os.length - 1] != o ? "," : ""));
		}
		System.out.println();
	}
}
