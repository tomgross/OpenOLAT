/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.admin.user;

import org.olat.admin.user.course.CourseOverviewController;
import org.olat.basesecurity.*;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.services.notifications.ui.NotificationSubscriptionController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.stack.BreadcrumbedStackedPanel;
import org.olat.core.gui.components.tabbedpane.Tab;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.course.certificate.ui.CertificateAndEfficiencyStatementListController;
import org.olat.group.ui.main.UserAdminBusinessGroupListController;
import org.olat.ldap.LDAPLoginManager;
import org.olat.ldap.LDAPLoginModule;
import org.olat.properties.Property;
import org.olat.user.*;
import org.olat.util.logging.activity.LoggingResourceable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  Initial Date:  Jul 29, 2003
 *  @author Sabina Jeger
 *  <pre>
 *  Complete rebuild on 17. jan 2006 by Florian Gnaegi
 *  
 *  Functionality to change or view all kind of things for this user 
 *  based on the configuration for the user manager. 
 *  This controller should only be used by the UserAdminMainController.
 *  
 * </pre>
 */
public class UserAdminController extends BasicController implements Activateable2 {

	// NLS support
	private static final String NLS_ERROR_NOACCESS_TO_USER = "error.noaccess.to.user";
	private static final String NLS_FOUND_PROPERTY	= "found.property";
	private static final String NLS_EDIT_UPROFILE = "edit.uprofile";
	private static final String NLS_EDIT_UPREFS			= "edit.uprefs";
	private static final String NLS_EDIT_UPWD 			= "edit.upwd";
	private static final String NLS_EDIT_UAUTH 			= "edit.uauth";
	private static final String NLS_EDIT_UPROP			= "edit.uprop";
	private static final String NLS_EDIT_UROLES			= "edit.uroles";
	private static final String NLS_EDIT_UQUOTA			= "edit.uquota";
	private static final String NLS_VIEW_GROUPS 		= "view.groups";
	private static final String NLS_VIEW_COURSES		= "view.courses";
	private static final String NLS_VIEW_EFF_STATEMENTS 		= "view.effStatements";
	private static final String NLS_VIEW_SUBSCRIPTIONS 		= "view.subscriptions";
	
	private VelocityContainer myContent;
		
	private Identity myIdentity = null;

	// controllers used in tabbed pane
	private TabbedPane userTabP;
	private Controller prefsCtr, propertiesCtr, pwdCtr, quotaCtr, policiesCtr, rolesCtr, userShortDescrCtr;
	private DisplayPortraitController portraitCtr;
	private UserAuthenticationsEditorController authenticationsCtr;
	private Link backLink;
	private ProfileAndHomePageEditController userProfileCtr;
	private CourseOverviewController courseCtr;
	private CertificateAndEfficiencyStatementListController efficicencyCtrl;

	private final boolean isOlatAdmin;
	
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private LDAPLoginModule ldapLoginModule;
	@Autowired
	private LDAPLoginManager ldapLoginManager;
	@Autowired
	private ApplicationContext applicationContext;

