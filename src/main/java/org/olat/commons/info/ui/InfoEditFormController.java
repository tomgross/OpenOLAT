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

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.FileUploadController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.mail.MailModule;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  26 jul. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoEditFormController extends FormBasicController {

	private TextElement title;
	private RichTextElement message;
	private final boolean showTitle;
	private FileElement attachmentEl;
	private int contactAttachmentMaxSizeInMb = 5;
	private List<FormLink> attachmentLinks = new ArrayList<FormLink>();
	private File attachementTempDir;
	private long attachmentSize = 0l;
	private Map<String,String> attachmentCss = new HashMap<String,String>();
	private Map<String,String> attachmentNames = new HashMap<String,String>();
	private FormLayoutContainer uploadCont;

	public InfoEditFormController(UserRequest ureq, WindowControl wControl, Form mainForm, boolean showTitle) {
		super(ureq, wControl, LAYOUT_DEFAULT, null, mainForm);
		this.showTitle = showTitle;
		this.contactAttachmentMaxSizeInMb = CoreSpringFactory.getImpl(MailModule.class).getMaxSizeForAttachement();
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		formLayout.setElementCssClass("o_sel_info_form");
		if (showTitle) {
			setFormTitle("edit.title");
		}
		
		title = uifactory.addTextElement("info_title", "edit.info_title", 512, "", formLayout);
		title.setElementCssClass("o_sel_info_title");
		title.setMandatory(true);
		
		message = uifactory.addRichTextElementForStringDataMinimalistic("edit.info_message", "edit.info_message", "", 6, 80,
				formLayout, getWindowControl());
		message.getEditorConfiguration().setRelativeUrls(false);
		message.getEditorConfiguration().setRemoveScriptHost(false);
		message.setMandatory(true);
		message.setMaxLength(2000);

		String VELOCITY_ROOT = Util.getPackageVelocityRoot(this.getClass());
		uploadCont = FormLayoutContainer.createCustomFormLayout("file_upload_inner", getTranslator(), VELOCITY_ROOT + "/attachments.html");
		uploadCont.setRootForm(mainForm);
		formLayout.add(uploadCont);

		attachmentEl = uifactory.addFileElement(getWindowControl(), "file_upload_1", "edit.info_attachment", formLayout);
		attachmentEl.setLabel("edit.info_attachment", null);
		attachmentEl.addActionListener(FormEvent.ONCHANGE);
		attachmentEl.setExampleKey("edit.info_attachment.maxsize", new String[]{Integer.toString(contactAttachmentMaxSizeInMb)});

	}
	
	@Override
	protected void doDispose() {
		cleanUpAttachments();
	}
	
	public String getTitle() {
		return title.getValue();
	}
	
	public void setTitle(String titleStr) {
		title.setValue(titleStr);
	}
	
	public String getMessage() {
		return message.getValue();
	}
	
	public void setMessage(String messageStr) {
		message.setValue(messageStr);
	}

	public File getAttachementTempDir() {
		return attachementTempDir;
	}

	public File[] getAttachments() {
		List<File> attachments = new ArrayList<File>();
		for (FormLink removeLink : attachmentLinks) {
			attachments.add((File)removeLink.getUserObject());
		}
		return attachments.toArray(new File[attachments.size()]);
	}

	public void setAttachments(File[] attachments) {
		if (attachments != null) {
			for (File attachment : attachments) {
				addAttachment(attachment.getName(), attachment.length(), attachment);
			}
		}
	}

	public void cleanUpAttachments() {
		if(attachementTempDir != null && attachementTempDir.exists()) {
			FileUtils.deleteDirsAndFiles(attachementTempDir, true, true);
			attachementTempDir = null;
		}
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}
	
	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		title.clearError();
		message.clearError();
		boolean allOk = true;
		
		String t = title.getValue();
		if(!StringHelper.containsNonWhitespace(t)) {
			title.setErrorKey("form.legende.mandatory", new String[] {});
			allOk = false;
		} else if (t.length() > 500) {
			title.setErrorKey("input.toolong", new String[] {"500", Integer.toString(t.length())});
			allOk = false;
		}
		
		String m = message.getValue();
		if(!StringHelper.containsNonWhitespace(m)) {
			message.setErrorKey("form.legende.mandatory", new String[] {});
			allOk = false;
		} else if (m.length() > 2000) {
			message.setErrorKey("input.toolong", new String[] {"2000", Integer.toString(m.length())});
			allOk = false;
		}
		
		return allOk && super.validateFormLogic(ureq);
	}

	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if (source == attachmentEl) {
			String filename = attachmentEl.getUploadFileName();
			if (attachementTempDir == null) {
				attachementTempDir = FileUtils.createTempDir("attachements", null, null);
			}

			long size = attachmentEl.getUploadSize();
			if (size + attachmentSize > (contactAttachmentMaxSizeInMb  * 1024 * 1024)) {
				showWarning("edit.info_attachment.maxsize", Integer.toString(contactAttachmentMaxSizeInMb));
				attachmentEl.reset();
			} else {
				File attachment = attachmentEl.moveUploadFileTo(attachementTempDir);
				if (attachment == null){
					attachmentEl.reset();
					logError("Could not move contact-form attachment to " + attachementTempDir.getAbsolutePath(), null);
					setTranslator(Util.createPackageTranslator(FileUploadController.class, getLocale(),getTranslator()));
					showError("FileMoveCopyFailed","");
					return;
				}
				attachmentEl.reset();
				addAttachment(filename, size, attachment);
				attachmentEl.setLabel(null, null);
			}
		} else if (attachmentLinks.contains(source)) {
			File uploadedFile = (File)source.getUserObject();
			if(uploadedFile != null && uploadedFile.exists()) {
				attachmentSize -= uploadedFile.length();
				uploadedFile.delete();
			}
			attachmentLinks.remove(source);
			uploadCont.remove(source);
			if(attachmentLinks.isEmpty()) {
				uploadCont.setLabel(null, null);
				attachmentEl.setLabel("edit.info_attachment", null);
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	private void addAttachment(String filename, long size, File attachment) {
		attachmentSize += size;
		FormLink removeFile = uifactory.addFormLink(attachment.getName(), "delete", null, uploadCont, Link.BUTTON_SMALL);
		removeFile.setUserObject(attachment);
		attachmentLinks.add(removeFile);
		//pretty labels
		uploadCont.setLabel("edit.info_attachment", null);
		attachmentNames.put(attachment.getName(), filename);
		attachmentCss.put(attachment.getName(), CSSHelper.createFiletypeIconCssClassFor(filename));
		uploadCont.contextPut("attachments", attachmentLinks);
		uploadCont.contextPut("attachmentNames", attachmentNames);
		uploadCont.contextPut("attachmentCss", attachmentCss);
	}

	public FormLayoutContainer getInitialFormItem() {
		return flc;
	}

}
