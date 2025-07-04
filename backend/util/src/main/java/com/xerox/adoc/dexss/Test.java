/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.xerox.adoc.dexss;

import java.io.*;
import org.xml.sax.SAXException;


/**
 * This class satisfies the @link DeXSSChangeListener interface and offers
 * a command-line utility for applying DeXSS to files.  It reports a possible
 * failure for any files that <em>don't</em> change.
 *
 * TODO: Do a better job of testing or expected removal and non-removal
 * of XSS code.
 */
public class Test implements DeXSSChangeListener {
  boolean changed = false;
  boolean showChanges = true;

  public void logXSSChange(String message) {
    if(showChanges)
      System.err.println("* " + message);
    changed = true;
  }

  public void logXSSChange(String message, String item1) {
    if(showChanges)
      System.err.println("* " + message + " " + item1);
    changed = true;
  }

  public void logXSSChange(String message, String item1, String item2) {
    if(showChanges)
      System.err.println("* " + message + " " + item1 + " " + item2);
    changed = true;
  }

  private boolean isChanged() {
    return changed;
  }

  private void resetChanged() {
    changed = false;
  }

  /**
   * This command-line test program processes the specified files or literals, and for each one
   * prints to System.out the following:
   * <ul>
   * <li>the file name (if any)</li>
   * <li>Any change messages from {@link DeXSSChangeListener}</li>
   * <li>Serialized XML result</li>
   * <li>A summary indicating whether the input changed or not (based on whether there were any XSSChangeListener messages)</li>
   * </ul>
   * TODO: A better test and regression harness.  More Test cases.
   *
   * @param argv Command-line args are files to process, or if first arg is hypen, strings to process.
   */
  public static void main(String[] argv) throws IOException, SAXException {
    OutputStreamWriter w = new OutputStreamWriter(System.out, "UTF-8");
    Test test = new Test();

    DeXSS xss = DeXSS.createInstance(test, w);
    if (argv.length > 0) {
      boolean isLiteral = argv[0].equals("-");
      if (isLiteral) {
        for (int i = 1; i < argv.length; i++) {
          xss.process(argv[i]);
        }
      } else {
        for (int i = 0; i < argv.length; i++) {
          String filename = argv[i];
          String string = readFile(filename);
          System.out.println(filename);
          xss.process(string);
          if (! test.isChanged()) {
            // it might not have failed because the HTML parser might have cleaned it up for us
            System.out.println("- " + filename + " unchanged");
            System.out.println(string);
          } else {
            test.resetChanged();
          }
        }
      }
    }
  }

  private static String readFile(String file) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(file));
    StringWriter out = new StringWriter();
    int c;

    while ((c = in.read()) != -1)
      out.write(c);

    in.close();
    out.close();
    return out.toString();
  }
}
