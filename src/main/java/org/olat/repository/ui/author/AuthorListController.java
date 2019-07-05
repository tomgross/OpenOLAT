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
package org.olat.repository.ui.author;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.olat.NewControllerFactory;
import org.olat.admin.user.UserSearchController;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.events.MultiIdentityChosenEvent;
import org.olat.basesecurity.events.SingleIdentityChosenEvent;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.SortKey;
import org.olat.core.commons.services.mark.Mark;
import org.olat.core.commons.services.mark.MarkManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.dropdown.Dropdown;
import org.olat.core.gui.components.dropdown.Dropdown.Spacer;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableFilter;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableSortOptions;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.BooleanCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DateFlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableCssDelegate;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableRendererType;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableSearchEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.TextFlexiCellRenderer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.resource.Resourceable;
import org.olat.course.CorruptedCourseException;
import org.olat.course.CourseModule;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryManagedFlag;
import org.olat.repository.RepositoryEntryRef;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryModule;
import org.olat.repository.RepositoryService;
import org.olat.repository.controllers.EntryChangedEvent;
import org.olat.repository.controllers.EntryChangedEvent.Change;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.repository.handlers.RepositoryHandlerFactory.OrderedRepositoryHandler;
import org.olat.repository.model.SearchAuthorRepositoryEntryViewParams;
import org.olat.repository.model.SearchAuthorRepositoryEntryViewParams.OrderBy;
import org.olat.repository.ui.RepositoyUIFactory;
import org.olat.repository.ui.author.AuthoringEntryDataModel.Cols;
import org.olat.user.UserManager;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * 
 * Initial date: 28.04.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.comx
 *
 */
public class AuthorListController extends FormBasicController implements Activateable2, AuthoringEntryDataSourceUIFactory, FlexiTableCssDelegate {

	private final String i18nName;
	protected FlexiTableElement tableEl;
	private final TooledStackedPanel stackPanel;
	
	private boolean withSearch;

	private AuthoringEntryDataModel model;
	private AuthoringEntryDataSource dataSource;
	private final SearchAuthorRepositoryEntryViewParams searchParams;

	private ToolsController toolsCtrl;
	protected CloseableModalController cmc;
	private StepsMainRunController wizardCtrl;
	private AuthorSearchController searchCtrl;
	private UserSearchController userSearchCtr;
	private DialogBoxController copyDialogCtrl;
	private CopyRepositoryEntryController copyCtrl;
	private ConfirmCloseController closeCtrl;
	private ConfirmDeleteSoftlyController confirmDeleteCtrl;
	private ImportRepositoryEntryController importCtrl;
	private CreateEntryController createCtrl;
	protected CloseableCalloutWindowController toolsCalloutCtrl;
	
	protected boolean isOlatAdmin;
	protected boolean hasAuthorRight;
	
	private Link importLink;
	private Dropdown createDropdown;
	private FormLink addOwnersButton, deleteButton, copyButton;

	private LockResult lockResult;
	private final AtomicInteger counter = new AtomicInteger();
	//only used as marker for dirty, model cannot load specific rows
	private final List<Long> dirtyRows = new ArrayList<>();

	private final DB dbInstance;
	private final UserManager userManager;
	private final MarkManager markManager;
	protected final RepositoryModule repositoryModule;
	protected final RepositoryService repositoryService;
	protected final RepositoryManager repositoryManager;
	protected final RepositoryHandlerFactory repositoryHandlerFactory;

