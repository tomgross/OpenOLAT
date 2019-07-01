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
package org.olat.repository.ui.list;

import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiTableDataSourceModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;

/**
 * 
 * Initial date: 29.01.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
class RepositoryEntryDataModel extends DefaultFlexiTableDataSourceModel<RepositoryEntryRow> {
	
	public RepositoryEntryDataModel(DefaultRepositoryEntryDataSource source, FlexiTableColumnModel columnModel) {
		super(source, columnModel);
	}

	@Override
	public void clear() {
		super.clear();
		getSourceDelegate().resetCount();
	}

	@Override
	public DefaultFlexiTableDataSourceModel<RepositoryEntryRow> createCopyWithEmptyList() {
		return new RepositoryEntryDataModel(getSourceDelegate(), getTableColumnModel());
	}

	@Override
	public DefaultRepositoryEntryDataSource getSourceDelegate() {
		return (DefaultRepositoryEntryDataSource)super.getSourceDelegate();
	}

	@Override
	public Object getValueAt(int row, int col) {
		RepositoryEntryRow item = getObject(row);
		if(item == null) {
			return null;//don't break here
		}
		return item.getValueAt(col);
	}
}
