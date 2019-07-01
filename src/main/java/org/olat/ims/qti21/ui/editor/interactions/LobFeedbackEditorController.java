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

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.richText.RichTextConfiguration;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.ims.qti21.model.xml.ModalFeedbackBuilder;
import org.olat.ims.qti21.model.xml.interactions.LobAssessmentItemBuilder;
import org.olat.ims.qti21.ui.editor.FeedbackEditorController;
import org.olat.ims.qti21.ui.editor.events.AssessmentItemEvent;

/**
 * 
 * Initial date: 09.08.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LobFeedbackEditorController extends FormBasicController {
	
	private TextElement hintTitleEl;
	private RichTextElement hintTextEl;
	private TextElement feedbackCorrectSolutionTitleEl;
	private RichTextElement feedbackCorrectSolutionTextEl;
	private TextElement feedbackTitleEl, feedbackEmptyTitleEl;
	private RichTextElement feedbackTextEl, feedbackEmptyTextEl;

	private final File itemFile;
	private final File rootDirectory;
	private final VFSContainer rootContainer;
	private final boolean restrictedEdit;
	private final LobAssessmentItemBuilder itemBuilder;
	
	public LobFeedbackEditorController(UserRequest ureq, WindowControl wControl, LobAssessmentItemBuilder itemBuilder,
			File rootDirectory, VFSContainer rootContainer, File itemFile, boolean restrictedEdit) {
		super(ureq, wControl, LAYOUT_DEFAULT_2_10);
		setTranslator(Util.createPackageTranslator(FeedbackEditorController.class, getLocale(), getTranslator()));
		this.itemBuilder = itemBuilder;
		this.restrictedEdit = restrictedEdit;
		this.itemFile = itemFile;
		this.rootDirectory = rootDirectory;
		this.rootContainer = rootContainer;
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormContextHelp("Test editor QTI 2.1 in detail#details_testeditor_feedback");
		formLayout.setElementCssClass("o_sel_assessment_item_feedbacks");

		String relativePath = rootDirectory.toPath().relativize(itemFile.toPath().getParent()).toString();
		VFSContainer itemContainer = (VFSContainer)rootContainer.resolve(relativePath);

		{
			ModalFeedbackBuilder hint = itemBuilder.getHint();
			String hintTitle = hint == null ? "" : hint.getTitle();
			hintTitleEl = uifactory.addTextElement("hintTitle", "form.imd.hint.title", -1, hintTitle, formLayout);
			hintTitleEl.setElementCssClass("o_sel_assessment_item_hint_title");
			hintTitleEl.setUserObject(hint);
			hintTitleEl.setEnabled(!restrictedEdit);
			String hintText = hint == null ? "" : hint.getText();
			hintTextEl = uifactory.addRichTextElementForQTI21("hintText", "form.imd.hint.text", hintText, 8, -1,
					itemContainer, formLayout, ureq.getUserSession(), getWindowControl());
			hintTextEl.setElementCssClass("o_sel_assessment_item_hint");
			hintTextEl.setEnabled(!restrictedEdit);
			hintTextEl.setHelpTextKey("feedback.hint.help", null);
			hintTextEl.setHelpUrlForManualPage("Test editor QTI 2.1 in detail#details_testeditor_feedback");
			RichTextConfiguration hintConfig = hintTextEl.getEditorConfiguration();
			hintConfig.setFileBrowserUploadRelPath("media");// set upload dir to the media dir
		}
		
		{//correct solution feedback for Word
			ModalFeedbackBuilder correctSolutionFeedback = itemBuilder.getCorrectSolutionFeedback();
			String correctSolutionTitle = correctSolutionFeedback == null ? "" : correctSolutionFeedback.getTitle();
			feedbackCorrectSolutionTitleEl = uifactory.addTextElement("correctSolutionTitle", "form.imd.correct.solution.title", -1, correctSolutionTitle, formLayout);
			feedbackCorrectSolutionTitleEl.setUserObject(correctSolutionFeedback);
			feedbackCorrectSolutionTitleEl.setEnabled(!restrictedEdit);
			feedbackCorrectSolutionTitleEl.setElementCssClass("o_sel_assessment_item_correct_solution_title");
			String correctSolutionText = correctSolutionFeedback == null ? "" : correctSolutionFeedback.getText();
			feedbackCorrectSolutionTextEl = uifactory.addRichTextElementForQTI21("correctSolutionText", "form.imd.correct.solution.text.word", correctSolutionText, 8, -1,
					itemContainer, formLayout, ureq.getUserSession(), getWindowControl());
			feedbackCorrectSolutionTextEl.setEnabled(!restrictedEdit);
			feedbackCorrectSolutionTextEl.setHelpTextKey("feedback.correctsolution.help", null);
			feedbackCorrectSolutionTextEl.setHelpUrlForManualPage("Test editor QTI 2.1 in detail#details_testeditor_feedback");
			feedbackCorrectSolutionTextEl.setElementCssClass("o_sel_assessment_item_correct_solution");
			RichTextConfiguration richTextConfig2 = feedbackCorrectSolutionTextEl.getEditorConfiguration();
			richTextConfig2.setFileBrowserUploadRelPath("media");// set upload dir to the media dir
		}
		
		{//feedback if response
			ModalFeedbackBuilder answeredFeedback = itemBuilder.getAnsweredFeedback();
			String correctTitle = answeredFeedback == null ? "" : answeredFeedback.getTitle();
			feedbackTitleEl = uifactory.addTextElement("answeredTitle", "form.imd.answered.title", -1, correctTitle, formLayout);
			feedbackTitleEl.setUserObject(answeredFeedback);
			feedbackTitleEl.setEnabled(!restrictedEdit);
			feedbackTitleEl.setElementCssClass("o_sel_assessment_item_answered_feedback_title");
			String correctText = answeredFeedback == null ? "" : answeredFeedback.getText();
			feedbackTextEl = uifactory.addRichTextElementForQTI21("answeredText", "form.imd.answered.text", correctText, 8, -1,
					itemContainer, formLayout, ureq.getUserSession(), getWindowControl());
			feedbackTextEl.setEnabled(!restrictedEdit);
			feedbackTextEl.setElementCssClass("o_sel_assessment_item_answered_feedback");
			RichTextConfiguration richTextConfig = feedbackTextEl.getEditorConfiguration();
			richTextConfig.setFileBrowserUploadRelPath("media");// set upload dir to the media dir
		}
		
		{// feedback if the answer is empty
			ModalFeedbackBuilder emptyFeedback = itemBuilder.getEmptyFeedback();
			String emptyTitle = emptyFeedback == null ? "" : emptyFeedback.getTitle();
			feedbackEmptyTitleEl = uifactory.addTextElement("emptyTitle", "form.imd.empty.title", -1, emptyTitle, formLayout);
			feedbackEmptyTitleEl.setUserObject(emptyFeedback);
			feedbackEmptyTitleEl.setEnabled(!restrictedEdit);
			feedbackEmptyTitleEl.setElementCssClass("o_sel_assessment_item_empty_feedback_title");
			String emptyText = emptyFeedback == null ? "" : emptyFeedback.getText();
			feedbackEmptyTextEl = uifactory.addRichTextElementForQTI21("emptyText", "form.imd.empty.text", emptyText, 8, -1,
					itemContainer, formLayout, ureq.getUserSession(), getWindowControl());
			feedbackEmptyTextEl.setEnabled(!restrictedEdit);
			feedbackEmptyTextEl.setElementCssClass("o_sel_assessment_item_empty_feedback");
			RichTextConfiguration emptyTextConfig = feedbackEmptyTextEl.getEditorConfiguration();
			emptyTextConfig.setFileBrowserUploadRelPath("media");// set upload dir to the media dir
		}
	
		// Submit Button
		if(!restrictedEdit) {
			FormLayoutContainer buttonsContainer = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
			buttonsContainer.setRootForm(mainForm);
			formLayout.add(buttonsContainer);
			uifactory.addFormSubmitButton("submit", buttonsContainer);
		}
	}

	@Override
	protected void formOK(UserRequest ureq) {
		if(restrictedEdit) return;
		
		String hintTitle = hintTitleEl.getValue();
		String hintText = hintTextEl.getRawValue();
		if(StringHelper.containsNonWhitespace(FilterFactory.getHtmlTagsFilter().filter(hintText))) {
			ModalFeedbackBuilder hintBuilder = itemBuilder.getHint();
			if(hintBuilder == null) {
				hintBuilder = itemBuilder.createHint();
			}
			hintBuilder.setTitle(hintTitle);
			hintBuilder.setText(hintText);
		} else {
			itemBuilder.removeHint();
		}
		
		String correctSolutionTitle = feedbackCorrectSolutionTitleEl.getValue();
		String correctSolutionText = feedbackCorrectSolutionTextEl.getRawValue();
		if(StringHelper.containsNonWhitespace(FilterFactory.getHtmlTagsFilter().filter(correctSolutionText))) {
			ModalFeedbackBuilder correctSolutionBuilder = itemBuilder.getCorrectSolutionFeedback();
			if(correctSolutionBuilder == null) {
				correctSolutionBuilder = itemBuilder.createCorrectSolutionFeedback();
			}
			correctSolutionBuilder.setTitle(correctSolutionTitle);
			correctSolutionBuilder.setText(correctSolutionText);
		} else {
			itemBuilder.removeCorrectSolutionFeedback();
		}
		
		String correctTitle = feedbackTitleEl.getValue();
		String correctText = feedbackTextEl.getRawValue();
		if(StringHelper.containsNonWhitespace(FilterFactory.getHtmlTagsFilter().filter(correctText))) {
			ModalFeedbackBuilder correctBuilder = itemBuilder.getAnsweredFeedback();
			if(correctBuilder == null) {
				correctBuilder = itemBuilder.createAnsweredFeedback();
			}
			correctBuilder.setTitle(correctTitle);
			correctBuilder.setText(correctText);
		} else {
			itemBuilder.removeAnsweredFeedback();
		}

		String emptyTitle = feedbackEmptyTitleEl.getValue();
		String emptyText = feedbackEmptyTextEl.getRawValue();
		if(StringHelper.containsNonWhitespace(FilterFactory.getHtmlTagsFilter().filter(emptyText))) {
			ModalFeedbackBuilder emptyBuilder = itemBuilder.getEmptyFeedback();
			if(emptyBuilder == null) {
				emptyBuilder = itemBuilder.createEmptyFeedback();
			}
			emptyBuilder.setTitle(emptyTitle);
			emptyBuilder.setText(emptyText);
		} else {
			itemBuilder.removeEmptyFeedback();
		}

		fireEvent(ureq, new AssessmentItemEvent(AssessmentItemEvent.ASSESSMENT_ITEM_CHANGED, itemBuilder.getAssessmentItem()));
	}
	

	@Override
	protected void doDispose() {
		//
	}
}
