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
package org.olat.selenium.page.core;

import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;

/**
 * 
 * Initial date: 19 mars 2018<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LicensesAdminstrationPage {
	
	private final WebDriver browser;
	
	public LicensesAdminstrationPage(WebDriver browser) {
		this.browser = browser;
	}
	
	public LicensesAdminstrationPage enableForResources(String license) {
		By resourceCheckBy = By.xpath("//div[contains(@class,'o_table_flexi')]//tr[td[text()[contains(.,'" + license + "')]]]/td[10]/div/label/input[@type='checkbox']");
		By resourceLabelBy = By.xpath("//div[contains(@class,'o_table_flexi')]//tr[td[text()[contains(.,'" + license + "')]]]/td[10]/div/label");

		WebElement resourceCheckEl = browser.findElement(resourceCheckBy);
		WebElement resourceLabelEl = browser.findElement(resourceLabelBy);
		if(browser instanceof ChromeDriver) {
			new Actions(browser).moveToElement(resourceLabelEl).build().perform();
			OOGraphene.waitingALittleBit();
		}
		
		OOGraphene.check(resourceLabelEl, resourceCheckEl, Boolean.TRUE);
		OOGraphene.waitBusy(browser);
		return this;
	}

}
