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
package org.olat.ims.qti21.ui.editor.interactions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.olat.core.commons.services.image.ImageService;
import org.olat.core.commons.services.image.Size;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FileElementEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.ims.qti21.model.QTI21QuestionType;
import org.olat.ims.qti21.model.xml.interactions.DrawingAssessmentItemBuilder;
import org.olat.ims.qti21.ui.editor.AssessmentTestEditorController;
import org.olat.ims.qti21.ui.editor.events.AssessmentItemEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 08.01.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class DrawingEditorController extends FormBasicController {
	
	private static final Set<String> mimeTypes = new HashSet<>();
	static {
		mimeTypes.add("image/gif");
		mimeTypes.add("image/jpg");
		mimeTypes.add("image/jpeg");
		mimeTypes.add("image/png");
	}
	
	private TextElement titleEl;
	private RichTextElement textEl;
	private FileElement backgroundEl;
	
	private final File itemFile;
	private final File rootDirectory;
	private final VFSContainer rootContainer;
	
	private File backgroundImage;
	private File initialBackgroundImage;
	
	private final boolean restrictedEdit;
	private final DrawingAssessmentItemBuilder itemBuilder;

	@Autowired
	private ImageService imageService;
	
	public DrawingEditorController(UserRequest ureq, WindowControl wControl, DrawingAssessmentItemBuilder itemBuilder,
			File rootDirectory, VFSContainer rootContainer, File itemFile, boolean restrictedEdit) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(AssessmentTestEditorController.class, getLocale()));
		this.itemFile = itemFile;
		this.itemBuilder = itemBuilder;
		this.rootDirectory = rootDirectory;
		this.rootContainer = rootContainer;
		this.restrictedEdit = restrictedEdit;
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormContextHelp("Test editor QTI 2.1 in detail#details_testeditor_fragetypen_drawing");
		
		titleEl = uifactory.addTextElement("title", "form.imd.title", -1, itemBuilder.getTitle(), formLayout);
		titleEl.setElementCssClass("o_sel_assessment_item_title");
		titleEl.setMandatory(true);
		
		String relativePath = rootDirectory.toPath().relativize(itemFile.toPath().getParent()).toString();
		VFSContainer itemContainer = (VFSContainer)rootContainer.resolve(relativePath);

		String description = itemBuilder.getQuestion();
		textEl = uifactory.addRichTextElementForQTI21("desc", "form.imd.descr", description, 12, -1, itemContainer,
				formLayout, ureq.getUserSession(), getWindowControl());
		textEl.setElementCssClass("o_sel_assessment_item_question");
		
		initialBackgroundImage = getCurrentBackground();
		backgroundEl = uifactory.addFileElement(getWindowControl(), "form.imd.background", "form.imd.background", formLayout);
		backgroundEl.setPreview(ureq.getUserSession(), true);
		backgroundEl.setEnabled(!restrictedEdit);
		if(initialBackgroundImage != null) {
			backgroundEl.setInitialFile(initialBackgroundImage);
		}
		backgroundEl.addActionListener(FormEvent.ONCHANGE);
		backgroundEl.setDeleteEnabled(true);
		backgroundEl.limitToMimeType(mimeTypes, "error.mimetype", new String[]{ mimeTypes.toString() });
		

		// Submit Button
		FormLayoutContainer buttonsContainer = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		buttonsContainer.setRootForm(mainForm);
		buttonsContainer.setElementCssClass("o_sel_lob_save");
		formLayout.add(buttonsContainer);
		uifactory.addFormSubmitButton("submit", buttonsContainer);
	}
	
	private File getCurrentBackground() {
		if(StringHelper.containsNonWhitespace(itemBuilder.getBackground())) {
			File itemDirectory = itemFile.getParentFile();
			Path backgroundPath = itemDirectory.toPath().resolve(itemBuilder.getBackground());
			if(Files.exists(backgroundPath)) {
				return backgroundPath.toFile();
			}
		}
		return null;
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;

		titleEl.clearError();
		if(!StringHelper.containsNonWhitespace(titleEl.getValue())) {
			titleEl.setErrorKey("form.legende.mandatory", null);
			allOk &= false;
		}
		
		backgroundEl.clearError();
		if(backgroundImage == null && initialBackgroundImage == null) {
			backgroundEl.setErrorKey("form.legende.mandatory", null);
			allOk &= false;
		} else {
			List<ValidationStatus> status = new ArrayList<>();
			backgroundEl.validate(status);
			allOk &= status.isEmpty();
		}

		return allOk & super.validateFormLogic(ureq);
	}
	
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(backgroundEl == source) {
			//upload in itemDirectory;
			if(FileElementEvent.DELETE.equals(event.getCommand())) {
				if(backgroundEl.getUploadFile() != null && backgroundEl.getUploadFile() != backgroundEl.getInitialFile()) {
					backgroundEl.reset();
					if(initialBackgroundImage != null) {
						backgroundEl.setInitialFile(initialBackgroundImage);
						backgroundImage = null;
					} else {
						backgroundEl.setInitialFile(null);
						backgroundImage = null;
					}
				} else if(initialBackgroundImage != null) {
					initialBackgroundImage = null;
					backgroundEl.setInitialFile(null);
				}
				flc.setDirty(true);
			} else if (backgroundEl.isUploadSuccess()) {
				List<ValidationStatus> status = new ArrayList<>();
				backgroundEl.validate(status);
				if(status.isEmpty()) {
					flc.setDirty(true);
					backgroundImage = backgroundEl.moveUploadFileTo(itemFile.getParentFile());
					backgroundEl.setInitialFile(backgroundImage);
				}
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//title
		itemBuilder.setTitle(titleEl.getValue());
		
		File objectImg = null;
		if(backgroundImage != null) {
			objectImg = backgroundImage;
		} else if(initialBackgroundImage != null) {
			objectImg = initialBackgroundImage;
		}
		
		if(objectImg != null) {
			String filename = objectImg.getName();
			String mimeType = WebappHelper.getMimeType(filename);
			Size size = imageService.getSize(new LocalFileImpl(objectImg), null);
			int height = -1;
			int width = -1;
			if(size != null) {
				height = size.getHeight();
				width = size.getWidth();
			}
			itemBuilder.setBackground(filename, mimeType, height, width);
		}

		//question
		String questionText = textEl.getRawValue();
		itemBuilder.setQuestion(questionText);
		
		fireEvent(ureq, new AssessmentItemEvent(AssessmentItemEvent.ASSESSMENT_ITEM_CHANGED, itemBuilder.getAssessmentItem(), QTI21QuestionType.drawing));
	}
}