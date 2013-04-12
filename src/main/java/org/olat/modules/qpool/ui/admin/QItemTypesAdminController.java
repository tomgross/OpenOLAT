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
package org.olat.modules.qpool.ui.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.BooleanCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableRendererType;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.TextFlexiCellRenderer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.table.TableDataModel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nItem;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;
import org.olat.core.util.i18n.ui.TranslationToolI18nItemEditCrumbController;
import org.olat.core.util.prefs.Preferences;
import org.olat.modules.qpool.QPoolService;
import org.olat.modules.qpool.model.QItemType;
import org.olat.modules.qpool.ui.MetadatasController;

/**
 * 
 * Manage the list of types
 * 
 * Initial date: 21.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QItemTypesAdminController extends FormBasicController {
	
	private FormLink createType;
	
	private QItemTypeDataModel model;
	private FlexiTableElement tableEl;

	private CloseableModalController cmc;
	private QItemTypeEditController editCtrl;
	private DialogBoxController confirmDeleteCtrl;
	private TranslationToolI18nItemEditCrumbController i18nItemEditCtr;
	
	private final QPoolService qpoolService;
	
	public QItemTypesAdminController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl, "types_admin");

		setTranslator(Util.createPackageTranslator(MetadatasController.class, ureq.getLocale(), getTranslator()));
		
		qpoolService = CoreSpringFactory.getImpl(QPoolService.class);
		initForm(ureq);
		reloadModel();
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		//add the table
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.id.i18nKey(), Cols.id.ordinal(), true, "key"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.type.i18nKey(), Cols.type.ordinal(), true, "name"));
		FlexiCellRenderer renderer = new StaticFlexiCellRenderer("translate", new TextFlexiCellRenderer());
		columnsModel.addFlexiColumnModel(new StaticFlexiColumnModel(Cols.typeI18n.i18nKey(), Cols.typeI18n.ordinal(), "translate", renderer));
		FlexiCellRenderer delRenderer = new BooleanCellRenderer(new StaticFlexiCellRenderer(translate("delete"), "delete-type"), null);
		columnsModel.addFlexiColumnModel(new StaticFlexiColumnModel("delete", Cols.deletable.ordinal(), "delete-type", delRenderer));

		model = new QItemTypeDataModel(columnsModel);
		tableEl = uifactory.addTableElement(ureq, "types", model, null, 20, false, getTranslator(), formLayout);
		tableEl.setRendererType(FlexiTableRendererType.classic);
		
		createType = uifactory.addFormLink("create.type", formLayout, Link.BUTTON);
	}
	
	private void reloadModel() {
		List<QItemType> rows = qpoolService.getAllItemTypes();
		model.setObjects(rows);
		tableEl.reset();	
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == createType) {
			doEdit(ureq, null);
		} else if(source == tableEl) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				if("delete-type".equals(se.getCommand())) {
					QItemType row = model.getObject(se.getIndex());
					doConfirmDelete(ureq, row);
				} else if("translate".equals(se.getCommand())) {
					QItemType row = model.getObject(se.getIndex());
					doOpenTranslationTool(ureq, row);
				}
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(source == editCtrl) {
			if(event == Event.DONE_EVENT) {
				reloadModel();
			}
			cmc.deactivate();
			cleanUp();
		} else if(source == confirmDeleteCtrl) {
			if(DialogBoxUIFactory.isOkEvent(event) || DialogBoxUIFactory.isYesEvent(event)) {
				QItemType type = (QItemType)confirmDeleteCtrl.getUserObject();
				doDelete(ureq, type);
			}
		} else if(source == cmc) {
			cleanUp();
		}
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(editCtrl);
		removeAsListenerAndDispose(cmc);
		editCtrl = null;
		cmc = null;
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}
	
	private void doOpenTranslationTool(UserRequest ureq, QItemType row) {
		Locale orgininalLocale = getLocale();
		Locale varLocale = new Locale(orgininalLocale.getLanguage(), orgininalLocale.getCountry(), "__customizing");

		I18nItem item = new I18nItem("org.olat.modules.qpool.ui", "item.type." + row.getType(), varLocale, 1, 1);
		List<I18nItem> i18nItems = new ArrayList<I18nItem>();
		i18nItems.add(item);

		Preferences guiPrefs = ureq.getUserSession().getGuiPreferences();
		List<String> referenceLangs = I18nModule.getTransToolReferenceLanguages();
		String referencePrefs = (String)guiPrefs.get(I18nModule.class, I18nModule.GUI_PREFS_PREFERRED_REFERENCE_LANG, referenceLangs.get(0));
		I18nManager i18nMgr = I18nManager.getInstance();
		Locale referenceLocale = i18nMgr.getLocaleOrNull(referencePrefs);
		
		
		i18nItemEditCtr = new TranslationToolI18nItemEditCrumbController(ureq, getWindowControl(), i18nItems, referenceLocale, true);
		listenTo(i18nItemEditCtr);
		
		i18nItemEditCtr.initialzeI18nitemAsCurrentItem(ureq, item);

		// Open in modal window
		if (cmc != null) removeAsListenerAndDispose(cmc);
		cmc = new CloseableModalController(getWindowControl(), "close", i18nItemEditCtr.getInitialComponent());
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doConfirmDelete(UserRequest ureq, QItemType type) {
		String title = translate("delete.type");
		String text = translate("delete.type.confirm", new String[]{ type.getType() });
		confirmDeleteCtrl = activateOkCancelDialog(ureq, title, text, confirmDeleteCtrl);
		confirmDeleteCtrl.setUserObject(type);
	}
	
	private void doDelete(UserRequest ureq, QItemType type) {
		//qpoolService.deletePool(pool);
		reloadModel();
	}
	
	private void doEdit(UserRequest ureq, QItemType type) {
		removeAsListenerAndDispose(editCtrl);
		editCtrl = new QItemTypeEditController(ureq, getWindowControl(), type);
		listenTo(editCtrl);
		
		cmc = new CloseableModalController(getWindowControl(), translate("close"),
				editCtrl.getInitialComponent(), true, translate("edit.pool"));
		cmc.activate();
		listenTo(cmc);	
	}
	
	private enum Cols {
		id("type.key"),
		type("type.type"),
		typeI18n("type.translation"),
		deletable("type.deletable");
		
		private final String i18nKey;
	
		private Cols(String i18nKey) {
			this.i18nKey = i18nKey;
		}
		
		public String i18nKey() {
			return i18nKey;
		}
	}
	
	private class QItemTypeDataModel implements FlexiTableDataModel, TableDataModel<QItemType> {

		private FlexiTableColumnModel columnModel;
		private List<QItemType> types;
		
		public QItemTypeDataModel(FlexiTableColumnModel columnModel) {
			this.columnModel = columnModel;
		}
		
		@Override
		public FlexiTableColumnModel getTableColumnModel() {
			return columnModel;
		}

		@Override
		public void setTableColumnModel(FlexiTableColumnModel columnModel) {
			this.columnModel = columnModel;
		}

		@Override
		public int getColumnCount() {
			return columnModel.getColumnCount();
		}

		@Override
		public QItemType getObject(int row) {
			if(types != null && row >= 0 && row < types.size()) {
				return types.get(row);
			}
			return null;
		}

		@Override
		public void setObjects(List<QItemType> objects) {
			types = new ArrayList<QItemType>(objects);
		}

		@Override
		public Object createCopyWithEmptyList() {
			return new QItemTypeDataModel(columnModel);
		}

		@Override
		public int getRowCount() {
			return types == null ? 0 : types.size();
		}

		@Override
		public boolean isRowLoaded(int row) {
			return types != null && row < types.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			QItemType type = getObject(row);
			switch(Cols.values()[col]) {
				case id: return type.getKey();
				case type: return type.getType();
				case typeI18n: {
					String i18nKey = "item.type." + type.getType().toLowerCase();
					String translation = getTranslator().translate(i18nKey);
					if(translation.length() > 256) {
						return i18nKey;
					}
					return translation;
				}
				case deletable: return type.isDeletable();
				default: return "";
			}
		}
	}

}
