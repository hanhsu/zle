import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.yahoo.platform.yui.compressor.CssCompressor;


public class CompressCss {
	public static void main(String[] args) throws IOException {
		if (args.length > 0 && !args[1].endsWith("_na_dir_")) {
			final File srcdir = new File(args[1]);
			for (File srcFile : getFiles(srcdir, new ArrayList<File>(30))) {
				System.out.println(srcFile.getPath());
				if (srcFile.getName().endsWith(".css.dsp.src")) {
					String currentPath = srcFile.getPath();
					String destPath = currentPath.replace(".css.dsp.src", ".css.dsp");
					File dspSrcFile = new File(currentPath);
					File dspFile = new File(destPath);
					
					/* Compress *.css.dsp.src to *.css.dsp */
					// bug fix for UTF8 BOM issue by TonyQ
					InputStreamReader in = new UnicodeReader(
							new FileInputStream(dspSrcFile), "UTF-8");
					OutputStreamWriter out = new OutputStreamWriter(
							new FileOutputStream(dspFile), "UTF-8");
					CssCompressor compressor = new CssCompressor(in);
					compressor.compress(out, -1);
					in.close();
					out.close();
				}
			}
		}
	}
	
	private static List<File> getFiles(File dir, List<File> list) {
		String dirName = dir.getName();
		if (".svn".equals(dirName) || ".git".equals(dirName)
				|| "CVS".equals(dirName))
			return list;
		if (dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				getFiles(f, list);
			}
		} else if (dir.isFile() && dirName.endsWith(".css.dsp.src"))
			list.add(dir);
		return list;
	}
}

