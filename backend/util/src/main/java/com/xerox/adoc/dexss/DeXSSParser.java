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

import java.util.Properties;

import com.learningobjects.tagsoup.Parser;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * The DeXSSParser object.
 * This class can be used as a SAX2 XML Parser that first applies TagSoup, then applies the {@link DeXSSFilterPipeline}.
 * <p>Example:
 * <pre>{
 *  DeXSSParser dexssParser = new DeXSSParser();
 *  dexssParser.setContentHandler(new XMLWriter(writer));
 *  InputSource inputSource = new InputSource();
 *  inputSource.setCharacterStream(new StringReader(inputString));
 *  dexssParser.parse(inputSource);
 *}
 * </pre>
 * </p>
 */
public class DeXSSParser extends DeXSSFilterPipeline {
  /**
   * Creates a DeXSSParser with the following feature set:
   * <ul>
   * <li>{@link DeXSSFilterPipeline#BODY_ONLY} <code>true</code></li>
   * </ul>
   * And uses as parent a {@link com.learningobjects.tagsoup.Parser} with the following feature set:
   * <ul>
   * <li>{@link com.learningobjects.tagsoup.Parser#ignoreBogonsFeature} <code>true</code></li>
   * <li>{@link com.learningobjects.tagsoup.Parser#defaultAttributesFeature} <code>false</code></li>
   * </ul>
   * TODO: Should be made more configurable.
   */
  public DeXSSParser() throws SAXNotRecognizedException, SAXNotSupportedException {
      this(new Properties());
  }

  public DeXSSParser(Properties p) throws SAXNotRecognizedException, SAXNotSupportedException {
    super(p);
    setFeature(DeXSSFilterPipeline.BODY_ONLY, true);
    Parser parser = new Parser();
    parser.setFeature(Parser.ignoreBogonsFeature, true);
    parser.setFeature(Parser.defaultAttributesFeature, false);
    setParent(parser);
  }
}
