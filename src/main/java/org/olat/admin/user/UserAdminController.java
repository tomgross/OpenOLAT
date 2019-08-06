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

import java.util.List;

import org.olat.admin.user.course.CourseOverviewController;
import org.olat.admin.user.groups.GroupOverviewController;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.services.notifications.ui.NotificationSubscriptionController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.stack.BreadcrumbedStackedPanel;
import org.olat.core.gui.components.tabbedpane.TabCreator;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.Identity;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.course.certificate.ui.CertificateAndEfficiencyStatementListController;
import org.olat.ldap.LDAPLoginManager;
import org.olat.ldap.LDAPLoginModule;
import org.olat.modules.lecture.LectureModule;
import org.olat.modules.lecture.ui.ParticipantLecturesOverviewController;
import org.olat.modules.taxonomy.TaxonomyModule;
import org.olat.modules.taxonomy.ui.IdentityCompetencesController;
import org.olat.properties.Property;
import org.olat.resource.accesscontrol.ui.UserOrderController;
import org.olat.user.ChangePrefsController;
import org.olat.user.DisplayPortraitController;
import org.olat.user.ProfileAndHomePageEditController;
import org.olat.user.PropFoundEvent;
import org.olat.user.UserManager;
import org.olat.user.UserPropertiesController;
import org.olat.user.ui.data.UserDataExportController;
import org.springframework.beans.factory.annotation.Autowired;

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
	private static final String NLS_FOUND_PROPERTY		= "found.property";
	private static final String NLS_EDIT_UPROFILE		= "edit.uprofile";
	private static final String NLS_EDIT_UPREFS			= "edit.uprefs";
	private static final String NLS_EDIT_UPWD 			= "edit.upwd";
	private static final String NLS_EDIT_UAUTH 			= "edit.uauth";
	private static final String NLS_EDIT_UPROP			= "edit.uprop";
	private static final String NLS_EDIT_UROLES			= "edit.uroles";
	private static final String NLS_EDIT_UQUOTA			= "edit.uquota";
	private static final String NLS_VIEW_GROUPS			= "view.groups";
	private static final String NLS_VIEW_COURSES			= "view.courses";
	private static final String NLS_VIEW_ACCESS			= "view.access";
	private static final String NLS_VIEW_EFF_STATEMENTS	= "view.effStatements";
	private static final String NLS_VIEW_SUBSCRIPTIONS 	= "view.subscriptions";
	private static final String NLS_VIEW_LECTURES		= "view.lectures";
	private static final String NLS_VIEW_COMPETENCES		= "view.competences";

	private VelocityContainer myContent;

	private Identity myIdentity = null;

	// controllers used in tabbed pane
	private TabbedPane userTabP;
	private Controller prefsCtr, propertiesCtr, pwdCtr, quotaCtr, rolesCtr, userShortDescrCtr;
	private DisplayPortraitController portraitCtr;
	private UserAuthenticationsEditorController authenticationsCtr;
	private Link backLink;
	private Link exportDataButton;
	private ProfileAndHomePageEditController userProfileCtr;
	private CourseOverviewController courseCtr;
	private GroupOverviewController grpCtr;
	private CloseableModalController cmc;
	private UserDataExportController exportDataCtrl;
	private IdentityCompetencesController competencesCtrl;
	private ParticipantLecturesOverviewController lecturesCtrl;
	private CertificateAndEfficiencyStatementListController efficicencyCtrl;

	private final boolean isOlatAdmin;

	@Autowired
	private UserManager userManager;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private LDAPLoginModule ldapLoginModule;
	@Autowired
	private LDAPLoginManager ldapLoginManager;
	@Autowired
	private LectureModule lectureModule;
	@Autowired
	private TaxonomyModule taxonomyModule;

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
			exportDataButton = LinkFactory.createButton("export.user.data", myContent, this);
			exportDataButton.setIconLeftCSS("o_icon o_icon_download");
			
			userShortDescrCtr = new UserShortDescription(ureq, wControl, identity);
			listenTo(userShortDescrCtr);
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
	
	public Identity getEditedIdentity() {
		return myIdentity;
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
		} else if(exportDataButton == source) {
			doExportData(ureq);
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
				showInfo(NLS_FOUND_PROPERTY, myfoundProperty.getKey().toString());
			}
		} else if (source == pwdCtr) {
			if (event == Event.DONE_EVENT) {
				// rebuild authentication tab, could be wrong now
				if (authenticationsCtr != null) {
					authenticationsCtr.rebuildAuthenticationsTableDataModel();
				}
			}
		} else if (source == userProfileCtr){
			if (event == Event.DONE_EVENT){
				//reload profile data on top
				myIdentity = securityManager.loadIdentityByKey(myIdentity.getKey());
				exposeUserDataToVC(ureq, myIdentity);
				userProfileCtr.resetForm(ureq);
			}
		} else if(source == exportDataCtrl) {
			cmc.deactivate();
			cleanUp();
		} else if(source == cmc) {
			cleanUp();
		}
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(exportDataCtrl);
		removeAsListenerAndDispose(cmc);
		exportDataCtrl = null;
		cmc = null;
	}
	
	private void doExportData(UserRequest ureq) {
		if(exportDataCtrl != null) return;
		
		exportDataCtrl = new UserDataExportController(ureq, getWindowControl(), myIdentity);
		listenTo(exportDataCtrl);
		
		String fullname = userManager.getUserDisplayName(myIdentity);
		String title = translate("export.user.data.title", new String[] { fullname });
		cmc = new CloseableModalController(getWindowControl(), translate("close"), exportDataCtrl.getInitialComponent(),
				true, title);
		listenTo(cmc);
		cmc.activate();
		
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
		// prevent editing of users that are in frentix-superadmin group (except "frentix" wants to change own profile)
		Identity editor = ureq.getUserSession().getIdentity();
		SecurityGroup frentixSuperAdminGroup =  securityManager.findSecurityGroupByName("fxadmins");
		if(securityManager.isIdentityInSecurityGroup(identity, frentixSuperAdminGroup)){
			if(editor.equals(identity) || securityManager.isIdentityInSecurityGroup(editor, frentixSuperAdminGroup)) {
				return true;
			}
			return false;
		}

		if (isOlatAdmin) {
			return true;
		}

		// only admins can administrate admin and usermanager users
		boolean isAdmin = securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_ADMIN);
		boolean isUserManager = securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_USERMANAGER);
		if (isAdmin || isUserManager) {
			return false;
		}
		// if user is author ony allowed to edit if configured
		boolean isAuthor = securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);
		Boolean canManageAuthor = BaseSecurityModule.USERMANAGER_CAN_MANAGE_AUTHORS;
		if (isAuthor && !canManageAuthor.booleanValue()) {
			return false;
		}
		// if user is groupmanager ony allowed to edit if configured
		boolean isGroupManager = securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GROUPMANAGER);
		Boolean canManageGroupmanager = BaseSecurityModule.USERMANAGER_CAN_MANAGE_GROUPMANAGERS;
		if (isGroupManager && !canManageGroupmanager.booleanValue()) {
			return false;
		}
		// if user is guest ony allowed to edit if configured
		boolean isGuestOnly = securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY);
		Boolean canManageGuest = BaseSecurityModule.USERMANAGER_CAN_MANAGE_GUESTS;
		if (isGuestOnly && !canManageGuest.booleanValue()) {
			return false;
		}
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

		/**
		 *  Determine, whether the user admin is or is not able to edit all fields in user
		 *  profile form. The system admin is always able to do so.
		 */
		Boolean canEditAllFields = BaseSecurityModule.USERMANAGER_CAN_EDIT_ALL_PROFILE_FIELDS;
		if (securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_ADMIN)) {
			canEditAllFields = Boolean.TRUE;
		}

		userProfileCtr = new ProfileAndHomePageEditController(ureq, getWindowControl(), identity, canEditAllFields.booleanValue());
		listenTo(userProfileCtr);
		userTabP.addTab(translate(NLS_EDIT_UPROFILE), userProfileCtr.getInitialComponent());

		userTabP.addTab(translate(NLS_EDIT_UPREFS), new TabCreator() {
			@Override
			public Component create(UserRequest uureq) {
				prefsCtr = new ChangePrefsController(uureq, getWindowControl(), identity);
				listenTo(prefsCtr);
				return prefsCtr.getInitialComponent();
			}
		});

		if (isPasswordChangesAllowed(identity)) {
			userTabP.addTab(translate(NLS_EDIT_UPWD), new TabCreator() {
				@Override
				public Component create(UserRequest uureq) {
					pwdCtr =  new UserChangePasswordController(uureq, getWindowControl(), identity);
					listenTo(pwdCtr);
					return pwdCtr.getInitialComponent();
				}
			});
		}

		Boolean canAuth = BaseSecurityModule.USERMANAGER_ACCESS_TO_AUTH;
		if (canAuth.booleanValue() || isOlatAdmin) {
			userTabP.addTab(translate(NLS_EDIT_UAUTH), new TabCreator() {
				@Override
				public Component create(UserRequest uureq) {
					authenticationsCtr =  new UserAuthenticationsEditorController(uureq, getWindowControl(), identity);
					listenTo(authenticationsCtr);
					return authenticationsCtr.getInitialComponent();
				}
			});
		}

		Boolean canProp = BaseSecurityModule.USERMANAGER_ACCESS_TO_PROP;
		if (canProp.booleanValue() || isOlatAdmin) {
			userTabP.addTab(translate(NLS_EDIT_UPROP), new TabCreator() {
				@Override
				public Component create(UserRequest uureq) {
					propertiesCtr = new UserPropertiesController(uureq, getWindowControl(), identity);
					listenTo(propertiesCtr);
					return propertiesCtr.getInitialComponent();
				}
			});
		}

		Boolean canStartGroups = BaseSecurityModule.USERMANAGER_CAN_START_GROUPS;
		userTabP.addTab(translate(NLS_VIEW_GROUPS), new TabCreator() {
			@Override
			public Component create(UserRequest uureq) {
				grpCtr = new GroupOverviewController(uureq, getWindowControl(), identity, canStartGroups);
				listenTo(grpCtr);
				return grpCtr.getInitialComponent();
			}
		});

		userTabP.addTab(translate(NLS_VIEW_COURSES), new TabCreator() {
			@Override
			public Component create(UserRequest uureq) {
				courseCtr = new CourseOverviewController(uureq, getWindowControl(), identity);
				listenTo(courseCtr);
				return courseCtr.getInitialComponent();
			}
		});

		if (isOlatAdmin) {
			userTabP.addTab(translate(NLS_VIEW_ACCESS), new TabCreator() {
				@Override
				public Component create(UserRequest uureq) {
					Controller accessCtr = new UserOrderController(uureq, getWindowControl(), identity);
					listenTo(accessCtr);
					return accessCtr.getInitialComponent();
				}
			});
		}

		if (isOlatAdmin) {
			userTabP.addTab(translate(NLS_VIEW_EFF_STATEMENTS), new TabCreator() {
				@Override
				public Component create(UserRequest uureq) {
					efficicencyCtrl = new CertificateAndEfficiencyStatementListController(uureq, getWindowControl(), identity, true);
					listenTo(efficicencyCtrl);
					BreadcrumbedStackedPanel stackPanel = new BreadcrumbedStackedPanel("statements", getTranslator(), efficicencyCtrl);
					stackPanel.pushController(translate(NLS_VIEW_EFF_STATEMENTS), efficicencyCtrl);
					efficicencyCtrl.setBreadcrumbPanel(stackPanel);
					stackPanel.setInvisibleCrumb(1);
					return stackPanel;
				}
			});
		}

		Boolean canSubscriptions = BaseSecurityModule.USERMANAGER_CAN_MODIFY_SUBSCRIPTIONS;
		if (canSubscriptions.booleanValue() || isOlatAdmin) {
			userTabP.addTab(translate(NLS_VIEW_SUBSCRIPTIONS), new TabCreator() {
				@Override
				public Component create(UserRequest uureq) {
					Controller subscriptionsCtr = new NotificationSubscriptionController(uureq, getWindowControl(), identity, true);
					listenTo(subscriptionsCtr);
					return subscriptionsCtr.getInitialComponent();
				}

			});
		}

		userTabP.addTab(translate(NLS_EDIT_UROLES), new TabCreator() {
			@Override
			public Component create(UserRequest uureq) {
				rolesCtr = new SystemRolesAndRightsController(getWindowControl(), uureq, identity);
				listenTo(rolesCtr);
				return rolesCtr.getInitialComponent();
			}
		});

		Boolean canQuota = BaseSecurityModule.USERMANAGER_ACCESS_TO_QUOTA;
		if (canQuota.booleanValue() || isOlatAdmin) {
			userTabP.addTab(translate(NLS_EDIT_UQUOTA), new TabCreator() {
				@Override
				public Component create(UserRequest uureq) {
					String relPath = FolderConfig.getUserHomes() + "/" + identity.getName();
					quotaCtr = QuotaManager.getInstance().getQuotaEditorInstance(uureq, getWindowControl(), relPath);
					return quotaCtr.getInitialComponent();
				}
			});
		}

		if(lectureModule.isEnabled()) {
			userTabP.addTab(translate(NLS_VIEW_LECTURES), new TabCreator() {
				@Override
				public Component create(UserRequest uureq) {
					lecturesCtrl = new ParticipantLecturesOverviewController(uureq, getWindowControl(), identity, true, true, true, true);
					listenTo(lecturesCtrl);
					BreadcrumbedStackedPanel stackPanel = new BreadcrumbedStackedPanel("lectures", getTranslator(), lecturesCtrl);
					stackPanel.pushController(translate(NLS_VIEW_LECTURES), lecturesCtrl);
					lecturesCtrl.setBreadcrumbPanel(stackPanel);
					stackPanel.setInvisibleCrumb(1);
					return stackPanel;
				}
			});
		}
		
		if(taxonomyModule.isEnabled()) {
			userTabP.addTab(translate(NLS_VIEW_COMPETENCES), new TabCreator() {
				@Override
				public Component create(UserRequest uureq) {
					competencesCtrl = new IdentityCompetencesController(uureq, getWindowControl(), identity);
					listenTo(competencesCtrl);
					BreadcrumbedStackedPanel stackPanel = new BreadcrumbedStackedPanel("competences", getTranslator(), competencesCtrl);
					stackPanel.pushController(translate(NLS_VIEW_COMPETENCES), competencesCtrl);
					competencesCtrl.setBreadcrumbPanel(stackPanel);
					stackPanel.setInvisibleCrumb(1);
					return stackPanel;
				}
			});
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

	/**
	 *
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}
}