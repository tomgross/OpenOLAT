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
package org.olat.modules.forms.ui;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.filter.FilterFactory;
import org.olat.modules.forms.model.xml.Title;
import org.olat.modules.portfolio.ui.editor.PageElementEditorController;
import org.olat.modules.portfolio.ui.editor.event.ChangePartEvent;

/**
 * 
 * Initial date: 04.07.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class TitleEditorController extends FormBasicController implements PageElementEditorController {
	
	private RichTextElement titleItem;
	private StaticTextElement staticItem;
	
	private Title title;
	private boolean editMode = false;
	
	public TitleEditorController(UserRequest ureq, WindowControl wControl, Title title) {
		super(ureq, wControl, "title_editor");
		this.title = title;
		
		initForm(ureq);
		setEditMode(editMode);
	}

	@Override
	public boolean isEditMode() {
		return editMode;
	}

	@Override
	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
		titleItem.setVisible(editMode);
		staticItem.setVisible(!editMode);
		flc.getFormItemComponent().contextPut("editMode", Boolean.valueOf(editMode));
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		List<String> headingFormatLinkNames = new ArrayList<>();
		for(int i=1; i<=6; i++) {
			FormLink headingFormatLink = uifactory.addFormLink("h" + i, "h" + i, "h" + i, null, formLayout, Link.LINK);
			headingFormatLinkNames.add(headingFormatLink.getComponent().getComponentName());
		}
		flc.getFormItemComponent().contextPut("headingFormatLinkNames", headingFormatLinkNames);

		String cmpId = "title-" + CodeHelper.getRAMUniqueID() + "h";
		String content = title.getContent();
		titleItem = uifactory.addRichTextElementForStringDataCompact(cmpId, null, content, 8, 80, null, formLayout, ureq.getUserSession(), getWindowControl());
		titleItem.getEditorConfiguration().setSendOnBlur(true);
		titleItem.getEditorConfiguration().disableMenuAndMenuBar();
		
		staticItem = uifactory.addStaticTextElement(cmpId + "_static", content, formLayout);
		flc.getFormItemComponent().contextPut("cmpId", cmpId);
	}

	@Override
	protected void propagateDirtinessToContainer(FormItem fiSrc, FormEvent fe) {
		//super.propagateDirtinessToContainer(fiSrc, fe);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source instanceof FormLink) {
			FormLink link = (FormLink)source;
			String cmd = link.getCmd();
			if(cmd != null && cmd.startsWith("h")) {
				doChangeHeading(ureq, cmd);
			}
		} else if(titleItem == source) {
			doSave(ureq);
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		doSave(ureq);
	}
	
	private void doChangeHeading(UserRequest ureq, String heading) {
		String content = titleItem.getValue();
		String text = FilterFactory.getHtmlTagsFilter().filter(content);
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(heading).append(">").append(text).append("</").append(heading).append(">");
		titleItem.setValue(sb.toString());
		doSave(ureq);
	}
	
	private void doSave(UserRequest ureq) {
		String content = titleItem.getValue();
		title.setContent(content);
		staticItem.setValue(content);
		fireEvent(ureq, new ChangePartEvent(title));
	}

	@Override
	protected void doDispose() {
		//
	}
}
