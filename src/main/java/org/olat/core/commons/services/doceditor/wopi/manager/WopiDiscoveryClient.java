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
package org.olat.core.commons.services.doceditor.wopi.manager;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.olat.core.commons.services.doceditor.wopi.Discovery;
import org.olat.core.commons.services.doceditor.wopi.model.DiscoveryImpl;
import org.apache.logging.log4j.Logger;
import org.olat.core.logging.Tracing;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 1 Mar 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
@Service
class WopiDiscoveryClient {
	
	private static final Logger log = Tracing.createLoggerFor(WopiDiscoveryClient.class);
	
	private static final int TIMEOUT_5000_MILLIS = 5000;
	private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
			.setSocketTimeout(TIMEOUT_5000_MILLIS)
			.setConnectTimeout(TIMEOUT_5000_MILLIS)
			.setConnectionRequestTimeout(TIMEOUT_5000_MILLIS)
			.build();

	String getRegularDiscoveryPath() {
		return "hosting/discovery";
	}
	
	Discovery getDiscovery(String discoveryUrl) {
		HttpGet request = new HttpGet(discoveryUrl);
		request.setConfig(REQUEST_CONFIG);
		
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse httpResponse = httpClient.execute(request);) {
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = httpResponse.getEntity();
				String xml = EntityUtils.toString(entity);
				return WopiXStream.fromXml(xml, DiscoveryImpl.class);
			}
			log.warn("Fetching WOPI discovery file return with status " + statusCode + ". URL: " + discoveryUrl);
		} catch(Exception e) {
			log.error("Error while fetching WOPI discovery file. URL: " + discoveryUrl, e);
		}
		return null;
	}

}
