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
package org.olat.user;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.olat.admin.user.SystemRolesAndRightsController;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.services.notifications.NotificationsManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.WindowManager;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;
import org.olat.core.id.Preferences;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;
import org.olat.core.util.mail.MailModule;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This form controller provides an interface to change the user's system
 * preferences, like language and font size.
 * <p>
 * Events fired by this event:
 * <ul>
 * <li>Event.DONE_EVENT when something has been changed</li>
 * <li>Event.DONE_CANELLED when user cancelled action</li>
 * </ul>
 * <P>
 * Initial Date: Dec 11, 2009 <br>
 * 
 * @author gwassmann
 */
public class PreferencesFormController extends FormBasicController {
	private static final String[] cssFontsizeKeys = new String[] { "80", "90", "100", "110", "120", "140" };
	private Identity tobeChangedIdentity;
	private SingleSelection language, fontsize, notificationInterval, mailSystem;
	private static final String[] mailIntern = new String[]{"intern.only","send.copy"};
	
	@Autowired
	private I18nModule i18nModule;

	/**
	 * Constructor for the user preferences form
	 * 
	 * @param ureq
	 * @param wControl
	 * @param tobeChangedIdentity the Identity which preferences are displayed and
	 *          edited. Not necessarily the same as ureq.getIdentity()
	 */
	public PreferencesFormController(UserRequest ureq, WindowControl wControl, Identity tobeChangedIdentity) {
		super(ureq, wControl, Util.createPackageTranslator(SystemRolesAndRightsController.class, ureq.getLocale()));
		this.tobeChangedIdentity = tobeChangedIdentity;
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	protected void formOK(UserRequest ureq) {
		UserManager um = UserManager.getInstance();
		BaseSecurity secMgr = BaseSecurityManager.getInstance();
		// Refresh user from DB to prevent stale object issues
		tobeChangedIdentity = secMgr.loadIdentityByKey(tobeChangedIdentity.getKey());
		Preferences prefs = tobeChangedIdentity.getUser().getPreferences();
		prefs.setLanguage(language.getSelectedKey());
		prefs.setFontsize(fontsize.getSelectedKey());
		if (notificationInterval != null) {
			// only read notification interval if available, could be disabled by configuration
			prefs.setNotificationInterval(notificationInterval.getSelectedKey());			
		}

		// Maybe the user changed the font size
		if (ureq.getIdentity().equalsByPersistableKey(tobeChangedIdentity)) {
			int fontSize = Integer.parseInt(fontsize.getSelectedKey());
			WindowManager wm = getWindowControl().getWindowBackOffice().getWindowManager();
			if(fontSize != wm.getFontSize()) {
				getWindowControl().getWindowBackOffice().getWindow().setDirty(true);
			}
		}
		
		if(mailSystem != null && mailSystem.isOneSelected()) {
			String val = mailSystem.isSelected(1) ? "true" : "false";
			prefs.setReceiveRealMail(val);
		}

		if (um.updateUserFromIdentity(tobeChangedIdentity)) {
			// Language change needs logout / login
			showInfo("preferences.successful");
		} else {
			showInfo("preferences.unsuccessful");
		}

		fireEvent(ureq, Event.DONE_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formCancelled(org.olat.core.gui.UserRequest)
	 */
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("title.prefs");
		setFormContextHelp("Configuration#_einstellungen");
		
		// load preferences
		Preferences prefs = tobeChangedIdentity.getUser().getPreferences();


		// Username
		StaticTextElement username = uifactory.addStaticTextElement("form.username", tobeChangedIdentity.getName(), formLayout);
		username.setElementCssClass("o_sel_home_settings_username");
		username.setEnabled(false);

		// Roles
		final String[] roleKeys = new String[] {
			Constants.GROUP_USERMANAGERS, Constants.GROUP_GROUPMANAGERS, Constants.GROUP_POOL_MANAGER,
			Constants.GROUP_AUTHORS, Constants.GROUP_INST_ORES_MANAGER, Constants.GROUP_ADMIN
		};
		String iname = getIdentity().getUser().getProperty("institutionalName", null);
		String ilabel = iname != null
				? translate("rightsForm.isInstitutionalResourceManager.institution",iname)
				: translate("rightsForm.isInstitutionalResourceManager");
		
		final String[] roleValues = new String[]{
				translate("rightsForm.isUsermanager"), translate("rightsForm.isGroupmanager"), translate("rightsForm.isPoolmanager"),
				translate("rightsForm.isAuthor"), ilabel, translate("rightsForm.isAdmin")
		};
		final BaseSecurity securityManager = CoreSpringFactory.getImpl(BaseSecurity.class);
		String userRoles = "";
		List<String> roles = securityManager.getRolesAsString(tobeChangedIdentity);
		for(String role:roles) {
			for(int i=0; i<roleKeys.length; i++) {
				if(roleKeys[i].equals(role)) {
					userRoles = userRoles + roleValues[i] + ", ";
				}
			}
		}
		if (userRoles.equals("")) {
			userRoles = translate("rightsForm.isAnonymous.false");
		} else {
			userRoles = userRoles.substring(0, userRoles.lastIndexOf(","));
		}
		uifactory.addStaticTextElement("rightsForm.roles", userRoles, formLayout);
		username.setElementCssClass("o_sel_home_settings_username");
		username.setEnabled(false);

		// Language
		Map<String, String> languages = I18nManager.getInstance().getEnabledLanguagesTranslated();
		String[] langKeys = StringHelper.getMapKeysAsStringArray(languages);
		String[] langValues = StringHelper.getMapValuesAsStringArray(languages);
		ArrayHelper.sort(langKeys, langValues, false, true, false);
		language = uifactory.addDropdownSingleselect("form.language", formLayout, langKeys, langValues, null);
		language.setElementCssClass("o_sel_home_settings_language");
		String langKey = prefs.getLanguage();
		// Preselect the users language if available. Maye not anymore enabled on
		// this server
		if (prefs.getLanguage() != null && i18nModule.getEnabledLanguageKeys().contains(langKey)) {
			language.select(prefs.getLanguage(), true);
		} else {
			language.select(I18nModule.getDefaultLocale().toString(), true);
		}

		// Font size
		String[] cssFontsizeValues = new String[] {
				translate("form.fontsize.xsmall"),
				translate("form.fontsize.small"),
				translate("form.fontsize.normal"),
				translate("form.fontsize.large"),
				translate("form.fontsize.xlarge"),
				translate("form.fontsize.presentation")
		};
		fontsize = uifactory.addDropdownSingleselect("form.fontsize", formLayout, cssFontsizeKeys, cssFontsizeValues, null);
		fontsize.setElementCssClass("o_sel_home_settings_fontsize");
		fontsize.select(prefs.getFontsize(), true);
		fontsize.addActionListener(FormEvent.ONCHANGE);
		
		// Email notification interval
		NotificationsManager nMgr = NotificationsManager.getInstance();
		List<String> intervals = nMgr.getEnabledNotificationIntervals();
		if (intervals.size() > 0) {
			String[] intervalKeys = new String[intervals.size()];
			intervals.toArray(intervalKeys);
			String[] intervalValues = new String[intervalKeys.length];
			String i18nPrefix = "interval.";
			for (int i = 0; i < intervalKeys.length; i++) {
				intervalValues[i] = translate(i18nPrefix + intervalKeys[i]);
			}
			notificationInterval = uifactory.addDropdownSingleselect("form.notification", formLayout, intervalKeys, intervalValues, null);
			notificationInterval.setElementCssClass("o_sel_home_settings_notification_interval");
			notificationInterval.select(prefs.getNotificationInterval(), true);			
		}
		//fxdiff VCRP-16: intern mail system
		MailModule mailModule = (MailModule)CoreSpringFactory.getBean("mailModule");
		if(mailModule.isInternSystem()) {
			String userEmail = UserManager.getInstance().getUserDisplayEmail(tobeChangedIdentity, ureq.getLocale());
			String[] mailInternLabels = new String[] { translate("mail." + mailIntern[0], userEmail), translate("mail." + mailIntern[1], userEmail) };
			mailSystem = uifactory.addRadiosVertical("mail-system", "mail.system", formLayout, mailIntern, mailInternLabels);
			mailSystem.setElementCssClass("o_sel_home_settings_mail");
			
			String mailPrefs = prefs.getReceiveRealMail();
			if(StringHelper.containsNonWhitespace(mailPrefs)) {
				if("true".equals(mailPrefs)) {
					mailSystem.select(mailIntern[1], true);
				} else {
					mailSystem.select(mailIntern[0], true);
				}
			} else if(mailModule.isReceiveRealMailUserDefaultSetting()) {
				mailSystem.select(mailIntern[1], true);
			} else {
				mailSystem.select(mailIntern[0], true);
			}
		}

		// Submit and cancel buttons
		final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("button_layout", getTranslator());
		formLayout.add(buttonLayout);
		buttonLayout.setElementCssClass("o_sel_home_settings_prefs_buttons");
		uifactory.addFormSubmitButton("submit", buttonLayout);
		uifactory.addFormCancelButton("cancel", buttonLayout, ureq, getWindowControl());
	}

	@Override
	protected void formInnerEvent (UserRequest ureq, FormItem source, FormEvent event) {
		if (source == fontsize && ureq.getIdentity().equalsByPersistableKey(tobeChangedIdentity)) {
			int fontSize = Integer.parseInt(fontsize.getSelectedKey());
			WindowManager wm = getWindowControl().getWindowBackOffice().getWindowManager();
			if(fontSize != wm.getFontSize()) {
				getWindowControl().getWindowBackOffice().getWindow().setDirty(true);
			}
		}
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	protected void doDispose() {
	// nothing to do
	}

}
