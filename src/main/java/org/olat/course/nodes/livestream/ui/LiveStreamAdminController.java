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
package org.olat.course.nodes.livestream.ui;

import static org.olat.core.gui.translator.TranslatorHelper.translateAll;
import static org.olat.course.nodes.livestream.ui.LiveStreamUIFactory.validateInteger;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.course.nodes.livestream.LiveStreamModule;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 5 Jun 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class LiveStreamAdminController extends FormBasicController {
	
	private static final String[] ENABLED_KEYS = new String[]{"on"};
	
	private MultipleSelectionElement enabledEl;
	private TextElement bufferBeforeMinEl;
	private TextElement bufferAfterMinEl;
	private MultipleSelectionElement coachCanEditEl;
	
	@Autowired
	private LiveStreamModule liveStreamModule;

	public LiveStreamAdminController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl, LAYOUT_BAREBONE);
		
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		FormLayoutContainer generalCont = FormLayoutContainer.createDefaultFormLayout("general", getTranslator());
		generalCont.setFormTitle(translate("admin.general.title"));
		generalCont.setRootForm(mainForm);
		formLayout.add("genearl", generalCont);
		
		enabledEl = uifactory.addCheckboxesHorizontal("admin.module.enabled", generalCont, ENABLED_KEYS,
				translateAll(getTranslator(), ENABLED_KEYS));
		enabledEl.select(ENABLED_KEYS[0], liveStreamModule.isEnabled());
		
		FormLayoutContainer defaultValuesCont = FormLayoutContainer.createDefaultFormLayout("default_values", getTranslator());
		defaultValuesCont.setFormTitle(translate("admin.default.values.title"));
		defaultValuesCont.setFormDescription(translate("admin.default.values.desc"));
		defaultValuesCont.setRootForm(mainForm);
		formLayout.add("defaultValues", defaultValuesCont);

		int bufferBeforeMin = liveStreamModule.getBufferBeforeMin();
		bufferBeforeMinEl = uifactory.addTextElement("admin.buffer.before.min", 4, String.valueOf(bufferBeforeMin),
				defaultValuesCont);
		bufferBeforeMinEl.setMandatory(true);

		int bufferAfterMin = liveStreamModule.getBufferAfterMin();
		bufferAfterMinEl = uifactory.addTextElement("admin.buffer.after.min", 4, String.valueOf(bufferAfterMin),
				defaultValuesCont);
		bufferAfterMinEl.setMandatory(true);
		
		coachCanEditEl = uifactory.addCheckboxesHorizontal("admin.coach.edit", defaultValuesCont, ENABLED_KEYS,
				translateAll(getTranslator(), ENABLED_KEYS));
		boolean coachCanEdit = liveStreamModule.isEditCoach();
		coachCanEditEl.select(ENABLED_KEYS[0], coachCanEdit);
		
		FormLayoutContainer buttonsCont = FormLayoutContainer.createDefaultFormLayout("buttons", getTranslator());
		buttonsCont.setRootForm(mainForm);
		formLayout.add("buttons", buttonsCont);
		uifactory.addFormSubmitButton("save", buttonsCont);
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = super.validateFormLogic(ureq);

		allOk &= validateInteger(bufferBeforeMinEl, true);
		allOk &= validateInteger(bufferAfterMinEl, true);

		return allOk;
	}

	@Override
	protected void formOK(UserRequest ureq) {
		boolean enabled = enabledEl.isAtLeastSelected(1);
		liveStreamModule.setEnabled(enabled);
		
		int bufferBeforeMin = Integer.parseInt(bufferBeforeMinEl.getValue());
		liveStreamModule.setBufferBeforeMin(bufferBeforeMin);
		
		int bufferAfterMin = Integer.parseInt(bufferAfterMinEl.getValue());
		liveStreamModule.setBufferAfterMin(bufferAfterMin);
		
		boolean coachCanEdit = coachCanEditEl.isAtLeastSelected(1);
		liveStreamModule.setEditCoach(coachCanEdit);
	}
	
	@Override
	protected void doDispose() {
		//
	}

}
