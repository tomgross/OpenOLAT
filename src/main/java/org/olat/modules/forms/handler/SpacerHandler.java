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
import java.util.concurrent.atomic.AtomicInteger;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.impl.elements.SpacerElementComponent;
import org.olat.core.gui.control.WindowControl;
import org.olat.modules.forms.model.xml.Spacer;
import org.olat.modules.forms.ui.SpacerEditorController;
import org.olat.modules.portfolio.ui.editor.PageRunComponent;
import org.olat.modules.portfolio.ui.editor.PageElement;
import org.olat.modules.portfolio.ui.editor.PageElementEditorController;
import org.olat.modules.portfolio.ui.editor.PageElementHandler;
import org.olat.modules.portfolio.ui.editor.PageElementRenderingHints;
import org.olat.modules.portfolio.ui.editor.PageRunElement;
import org.olat.modules.portfolio.ui.editor.SimpleAddPageElementHandler;

/**
 * 
 * Initial date: 04.07.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class SpacerHandler implements PageElementHandler, SimpleAddPageElementHandler {
	
	private static final AtomicInteger idGenerator = new AtomicInteger();

	@Override
	public String getType() {
		return "formhr";
	}

	@Override
	public String getIconCssClass() {
		return "o_icon_spacer";
	}

	@Override
	public PageRunElement getContent(UserRequest ureq, WindowControl wControl, PageElement element, PageElementRenderingHints hints) {
		if(element instanceof Spacer) {
			Component cmp = new SpacerElementComponent("spacer_" + idGenerator.incrementAndGet());
			return new PageRunComponent(cmp);
		}
		return null;
	}

	@Override
	public PageElementEditorController getEditor(UserRequest ureq, WindowControl wControl, PageElement element) {
		if(element instanceof Spacer) {
			return new SpacerEditorController(ureq, wControl);
		}
		return null;
	}

	@Override
	public PageElement createPageElement(Locale locale) {
		String content = "<hr/>";
		Spacer part = new Spacer();
		part.setId(UUID.randomUUID().toString());
		part.setContent(content);
		return part;
	}
}