	public AuthorListController(UserRequest ureq,
								WindowControl wControl,
								String i18nName,
								SearchAuthorRepositoryEntryViewParams searchParams,
								boolean withSearch,
								DB dbInstance,
								UserManager userManager,
								MarkManager markManager,
								RepositoryModule repositoryModule,
								RepositoryService repositoryService,
								RepositoryManager repositoryManager,
								RepositoryHandlerFactory repositoryHandlerFactory,
								AuthoringEntryRowFactory authoringEntryRowFactory) {
		super(ureq, wControl, "entries");
		this.i18nName = i18nName;
		this.withSearch = withSearch;
		this.searchParams = searchParams;
		this.dbInstance = dbInstance;
		this.userManager = userManager;
		this.markManager = markManager;
		this.repositoryModule = repositoryModule;
		this.repositoryService = repositoryService;
		this.repositoryManager = repositoryManager;
		this.repositoryHandlerFactory = repositoryHandlerFactory;

		setTranslator(Util.createPackageTranslator(RepositoryService.class, getLocale(), getTranslator()));

		OLATResourceable ores = OresHelper.createOLATResourceableType("RepositorySite");
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ores));
		
		Roles roles = ureq.getUserSession().getRoles();
		isOlatAdmin = roles.isOLATAdmin() || roles.isInstitutionalResourceManager();
		hasAuthorRight = roles.isAuthor() || roles.isInstitutionalResourceManager() || roles.isOLATAdmin();

		dataSource = new AuthoringEntryDataSource(searchParams, authoringEntryRowFactory, this);
		initForm(ureq);

		/**
		 * TODO sev26
		 * Verify if this is the best way to inform the controller about list
		 * entry changes.
		 */
		ureq.getUserSession().getSingleUserEventCenter().registerFor(new GenericEventListener() {
			@Override
			public void event(Event event) {
				reloadRows();
			}
		}, ureq.getIdentity(), new Resourceable("CourseModule", null));

		stackPanel = new TooledStackedPanel(i18nName, getTranslator(), this);
		stackPanel.pushController(translate(i18nName), this);
		initTools();
	}

	protected void initTools() {
		if(!withSearch && hasAuthorRight) {
			importLink = LinkFactory.createLink("cmd.import.ressource", getTranslator(), this);
			importLink.setDomReplacementWrapperRequired(false);
			importLink.setIconLeftCSS("o_icon o_icon_import");
			importLink.setElementCssClass("o_sel_author_import");
			stackPanel.addTool(importLink, Align.left);
			
			List<OrderedRepositoryHandler> handlers = repositoryHandlerFactory.getOrderRepositoryHandlers();
			createDropdown = new Dropdown("cmd.create.ressource", "cmd.create.ressource", false, getTranslator());
			createDropdown.setElementCssClass("o_sel_author_create");
			createDropdown.setIconCSS("o_icon o_icon_add");
			int lastGroup = 0;
			for(OrderedRepositoryHandler orderedHandler:handlers) {
				RepositoryHandler handler = orderedHandler.getHandler();
				
				if(handler != null && handler.isCreate()) {
					// for each 10-group, create a separator
					int group = orderedHandler.getOrder() / 10;
					if (group > lastGroup) {
						createDropdown.addComponent(new Spacer("spacer" + orderedHandler.getOrder()));
						lastGroup = group;
					}
					addCreateLink(handler, createDropdown);
				}
			}
			stackPanel.addTool(createDropdown, Align.left);
		}
	}

	public String getI18nName() {
		return i18nName;
	}
	
	@Override
	public Translator getTranslator() {
		return super.getTranslator();
	}

	private void addCreateLink(RepositoryHandler handler, Dropdown dropdown) {
		Link createLink = LinkFactory.createLink(handler.getSupportedType(), getTranslator(), this);
		createLink.setIconLeftCSS("o_icon o_icon-fw " + RepositoyUIFactory.getIconCssClass(handler.getSupportedType()));
		createLink.setElementCssClass("o_sel_author_create-" + handler.getSupportedType());
		createLink.setUserObject(handler);
		dropdown.addComponent(createLink);
	}
	
	public boolean isEmpty() {
		return dataSource.getRowCount() == 0;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		//search form
		if(withSearch) {
			setFormDescription("table.search.author.desc");
			searchCtrl = new AuthorSearchController(ureq, getWindowControl(), true, mainForm);
			searchCtrl.setEnabled(false);
			listenTo(searchCtrl);
		}
		
		//add the table
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.key.i18nKey(), Cols.key.ordinal(), true, OrderBy.key.name()));
		DefaultFlexiColumnModel markColumn = new DefaultFlexiColumnModel(true, Cols.mark.i18nKey(), Cols.mark.ordinal(), true, OrderBy.favorit.name());
		markColumn.setExportable(false);
		columnsModel.addFlexiColumnModel(markColumn);
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.type.i18nKey(), Cols.type.ordinal(), true, OrderBy.type.name(),
				FlexiColumnModel.ALIGNMENT_LEFT, new TypeRenderer()));
		FlexiCellRenderer renderer = new StaticFlexiCellRenderer("select", new TextFlexiCellRenderer());
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.displayName.i18nKey(), Cols.displayName.ordinal(), "select",
				true, OrderBy.displayname.name(), renderer));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.authors.i18nKey(), Cols.authors.ordinal(),
				true, OrderBy.authors.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.location.i18nKey(), Cols.location.ordinal(),
				true, OrderBy.location.name()));
		if(repositoryModule.isManagedRepositoryEntries()) {
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.externalId.i18nKey(), Cols.externalId.ordinal(),
					true, OrderBy.externalId.name()));
		}
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.externalRef.i18nKey(), Cols.externalRef.ordinal(),
				true, OrderBy.externalRef.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.lifecycleLabel.i18nKey(), Cols.lifecycleLabel.ordinal(),
				true, OrderBy.lifecycleLabel.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.lifecycleSoftkey.i18nKey(), Cols.lifecycleSoftkey.ordinal(),
				true, OrderBy.lifecycleSoftkey.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.lifecycleStart.i18nKey(), Cols.lifecycleStart.ordinal(),
				true, OrderBy.lifecycleStart.name(), FlexiColumnModel.ALIGNMENT_LEFT, new DateFlexiCellRenderer(getLocale())));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.lifecycleEnd.i18nKey(), Cols.lifecycleEnd.ordinal(),
				true, OrderBy.lifecycleEnd.name(), FlexiColumnModel.ALIGNMENT_LEFT, new DateFlexiCellRenderer(getLocale())));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.author.i18nKey(), Cols.author.ordinal(),
				true, OrderBy.author.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.access.i18nKey(), Cols.access.ordinal(),
				true, OrderBy.access.name(), FlexiColumnModel.ALIGNMENT_LEFT, new AccessRenderer()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.ac.i18nKey(), Cols.ac.ordinal(),
				true, OrderBy.ac.name(), FlexiColumnModel.ALIGNMENT_LEFT, new ACRenderer()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.creationDate.i18nKey(), Cols.creationDate.ordinal(),
				true, OrderBy.creationDate.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.lastUsage.i18nKey(), Cols.lastUsage.ordinal(),
				true, OrderBy.lastUsage.name()));
		initActionsColumns(columnsModel);
		
		model = new AuthoringEntryDataModel(dataSource, columnsModel);
		tableEl = uifactory.addTableElement(getWindowControl(), "table", model, 20, false, getTranslator(), formLayout, AuthorSearchController.MINIMAL_CHAR_LENGTH_SEARCH_FIELD);
		tableEl.setSearchEnabled(withSearch);
		tableEl.setCssDelegate(this);
		tableEl.setExportEnabled(true);
		tableEl.setExtendedSearch(searchCtrl);
		tableEl.setCustomizeColumns(true);
		tableEl.setElementCssClass("o_coursetable");
		tableEl.setShowAllRowsEnabled(true);
		tableEl.setMultiSelect(true);
		tableEl.setSelectAllEnable(true);
		tableEl.setEmtpyTableMessageKey("table.sEmptyTable");
		tableEl.setSortSettings(new FlexiTableSortOptions(true, new SortKey(OrderBy.creationDate.name(), false)));
		tableEl.setAndLoadPersistedPreferences(ureq, "authors-list-" + i18nName);

		if(!withSearch) {
			tableEl.reloadData();
			tableEl.setFilters(null, getFilters(), false);
		}
		
		initBatchButtons(formLayout);
	}

	protected void initActionsColumns(FlexiTableColumnModel columnsModel) {
		DefaultFlexiColumnModel detailsColumn = new DefaultFlexiColumnModel(Cols.detailsSupported.i18nKey(), Cols.detailsSupported.ordinal(), "details",
				new StaticFlexiCellRenderer("", "details", "o_icon o_icon-lg o_icon_details", translate("details")));
		detailsColumn.setExportable(false);
		columnsModel.addFlexiColumnModel(detailsColumn);
		if(hasAuthorRight) {
			DefaultFlexiColumnModel editColumn = new DefaultFlexiColumnModel(Cols.editionSupported.i18nKey(), Cols.editionSupported.ordinal(), "edit",
				new BooleanCellRenderer(new StaticFlexiCellRenderer("", "edit", "o_icon o_icon-lg o_icon_edit", translate("edit")), null));
			editColumn.setExportable(false);
			columnsModel.addFlexiColumnModel(editColumn);
			DefaultFlexiColumnModel toolsColumn = new DefaultFlexiColumnModel(Cols.tools.i18nKey(), Cols.tools.ordinal());
			toolsColumn.setExportable(false);
			columnsModel.addFlexiColumnModel(toolsColumn);
		}
	}

	protected void initBatchButtons(FormItemContainer formLayout) {
		if(hasAuthorRight) {
			addOwnersButton = uifactory.addFormLink("tools.add.owners", formLayout, Link.BUTTON);
			copyButton = uifactory.addFormLink("details.copy", formLayout, Link.BUTTON);
			deleteButton = uifactory.addFormLink("details.delete", formLayout, Link.BUTTON);
		}
	}
	
	private List<FlexiTableFilter> getFilters() {
		List<OrderedRepositoryHandler> supportedHandlers = repositoryHandlerFactory.getOrderRepositoryHandlers();
		List<FlexiTableFilter> resources = new ArrayList<>(supportedHandlers.size() + 1);
		int lastGroup = 0;
		for(OrderedRepositoryHandler handler:supportedHandlers) {
			if (!handler.getHandler().getSupportedType().startsWith("FileResource.IMSQTI21") &&
				!handler.getHandler().getSupportedType().startsWith("FileResource.FORM") &&
				!handler.getHandler().getSupportedType().startsWith("FileResource.VIDEO") &&
				!handler.getHandler().getSupportedType().startsWith("BinderTemplate") &&
				!handler.getHandler().getSupportedType().startsWith("EPStructuredMapTemplate")) {
				// for each 10-group, crate a separator
				int group = handler.getOrder() / 10;
				if (group > lastGroup) {
					resources.add(FlexiTableFilter.SPACER);
					lastGroup = group;
				}
				String type = handler.getHandler().getSupportedType();
				String inconLeftCss = RepositoyUIFactory.getIconCssClass(type);
				resources.add(new FlexiTableFilter(translate(type), type, inconLeftCss));
			}
		}
		return resources;
	}
	
	@Override
	public String getWrapperCssClass(FlexiTableRendererType type) {
		return null;
	}

	@Override
	public String getTableCssClass(FlexiTableRendererType type) {
		return null;
	}

	@Override
	public String getRowCssClass(FlexiTableRendererType type, int pos) {
		AuthoringEntryRow row = model.getObject(pos);
		if(row.getAccess() == 0) {
			return "o_entry_deleted";
		}
		if(row.getRepositoryEntryStatus().isClosed()) {
			return "o_entry_closed";
		}
		if(row.getRepositoryEntryStatus().isUnpublished()) {
			return "o_entry_unpublished";
		}
		return null;
	}

	public TooledStackedPanel getStackPanel() {
		return stackPanel;
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(state instanceof AuthorListState) {
			AuthorListState se = (AuthorListState)state;
			if(se.getTableState() != null) {
				tableEl.setStateEntry(ureq, se.getTableState());
			}
			if(se.getSearchEvent() != null) {
				searchCtrl.update(se.getSearchEvent());
				doSearch(ureq, se.getSearchEvent());
			}
		}
	}
	
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(importLink == source) {
			doImport(ureq);
		} else if(source instanceof Link && ((Link)source).getUserObject() instanceof RepositoryHandler) {
			RepositoryHandler handler = (RepositoryHandler)((Link)source).getUserObject();
			if(handler != null) {
				doCreate(ureq, handler);
			}
		}
		super.event(ureq, source, event);
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(cmc == source) {
			cleanUp();
		} else if(createCtrl == source) {
			cmc.deactivate();
			if(Event.DONE_EVENT.equals(event)) {
				launchEditDescription(ureq, createCtrl.getAddedEntry());
				reloadRows();
				cleanUp();
			} else if(CreateRepositoryEntryController.CREATION_WIZARD.equals(event)) {
				doPostCreateWizard(ureq, createCtrl.getAddedEntry(), createCtrl.getHandler());
			} else {
				cleanUp();
			}
		} else if(copyCtrl == source) {
			cmc.deactivate();
			if (event == Event.DONE_EVENT) {
				reloadRows();
				launchEditDescription(ureq, copyCtrl.getCopiedEntry());
			}
			cleanUp();
		} else if(importCtrl == source) {
			cmc.deactivate();
			if(Event.DONE_EVENT.equals(event)) {
				reloadRows();
				launchEditDescription(ureq, importCtrl.getImportedEntry());
			}
			cleanUp();
		} else if(wizardCtrl == source) {
			if (event.equals(Event.CHANGED_EVENT) || event.equals(Event.CANCELLED_EVENT)) {
				getWindowControl().pop();
				RepositoryEntry newEntry = (RepositoryEntry)wizardCtrl.getRunContext().get("authoringNewEntry");
				reloadRows();
				cleanUp();
				launchEditDescription(ureq, newEntry);
			}
		} else if(searchCtrl == source) {
			if(event instanceof SearchEvent) {
				SearchEvent se = (SearchEvent)event;
				doSearch(ureq, se);
			} else if(event == Event.CANCELLED_EVENT) {
				searchParams.setResourceTypes(null);
				searchParams.setIdAndRefs(null);
				searchParams.setAuthor(null);
				searchParams.setDisplayname(null);
				searchParams.setDescription(null);
				searchParams.setOwnedResourcesOnly(false);
			}
		} else if(userSearchCtr == source) {
			@SuppressWarnings("unchecked")
			List<AuthoringEntryRow> rows = (List<AuthoringEntryRow>)userSearchCtr.getUserObject();
			if (event instanceof MultiIdentityChosenEvent) {
				MultiIdentityChosenEvent mice = (MultiIdentityChosenEvent) event; 
				doAddOwners(mice.getChosenIdentities(), rows);
			} else if (event instanceof SingleIdentityChosenEvent) {
				SingleIdentityChosenEvent  sice = (SingleIdentityChosenEvent) event;
				List<Identity> futureOwners = Collections.singletonList(sice.getChosenIdentity());
				doAddOwners(futureOwners, rows);
			}
			cmc.deactivate();
			cleanUp();
		} else if(toolsCtrl == source) {
			if(event == Event.DONE_EVENT) {
				toolsCalloutCtrl.deactivate();
				cleanUp();
			}
		} else if(closeCtrl == source) {
			if(event == Event.CANCELLED_EVENT) {
				cmc.deactivate();
				cleanUp();
			} else if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				cmc.deactivate();
				reloadRows();
				cleanUp();
			}
		} else if(confirmDeleteCtrl == source) {
			if(event == Event.CANCELLED_EVENT) {
				cmc.deactivate();
				cleanUp();
			} else if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				cmc.deactivate();
				reloadRows();
				cleanUp();
			}
		} else if(copyDialogCtrl == source) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				@SuppressWarnings("unchecked")
				List<AuthoringEntryRow> rows = (List<AuthoringEntryRow>)copyDialogCtrl.getUserObject();
				doCompleteCopy(ureq, rows);
				reloadRows();
			}
		}
		super.event(ureq, source, event);
	}
	
	protected void cleanUp() {
		removeAsListenerAndDispose(confirmDeleteCtrl);
		removeAsListenerAndDispose(toolsCalloutCtrl);
		removeAsListenerAndDispose(userSearchCtr);
		removeAsListenerAndDispose(createCtrl);
		removeAsListenerAndDispose(importCtrl);
		removeAsListenerAndDispose(wizardCtrl);
		removeAsListenerAndDispose(toolsCtrl);
		removeAsListenerAndDispose(closeCtrl);
		removeAsListenerAndDispose(cmc);
		confirmDeleteCtrl = null;
		toolsCalloutCtrl = null;
		userSearchCtr = null;
		createCtrl = null;
		importCtrl = null;
		wizardCtrl = null;
		toolsCtrl = null;
		closeCtrl = null;
		cmc = null;
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(addOwnersButton == source) {
			List<AuthoringEntryRow> rows = getMultiSelectedRows();
			if(!rows.isEmpty()) {
				doAddOwners(ureq, rows);
			} else {
				showWarning("bulk.update.nothing.selected");
			}
		} else if(copyButton == source) {
			List<AuthoringEntryRow> rows = getMultiSelectedRows();
			if(!rows.isEmpty()) {
				doConfirmCopy(ureq, rows);
			} else {
				showWarning("bulk.update.nothing.selected");
			}
		} else if(deleteButton == source) {
			List<AuthoringEntryRow> rows = getMultiSelectedRows();
			if(!rows.isEmpty()) {
				doDelete(ureq, rows);
			} else {
				showWarning("bulk.update.nothing.selected");
			}
		} else if(source instanceof FormLink) {
			FormLink link = (FormLink)source;
			String cmd = link.getCmd();
			if("mark".equals(cmd)) {
				AuthoringEntryRow row = (AuthoringEntryRow)link.getUserObject();
				boolean marked = doMark(ureq, row);
				link.setIconLeftCSS(marked ? "o_icon o_icon_bookmark o_icon-lg" : "o_icon o_icon_bookmark_add o_icon-lg");
				link.setTitle(translate(marked ? "details.bookmark.remove" : "details.bookmark"));
				link.getComponent().setDirty(true);
				row.setMarked(marked);
			} else if("tools".equals(cmd)) {
				AuthoringEntryRow row = (AuthoringEntryRow)link.getUserObject();
				doOpenTools(ureq, row, link);
			}
		} else if(source == tableEl) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				String cmd = se.getCommand();
				AuthoringEntryRow row = model.getObject(se.getIndex());
				if("details".equals(cmd)) {
					launchDetails(ureq, row);
				} else if("edit".equals(cmd)) {
					launchEditor(ureq, row);
				} else if("select".equals(cmd)) {
					launch(ureq, row);
				}
			} else if(event instanceof FlexiTableSearchEvent) {
				AuthorListState stateEntry = new AuthorListState();
				stateEntry.setTableState(tableEl.getStateEntry());
				addToHistory(ureq, stateEntry);
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void propagateDirtinessToContainer(FormItem fiSrc, FormEvent event) {
		//do not update the 
	}
	
	public void addDirtyRows(Long entryKey) {
		dirtyRows.add(entryKey);
	}
	
	protected void reloadDirtyRows() {
		if(dirtyRows.size() > 0) {
			reloadRows();
		}
	}
	protected void reloadRows() {
		tableEl.deselectAll();
		tableEl.reloadData();
		dirtyRows.clear();
	}

	private void doOpenTools(UserRequest ureq, AuthoringEntryRow row, FormLink link) {
		removeAsListenerAndDispose(toolsCtrl);
		removeAsListenerAndDispose(toolsCalloutCtrl);

		RepositoryEntry entry = repositoryService.loadByKey(row.getKey());
		if(entry == null) {
			tableEl.reloadData();
			showWarning("repositoryentry.not.existing");
		} else {
			toolsCtrl = createToolsController(ureq, row, entry);
			listenTo(toolsCtrl);
	
			toolsCalloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(),
					toolsCtrl.getInitialComponent(), link.getFormDispatchId(), "", true, "");
			listenTo(toolsCalloutCtrl);
			toolsCalloutCtrl.activate();
		}
	}

	protected ToolsController createToolsController(UserRequest ureq, AuthoringEntryRow row, RepositoryEntry entry) {
		return new ToolsController(ureq, getWindowControl(), row, entry);
	}
	
	private void doImport(UserRequest ureq) {
		if(importCtrl != null) return;

		removeAsListenerAndDispose(importCtrl);
		importCtrl = new ImportRepositoryEntryController(ureq, getWindowControl());
		listenTo(importCtrl);
		removeAsListenerAndDispose(cmc);
		
		String title = translate("cmd.import.ressource");
		cmc = new CloseableModalController(getWindowControl(), translate("close"), importCtrl.getInitialComponent(),
				true, title);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doCreate(UserRequest ureq, RepositoryHandler handler) {
		if(createCtrl != null) return;

		removeAsListenerAndDispose(createCtrl);
		createCtrl = handler.createCreateRepositoryEntryController(ureq, getWindowControl());
		listenTo(createCtrl);
		removeAsListenerAndDispose(cmc);
		
		String title = translate(handler.getCreateLabelI18nKey());
		cmc = new CloseableModalController(getWindowControl(), translate("close"), createCtrl.getInitialComponent(),
				true, title);
		cmc.setCustomWindowCSS("o_sel_author_create_popup");
		listenTo(cmc);
		cmc.activate();
	}
	
	protected boolean doMark(UserRequest ureq, AuthoringEntryRow row) {
		OLATResourceable item = OresHelper.createOLATResourceableInstance("RepositoryEntry", row.getKey());
		if(markManager.isMarked(item, getIdentity(), null)) {
			markManager.removeMark(item, getIdentity(), null);

			dbInstance.commit();//before sending, save the changes
			EntryChangedEvent e = new EntryChangedEvent(row, getIdentity(), Change.removeBookmark, i18nName);
			ureq.getUserSession().getSingleUserEventCenter().fireEventToListenersOf(e, RepositoryService.REPOSITORY_EVENT_ORES);
			return false;
		}
		
		String businessPath = "[RepositoryEntry:" + item.getResourceableId() + "]";
		markManager.setMark(item, getIdentity(), null, businessPath);
		
		dbInstance.commit();//before sending, save the changes
		EntryChangedEvent e = new EntryChangedEvent(row, getIdentity(), Change.addBookmark, i18nName);
		ureq.getUserSession().getSingleUserEventCenter().fireEventToListenersOf(e, RepositoryService.REPOSITORY_EVENT_ORES);
		return true;
	}
	
	private void doPostCreateWizard(UserRequest ureq, RepositoryEntry newEntry, RepositoryHandler handler) {
		if(wizardCtrl != null) return;
		
		cleanUp();
		wizardCtrl = handler.createWizardController(newEntry, ureq, getWindowControl());
		wizardCtrl.getRunContext().put("authoringNewEntry", newEntry);
		listenTo(wizardCtrl);
		getWindowControl().pushAsModalDialog(wizardCtrl.getInitialComponent());
	}
	
	private void doSearch(UserRequest ureq, SearchEvent se) {
		if(StringHelper.containsNonWhitespace(se.getType())) {
			searchParams.setResourceTypes(Collections.singletonList(se.getType()));
		} else {
			searchParams.setResourceTypes(null);
		}

		searchParams.setIdAndRefs(se.getId());
		searchParams.setAuthor(se.getAuthor());
		searchParams.setOwnedResourcesOnly(se.isOwnedResourcesOnly());
		searchParams.setDisplayname(se.getDisplayname());
		searchParams.setDescription(se.getDescription());
		tableEl.reset(true, true, true);
		
		AuthorListState stateEntry = new AuthorListState();
		stateEntry.setSearchEvent(se);
		stateEntry.setTableState(tableEl.getStateEntry());
		addToHistory(ureq, stateEntry);
	}
	
	protected List<AuthoringEntryRow> getMultiSelectedRows() {
		Set<Integer> selections = tableEl.getMultiSelectedIndex();
		List<AuthoringEntryRow> rows = new ArrayList<>(selections.size());
		if(selections.isEmpty()) {
			
		} else {
			for(Integer i:selections) {
				AuthoringEntryRow row = model.getObject(i.intValue());
				if(row != null) {
					rows.add(row);
				}
			}
		}
		return rows;
	}
	
	private void doAddOwners(UserRequest ureq, List<AuthoringEntryRow> rows) {
		if(userSearchCtr != null) return;
		
		Roles roles = ureq.getUserSession().getRoles();
		List<AuthoringEntryRow> manageableRows = new ArrayList<>(rows.size());
		for(AuthoringEntryRow row:rows) {
			boolean managed = RepositoryEntryManagedFlag.isManaged(row.getManagedFlags(), RepositoryEntryManagedFlag.membersmanagement);
			boolean canAddOwner = roles.isOLATAdmin() || repositoryService.hasRole(ureq.getIdentity(), row, GroupRoles.owner.name())
					|| repositoryManager.isInstitutionalRessourceManagerFor(getIdentity(), roles, row);
			if(canAddOwner && !managed) {
				manageableRows.add(row);
			}
		}
		
		if(manageableRows.isEmpty()) {
			showWarning("bulk.update.nothing.applicable.selected");
		} else {
			removeAsListenerAndDispose(userSearchCtr);
			userSearchCtr = new UserSearchController(ureq, getWindowControl(), false, true, UserSearchController.ACTION_KEY_CHOOSE_FINISH);
			userSearchCtr.setUserObject(manageableRows);
			listenTo(userSearchCtr);
			
			String title = translate("tools.add.owners");
			cmc = new CloseableModalController(getWindowControl(), translate("close"), userSearchCtr.getInitialComponent(),
					true, title);
			listenTo(cmc);
			cmc.activate();
		}
	}
	
	private void doAddOwners(List<Identity> futureOwners, List<AuthoringEntryRow> rows) {
		for(AuthoringEntryRow row:rows) {
			RepositoryEntry re = repositoryService.loadByKey(row.getKey());
			for(Identity futureOwner:futureOwners) {
				repositoryService.addRole(futureOwner, re, GroupRoles.owner.name());
			}
		}
	}
	
	private void doConfirmCopy(UserRequest ureq, List<AuthoringEntryRow> rows) {
		boolean deleted = false;
		Roles roles = ureq.getUserSession().getRoles();
		List<AuthoringEntryRow> copyableRows = new ArrayList<>(rows.size());
		for(AuthoringEntryRow row:rows) {
			RepositoryEntry entry = repositoryService.loadByKey(row.getKey());
			if(entry == null) {
				deleted = true;
			} else {
				boolean isInstitutionalResourceManager = repositoryManager.isInstitutionalRessourceManagerFor(getIdentity(), roles, row);
				boolean isOwner = roles.isOLATAdmin()
						|| repositoryService.hasRole(ureq.getIdentity(), row, GroupRoles.owner.name())
						|| isInstitutionalResourceManager;
	
				boolean isAuthor = roles.isOLATAdmin() || roles.isAuthor() || isInstitutionalResourceManager;
				
				boolean copyManaged = RepositoryEntryManagedFlag.isManaged(entry, RepositoryEntryManagedFlag.copy);
				boolean canCopy = (isAuthor || isOwner) && (entry.getCanCopy() || isOwner) && !copyManaged;
				if(canCopy) {
					copyableRows.add(row);
				}
			}
		}
		
		if(deleted) {
			showWarning("repositoryentry.not.existing");
			tableEl.reloadData();
		} else if(copyableRows.isEmpty()) {
			showWarning("bulk.update.nothing.applicable.selected");
		} else {
			StringBuilder sb = new StringBuilder();
			for(AuthoringEntryRow row:copyableRows) {
				if(sb.length() > 0) sb.append(", ");
				sb.append(StringHelper.escapeHtml(row.getDisplayname()));
			}
			
			String dialogText = (copyableRows.size() != rows.size())
					? translate("details.copy.confirm.warning", sb.toString()) : translate("details.copy.confirm", sb.toString());
			copyDialogCtrl = activateYesNoDialog(ureq, translate("details.copy"), dialogText, copyDialogCtrl);
			copyDialogCtrl.setUserObject(copyableRows);
		}
	}
	
	private void doCompleteCopy(UserRequest ureq, List<AuthoringEntryRow> rows) {
		Roles roles = ureq.getUserSession().getRoles();
		boolean isAdmin = roles.isOLATAdmin();

		int skippedRows = 0;
		for(AuthoringEntryRow row:rows) {
			RepositoryEntry sourceEntry = repositoryService.loadByKey(row.getKey());
			if (!isAdmin && sourceEntry.exceedsSizeLimit()) {
				logWarn("Course " + sourceEntry.getDisplayname() + " is not copied because it exceeds the size limit", null);
				skippedRows++;
			} else {
				String displayname = "Copy of " + sourceEntry.getDisplayname();
				if (displayname.length() > 255) {
					displayname = displayname.substring(0, 255);
				}
				repositoryService.copy(sourceEntry, getIdentity(), displayname);
			}
		}

		if (skippedRows > 0) {
			showError("details.copy.success.with.some.skipped", new String[]{ Integer.toString(rows.size() - skippedRows), Integer.toString(skippedRows) });
		} else {
			showInfo("details.copy.success", Integer.toString(rows.size()));
		}
	}
	
	private void doCloseResource(UserRequest ureq, AuthoringEntryRow row) {
		removeAsListenerAndDispose(closeCtrl);
		
		RepositoryEntry entry = repositoryService.loadByKey(row.getKey());

		List<RepositoryEntry> entryToClose = Collections.singletonList(entry);
		closeCtrl = new ConfirmCloseController(ureq, getWindowControl(), entryToClose);
		listenTo(closeCtrl);

		String title = translate("read.only.header", entry.getDisplayname());
		cmc = new CloseableModalController(getWindowControl(), "close", closeCtrl.getInitialComponent(), true, title);
		listenTo(cmc);
		cmc.activate();
	}

	private void doOverrideCloseResource(UserRequest ureq, AuthoringEntryRow row) {
		try {
			RepositoryHandler handler = repositoryHandlerFactory.getRepositoryHandler(row.getResourceType());
			if(handler != null) {
				String businessPath = "[RepositoryEntry:" + row.getKey() + "]";
				ureq.getUserSession().putEntry("override_readonly_" + row.getKey(), Boolean.TRUE);
				if(!NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl())) {
					tableEl.reloadData();
				}
			}
		} catch (CorruptedCourseException e) {
			logError("Course corrupted: " + row.getKey() + " (" + row.getOLATResourceable().getResourceableId() + ")", e);
			showError("cif.error.corrupted");
		}
	}

	protected void doCopy(UserRequest ureq, AuthoringEntryRow row) {
		removeAsListenerAndDispose(cmc);
		removeAsListenerAndDispose(copyCtrl);

		RepositoryEntry entry = repositoryService.loadByKey(row.getKey());
		copyCtrl = new CopyRepositoryEntryController(ureq, getWindowControl(), entry);
		listenTo(copyCtrl);
		
		String title = translate("details.copy");
		cmc = new CloseableModalController(getWindowControl(), translate("close"), copyCtrl.getInitialComponent(), true, title);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doDelete(UserRequest ureq, List<AuthoringEntryRow> rows) {
		Roles roles = ureq.getUserSession().getRoles();
		List<Long> deleteableRowKeys = new ArrayList<>(rows.size());
		for(AuthoringEntryRow row:rows) {
			if (isDeletable(row, ureq, roles)) {
				deleteableRowKeys.add(row.getKey());
			}
		}
		
		List<RepositoryEntry> entriesToDelete = repositoryManager.lookupRepositoryEntries(deleteableRowKeys);
		if(entriesToDelete.isEmpty()) {
			showWarning("bulk.update.nothing.applicable.selected");
		} else {
			removeAsListenerAndDispose(confirmDeleteCtrl);
			removeAsListenerAndDispose(cmc);
			
			confirmDeleteCtrl = new ConfirmDeleteSoftlyController(ureq, getWindowControl(), entriesToDelete, rows.size() != entriesToDelete.size());
			listenTo(confirmDeleteCtrl);
			
			String title = translate("details.delete");
			cmc = new CloseableModalController(getWindowControl(), translate("close"), confirmDeleteCtrl.getInitialComponent(), true, title);
			listenTo(cmc);
			cmc.activate();
		}
	}

	protected boolean isDeletable(AuthoringEntryRow row, UserRequest ureq, Roles roles) {
		boolean managed = RepositoryEntryManagedFlag.isManaged(row.getManagedFlags(), RepositoryEntryManagedFlag.delete);
		boolean canDelete = roles.isOLATAdmin() || repositoryService.hasRole(ureq.getIdentity(), row, GroupRoles.owner.name())
				|| repositoryManager.isInstitutionalRessourceManagerFor(getIdentity(), roles, row);
		return canDelete && !managed;
	}

	protected void doDownload(UserRequest ureq, AuthoringEntryRow row) {
		Roles roles = ureq.getUserSession().getRoles();
		boolean isAdmin = roles.isOLATAdmin();

		RepositoryHandler typeToDownload = repositoryHandlerFactory.getRepositoryHandler(row.getResourceType());
		if (typeToDownload == null) {
			StringBuilder sb = new StringBuilder(translate("error.download"));
			sb.append(": No download handler for repository entry: ")
			  .append(row.getKey());
			showError(sb.toString());
			return;
		}
		
		RepositoryEntry entry = repositoryService.loadByKey(row.getKey());
		OLATResourceable ores = entry.getOlatResource();
		if (ores == null) {
			showError("error.download");
			return;
		}

		if (!isAdmin && entry.exceedsSizeLimit()) {
			logError("Course " + entry.getDisplayname() + " is not exported because it exceeds the size limit", null);
			showError("error.export.size.exceeded", new String[] { entry.getDisplayname() });
			return;
		}

		boolean isAlreadyLocked = typeToDownload.isLocked(ores);
		try {			
		  lockResult = typeToDownload.acquireLock(ores, ureq.getIdentity());
		  if(lockResult == null || (lockResult !=null && lockResult.isSuccess() && !isAlreadyLocked)) {
		    MediaResource mr = typeToDownload.getAsMediaResource(ores, false);
		    if(mr!=null) {
		      repositoryService.incrementDownloadCounter(entry);
		      ureq.getDispatchResult().setResultingMediaResource(mr);
		    } else {
			    showError("error.export");
			    fireEvent(ureq, Event.FAILED_EVENT);			
		    }
		  } else if(lockResult !=null && lockResult.isSuccess() && isAlreadyLocked) {
		  	String fullName = userManager.getUserDisplayName(lockResult.getOwner());
		  	showInfo("warning.course.alreadylocked.bySameUser", fullName);
		  	lockResult = null; //invalid lock, it was already locked
		  } else {
		  	String fullName = userManager.getUserDisplayName(lockResult.getOwner());
		  	showInfo("warning.course.alreadylocked", fullName);
		  }
		} finally {	
			if((lockResult!=null && lockResult.isSuccess() && !isAlreadyLocked)) {
			  typeToDownload.releaseLock(lockResult);		
			  lockResult = null;
			}
		}
	}
	
	private void launch(UserRequest ureq, AuthoringEntryRow row) {
		try {
			RepositoryHandler handler = repositoryHandlerFactory.getRepositoryHandler(row.getResourceType());
			if(handler != null) {
				String businessPath = "[RepositoryEntry:" + row.getKey() + "]";
				if(!NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl())) {
					tableEl.reloadData();
				}
			}
		} catch (CorruptedCourseException e) {
			logError("Course corrupted: " + row.getKey() + " (" + row.getOLATResourceable().getResourceableId() + ")", e);
			showError("cif.error.corrupted");
		}
	}
	
	private void launchCatalog(UserRequest ureq, RepositoryEntryRef ref) {
		String businessPath = "[RepositoryEntry:" + ref.getKey() + "][Catalog:0]";
		NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
	}
	
	private void launchDetails(UserRequest ureq, RepositoryEntryRef ref) {
		String businessPath = "[RepositoryEntry:" + ref.getKey() + "][Infos:0]";
		if(!NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl())) {
			tableEl.reloadData();
		}
	}
	
	private void launchEditDescription(UserRequest ureq, RepositoryEntry re) {
		if(re != null) {
			RepositoryHandler handler = repositoryHandlerFactory.getRepositoryHandler(re);
			if(handler != null) {
				String businessPath = "[RepositoryEntry:" + re.getKey() + "][EditDescription:0]";
				if(!NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl())) {
					tableEl.reloadData();
				}
			}
		}
	}
	
	private void launchEditDescription(UserRequest ureq, RepositoryEntryRef re) {
		String businessPath = "[RepositoryEntry:" + re.getKey() + "][EditDescription:0]";
		if(!NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl())) {
			tableEl.reloadData();
		}
	}
	
	private void launchEditor(UserRequest ureq, AuthoringEntryRow row) {
		try {
			String businessPath = "[RepositoryEntry:" + row.getKey() + "][Editor:0]";
			if(!NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl())) {
				tableEl.reloadData();
			}
		} catch (CorruptedCourseException e) {
			logError("Course corrupted: " + row.getKey() + " (" + row.getOLATResourceable().getResourceableId() + ")", e);
			showError("cif.error.corrupted");
		}
	}
	
	private void launchMembers(UserRequest ureq, AuthoringEntryRow row) {
		try {
			String businessPath = "[RepositoryEntry:" + row.getKey() + "][MembersMgmt:0]";
			NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
		} catch (CorruptedCourseException e) {
			logError("Course corrupted: " + row.getKey() + " (" + row.getOLATResourceable().getResourceableId() + ")", e);
			showError("cif.error.corrupted");
		}
	}

	@Override
	public void forgeLinks(AuthoringEntryRow row) {
		//mark
		FormLink markLink = uifactory.addFormLink("mark_" + counter.incrementAndGet(), "mark", "", null, null, Link.NONTRANSLATED);
		markLink.setIconLeftCSS(row.isMarked() ? Mark.MARK_CSS_LARGE : Mark.MARK_ADD_CSS_LARGE);
		markLink.setTitle(translate(row.isMarked() ? "details.bookmark.remove" : "details.bookmark"));
		markLink.setUserObject(row);
		row.setMarkLink(markLink);
		//tools
		FormLink toolsLink = uifactory.addFormLink("tools_" + counter.incrementAndGet(), "tools", "", null, null, Link.NONTRANSLATED);
		toolsLink.setIconLeftCSS("o_icon o_icon_actions o_icon-lg");
		toolsLink.setUserObject(row);
		row.setToolsLink(toolsLink);
	}
	
	protected class ToolsController extends BasicController {
		
		protected final AuthoringEntryRow row;
		protected final RepositoryEntry entry;

		private final VelocityContainer mainVC;
		
		private boolean isOwner;
		private boolean isAuthor;
		
		public ToolsController(UserRequest ureq, WindowControl wControl, AuthoringEntryRow row, RepositoryEntry entry) {
			super(ureq, wControl);
			setTranslator(AuthorListController.this.getTranslator());
			this.row = row;
			this.entry = entry;
			
			Identity identity = getIdentity();
			Roles roles = ureq.getUserSession().getRoles();
			boolean isInstitutionalResourceManager = !roles.isGuestOnly()
						&& repositoryManager.isInstitutionalRessourceManagerFor(identity, roles, entry);
			isOwner = isOlatAdmin || repositoryService.hasRole(ureq.getIdentity(), entry, GroupRoles.owner.name())
						|| isInstitutionalResourceManager;
			isAuthor = isOlatAdmin || roles.isAuthor() | isInstitutionalResourceManager;
			
			RepositoryHandler handler = repositoryHandlerFactory.getRepositoryHandler(entry);

			mainVC = createVelocityContainer("tools");
			List<String> links = new ArrayList<>();

			if(isOwner) {
				addLink("tools.edit.description", "description", "o_icon o_icon-fw o_icon_details", links);
				if(repositoryModule.isCatalogEnabled()) {
					addLink("tools.edit.catalog", "catalog", "o_icon o_icon-fw o_icon_catalog", links);
				}
				addLink("details.members", "members", "o_icon o_icon-fw o_icon_membersmanagement", links);
			}
			
			boolean copyManaged = RepositoryEntryManagedFlag.isManaged(entry, RepositoryEntryManagedFlag.copy);
			boolean canCopy = (isAuthor || isOwner) && (entry.getCanCopy() || isOwner) && !copyManaged;
			
			boolean canDownload = entry.getCanDownload() && handler.supportsDownload();
			// disable download for courses if not author or owner
			if (entry.getOlatResource().getResourceableTypeName().equals(CourseModule.getCourseTypeName()) && !(isOwner || isAuthor)) {
				canDownload = false;
			}
			// always enable download for owners
			if (isOwner && handler.supportsDownload()) {
				canDownload = true;
			}
			
			if(canCopy || canDownload) {
				links.add("-");
				if (canCopy) {
					addLink("details.copy", "copy", "o_icon o_icon-fw o_icon_copy", links);
				}
				if(canDownload) {
					addLink("details.download", "download", "o_icon o_icon-fw o_icon_download", links);
				}
			}
			
			boolean canClose = OresHelper.isOfType(entry.getOlatResource(), CourseModule.class)
					&& !RepositoryEntryManagedFlag.isManaged(entry, RepositoryEntryManagedFlag.close)
					&& !entry.getRepositoryEntryStatus().isClosed()
					&& !entry.getRepositoryEntryStatus().isUnpublished();
			
			if(isOwner) {
				boolean deleteManaged = RepositoryEntryManagedFlag.isManaged(entry, RepositoryEntryManagedFlag.delete);
				if(canClose || !deleteManaged) {
					links.add("-");
				}

				boolean closed = entry.getRepositoryEntryStatus().isClosed();
				if(closed && "CourseModule".equals(entry.getOlatResource().getResourceableTypeName())) {
					addLink("details.override.close", "override-close", "o_icon o_icon-fw o_icon_close_resource", links);
				} else if(canClose) {
					addLink("details.close.ressoure", "close", "o_icon o_icon-fw o_icon_close_resource", links);
				}
				if(!deleteManaged) {
					addDeleteLink(links);
				}
			}

			mainVC.contextPut("links", links);
			putInitialPanel(mainVC);
		}

		protected void addDeleteLink(List<String> links) {
			addLink("details.delete", "delete", "o_icon o_icon-fw o_icon_delete_item", links);
		}
		
		protected void addLink(String name, String cmd, String iconCSS, List<String> links) {
			Link link = LinkFactory.createLink(name, cmd, getTranslator(), mainVC, this, Link.LINK);
			if(iconCSS != null) {
				link.setIconLeftCSS(iconCSS);
			}
			mainVC.put(name, link);
			links.add(name);
		}

		@Override
		protected void event(UserRequest ureq, Component source, Event event) {
			fireEvent(ureq, Event.DONE_EVENT);
			if(source instanceof Link) {
				Link link = (Link)source;
				String cmd = link.getCommand();
				if("description".equals(cmd)) {
					launchEditDescription(ureq, row);
				} else if("catalog".equals(cmd)) {
					launchCatalog(ureq, row);
				}  else if("members".equals(cmd)) {
					launchMembers(ureq, row);
				} else if("copy".equals(cmd)) {
					doCopy(ureq, row);
				} else if("download".equals(cmd)) {
					doDownload(ureq, row);
				} else if("close".equals(cmd)) {
					doCloseResource(ureq, row);
				} else if("override-close".equals(cmd)) {
					doOverrideCloseResource(ureq, row);
				} else if("delete".equals(cmd)) {
					doDelete(ureq, Collections.singletonList(row));
				}
			}
		}

		@Override
		protected void doDispose() {
			//
		}
	}
}