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
package org.olat.repository.ui.author;

import org.olat.NewControllerFactory;
import org.olat.core.commons.services.license.LicenseModule;
import org.olat.core.commons.services.license.LicenseService;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.logging.activity.LearningResourceLoggingAction;
import org.olat.core.logging.activity.OlatResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.EntryChangedEvent;
import org.olat.repository.controllers.EntryChangedEvent.Change;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.manager.RepositoryEntryLicenseHandler;
import org.olat.util.logging.activity.LoggingResourceable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 29.04.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CreateRepositoryEntryController extends FormBasicController implements CreateEntryController {
	
	public static final Event CREATION_WIZARD = new Event("start_wizard");
	
	private FormLink wizardButton;
	private TextElement displaynameEl;
	private FormLayoutContainer exampleHelpEl;
	
	private RepositoryEntry addedEntry;
	private final RepositoryHandler handler;
	
	private Object userObject;
	
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private LicenseService licenseService;
	@Autowired
	private LicenseModule licenseModule;
	@Autowired
	private RepositoryEntryLicenseHandler licenseHandler;
	
	public CreateRepositoryEntryController(UserRequest ureq, WindowControl wControl, RepositoryHandler handler) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(RepositoryManager.class, getLocale(), getTranslator()));
		this.handler = handler;
		initForm(ureq);
	}

	@Override
	public RepositoryHandler getHandler() {
		return handler;
	}

	@Override
	public RepositoryEntry getAddedEntry() {
		return addedEntry;
	}

	public Object getCreateObject() {
		return userObject;
	}

	@Override
	public void setCreateObject(Object userObject) {
		this.userObject = userObject;
	}
	
	public void setDisplayname(String displayname) {
		displaynameEl.setValue(displayname);
	}
	
	public void setExampleAndHelp(String text, String helpUrl) {
		exampleHelpEl.contextPut("text", text);
		exampleHelpEl.contextPut("helpUrl", helpUrl);
		exampleHelpEl.setVisible(true);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		String typeName;
		if (handler != null) {
			typeName = NewControllerFactory.translateResourceableTypeName(handler.getSupportedType(), getLocale());
		} else {
			typeName = translate("cif.type.na");
		}
		StaticTextElement typeEl = uifactory.addStaticTextElement("cif.type", typeName, formLayout);
		typeEl.setElementCssClass("o_sel_author_type");

		displaynameEl = uifactory.addTextElement("cif.displayname", "cif.displayname", 255, "", formLayout);
		displaynameEl.setElementCssClass("o_sel_author_displayname");
		displaynameEl.setFocus(true);
		displaynameEl.setDisplaySize(30);
		displaynameEl.setMandatory(true);
		
		String page = velocity_root + "/example_help.html";
		exampleHelpEl = FormLayoutContainer.createCustomFormLayout("example.help", "example.help", getTranslator(), page);
		formLayout.add(exampleHelpEl);
		exampleHelpEl.setVisible(false);
		
		FormLayoutContainer buttonContainer = FormLayoutContainer.createButtonLayout("buttonContainer", getTranslator());
		formLayout.add("buttonContainer", buttonContainer);
		buttonContainer.setElementCssClass("o_sel_repo_save_details");
		FormSubmit submit = uifactory.addFormSubmitButton("cmd.create.ressource", buttonContainer);
		submit.setElementCssClass("o_sel_author_create_submit");
		
		if(handler.isPostCreateWizardAvailable()) {
			wizardButton = uifactory.addFormLink("csc.startwizard", buttonContainer, Link.BUTTON);
			wizardButton.setElementCssClass("o_sel_author_create_wizard");
		}

		uifactory.addFormCancelButton("cancel", buttonContainer, ureq, getWindowControl());
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		displaynameEl.clearError();
		if (!StringHelper.containsNonWhitespace(displaynameEl.getValue())) {
			displaynameEl.setErrorKey("cif.error.displayname.empty", new String[] {});
			allOk &= false;
		} else if (displaynameEl.hasError()) {
			allOk &= false;
		}
		
		return allOk & super.validateFormLogic(ureq);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		doCreate();
		fireEvent(ureq, Event.DONE_EVENT);
		fireEvent(ureq, new EntryChangedEvent(addedEntry, getIdentity(), Change.added, "create"));
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(wizardButton == source) {
			if(validateFormLogic(ureq)) {
				doCreate();
				fireEvent(ureq, CREATION_WIZARD);
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	private void doCreate() {
		String displayname = displaynameEl.getValue();
		
		addedEntry = handler.createResource(getIdentity(), displayname, "", getCreateObject(), getLocale());
		if (licenseModule.isEnabled(licenseHandler)) {
			licenseService.createDefaultLicense(addedEntry.getOlatResource(), licenseHandler, getIdentity());
		}

		repositoryManager.triggerIndexer(addedEntry);

		ThreadLocalUserActivityLogger.log(LearningResourceLoggingAction.LEARNING_RESOURCE_CREATE, getClass(),
				LoggingResourceable.wrap(addedEntry, OlatResourceableType.genRepoEntry));
	}
}
