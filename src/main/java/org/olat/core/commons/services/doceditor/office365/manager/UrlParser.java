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
package org.olat.core.commons.services.doceditor.office365.manager;

import java.net.URI;

import org.apache.logging.log4j.Logger;
import org.olat.core.logging.Tracing;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 2 May 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
@Service
class UrlParser {

	private static final Logger log = Tracing.createLoggerFor(UrlParser.class);
	
	private static final String LANGUAGE_PARAMETER = "UI_LLCC";

	String getProtocolAndDomain(String url) {
		try {
			String stripped = stripQuery(url);
			if (stripped != null) {
				URI uri = new URI(stripped);
				return uri.getScheme() + "://" + uri.getHost();
			}
		} catch (Exception e) {
			log.error("", e);
		}
		return null;
	}

	String stripQuery(String url) {
		return url != null && url.indexOf("?") > -1? url.substring(0, url.indexOf("?")): null;
	}

	String getLanguageParameter(String url) {
		int languageParameterIndey = url.indexOf(LANGUAGE_PARAMETER);
		if (languageParameterIndey > -1) {
			int start = url.lastIndexOf("<", languageParameterIndey);
			if (start > -1) {
				int end = url.lastIndexOf("=", languageParameterIndey);
				if (end > -1) {
					return url.substring(start + 1, end);
				}
			}
		}
		return null;
	}

}
