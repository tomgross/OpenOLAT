/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.core.util.filter.impl;

import java.io.IOException;
import java.io.StringReader;

import org.cyberneko.html.parsers.SAXParser;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.io.LimitedContentWriter;
import org.olat.search.service.document.file.FileDocumentFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Description:<br>
 * The html tags filter takes a string and filters all HTML tags. The filter
 * does not remove the code within the tags, only the tag itself. Example:
 * '&lt;font color="red"&gt;hello&lt;/font&gt;world' will become 'hello world'
 * <p>
 * The filter might not be perfect, its a simple version. All tag attributes
 * will be removed as well.
 * <p>
 * Use the SimpleHTMLTagsFilterTest to add new testcases that must work with 
 * this filter.
 * 
 * <P>
 * Initial Date: 15.07.2009 <br>
 * 
 * @author gnaegi
 */
public class SimpleHTMLTagsFilter extends StripHTMLTagsFilter {
	private static final OLog log = Tracing.createLoggerFor(SimpleHTMLTagsFilter.class);

	@Override
	public String filter(String original) {
		if(original == null) return null;
		if(original.isEmpty()) return "";
		
		try {
			String text = super.filter(original);
			text = StringHelper.escapeHtml(text);
			return text;
		} catch (Exception e) {
			log.error("", e);
			return null;
		}
	}
}
