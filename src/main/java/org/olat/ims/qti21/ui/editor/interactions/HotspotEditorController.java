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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.core.commons.services.image.ImageService;
import org.olat.core.commons.services.image.Size;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FileElementEvent;
import org.olat.core.gui.components.htmlheader.jscss.JSAndCSSFormItem;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.ims.qti21.model.IdentifierGenerator;
import org.olat.ims.qti21.model.QTI21QuestionType;
import org.olat.ims.qti21.model.xml.interactions.HotspotAssessmentItemBuilder;
import org.olat.ims.qti21.ui.editor.AssessmentTestEditorController;
import org.olat.ims.qti21.ui.editor.events.AssessmentItemEvent;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ed.ph.jqtiplus.node.expression.operator.Shape;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.graphic.HotspotChoice;
import uk.ac.ed.ph.jqtiplus.types.Identifier;

/**
 * 
 * Initial date: 16.03.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class HotspotEditorController extends FormBasicController {
	
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
	private FormLayoutContainer hotspotsCont;
	private FormLink newCircleButton, newRectButton;
	private MultipleSelectionElement correctHotspotsEl;
	
	private final boolean restrictedEdit;
	private final HotspotAssessmentItemBuilder itemBuilder;
	
	private File itemFile;
	private File rootDirectory;
	private VFSContainer rootContainer;
	
	private File backgroundImage;
	private File initialBackgroundImage;
	
	private List<HotspotWrapper> choiceWrappers = new ArrayList<>();
	
	private final String backgroundMapperUri;
	
	@Autowired
	private ImageService imageService;
	
	public HotspotEditorController(UserRequest ureq, WindowControl wControl, HotspotAssessmentItemBuilder itemBuilder,
			File rootDirectory, VFSContainer rootContainer, File itemFile, boolean restrictedEdit) {
		super(ureq, wControl, LAYOUT_DEFAULT_2_10);
		setTranslator(Util.createPackageTranslator(AssessmentTestEditorController.class, getLocale()));
		this.itemFile = itemFile;
		this.itemBuilder = itemBuilder;
		this.rootDirectory = rootDirectory;
		this.rootContainer = rootContainer;
		this.restrictedEdit = restrictedEdit;
		backgroundMapperUri = registerMapper(ureq, new BackgroundMapper(itemFile));
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormContextHelp("Test editor QTI 2.1 in detail#details_testeditor_fragetypen_hotspot");
		
		titleEl = uifactory.addTextElement("title", "form.imd.title", -1, itemBuilder.getTitle(), formLayout);
		titleEl.setElementCssClass("o_sel_assessment_item_title");
		titleEl.setMandatory(true);
		
		String relativePath = rootDirectory.toPath().relativize(itemFile.toPath().getParent()).toString();
		VFSContainer itemContainer = (VFSContainer)rootContainer.resolve(relativePath);
		
		String question = itemBuilder.getQuestion();
		textEl = uifactory.addRichTextElementForQTI21("desc", "form.imd.descr", question, 8, -1, itemContainer,
				formLayout, ureq.getUserSession(), getWindowControl());
		textEl.addActionListener(FormEvent.ONCLICK);
		
		initialBackgroundImage = getCurrentBackground();
		backgroundEl = uifactory.addFileElement(getWindowControl(), "form.imd.background", "form.imd.background", formLayout);
		backgroundEl.setEnabled(!restrictedEdit);
		if(initialBackgroundImage != null) {
			backgroundEl.setInitialFile(initialBackgroundImage);
		}
		backgroundEl.addActionListener(FormEvent.ONCHANGE);
		backgroundEl.setDeleteEnabled(true);
		backgroundEl.limitToMimeType(mimeTypes, "error.mimetype", new String[]{ mimeTypes.toString() });

		//responses
		String page = velocity_root + "/hotspots.html";
		hotspotsCont = FormLayoutContainer.createCustomFormLayout("answers", getTranslator(), page);
		hotspotsCont.getFormItemComponent().addListener(this);
		hotspotsCont.setLabel("new.spots", null);
		hotspotsCont.setRootForm(mainForm);
		hotspotsCont.contextPut("mapperUri", backgroundMapperUri);
		hotspotsCont.contextPut("restrictedEdit", restrictedEdit);
		JSAndCSSFormItem js = new JSAndCSSFormItem("js", new String[] { "js/jquery/openolat/jquery.drawing.js" });
		formLayout.add(js);
		formLayout.add(hotspotsCont);
		
		newCircleButton = uifactory.addFormLink("new.circle", "new.circle", null, hotspotsCont, Link.BUTTON);
		newCircleButton.setIconLeftCSS("o_icon o_icon-lg o_icon_circle");
		newCircleButton.setVisible(!restrictedEdit);
		newRectButton = uifactory.addFormLink("new.rectangle", "new.rectangle", null, hotspotsCont, Link.BUTTON);
		newRectButton.setIconLeftCSS("o_icon o_icon-lg o_icon_rectangle");
		newRectButton.setVisible(!restrictedEdit);
		
		updateBackground();

		String[] emptyKeys = new String[0];
		correctHotspotsEl = uifactory.addCheckboxesHorizontal("form.imd.correct.spots", formLayout, emptyKeys, emptyKeys);
		correctHotspotsEl.setEnabled(!restrictedEdit);
		correctHotspotsEl.addActionListener(FormEvent.ONCHANGE);
		rebuildWrappersAndCorrectSelection();

		// Submit Button
		FormLayoutContainer buttonsContainer = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		buttonsContainer.setRootForm(mainForm);
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
	public void event(UserRequest ureq, Component source, Event event) {
		if(hotspotsCont.getFormItemComponent() == source) {
			String cmd = event.getCommand();
			if("delete-hotspot".equals(cmd)) {
				doDeleteHotspot(ureq);
			} else if("move-hotspot".equals(cmd)) {
				doMoveHotspot(ureq);
			}
		}
		super.event(ureq, source, event);
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		super.event(ureq, source, event);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(newCircleButton == source) {
			createHotspotChoice(Shape.CIRCLE, "60,60,25");
			updateHotspots(ureq);
		} else if(newRectButton == source) {
			createHotspotChoice(Shape.RECT, "50,50,100,100");
			updateHotspots(ureq);
		} else if(backgroundEl == source) {
			//upload in itemDirectory;
			if(FileElementEvent.DELETE.equals(event.getCommand())) {
				if(backgroundEl.getUploadFile() != null && backgroundEl.getUploadFile() != backgroundEl.getInitialFile()) {
					backgroundEl.reset();
					if(initialBackgroundImage != null) {
						backgroundEl.setInitialFile(initialBackgroundImage);
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
				}
			}
			Size backgroundSize = updateBackground();
			updateHotspots(ureq);
			updateHotspotsPosition(backgroundSize);
		} else if(correctHotspotsEl == source) {
			MultipleSelectionElement correctEl = (MultipleSelectionElement)source;
			Collection<String> correctResponseIds = correctEl.getSelectedKeys();
			doCorrectAnswers(correctResponseIds);
			flc.setDirty(true);
		}
		super.formInnerEvent(ureq, source, event);
	}
	
	private void doMoveHotspot(UserRequest ureq) {
		if(restrictedEdit) return;
		
		String coords = ureq.getParameter("coords");
		String hotspotId = ureq.getParameter("hotspot");
		if(StringHelper.containsNonWhitespace(hotspotId) && StringHelper.containsNonWhitespace(coords)) {
			for(HotspotWrapper choiceWrapper:choiceWrappers) {
				if(choiceWrapper.getIdentifier().equals(hotspotId)) {
					choiceWrapper.setCoords(coords);
				}
			}
		}
	}
	
	private void doDeleteHotspot(UserRequest ureq) {
		if(restrictedEdit) return;
		
		String hotspotId = ureq.getParameter("hotspot");
		HotspotChoice choiceToDelete = itemBuilder.getHotspotChoice(hotspotId);
		if(choiceToDelete != null) {
			itemBuilder.deleteHotspotChoice(choiceToDelete);
			rebuildWrappersAndCorrectSelection();
		}
	}
	
	private void createHotspotChoice(Shape shape, String coords) {
		Identifier identifier = IdentifierGenerator.newNumberAsIdentifier("hc");
		itemBuilder.createHotspotChoice(identifier, shape, coords);
		rebuildWrappersAndCorrectSelection();
	}
	
	private void rebuildWrappersAndCorrectSelection() {
		choiceWrappers.clear();
		
		List<HotspotChoice> choices = itemBuilder.getHotspotChoices();
		String[] keys = new String[choices.size()];
		String[] values = new String[choices.size()];
		for(int i=0; i<choices.size(); i++) {
			HotspotChoice choice = choices.get(i);
			keys[i] = choice.getIdentifier().toString();
			values[i] = Integer.toString(i + 1) + ".";
			choiceWrappers.add(new HotspotWrapper(choice, itemBuilder));
		}
		correctHotspotsEl.setKeysAndValues(keys, values);
		for(int i=0; i<choices.size(); i++) {
			if(itemBuilder.isCorrect(choices.get(i))) {
				correctHotspotsEl.select(keys[i], true);
			}
		}
		hotspotsCont.contextPut("hotspots", choiceWrappers);
	}
	
	private void doCorrectAnswers(Collection<String> correctResponseIds) {
		List<HotspotChoice> choices = itemBuilder.getHotspotChoices();
		for(int i=0; i<choices.size(); i++) {
			HotspotChoice choice = choices.get(i);
			boolean correct = correctResponseIds.contains(choice.getIdentifier().toString());
			itemBuilder.setCorrect(choice, correct);
		}
	}
	
	private Size updateBackground() {
		Size size = null;
		File objectImg = null;
		if(backgroundImage != null) {
			objectImg = backgroundImage;
		} else if(initialBackgroundImage != null) {
			objectImg = initialBackgroundImage;
		}
		
		if(objectImg != null) {
			String filename = objectImg.getName();
			size = imageService.getSize(new LocalFileImpl(objectImg), null);
			hotspotsCont.contextPut("filename", filename);
			if(size != null) {
				if(size.getHeight() > 0) {
					hotspotsCont.contextPut("height", Integer.toString(size.getHeight()));
				} else {
					hotspotsCont.contextRemove("height");
				}
				if(size.getWidth() > 0) {
					hotspotsCont.contextPut("width", Integer.toString(size.getWidth()));
				} else {
					hotspotsCont.contextRemove("width");
				}
			}
		} else {
			hotspotsCont.contextRemove("filename");
		}
		return size;
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		itemBuilder.setTitle(titleEl.getValue());
		//set the question with the text entries
		String questionText = textEl.getRawValue();
		itemBuilder.setQuestion(questionText);
		
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
		updateHotspots(ureq);
		
		fireEvent(ureq, new AssessmentItemEvent(AssessmentItemEvent.ASSESSMENT_ITEM_CHANGED, itemBuilder.getAssessmentItem(), QTI21QuestionType.hotspot));
	}
	
	private void updateHotspots(UserRequest ureq) {
		Map<String,HotspotWrapper> wrapperMap = new HashMap<>();
		for(HotspotWrapper wrapper:choiceWrappers) {
			wrapperMap.put(wrapper.getIdentifier(), wrapper);
		}
		
		for(Enumeration<String> parameterNames = ureq.getHttpReq().getParameterNames(); parameterNames.hasMoreElements(); ) {
			String name = parameterNames.nextElement();
			String value = ureq.getHttpReq().getParameter(name);
			if(name.endsWith("_shape")) {
				String hotspotIdentifier = name.substring(0, name.length() - 6);
				HotspotWrapper spot = wrapperMap.get(hotspotIdentifier);
				if(spot != null) {
					spot.setShape(value);
				}
			} else if(name.endsWith("_coords")) {
				String hotspotIdentifier = name.substring(0, name.length() - 7);
				HotspotWrapper spot = wrapperMap.get(hotspotIdentifier);
				if(spot != null) {
					spot.setCoords(value);
				}
			}
		}
	}
	
	/**
	 * If the image is too small, translate the hotspots to match
	 * approximatively the new image.
	 * 
	 * @param backgroundSize
	 */
	private void updateHotspotsPosition(Size backgroundSize) {
		if(backgroundSize == null || choiceWrappers.isEmpty()) return;
		int width = backgroundSize.getWidth();
		int height = backgroundSize.getHeight();
		if(width <= 0 || height <= 0) return;
		
		for(HotspotWrapper wrapper:choiceWrappers) {
			HotspotChoice choice = wrapper.getChoice();
			if(choice != null) {
				if(Shape.CIRCLE.equals(choice.getShape())) {
					translateCircle(choice.getCoords(), width, height);
				} else if(Shape.RECT.equals(choice.getShape())) {
					translateRect(choice.getCoords(), width, height);
				}
			}
		}	
	}

	private void translateCircle(List<Integer> coords, int width, int height) {
		if(coords.size() != 3) return;
		
		int centerX = coords.get(0);
		int centerY = coords.get(1);
		int radius = coords.get(2);
		
		int translateX = 0;
		int translateY = 0;
		if(centerX > width) {
			translateX = centerX - width;
			if((width - translateX) < radius) {
				translateX = width - radius;
			}
		}
		if(centerY > height) {
			translateY = centerY - height;
			if((height - translateY) < radius) {
				translateY = height - radius;
			}
		}
		if(translateX > 0) {
			coords.set(0, (centerX - translateX));
		}
		if(translateY > 0) {
			coords.set(1, (centerY - translateY));
		}
	}
	
	private void translateRect(List<Integer> coords, int width, int height) {
		if(coords.size() != 4) return;
		
		int leftX = coords.get(0);
		int topY = coords.get(1);
		int rightX = coords.get(2);
		int bottomY = coords.get(3);
		
		int translateX = 0;
		int translateY = 0;
		if(rightX > width) {
			translateX = rightX - width;
			if(translateX > leftX) {
				translateX = leftX;
			}
		}
		if(bottomY > height) {
			translateY = Math.min(topY, bottomY - height);
			if(translateY > topY) {
				translateY = topY;
			}
		}
		if(translateX > 0) {
			coords.set(0, (leftX - translateX));
			coords.set(2, (rightX - translateX));
		}
		if(translateY > 0) {
			coords.set(1, (topY - translateY));
			coords.set(3, (bottomY - translateY));
		}
	}
}
