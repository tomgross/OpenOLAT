package org.olat.group.ui.main;

import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableComponent;
import org.olat.core.gui.components.table.CustomCellRenderer;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.group.BusinessGroupShort;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.jcodec.common.Assert.assertNotNull;

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
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class GroupCellRenderer implements CustomCellRenderer, FlexiCellRenderer {

	@Override
	public void render(Renderer renderer, StringOutput target, Object cellValue, int row,
			FlexiTableComponent source, URLBuilder ubu, Translator translator) {
		if (cellValue instanceof MemberView) {
			render(target, (MemberView) cellValue, translator.getLocale());
		}
	}

	@Override
	public void render(StringOutput sb, Renderer renderer, Object val, Locale locale, int alignment, String action) {
		if (val instanceof MemberView) {
			render(sb, (MemberView) val, locale);
		}
	}
	
	private void render(StringOutput sb, MemberView member, Locale locale) {
		assertNotNull(locale);

		List<BusinessGroupShort> groups = member.getGroups();

		if (groups == null) {
			return;
		}

		List<String> groupNames = new ArrayList<>();
		for (BusinessGroupShort group : groups) {
			if (group.getName() == null) {
				groupNames.add(group.getKey().toString());
			} else {
				groupNames.add(group.getName());
			}
		}

		// Sort group names
		Collator collator = Collator.getInstance(locale);
		groupNames.sort(collator);

		// Concat group names
		for (String groupName : groupNames) {
			sb.append(groupName).append(", ");
		}

		// Remove last ", "
		sb.setLength(sb.length() - 2);
	}
}
