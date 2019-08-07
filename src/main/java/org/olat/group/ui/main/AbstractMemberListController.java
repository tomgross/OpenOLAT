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
package org.olat.group.ui.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.SearchIdentityParams;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.commons.persistence.SortKey;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableSortOptions;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableSearchEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.TextFlexiCellRenderer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.TooledStackedPanel;
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
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.UserConstants;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.ContactMessage;
import org.olat.core.util.mail.MailPackage;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.session.UserSessionManager;
import org.olat.course.assessment.manager.UserCourseInformationsManager;
import org.olat.course.member.MemberListController;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagedFlag;
import org.olat.group.BusinessGroupMembership;
import org.olat.group.BusinessGroupModule;
import org.olat.group.BusinessGroupOrder;
import org.olat.group.BusinessGroupService;
import org.olat.group.BusinessGroupShort;
import org.olat.group.model.BusinessGroupMembershipChange;
import org.olat.group.ui.main.MemberListTableModel.Cols;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.instantMessaging.InstantMessagingService;
import org.olat.instantMessaging.OpenInstantMessageEvent;
import org.olat.instantMessaging.model.Buddy;
import org.olat.instantMessaging.model.Presence;
import org.olat.modules.co.ContactFormController;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryManagedFlag;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.model.RepositoryEntryMembership;
import org.olat.repository.model.RepositoryEntryPermissionChangeEvent;
import org.olat.resource.OLATResource;
import org.olat.resource.accesscontrol.ACService;
import org.olat.resource.accesscontrol.ResourceReservation;
import org.olat.user.UserInfoMainController;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public abstract class AbstractMemberListController extends FormBasicController implements Activateable2 {

	protected static final String USER_PROPS_ID = MemberListController.class.getCanonicalName();
	
	public static final int USER_PROPS_OFFSET = 500;
	public static final int BUSINESS_COLUMNS_OFFSET = 1000;  // Must be larger than USER_PROPS_OFFSET

	public static final String TABLE_ACTION_EDIT = "tbl_edit";
	public static final String TABLE_ACTION_MAIL = "tbl_mail";
	public static final String TABLE_ACTION_REMOVE = "tbl_remove";
	public static final String TABLE_ACTION_GRADUATE = "tbl_graduate";
	public static final String TABLE_ACTION_IM = "tbl_im";
	public static final String TABLE_ACTION_HOME = "tbl_home";
	public static final String TABLE_ACTION_CONTACT = "tbl_contact";
	public static final String TABLE_ACTION_ASSESSMENT = "tbl_assessment";

	protected FlexiTableElement membersTable;
	protected MemberListTableModel memberListModel;
	protected final TooledStackedPanel toolbarPanel;
	private FormLink editButton, mailButton, removeButton;
	
	private ToolsController toolsCtrl;
	protected CloseableModalController cmc;
	private ContactFormController contactCtrl;
	private DialogBoxController confirmSendMailBox;
	private UserInfoMainController visitingCardCtrl;
	private EditMembershipController editMembersCtrl;
	private MemberLeaveConfirmationController leaveDialogBox;
	private CloseableCalloutWindowController toolsCalloutCtrl;
	private EditSingleMembershipController editSingleMemberCtrl;
	private final List<UserPropertyHandler> userPropertyHandlers;
	private List<BusinessGroup> businessGroupColumnHeaders = new ArrayList<>();

	private final AtomicInteger counter = new AtomicInteger();
	protected final RepositoryEntry repoEntry;
	private final BusinessGroup businessGroup;
	private final boolean isLastVisitVisible;
	private final boolean isAdministrativeUser;
	private final boolean chatEnabled;
	
	private final boolean readOnly;
	private boolean overrideManaged = false;
	private final boolean globallyManaged;
	
	@Autowired
	protected UserManager userManager;
	@Autowired
	protected BaseSecurity securityManager;
	@Autowired
	private BaseSecurityModule securityModule;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private BusinessGroupService businessGroupService;
	@Autowired
	private UserCourseInformationsManager userInfosMgr;
	@Autowired
	private BusinessGroupModule groupModule;
	@Autowired
	private ACService acService;
	@Autowired
	private InstantMessagingModule imModule;
	@Autowired
	private InstantMessagingService imService;
	@Autowired
	private UserSessionManager sessionManager;

	public AbstractMemberListController(UserRequest ureq, WindowControl wControl, RepositoryEntry repoEntry,
			String page,boolean readOnly,  TooledStackedPanel stackPanel) {
		this(ureq, wControl, repoEntry, null, page, readOnly, stackPanel, Util.createPackageTranslator(AbstractMemberListController.class, ureq.getLocale()));
	}
	
	public AbstractMemberListController(UserRequest ureq, WindowControl wControl, BusinessGroup group,
			String page, boolean readOnly, TooledStackedPanel stackPanel) {
		this(ureq, wControl, null, group, page, readOnly, stackPanel, Util.createPackageTranslator(AbstractMemberListController.class, ureq.getLocale()));
	}
	
	protected AbstractMemberListController(UserRequest ureq, WindowControl wControl, RepositoryEntry repoEntry, BusinessGroup group,
			String page, boolean readOnly, TooledStackedPanel stackPanel, Translator translator) {
		super(ureq, wControl, page, Util.createPackageTranslator(UserPropertyHandler.class, ureq.getLocale(), translator));
		
		this.businessGroup = group;
		this.repoEntry = repoEntry;
		this.toolbarPanel = stackPanel;
		this.readOnly = readOnly;

		globallyManaged = calcGloballyManaged();
		
		Roles roles = ureq.getUserSession().getRoles();
		chatEnabled = imModule.isEnabled() && imModule.isPrivateEnabled() && !readOnly;
		isAdministrativeUser = securityModule.isUserAllowedAdminProps(roles);
		isLastVisitVisible = securityModule.isUserLastVisitVisible(roles);
		userPropertyHandlers = userManager.getUserPropertyHandlersFor(USER_PROPS_ID, isAdministrativeUser);

		if (repoEntry != null) {
			// Ascending sorted
			businessGroupColumnHeaders = businessGroupService.findBusinessGroups(null, repoEntry, 0, -1, BusinessGroupOrder.nameAsc);
		}

		initForm(ureq);
	}
	
	public void overrideManaged(UserRequest ureq, boolean override) {
		if(ureq.getUserSession().getRoles().isOLATAdmin()) {
			overrideManaged = override;
			editButton.setVisible((!globallyManaged || overrideManaged) && !readOnly);
			removeButton.setVisible((!globallyManaged || overrideManaged) && !readOnly);
			flc.setDirty(true);
		}
	}
	
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		SortKey defaultSortKey = initColumns(columnsModel);
		
		memberListModel = new MemberListTableModel(columnsModel, imModule.isOnlineStatusEnabled(), businessGroupColumnHeaders);
		membersTable = uifactory.addTableElement(getWindowControl(), "memberList", memberListModel, 20, false, getTranslator(), formLayout);
		membersTable.setMultiSelect(true);
		membersTable.setEmtpyTableMessageKey("nomembers");
		membersTable.setAndLoadPersistedPreferences(ureq, this.getClass().getSimpleName());
		membersTable.setSearchEnabled(true);
		
		membersTable.setExportEnabled(true);
		membersTable.setSelectAllEnable(true);
		membersTable.setElementCssClass("o_sel_member_list");
		
		if(defaultSortKey != null) {
			FlexiTableSortOptions options = new FlexiTableSortOptions();
			options.setDefaultOrderBy(defaultSortKey);
			membersTable.setSortSettings(options);
		}

		editButton = uifactory.addFormLink("edit.members", formLayout, Link.BUTTON);
		editButton.setVisible((!globallyManaged || overrideManaged) && !readOnly);
		mailButton = uifactory.addFormLink("table.header.mail", formLayout, Link.BUTTON);
		removeButton = uifactory.addFormLink("table.header.remove", formLayout, Link.BUTTON);
		removeButton.setVisible((!globallyManaged || overrideManaged) && !readOnly);
	}
	
	private boolean calcGloballyManaged() {
		boolean managed = true;
		if(businessGroup != null) {
			managed &= BusinessGroupManagedFlag.isManaged(businessGroup, BusinessGroupManagedFlag.membersmanagement);
		}
		if(repoEntry != null) {
			boolean managedEntry = RepositoryEntryManagedFlag.isManaged(repoEntry, RepositoryEntryManagedFlag.membersmanagement);
			managed &= managedEntry;
			
			List<BusinessGroup> groups = businessGroupService.findBusinessGroups(null, repoEntry, 0, -1);
			for(BusinessGroup group:groups) {
				managed &= BusinessGroupManagedFlag.isManaged(group, BusinessGroupManagedFlag.membersmanagement);
			}
		}
		return managed;
	}
	
	@Override
	protected void doDispose() {
		//
	}
	
	private SortKey initColumns(FlexiTableColumnModel columnsModel) {
		SortKey defaultSortKey = null;
		String editAction = readOnly ? null : TABLE_ACTION_EDIT;
		
		if(chatEnabled) {
			DefaultFlexiColumnModel chatCol = new DefaultFlexiColumnModel(Cols.online.i18n(), Cols.online.ordinal());
			chatCol.setExportable(false);
			columnsModel.addFlexiColumnModel(chatCol);
		}
		if(isAdministrativeUser) {
			FlexiCellRenderer renderer = new StaticFlexiCellRenderer(editAction, new TextFlexiCellRenderer());
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.username.i18n(), Cols.username.ordinal(), editAction,
					true, Cols.username.name(), renderer));
			defaultSortKey = new SortKey(Cols.username.name(), true);
		}
		
		int colPos = USER_PROPS_OFFSET;
		for (UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			if (userPropertyHandler == null) continue;

			String propName = userPropertyHandler.getName();
			boolean visible = userManager.isMandatoryUserProperty(USER_PROPS_ID , userPropertyHandler);

			FlexiColumnModel col;
			if(UserConstants.FIRSTNAME.equals(propName) || UserConstants.LASTNAME.equals(propName)) {
				col = new DefaultFlexiColumnModel(userPropertyHandler.i18nColumnDescriptorLabelKey(),
						colPos, editAction, true, propName,
						new StaticFlexiCellRenderer(editAction, new TextFlexiCellRenderer()));
			} else {
				col = new DefaultFlexiColumnModel(visible, userPropertyHandler.i18nColumnDescriptorLabelKey(), colPos, true, propName);
			}
			columnsModel.addFlexiColumnModel(col);
			colPos++;
			
			if(defaultSortKey == null) {
				defaultSortKey = new SortKey(propName, true);
			}
		}

		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.firstTime.i18n(), Cols.firstTime.ordinal(), true, Cols.firstTime.name()));
		if(isLastVisitVisible) {
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.lastTime.i18n(), Cols.lastTime.ordinal(), true, Cols.lastTime.name()));
		}
		
		CourseRoleCellRenderer roleRenderer = new CourseRoleCellRenderer(getLocale());
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.role.i18n(), Cols.role.ordinal(), true, Cols.role.name(), roleRenderer));
		if(repoEntry != null) {
			GroupCellRenderer groupRenderer = new GroupCellRenderer();
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.groups.i18n(), Cols.groups.ordinal(), true, Cols.groups.name(), groupRenderer));
		}
		
		DefaultFlexiColumnModel toolsCol = new DefaultFlexiColumnModel(Cols.tools.i18n(), Cols.tools.ordinal());
		toolsCol.setExportable(false);
		toolsCol.setAlwaysVisible(true);
		columnsModel.addFlexiColumnModel(toolsCol);
		return defaultSortKey;
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == membersTable) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				String cmd = se.getCommand();
				MemberView row = memberListModel.getObject(se.getIndex());
				if(TABLE_ACTION_IM.equals(cmd)) {
					doIm(ureq, row);
				} else if(TABLE_ACTION_EDIT.equals(cmd)) {
					openEdit(ureq, row);
				}
			} else if(event instanceof FlexiTableSearchEvent) {
				String cmd = event.getCommand();
				if(FlexiTableSearchEvent.SEARCH.equals(event.getCommand())) {
					FlexiTableSearchEvent se = (FlexiTableSearchEvent)event;
					String search = se.getSearch();
					doSearch(search);
				} else if(FlexiTableSearchEvent.QUICK_SEARCH.equals(event.getCommand())) {
					FlexiTableSearchEvent se = (FlexiTableSearchEvent)event;
					String search = se.getSearch();
					doSearch(search);
				} else if(FlexiTableSearchEvent.RESET.getCommand().equals(cmd)) {
					doResetSearch();
				}
			}
		} else if(editButton == source) {
			List<MemberView> selectedItems = getMultiSelectedRows();
			openEdit(ureq, selectedItems);
		} else if(mailButton == source) {
			List<MemberView> selectedItems = getMultiSelectedRows();
			doSendMail(ureq, selectedItems);
		} else if(removeButton == source) {
			List<MemberView> selectedItems = getMultiSelectedRows();
			confirmDelete(ureq, selectedItems);
		} else if(source instanceof FormLink) {
			FormLink link = (FormLink)source;
			String cmd = link.getCmd();
			if("tools".equals(cmd)) {
				MemberView row = (MemberView)link.getUserObject();
				doOpenTools(ureq, row, link);
			} else if("im".equals(cmd)) {
				MemberView row = (MemberView)link.getUserObject();
				doIm(ureq, row);
			}
		}
		super.formInnerEvent(ureq, source, event);
	}
	
	private List<MemberView> getMultiSelectedRows() {
		Set<Integer> selections = membersTable.getMultiSelectedIndex();
		List<MemberView> rows = new ArrayList<>(selections.size());
		if(selections.isEmpty()) {
			//do nothing
		} else {
			for(Integer i:selections) {
				int index = i.intValue();
				if(index >= 0 && index < memberListModel.getRowCount()) {
					MemberView row = memberListModel.getObject(index);
					if(row != null) {
						rows.add(row);
					}
				}
			}
		}
		return rows;
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == leaveDialogBox) {
			if (Event.DONE_EVENT == event) {
				List<Identity> members = leaveDialogBox.getIdentities();
				doLeave(members, leaveDialogBox.isSendMail());
				reloadModel();
			}
			cmc.deactivate();
			cleanUpPopups();
		} else if(source == editMembersCtrl) {
			cmc.deactivate();
			if(event instanceof MemberPermissionChangeEvent) {
				MemberPermissionChangeEvent e = (MemberPermissionChangeEvent)event;
				doConfirmChangePermission(ureq, e, editMembersCtrl.getMembers());
			}
		} else if(source == editSingleMemberCtrl) {
			cmc.deactivate();
			cleanUpPopups();
			if(event instanceof MemberPermissionChangeEvent) {
				MemberPermissionChangeEvent e = (MemberPermissionChangeEvent)event;
				doConfirmChangePermission(ureq, e, null);
			}
		} else if(confirmSendMailBox == source) {
			boolean sendMail = DialogBoxUIFactory.isYesEvent(event) || DialogBoxUIFactory.isOkEvent(event);
			MailConfirmation confirmation = (MailConfirmation)confirmSendMailBox.getUserObject();
			MemberPermissionChangeEvent e =confirmation.getE();
			if(e.getMember() != null) {
				doChangePermission(ureq, e, sendMail);
			} else {
				doChangePermission(ureq, e, confirmation.getMembers(), sendMail);
			}
		} else if (source == contactCtrl) {
			if(cmc != null) {
				cmc.deactivate();
			} else {
				toolbarPanel.popController(contactCtrl);
			}
			cleanUpPopups();
		} else if(toolsCtrl == source) {
			if(event == Event.DONE_EVENT) {
				toolsCalloutCtrl.deactivate();
				cleanUpPopups();
			}
		} else if (source == cmc) {
			cleanUpPopups();
		}
	}
	
	/**
	 * Aggressive clean up all popup controllers
	 */
	protected void cleanUpPopups() {
		removeAsListenerAndDispose(cmc);
		removeAsListenerAndDispose(editMembersCtrl);
		removeAsListenerAndDispose(editSingleMemberCtrl);
		removeAsListenerAndDispose(leaveDialogBox);
		removeAsListenerAndDispose(contactCtrl);
		cmc = null;
		contactCtrl = null;
		leaveDialogBox = null;
		editMembersCtrl = null;
		editSingleMemberCtrl = null;
	}
	
	protected void confirmDelete(UserRequest ureq, List<MemberView> members) {
		if(members.isEmpty()) {
			showWarning("error.select.one.user");
		} else {
			int numOfOwners =
					repoEntry == null ? businessGroupService.countMembers(businessGroup, GroupRoles.coach.name())
					: repositoryService.countMembers(repoEntry, GroupRoles.owner.name());
			
			int numOfRemovedOwner = 0;
			List<Long> identityKeys = new ArrayList<Long>();
			for(MemberView member:members) {
				identityKeys.add(member.getIdentityKey());
				if(member.getMembership().isOwner()) {
					numOfRemovedOwner++;
				}
			}
			if(numOfRemovedOwner == 0 || numOfOwners - numOfRemovedOwner > 0) {
				List<Identity> ids = securityManager.loadIdentityByKeys(identityKeys);
				leaveDialogBox = new MemberLeaveConfirmationController(ureq, getWindowControl(), ids, repoEntry != null);
				listenTo(leaveDialogBox);
				
				cmc = new CloseableModalController(getWindowControl(), translate("close"), leaveDialogBox.getInitialComponent(),
						true, translate("edit.member"));
				cmc.activate();
				listenTo(cmc);
			} else {
				showWarning("error.atleastone");
			}
		}
	}
	
	protected void openEdit(UserRequest ureq, MemberView member) {
		if(editSingleMemberCtrl != null) return;
		
		Identity identity = securityManager.loadIdentityByKey(member.getIdentityKey());
		editSingleMemberCtrl = new EditSingleMembershipController(ureq, getWindowControl(), identity, repoEntry, businessGroup, false, overrideManaged);
		listenTo(editSingleMemberCtrl);
		cmc = new CloseableModalController(getWindowControl(), translate("close"), editSingleMemberCtrl.getInitialComponent(),
				true, translate("edit.member"));
		cmc.activate();
		listenTo(cmc);
	}
	
	protected void openEdit(UserRequest ureq, List<MemberView> members) {
		if(members.isEmpty()) {
			showWarning("error.select.one.user");
		} else {
			List<Long> identityKeys = getMemberKeys(members);
			List<Identity> identities = securityManager.loadIdentityByKeys(identityKeys);
			if(identities.size() == 1) {
				editSingleMemberCtrl = new EditSingleMembershipController(ureq, getWindowControl(), identities.get(0), repoEntry, businessGroup, false, overrideManaged);
				listenTo(editSingleMemberCtrl);
				cmc = new CloseableModalController(getWindowControl(), translate("close"), editSingleMemberCtrl.getInitialComponent(),
						true, translate("edit.member"));
			} else {
				editMembersCtrl = new EditMembershipController(ureq, getWindowControl(), identities, repoEntry, businessGroup, overrideManaged);
				listenTo(editMembersCtrl);
				cmc = new CloseableModalController(getWindowControl(), translate("close"), editMembersCtrl.getInitialComponent(),
						true, translate("edit.member"));
			}
			cmc.activate();
			listenTo(cmc);
		}
	}
	
	protected void doSearch(String search) {
		getSearchParams().setSearchString(search);
		reloadModel();
	}
	
	protected void doResetSearch() {
		getSearchParams().setSearchString(null);
		reloadModel();
	}
	
	private void doOpenTools(UserRequest ureq, MemberView row, FormLink link) {
		removeAsListenerAndDispose(toolsCtrl);
		removeAsListenerAndDispose(toolsCalloutCtrl);

		toolsCtrl = new ToolsController(ureq, getWindowControl(), row);
		listenTo(toolsCtrl);

		toolsCalloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(),
				toolsCtrl.getInitialComponent(), link.getFormDispatchId(), "", true, "");
		listenTo(toolsCalloutCtrl);
		toolsCalloutCtrl.activate();
	}
	
	/**
	 * Open private chat
	 * @param ureq
	 * @param member
	 */
	protected void doIm(UserRequest ureq, MemberView member) {
		Buddy buddy = imService.getBuddyById(member.getIdentityKey());
		OpenInstantMessageEvent e = new OpenInstantMessageEvent(ureq, buddy);
		ureq.getUserSession().getSingleUserEventCenter().fireEventToListenersOf(e, InstantMessagingService.TOWER_EVENT_ORES);
	}
	
	protected void doConfirmChangePermission(UserRequest ureq, MemberPermissionChangeEvent e, List<Identity> members) {
		boolean groupChangesEmpty = e.getGroupChanges() == null || e.getGroupChanges().isEmpty();
		boolean repoChangesEmpty = e.getRepoOwner() == null && e.getRepoParticipant() == null && e.getRepoTutor() == null;
		if(groupChangesEmpty && repoChangesEmpty) {
			//nothing to do
			return;
		}

		boolean mailMandatory = groupModule.isMandatoryEnrolmentEmail(ureq.getUserSession().getRoles());
		if(mailMandatory) {
			if(members == null) {
				doChangePermission(ureq, e, true);
			} else {
				doChangePermission(ureq, e, members, true);
			}
		} else {
			confirmSendMailBox = activateYesNoDialog(ureq, null, translate("dialog.modal.bg.send.mail"), confirmSendMailBox);
			confirmSendMailBox.setUserObject(new MailConfirmation(e, members));
		}
	}
	
	protected void doChangePermission(UserRequest ureq, MemberPermissionChangeEvent e, boolean sendMail) {
		MailPackage mailing = new MailPackage(sendMail);
		if(repoEntry != null) {
			List<RepositoryEntryPermissionChangeEvent> changes = Collections.singletonList((RepositoryEntryPermissionChangeEvent)e);
			repositoryManager.updateRepositoryEntryMemberships(getIdentity(), ureq.getUserSession().getRoles(), repoEntry, changes, mailing);
		}

		businessGroupService.updateMemberships(getIdentity(), e.getGroupChanges(), mailing);
		//make sure all is committed before loading the model again (I see issues without)
		DBFactory.getInstance().commitAndCloseSession();
		reloadModel();
	}
	
	protected void doChangePermission(UserRequest ureq, MemberPermissionChangeEvent changes, List<Identity> members, boolean sendMail) {
		MailPackage mailing = new MailPackage(sendMail);
		if(repoEntry != null) {
			List<RepositoryEntryPermissionChangeEvent> repoChanges = changes.generateRepositoryChanges(members);
			repositoryManager.updateRepositoryEntryMemberships(getIdentity(), ureq.getUserSession().getRoles(), repoEntry, repoChanges, mailing);
		}

		//commit all changes to the group memberships
		List<BusinessGroupMembershipChange> allModifications = changes.generateBusinessGroupMembershipChange(members);
		businessGroupService.updateMemberships(getIdentity(), allModifications, mailing);

		reloadModel();
	}
	
	protected void doLeave(List<Identity> members, boolean sendMail) {
		MailPackage mailing = new MailPackage(sendMail);
		if(repoEntry != null) {
			businessGroupService.removeMembers(getIdentity(), members, repoEntry.getOlatResource(), mailing);
			repositoryManager.removeMembers(getIdentity(), members, repoEntry, mailing);
		} else {
			businessGroupService.removeMembers(getIdentity(), members, businessGroup.getResource(), mailing);
		}
		reloadModel();
	}
	
	protected void doSendMail(UserRequest ureq, List<MemberView> members) {
		List<Long> identityKeys = getMemberKeys(members);
		List<Identity> identities = securityManager.loadIdentityByKeys(identityKeys);
		if(identities.isEmpty()) {
			showWarning("error.msg.send.no.rcps");
			return;
		}
		
		ContactMessage contactMessage = new ContactMessage(getIdentity());
		String name = repoEntry != null ? repoEntry.getDisplayname() : businessGroup.getName();
		ContactList contactList = new ContactList(name);
		contactList.addAllIdentites(identities);
		contactMessage.addEmailTo(contactList);
		
		contactCtrl = new ContactFormController(ureq, getWindowControl(), true, false, false, contactMessage);
		listenTo(contactCtrl);

		cmc = new CloseableModalController(getWindowControl(), translate("close"), contactCtrl.getInitialComponent(),
				true, translate("mail.member"));
		cmc.activate();
		listenTo(cmc);
	}
	
	protected void doGraduate(List<MemberView> members) {
		if(businessGroup != null) {
			List<Long> identityKeys = getMemberKeys(members);
			List<Identity> identitiesToGraduate = securityManager.loadIdentityByKeys(identityKeys);
			businessGroupService.moveIdentityFromWaitingListToParticipant(getIdentity(), identitiesToGraduate,
					businessGroup, null);
		} else {
			Map<Long, BusinessGroup> groupsMap = new HashMap<>();
			Map<BusinessGroup, List<Identity>> graduatesMap = new HashMap<>();
			for(MemberView member:members) {
				List<BusinessGroupShort> groups = member.getGroups();
				if(groups != null && groups.size() > 0) {
					Identity memberIdentity = securityManager.loadIdentityByKey(member.getIdentityKey());
					for(BusinessGroupShort group:groups) {
						if(businessGroupService.hasRoles(memberIdentity, group, GroupRoles.waiting.name())) {
							BusinessGroup fullGroup = groupsMap.get(group.getKey());
							if(fullGroup == null) {
								fullGroup = businessGroupService.loadBusinessGroup(group.getKey());
								groupsMap.put(group.getKey(), fullGroup);
							}
							
							List<Identity> identitiesToGraduate = graduatesMap.get(fullGroup);
							if(identitiesToGraduate == null) {
								 identitiesToGraduate = new ArrayList<>();
								 graduatesMap.put(fullGroup, identitiesToGraduate);
							}
							identitiesToGraduate.add(memberIdentity);
						}
					}
				}
			}
			
			for(Map.Entry<BusinessGroup, List<Identity>> entry:graduatesMap.entrySet()) {
				BusinessGroup fullGroup = entry.getKey();
				List<Identity> identitiesToGraduate = entry.getValue();
				businessGroupService.moveIdentityFromWaitingListToParticipant(getIdentity(), identitiesToGraduate,
						fullGroup, null);
			}
		}
		reloadModel();
	}
	
	protected void doOpenVisitingCard(UserRequest ureq, MemberView member) {
		removeAsListenerAndDispose(visitingCardCtrl);
		Identity choosenIdentity = securityManager.loadIdentityByKey(member.getIdentityKey());
		visitingCardCtrl = new UserInfoMainController(ureq, getWindowControl(), choosenIdentity, false, false);
		listenTo(visitingCardCtrl);
		
		String fullname = userManager.getUserDisplayName(choosenIdentity);
		toolbarPanel.pushController(fullname, visitingCardCtrl);
	}
	
	protected void doOpenContact(UserRequest ureq, MemberView member) {
		removeAsListenerAndDispose(contactCtrl);
		
		Identity choosenIdentity = securityManager.loadIdentityByKey(member.getIdentityKey());
		String fullname = userManager.getUserDisplayName(choosenIdentity);
		
		ContactMessage cmsg = new ContactMessage(ureq.getIdentity());
		ContactList emailList = new ContactList(fullname);
		emailList.add(choosenIdentity);
		cmsg.addEmailTo(emailList);
		
		OLATResourceable ores = OresHelper.createOLATResourceableType("Contact");
		WindowControl bwControl = addToHistory(ureq, ores, null);
		contactCtrl = new ContactFormController(ureq, bwControl, true, false, false, cmsg);
		listenTo(contactCtrl);
		
		toolbarPanel.pushController(fullname, contactCtrl);
	}
	
	protected abstract void doOpenAssessmentTool(UserRequest ureq, MemberView member);
	
	protected List<Long> getMemberKeys(List<MemberView> members) {
		List<Long> keys = new ArrayList<Long>(members.size());
		if(members != null && !members.isEmpty()) {
			for(MemberView member:members) {
				keys.add(member.getIdentityKey());
			}
		}
		return keys;
	}

	protected abstract SearchMembersParams getSearchParams();
	
	public void reloadModel() {
		updateTableModel(getSearchParams());
	}

	protected List<MemberView> updateTableModel(SearchMembersParams params) {
		//course membership
		boolean managedMembersRepo = 
				RepositoryEntryManagedFlag.isManaged(repoEntry, RepositoryEntryManagedFlag.membersmanagement);
		
		List<RepositoryEntryMembership> repoMemberships =
				repoEntry == null ? Collections.<RepositoryEntryMembership>emptyList()
				: repositoryManager.getRepositoryEntryMembership(repoEntry);

		//groups membership
		List<BusinessGroup> groups = 
				repoEntry == null ? Collections.singletonList(businessGroup)
				: businessGroupService.findBusinessGroups(null, repoEntry, 0, -1);
				
		List<Long> groupKeys = new ArrayList<Long>();
		Map<Long,BusinessGroupShort> keyToGroupMap = new HashMap<>();
		for(BusinessGroup group:groups) {
			groupKeys.add(group.getKey());
			keyToGroupMap.put(group.getKey(), group);
		}

		List<BusinessGroupMembership> memberships = groups.isEmpty() ? Collections.<BusinessGroupMembership>emptyList() :
			businessGroupService.getBusinessGroupsMembership(groups);

		//get identities
		Set<Long> identityKeys = new HashSet<>();
		for(RepositoryEntryMembership membership: repoMemberships) {
			identityKeys.add(membership.getIdentityKey());
		}
		for(BusinessGroupMembership membership:memberships) {
			identityKeys.add(membership.getIdentityKey());
		}
		
		List<Identity> identities;
		if(identityKeys.isEmpty()) {
			identities = new ArrayList<>(0);
		} else  {
			identities = filterIdentities(params, identityKeys);
		}

		Map<Long,MemberView> keyToMemberMap = new HashMap<>();
		List<MemberView> memberList = new ArrayList<>();
		Locale locale = getLocale();

		//reservations
		if(params.isPending()) {
			List<OLATResource> resourcesForReservations = new ArrayList<>();
			if(repoEntry != null) {
				resourcesForReservations.add(repoEntry.getOlatResource());
			}
			for(BusinessGroup group:groups) {
				resourcesForReservations.add(group.getResource());
			}
			List<ResourceReservation> reservations = acService.getReservations(resourcesForReservations);
			List<Long> pendingIdentityKeys = new ArrayList<>(reservations.size());
			for(ResourceReservation reservation:reservations) {
				pendingIdentityKeys.add(reservation.getIdentity().getKey());
			}
			
			if(StringHelper.containsNonWhitespace(params.getSearchString())
					|| StringHelper.containsNonWhitespace(params.getLogin())
					|| (params.getUserPropertiesSearch() != null && !params.getUserPropertiesSearch().isEmpty())) {
				
				List<Identity> pendingIdentities = filterIdentities(params, pendingIdentityKeys);
				pendingIdentityKeys.retainAll(PersistenceHelper.toKeys(pendingIdentities));
			}
			
			for(ResourceReservation reservation:reservations) {
				Identity identity = reservation.getIdentity();
				if(pendingIdentityKeys.contains(identity.getKey())) {
					MemberView member = new MemberView(identity, userPropertyHandlers, locale);
					member.getMembership().setPending(true);
					memberList.add(member);
					forgeLinks(member);
					keyToMemberMap.put(identity.getKey(), member);
				}
			}
		}
		
		Long me = getIdentity().getKey();
		Set<Long> loadStatus = new HashSet<>();
		for(Identity identity:identities) {
			MemberView member = new MemberView(identity, userPropertyHandlers, locale);
			if(chatEnabled) {
				if(identity.getKey().equals(me)) {
					member.setOnlineStatus("me");
				} else if(sessionManager.isOnline(identity.getKey())) {
					loadStatus.add(identity.getKey());
				} else {
					member.setOnlineStatus(Presence.unavailable.name());
				}
			}
			memberList.add(member);
			forgeLinks(member);
			keyToMemberMap.put(identity.getKey(), member);
		}
		
		if(loadStatus.size() > 0) {
			List<Long> statusToLoadList = new ArrayList<>(loadStatus);
			Map<Long,String> statusMap = imService.getBuddyStatus(statusToLoadList);
			for(Long toLoad:statusToLoadList) {
				String status = statusMap.get(toLoad);
				MemberView member = keyToMemberMap.get(toLoad);
				if(status == null) {
					member.setOnlineStatus(Presence.available.name());	
				} else {
					member.setOnlineStatus(status);	
				}
			}
		}

		for(BusinessGroupMembership membership:memberships) {
			Long identityKey = membership.getIdentityKey();
			MemberView memberView = keyToMemberMap.get(identityKey);
			if(memberView != null) {
				memberView.setFirstTime(membership.getCreationDate());
				memberView.setLastTime(membership.getLastModified());
				if(membership.isOwner()) {
					memberView.getMembership().setGroupTutor(true);
				}
				if(membership.isParticipant()) {
					memberView.getMembership().setGroupParticipant(true);
				}
				if(membership.isWaiting()) {
					memberView.getMembership().setGroupWaiting(true);
				}
				
				Long groupKey = membership.getGroupKey();
				BusinessGroupShort group = keyToGroupMap.get(groupKey);
				memberView.addGroup(group);
			}
		}
		
		for(RepositoryEntryMembership membership:repoMemberships) {
			Long identityKey = membership.getIdentityKey();
			MemberView memberView = keyToMemberMap.get(identityKey);
			if(memberView != null) {
				memberView.setFirstTime(membership.getCreationDate());
				memberView.setLastTime(membership.getLastModified());
				memberView.getMembership().setManagedMembersRepo(managedMembersRepo);
				if(membership.isOwner()) {
					memberView.getMembership().setRepoOwner(true);
				}
				if(membership.isCoach()) {
					memberView.getMembership().setRepoTutor(true);
				}
				if(membership.isParticipant()) {
					memberView.getMembership().setRepoParticipant(true);
				}
			}
		}
		
		if(repoEntry != null) {
			Map<Long,Date> lastLaunchDates = userInfosMgr.getRecentLaunchDates(repoEntry.getOlatResource());
			for(MemberView memberView:keyToMemberMap.values()) {
				Long identityKey = memberView.getIdentityKey();
				Date date = lastLaunchDates.get(identityKey);
				memberView.setLastTime(date);
			}
		}
		
		//the order of the filter is important
		filterByRoles(memberList, params);
		filterByOrigin(memberList, params);
		
		memberListModel.setObjects(memberList);
		membersTable.reset(true, true, true);
		return memberList;
	}
	
	private List<Identity> filterIdentities(SearchMembersParams params, Collection<Long> identityKeys) {
		SearchIdentityParams idParams = new SearchIdentityParams();
		if(StringHelper.containsNonWhitespace(params.getSearchString())) {
			String searchString = params.getSearchString();
			
			Map<String,String> propertiesSearch = new HashMap<>();
			for(UserPropertyHandler handler:userPropertyHandlers) {
				propertiesSearch.put(handler.getName(), searchString);
			}
			idParams.setLogin(searchString);
			idParams.setUserProperties(propertiesSearch);
		} else {
			if(params.getUserPropertiesSearch() != null && !params.getUserPropertiesSearch().isEmpty()) {
				idParams.setUserProperties(params.getUserPropertiesSearch());
			}
			if(StringHelper.containsNonWhitespace(params.getLogin())) {
				idParams.setLogin(params.getLogin());
			}
		}
		
		List<Long> identityKeyList = new ArrayList<>(identityKeys);
		List<Identity> identities = new ArrayList<>(identityKeyList.size());

		int count = 0;
		int batch = 500;
		do {
			int toIndex = Math.min(count + batch, identityKeyList.size());
			List<Long> toLoad = identityKeyList.subList(count, toIndex);
			idParams.setIdentityKeys(toLoad);

			List<Identity> batchOfIdentities = securityManager.getIdentitiesByPowerSearch(idParams, 0, -1);
			identities.addAll(batchOfIdentities);
			count += batch;
		} while(count < identityKeyList.size());
		
		return identities;
	}
	
	protected void forgeLinks(MemberView row) {
		FormLink toolsLink = uifactory.addFormLink("tools_" + counter.incrementAndGet(), "tools", "", null, null, Link.NONTRANSLATED);
		toolsLink.setIconLeftCSS("o_icon o_icon_actions o_icon-lg");
		toolsLink.setUserObject(row);
		row.setToolsLink(toolsLink);
		
		FormLink chatLink = uifactory.addFormLink("tools_" + counter.incrementAndGet(), "im", "", null, null, Link.NONTRANSLATED);
		chatLink.setIconLeftCSS("o_icon o_icon_status_unavailable");
		chatLink.setUserObject(row);
		row.setChatLink(chatLink);
	}
	
	private void filterByOrigin(List<MemberView> memberList, SearchMembersParams params) {
		if(params.isGroupOrigin() && params.isRepoOrigin()) {
			//do nothing not very useful :-)
		} else if(params.isGroupOrigin()) {
			for(Iterator<MemberView> it=memberList.iterator(); it.hasNext(); ) {
				CourseMembership m = it.next().getMembership();
				if(!m.isGroupTutor() && !m.isGroupParticipant() && !m.isGroupWaiting()) {
					it.remove();
				}
			}
		} else if(params.isRepoOrigin()) {
			for(Iterator<MemberView> it=memberList.iterator(); it.hasNext(); ) {
				CourseMembership m = it.next().getMembership();
				if(!m.isRepoOwner() && !m.isRepoTutor() && !m.isRepoParticipant()) {
					it.remove();
				}
			}
		}
	}
	
	/**
	 * This filter method preserve the multiple roles of a member. If we want only the waiting list but
	 * a member is in the waiting list and owner of the course, we want it to know.
	 * @param memberList
	 * @param params
	 * @return
	 */
	private void filterByRoles(List<MemberView> memberList, SearchMembersParams params) {
		List<MemberView> members = new ArrayList<MemberView>(memberList);

		if(params.isRepoOwners()) {
			for(Iterator<MemberView> it=members.iterator(); it.hasNext(); ) {
				if(it.next().getMembership().isRepoOwner()) {
					it.remove();
				}
			}
		}
		
		if(params.isRepoTutors()) {
			for(Iterator<MemberView> it=members.iterator(); it.hasNext(); ) {
				if(it.next().getMembership().isRepoTutor()) {
					it.remove();
				}
			}
		}
		
		if(params.isRepoParticipants()) {
			for(Iterator<MemberView> it=members.iterator(); it.hasNext(); ) {
				if(it.next().getMembership().isRepoParticipant()) {
					it.remove();
				}
			}
		}
		
		if(params.isGroupTutors()) {
			for(Iterator<MemberView> it=members.iterator(); it.hasNext(); ) {
				if(it.next().getMembership().isGroupTutor()) {
					it.remove();
				}
			}
		}
		
		if(params.isGroupParticipants()) {
			for(Iterator<MemberView> it=members.iterator(); it.hasNext(); ) {
				if(it.next().getMembership().isGroupParticipant()) {
					it.remove();
				}
			}
		}
		
		if(params.isGroupWaitingList()) {
			for(Iterator<MemberView> it=members.iterator(); it.hasNext(); ) {
				if(it.next().getMembership().isGroupWaiting()) {
					it.remove();
				}
			}
		}
		
		if(params.isPending()) {
			for(Iterator<MemberView> it=members.iterator(); it.hasNext(); ) {
				if(it.next().getMembership().isPending()) {
					it.remove();
				}
			}
		}
		
		memberList.removeAll(members);
	}
	
	private class MailConfirmation {
		private final List<Identity> members;
		private final MemberPermissionChangeEvent e;
		
		public MailConfirmation(MemberPermissionChangeEvent e, List<Identity> members) {
			this.e = e;
			this.members = members;
		}

		public List<Identity> getMembers() {
			return members;
		}

		public MemberPermissionChangeEvent getE() {
			return e;
		}
	}
	
	private class ToolsController extends BasicController {
		
		private final MemberView row;
		
		private final VelocityContainer mainVC;
		
		public ToolsController(UserRequest ureq, WindowControl wControl, MemberView row) {
			super(ureq, wControl);
			this.row = row;
			
			mainVC = createVelocityContainer("tools");
			List<String> links = new ArrayList<>();
			
			//links
			addLink("home", TABLE_ACTION_HOME, "o_icon o_icon_home", links);
			addLink("contact", TABLE_ACTION_CONTACT, "o_icon o_icon_mail", links);
			if(repoEntry != null && "CourseModule".equals(repoEntry.getOlatResource().getResourceableTypeName())) {
				addLink("assessment", TABLE_ACTION_ASSESSMENT, "o_icon o_icon_certificate", links);
			}
			
			links.add("-");
			
			if(row.getMembership().isGroupWaiting() && !readOnly) {
				addLink("table.header.graduate", TABLE_ACTION_GRADUATE, "o_icon o_icon_graduate", links);
			}

			if(!readOnly) {
				addLink("edit.member", TABLE_ACTION_EDIT, "o_icon o_icon_edit", links);
			}
			
			if(!globallyManaged || overrideManaged) {
				addLink("table.header.remove", TABLE_ACTION_REMOVE, "o_icon o_icon_remove", links);
			}

			mainVC.contextPut("links", links);
			putInitialPanel(mainVC);
		}
		
		private void addLink(String name, String cmd, String iconCSS, List<String> links) {
			Link link = LinkFactory.createLink(name, cmd, getTranslator(), mainVC, this, Link.LINK);
			if(iconCSS != null) {
				link.setIconLeftCSS(iconCSS);
			}
			mainVC.put(name, link);
			links.add(name);
		}

		@Override
		protected void doDispose() {
			//
		}
		
		@Override
		protected void event(UserRequest ureq, Component source, Event event) {
			fireEvent(ureq, Event.DONE_EVENT);
			if(source instanceof Link) {
				Link link = (Link)source;
				String cmd = link.getCommand();
				if(TABLE_ACTION_GRADUATE.equals(cmd)) {
					doGraduate(Collections.singletonList(row));
				} else if(TABLE_ACTION_EDIT.equals(cmd)) {
					openEdit(ureq, row);
				} else if(TABLE_ACTION_REMOVE.equals(cmd)) {
					confirmDelete(ureq, Collections.singletonList(row));
				} else if(TABLE_ACTION_HOME.equals(cmd)) {
					doOpenVisitingCard(ureq, row);
				} else if(TABLE_ACTION_CONTACT.equals(cmd)) {
					doOpenContact(ureq, row);
				} else if(TABLE_ACTION_ASSESSMENT.equals(cmd)) {
					doOpenAssessmentTool(ureq, row);
				}
			}
		}
	}
}
