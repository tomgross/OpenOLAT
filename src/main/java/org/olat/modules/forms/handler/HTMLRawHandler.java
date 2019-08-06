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
package org.olat.modules.forms.handler;

import java.util.Locale;
import java.util.UUID;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.text.TextFactory;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.Util;
import org.olat.modules.forms.model.xml.HTMLRaw;
import org.olat.modules.forms.ui.HTMLRawEditorController;
import org.olat.modules.portfolio.ui.editor.PageElement;
import org.olat.modules.portfolio.ui.editor.PageElementEditorController;
import org.olat.modules.portfolio.ui.editor.PageElementHandler;
import org.olat.modules.portfolio.ui.editor.PageElementRenderingHints;
import org.olat.modules.portfolio.ui.editor.PageRunComponent;
import org.olat.modules.portfolio.ui.editor.PageRunElement;
import org.olat.modules.portfolio.ui.editor.SimpleAddPageElementHandler;

/**
 * 
 * Initial date: 12 déc. 2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class HTMLRawHandler implements PageElementHandler, SimpleAddPageElementHandler {

	@Override
	public String getType() {
		return "formhtmlraw";
	}

	@Override
	public String getIconCssClass() {
		return "o_icon_paragraph";
	}

	@Override
	public PageRunElement getContent(UserRequest ureq, WindowControl wControl, PageElement element, PageElementRenderingHints hints) {
		String content = "";
		if(element instanceof HTMLRaw) {
			content = ((HTMLRaw)element).getContent();
		}
		Component cmp = TextFactory.createTextComponentFromString("htmlraw_" + CodeHelper.getRAMUniqueID(), content, null, false, null);
		return new PageRunComponent(cmp);
	}

	@Override
	public PageElementEditorController getEditor(UserRequest ureq, WindowControl wControl, PageElement element) {
		if(element instanceof HTMLRaw) {
			return new HTMLRawEditorController(ureq, wControl, (HTMLRaw)element);
		}
		return null;
	}

	@Override
	public PageElement createPageElement(Locale locale) {
		Translator translator = Util.createPackageTranslator(HTMLRawEditorController.class, locale);
		String content = translator.translate("raw.example");
		HTMLRaw part = new HTMLRaw();
		part.setId(UUID.randomUUID().toString());
		part.setContent(content);
		return part;
	}
}
