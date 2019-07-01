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
package org.olat.modules.qpool.ui;

import java.util.List;

import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiTableDataSourceModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataSourceDelegate;
import org.olat.core.gui.translator.Translator;
import org.olat.modules.qpool.QuestionStatus;
import org.olat.modules.qpool.ui.metadata.MetaUIFactory;

/**
 * 
 * Initial date: 23.01.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QuestionItemDataModel extends DefaultFlexiTableDataSourceModel<ItemRow> {

	private final Translator translator;
	
	public QuestionItemDataModel(FlexiTableColumnModel columnModel, FlexiTableDataSourceDelegate<ItemRow> source, Translator translator) {
		super(source, columnModel);
		this.translator = translator;
	}
	
	public ItemRow getObjectByKey(Long key) {
		List<ItemRow> rows = getObjects();
		for(ItemRow row:rows) {
			if(row != null && row.getKey().equals(key)) {
				return row;
			}
		}
		return null;
	}
	
	@Override
	public QuestionItemDataModel createCopyWithEmptyList() {
		return new QuestionItemDataModel(getTableColumnModel(), getSourceDelegate(), translator);
	}

	@Override
	public Object getValueAt(int row, int col) {
		ItemRow item = getObject(row);
		if(item == null) {
			return null;//don't break here
		}
		
		switch(Cols.values()[col]) {
			case key: return item.getKey();
			case identifier: return item.getIdentifier();
			case masterIdentifier: return item.getMasterIdentifier();
			case title: return item.getTitle();
			case keywords: return item.getKeywords();
			case coverage: return item.getCoverage();
			case additionalInfos: return item.getAdditionalInformations();
			case creationDate: return item.getCreationDate();
			case lastModified: return item.getLastModified();
			case taxnonomyLevel: return item.getTaxonomyLevelName();
			case difficulty: return MetaUIFactory.bigDToString(item.getDifficulty());
			case stdevDifficulty: return MetaUIFactory.bigDToString(item.getStdevDifficulty());
			case differentiation: return MetaUIFactory.bigDToString(item.getDifferentiation());
			case numOfAnswerAlternatives:
				return item.getNumOfAnswerAlternatives() > 0 ? Integer.toString(item.getNumOfAnswerAlternatives()) : "";
			// Disabled as a quick fix for LMSUZH-671
			//case usage:
			//	return item.getUsage() > 0 ? Integer.toString(item.getUsage()) : "";
			case type: {
				String type = item.getItemType();
				if(type == null) {
					return "";
				}
				return type;
			}
			case rating: return item.getRating();
			case format: return item.getFormat();
			case itemVersion: return item.getItemVersion();
			case status: {
				QuestionStatus s = item.getQuestionStatus();
				if(s == null) {
					return "";
				}
				return translator.translate("lifecycle.status." + s.name());
			}
			case editable: return item.isEditable() ? Boolean.TRUE : Boolean.FALSE;
			case mark: return item.getMarkLink();
			default: return "-";
		}
	}
	
	public enum Cols {
		key("general.key"),
		identifier("general.identifier"),
		masterIdentifier("general.master.identifier"),
		title("general.title"),
		keywords("general.keywords"),
		coverage("general.coverage"),
		additionalInfos("general.additional.informations"),
		creationDate("technical.creation"),
		lastModified("technical.lastModified"),
		taxnonomyLevel("classification.taxonomy.level"),
		difficulty("question.difficulty"),
		stdevDifficulty("question.stdevDifficulty"),
		differentiation("question.differentiation"),
		numOfAnswerAlternatives("question.numOfAnswerAlternatives"),
		//usage("question.usage"), // Disabled as a quick fix for LMSUZH-671
		type("question.type"),
		format("technical.format"),
		rating("rating"),
		itemVersion("lifecycle.version"),
		status("lifecycle.status"),
		editable("editable"),
		mark("mark");
		
		private final String i18nKey;
	
		private Cols(String i18nKey) {
			this.i18nKey = i18nKey;
		}
		
		public String i18nKey() {
			return i18nKey;
		}
	}
}