	/**
	 * Constructor that creates a back - link as default
	 * @param ureq
	 * @param wControl
	 * @param identity
	 */
	public UserAdminController(UserRequest ureq, WindowControl wControl, Identity identity) {
		super(ureq, wControl);
		isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();

		if (!securityManager.isIdentityPermittedOnResourceable(
				ureq.getIdentity(), 
				Constants.PERMISSION_ACCESS, 
				OresHelper.lookupType(this.getClass()))) {
			throw new OLATSecurityException("Insufficient permissions to access UserAdminController");
		}
		
		myIdentity = identity;
				
		if (allowedToManageUser(ureq, myIdentity)) {			
			myContent = createVelocityContainer("udispatcher");
			backLink = LinkFactory.createLinkBack(myContent, this);
			userShortDescrCtr = new UserShortDescription(ureq, wControl, identity);
			myContent.put("userShortDescription", userShortDescrCtr.getInitialComponent());
			
			setBackButtonEnabled(true); // default
			initTabbedPane(myIdentity, ureq);
			exposeUserDataToVC(ureq, myIdentity);					
			putInitialPanel(myContent);
		} else {
			String supportAddr = WebappHelper.getMailConfig("mailSupport");			
			showWarning(NLS_ERROR_NOACCESS_TO_USER, supportAddr);			
			putInitialPanel(new Panel("empty"));
		}
	}
	
	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
		String entryPoint = entries.get(0).getOLATResourceable().getResourceableTypeName();
		if("tab".equals(entryPoint)) {
			userTabP.activate(ureq, entries, state);
		} else if("table".equals(entryPoint)) {
			if(entries.size() > 2) {
				List<ContextEntry> subEntries = entries.subList(2, entries.size());
				userTabP.activate(ureq, subEntries, state);
			}
		} else if (userTabP != null) {
			userTabP.setSelectedPane(translate(entryPoint));
		}
	}

	/**
	 * @param backButtonEnabled
	 */
	public void setBackButtonEnabled(boolean backButtonEnabled) {
		if (myContent != null) {
			myContent.contextPut("showButton", Boolean.valueOf(backButtonEnabled));
		}
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == backLink) {
			fireEvent(ureq, Event.BACK_EVENT);
		} else if (source == userTabP) {
			userTabP.addToHistory(ureq, getWindowControl());
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == propertiesCtr) {
			if (event.getCommand().equals("PropFound")){
				PropFoundEvent foundEvent = (PropFoundEvent) event;
				Property myfoundProperty = foundEvent.getProperty();				
				this.showInfo(NLS_FOUND_PROPERTY,myfoundProperty.getKey().toString());								
			}
		} else if (source == pwdCtr) {
			if (event == Event.DONE_EVENT) {
				// rebuild authentication tab, could be wrong now
				if (authenticationsCtr != null) authenticationsCtr.rebuildAuthenticationsTableDataModel();
			}
		} else if (source == userProfileCtr){
			if (event == Event.DONE_EVENT){
				//reload profile data on top
				myIdentity = (Identity) DBFactory.getInstance().loadObject(myIdentity);
				exposeUserDataToVC(ureq, myIdentity);
				userProfileCtr.resetForm(ureq);
			}
		}
	}

	/**
	 * Check if user allowed to modify this identity. Only modification of user
	 * that have lower rights is allowed. No one exept admins can manage usermanager
	 * and admins
	 * @param ureq
	 * @param identity
	 * @return boolean
	 */
	private boolean allowedToManageUser(UserRequest ureq, Identity identity) {
		//fxdiff 	FXOLAT-184 prevent editing of users that are in frentix-superadmin group (except "frentix" wants to change own profile)
		Identity editor = ureq.getUserSession().getIdentity();
		SecurityGroup frentixSuperAdminGroup =  BaseSecurityManager.getInstance().findSecurityGroupByName("fxadmins");
		if(BaseSecurityManager.getInstance().isIdentityInSecurityGroup(identity, frentixSuperAdminGroup)){
			if(editor.equals(identity) || BaseSecurityManager.getInstance().isIdentityInSecurityGroup(editor, frentixSuperAdminGroup)) {
				return true;
			}
			return false;
		}

		if (isOlatAdmin) return true;

		// only admins can administrate admin and usermanager users
		boolean isAdmin = securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_ADMIN);
		boolean isUserManager = securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_USERMANAGER);
		if (isAdmin || isUserManager) return false;
		// if user is author ony allowed to edit if configured
		boolean isAuthor = securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);
		Boolean canManageAuthor = BaseSecurityModule.USERMANAGER_CAN_MANAGE_AUTHORS;
		if (isAuthor && !canManageAuthor.booleanValue()) return false;
		// if user is groupmanager ony allowed to edit if configured
		boolean isGroupManager = securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GROUPMANAGER);
		Boolean canManageGroupmanager = BaseSecurityModule.USERMANAGER_CAN_MANAGE_GROUPMANAGERS;
		if (isGroupManager && !canManageGroupmanager.booleanValue()) return false;
		// if user is guest ony allowed to edit if configured
		boolean isGuestOnly = securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY);
		Boolean canManageGuest = BaseSecurityModule.USERMANAGER_CAN_MANAGE_GUESTS;
		if (isGuestOnly && !canManageGuest.booleanValue()) return false;
		// passed all tests, current user is allowed to edit given identity
		return true;
	}
	
	/**
	 * Initialize the tabbed pane according to the users rights and the system
	 * configuration
	 * @param identity
	 * @param ureq
	 */
	private void initTabbedPane(Identity identity, UserRequest ureq) {
		// first Initialize the user details tabbed pane
		userTabP = new TabbedPane("userTabP", ureq.getLocale());
		userTabP.addListener(this);
		
		/*
		 *  Determine, whether the user admin is or is not able to edit all fields in user
		 *  profile form. The system admin is always able to do so.
		 */
		Boolean canEditAllFields = BaseSecurityModule.USERMANAGER_CAN_EDIT_ALL_PROFILE_FIELDS;
		if (BaseSecurityManager.getInstance().isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_ADMIN)) {
			canEditAllFields = Boolean.TRUE;
		}

		// Prepare tab entries
		List<Tab> tabs = new ArrayList<>();

		userProfileCtr = new ProfileAndHomePageEditController(ureq, getWindowControl(), identity, canEditAllFields.booleanValue());
		listenTo(userProfileCtr);
		tabs.add(new Tab(translate(NLS_EDIT_UPROFILE), userProfileCtr.getInitialComponent(), 10));
		
		prefsCtr = new ChangePrefsController(ureq, getWindowControl(), identity);
		tabs.add(new Tab(translate(NLS_EDIT_UPREFS), prefsCtr.getInitialComponent(), 20));

		if (isPasswordChangesAllowed(identity)) {
			pwdCtr =  new UserChangePasswordController(ureq, getWindowControl(), identity);				
			listenTo(pwdCtr); // listen when finished to update authentications model
			tabs.add(new Tab(translate(NLS_EDIT_UPWD), pwdCtr.getInitialComponent(), 30));
		}
		
		Boolean canAuth = BaseSecurityModule.USERMANAGER_ACCESS_TO_AUTH;
		if (canAuth.booleanValue() || isOlatAdmin) {
			authenticationsCtr =  new UserAuthenticationsEditorController(ureq, getWindowControl(), identity);
			tabs.add(new Tab(translate(NLS_EDIT_UAUTH), authenticationsCtr.getInitialComponent(), 40));
		}
		
		Boolean canProp = BaseSecurityModule.USERMANAGER_ACCESS_TO_PROP;
		if (canProp.booleanValue() || isOlatAdmin) {
			propertiesCtr = new UserPropertiesController(ureq, getWindowControl(), identity);			
			this.listenTo(propertiesCtr);
			tabs.add(new Tab(translate(NLS_EDIT_UPROP), propertiesCtr.getInitialComponent(), 50));
		}

		OLATResourceable ores = OresHelper.createOLATResourceableInstance("AllGroups", 0L);
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ores));
		WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ores, null, getWindowControl());
		UserAdminBusinessGroupListController userAdminBusinessGroupListController = new UserAdminBusinessGroupListController(ureq, bwControl, "adm", identity);
		listenTo(userAdminBusinessGroupListController);
		tabs.add(new Tab(translate(NLS_VIEW_GROUPS), userAdminBusinessGroupListController.getInitialComponent(), 60));
		userAdminBusinessGroupListController.doDefaultSearch();

		courseCtr = new CourseOverviewController(ureq, getWindowControl(), identity);
		listenTo(courseCtr);
		tabs.add(new Tab(translate(NLS_VIEW_COURSES), courseCtr.getInitialComponent(), 70));
		
		if (isOlatAdmin) {
			efficicencyCtrl = new CertificateAndEfficiencyStatementListController(ureq, getWindowControl(), identity, true);
			BreadcrumbedStackedPanel stackPanel = new BreadcrumbedStackedPanel("statements", getTranslator(), efficicencyCtrl);
			stackPanel.pushController(translate(NLS_VIEW_EFF_STATEMENTS), efficicencyCtrl);
			efficicencyCtrl.setBreadcrumbPanel(stackPanel);
			stackPanel.setInvisibleCrumb(1);
			tabs.add(new Tab(translate(NLS_VIEW_EFF_STATEMENTS), stackPanel, 80));
		}

		Boolean canSubscriptions = BaseSecurityModule.USERMANAGER_CAN_MODIFY_SUBSCRIPTIONS;
		if (canSubscriptions.booleanValue() || isOlatAdmin) {
			Controller subscriptionsCtr = new NotificationSubscriptionController(ureq, getWindowControl(), identity, true);
			listenTo(subscriptionsCtr); // auto-dispose
			tabs.add(new Tab(translate(NLS_VIEW_SUBSCRIPTIONS), subscriptionsCtr.getInitialComponent(), 90));
		}
		
		rolesCtr = new SystemRolesAndRightsController(getWindowControl(), ureq, identity);
		tabs.add(new Tab(translate(NLS_EDIT_UROLES), rolesCtr.getInitialComponent(), 100));

		Boolean canQuota = BaseSecurityModule.USERMANAGER_ACCESS_TO_QUOTA;
		if (canQuota.booleanValue() || isOlatAdmin) {
			String relPath = FolderConfig.getUserHomes() + "/" + identity.getName();
			quotaCtr = QuotaManager.getInstance().getQuotaEditorInstance(ureq, getWindowControl(), relPath, false);
			tabs.add(new Tab(translate(NLS_EDIT_UQUOTA), quotaCtr.getInitialComponent(), 110));
		}

		// Additional tabs (e.g. of an extension)
		UserAdminControllerAdditionalTabs userAdminControllerAdditionalTabs =
				(UserAdminControllerAdditionalTabs) applicationContext.getBean("userAdminControllerAdditionalTabs", ureq, getWindowControl(), identity);
		tabs.addAll(userAdminControllerAdditionalTabs.getTabs());

		// Sort tabs and add them to tabbed pane
		Collections.sort(tabs);
		for (Tab tab : tabs) {
			userTabP.addTab(tab);
		}
		
		// now push to velocity
		myContent.put("userTabP", userTabP);
	}
	
	private boolean isPasswordChangesAllowed(Identity identity) {
		Boolean canChangePwd = BaseSecurityModule.USERMANAGER_CAN_MODIFY_PWD;
		if (canChangePwd.booleanValue()  || isOlatAdmin) {
			// show pwd form only if user has also right to create new passwords in case
			// of a user that has no password yet
			if(ldapLoginModule.isLDAPEnabled() && ldapLoginManager.isIdentityInLDAPSecGroup(identity)) {
				// it's an ldap-user
				return ldapLoginModule.isPropagatePasswordChangedOnLdapServer();
			}
			
			Boolean canCreatePwd = BaseSecurityModule.USERMANAGER_CAN_CREATE_PWD;
			Authentication olatAuth = securityManager.findAuthentication(identity, BaseSecurityModule.getDefaultAuthProviderIdentifier());
			if (olatAuth != null || canCreatePwd.booleanValue() || isOlatAdmin) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Add some user data to velocity container including the users portrait
	 * @param ureq
	 * @param identity
	 */
	private void exposeUserDataToVC(UserRequest ureq, Identity identity) {		
		removeAsListenerAndDispose(portraitCtr);
		portraitCtr = new DisplayPortraitController(ureq, getWindowControl(), identity, true, true);
		myContent.put("portrait", portraitCtr.getInitialComponent());
		removeAsListenerAndDispose(userShortDescrCtr);
		userShortDescrCtr = new UserShortDescription(ureq, getWindowControl(), identity);
		myContent.put("userShortDescription", userShortDescrCtr.getInitialComponent());
	}

	@Override
	protected void doDispose() {
		//child controllers registered with listenTo get disposed in BasicController
		if (quotaCtr != null) {
			quotaCtr.dispose();
			quotaCtr = null;
		}
		if (authenticationsCtr != null) {
			authenticationsCtr.dispose();
			authenticationsCtr = null;
		}
		if (prefsCtr != null) {
			prefsCtr.dispose();
			prefsCtr = null;
		}			
		if (policiesCtr != null) {
			policiesCtr.dispose();
			policiesCtr = null;
		}
		if (rolesCtr != null) {
			rolesCtr.dispose();
			rolesCtr = null;
		}
		if (portraitCtr != null) {
			portraitCtr.dispose();
			portraitCtr = null;
		}
		if (userShortDescrCtr!=null) {
			userShortDescrCtr.dispose();
			userShortDescrCtr = null;
		}
	}


}