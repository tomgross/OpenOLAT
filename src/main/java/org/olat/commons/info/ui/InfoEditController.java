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

package org.olat.commons.info.ui;

import java.io.File;
import java.util.Date;

import org.olat.commons.info.manager.InfoMessageFrontendManager;
import org.olat.commons.info.model.InfoMessage;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.logging.activity.CourseLoggingAction;
import org.olat.core.logging.activity.OlatResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * 
 * Description:<br>
 * TODO: srosse Class Description for InfoEditController
 * 
 * <P>
 * Initial Date:  24 aug. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoEditController extends FormBasicController {
	
	private final InfoMessage messageToEdit;
	private final InfoEditFormController editForm;
	private final InfoMessageFrontendManager infoFrontendManager;

	public InfoEditController(UserRequest ureq, WindowControl wControl, InfoMessage messageToEdit) {
		super(ureq, wControl, "edit");
		
		this.messageToEdit = messageToEdit;
		infoFrontendManager = CoreSpringFactory.getImpl(InfoMessageFrontendManager.class);
		editForm = new InfoEditFormController(ureq, wControl, mainForm, false);
		editForm.setTitle(messageToEdit.getTitle());
		editForm.setMessage(messageToEdit.getMessage());
		editForm.setAttachments(messageToEdit.getAttachments());
		listenTo(editForm);
		
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		FormLayoutContainer editCont = editForm.getInitialFormItem();

		final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("button_layout", getTranslator());
		editCont.add(buttonLayout);
		uifactory.addFormSubmitButton("submit", buttonLayout);
		uifactory.addFormCancelButton("cancel", buttonLayout, ureq, getWindowControl());
		
		flc.add("edit", editCont);
	}
	
	@Override
	protected void doDispose() {
		//
	}
	
	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		return editForm.validateFormLogic(ureq);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		String title = editForm.getTitle();
		String message = editForm.getMessage();
		
		messageToEdit.setTitle(title);
		messageToEdit.setMessage(message);
		messageToEdit.setModificationDate(new Date());
		messageToEdit.setModifier(getIdentity());
		for (File attachment : editForm.getAttachments()) {
			// If form was opened with some existing attachments and no additional attachments were uploaded,
			//  then temp dir is not yet set
			File attachmentTempDir = editForm.getAttachementTempDir();
			if (attachmentTempDir != null && attachment.getAbsolutePath().startsWith(attachmentTempDir.getAbsolutePath())) {
				// copy new file to the media folder
				if (!messageToEdit.copyAttachmentToMediaFolder(attachment)) {
					getLogger().warn("Failed to copy attachment into media folder: " + attachment.getName());
				}
			}
		}
		messageToEdit.setAttachments(editForm.getAttachments());
		infoFrontendManager.sendInfoMessage(messageToEdit, null, null, ureq.getIdentity(), null);
		
		ThreadLocalUserActivityLogger.log(CourseLoggingAction.INFO_MESSAGE_UPDATED, getClass(),
				LoggingResourceable.wrap(messageToEdit.getOLATResourceable(), OlatResourceableType.infoMessage));

		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
}