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
package org.olat.core.commons.services.help.spi;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.olat.core.helpers.SettingsTest;

/**
 * 
 * Initial date: 07.01.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class ConfluenceLinkSPITest {
	
	@Test
	public void getURL_confluence() {
		ConfluenceLinkSPI linkSPI = new ConfluenceLinkSPI();

		String url1 = linkSPI.generateSpace("10.1.1", Locale.GERMAN);
		Assert.assertNotNull(url1);
		Assert.assertTrue(url1.startsWith("/OO101DE/"));
		
		String url2 = linkSPI.generateSpace("10.1", Locale.ENGLISH);
		Assert.assertNotNull(url2);
		Assert.assertTrue(url2.startsWith("/OO101EN/"));
		
		String url3 = linkSPI.generateSpace("10.1a", Locale.ENGLISH);
		Assert.assertNotNull(url3);
		Assert.assertTrue(url3.startsWith("/OO101EN/"));
		
		String url4 = linkSPI.generateSpace("11a", Locale.ENGLISH);
		Assert.assertNotNull(url4);
		Assert.assertTrue(url4.startsWith("/OO110EN/"));
	}
	
	@Test
	public void getUrl() {
		// init settings to set version, required by ConfluenceLinkSPI
		SettingsTest.createHttpDefaultPortSettings();
		
		ConfluenceLinkSPI linkSPI = new ConfluenceLinkSPI();
		//Data%20Management#DataManagement-qb_import
		// Standard Case in English
		String url1 = linkSPI.getURL(Locale.ENGLISH, "Data Management");
		Assert.assertNotNull(url1);
		Assert.assertTrue(url1.endsWith("Data%20Management"));
		
		// Special handing for anchors in confluence
		String url2 = linkSPI.getURL(Locale.ENGLISH, "Data Management#qb_import");
		Assert.assertNotNull(url2);
		Assert.assertTrue(url2.endsWith("Data%20Management#DataManagement-qb_import"));
	}
	
	@Test
	public void getTranslatedUrl() {
		// init settings to set version, required by ConfluenceLinkSPI
		SettingsTest.createHttpDefaultPortSettings();
		
		ConfluenceLinkSPI linkSPI = new ConfluenceLinkSPI();
		// Standard Case in German - same as in english
		String url1 = linkSPI.getURL(Locale.GERMAN, "Data Management");
		Assert.assertNotNull(url1);
		Assert.assertTrue(url1.endsWith("Data%20Management"));
		
		// Special handing for anchors in confluence
		// Here some magic is needed since the CustomWare Redirection Plugin
		// plugin we use in Confluence can not redirec links with anchors. The
		// anchor is deleted.
		// We have to translate this here
		// First time it won't return the translated link as it does the translation asynchronously in a separate thread to not block the UI
		String url2 = linkSPI.getURL(Locale.GERMAN, "Data Management#qb_import");
		Assert.assertNotNull(url2);
		Assert.assertTrue(url2.endsWith("Data%20Management#DataManagement-qb_import"));
		// Wait 5secs and try it again, should be translated now
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		url2 = linkSPI.getURL(Locale.GERMAN, "Data Management#qb_import");
		Assert.assertNotNull(url2);
		Assert.assertTrue(url2.endsWith("Handhabung%20der%20Daten#HandhabungderDaten-qb_import"));
	}
}
