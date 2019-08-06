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
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.modules.forms.model.xml.Rubric;
import org.olat.modules.forms.model.xml.Rubric.SliderType;
import org.olat.modules.forms.model.xml.Slider;
import org.olat.modules.forms.ui.RubricController;
import org.olat.modules.forms.ui.RubricEditorController;
import org.olat.modules.portfolio.ui.editor.PageRunControllerElement;
import org.olat.modules.portfolio.ui.editor.PageElement;
import org.olat.modules.portfolio.ui.editor.PageElementHandler;
import org.olat.modules.portfolio.ui.editor.PageElementRenderingHints;
import org.olat.modules.portfolio.ui.editor.PageRunElement;
import org.olat.modules.portfolio.ui.editor.SimpleAddPageElementHandler;

/**
 * 
 * Initial date: 7 déc. 2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class RubricHandler implements PageElementHandler, SimpleAddPageElementHandler {
	
	private final boolean restrictedEdit;
	
	public RubricHandler(boolean restrictedEdit) {
		this.restrictedEdit = restrictedEdit;
	}

	@Override
	public String getType() {
		return "formrubric";
	}

	@Override
	public String getIconCssClass() {
		return "o_icon_rubric";
	}

	@Override
	public PageRunElement getContent(UserRequest ureq, WindowControl wControl, PageElement element, PageElementRenderingHints hints) {
		if(element instanceof Rubric) {
			Controller ctrl = new RubricController(ureq, wControl, (Rubric)element);
			return new PageRunControllerElement(ctrl);
		}
		return null;
	}

	@Override
	public Controller getEditor(UserRequest ureq, WindowControl wControl, PageElement element) {
		if(element instanceof Rubric) {
			return new RubricEditorController(ureq, wControl, (Rubric)element, restrictedEdit);
		}
		return null;
	}
	
	@Override
	public PageElement createPageElement(Locale locale) {
		Rubric rubric = new Rubric();
		rubric.setId(UUID.randomUUID().toString());
		rubric.setStart(1);
		rubric.setEnd(5);
		rubric.setSteps(5);
		rubric.setSliderType(SliderType.discrete);
		
		Slider slider = new Slider();
		slider.setId(UUID.randomUUID().toString());
		slider.setStartLabel("Start");
		rubric.getSliders().add(slider);
		return rubric;
	}
}
