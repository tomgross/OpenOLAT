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

package org.olat.modules.co;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.velocity.VelocityContext;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.core.util.mail.*;
import org.olat.core.util.mail.model.SimpleMailContent;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.manager.RepositoryEntryDAO;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Fires Event: </b>
 * <UL>
 * <LI><b>Event.DONE_EVENT: </B> <BR>
 * email was sent successfully by the underlying Email subsystem</LI>
 * <LI><b>Event.FAILED_EVENT: </B> <BR>
 * email was not sent correct by the underlying Email subsystem <BR>
 * email may be partially sent correct, but some parts failed.</LI>
 * <LI><b>Event.CANCELLED_EVENT: </B> <BR>
 * user interaction, i.e. canceled message creation</LI>
 * </UL>
 * <p>
 * <b>Consumes Events from: </b>
 * <UL>
 * <LI>ContactForm:</LI>
 * <UL>
 * <LI>Form.EVENT_FORM_CANCELLED</LI>
 * <LI>Form.EVENT_VALIDATION_OK</LI>
 * </UL>
 * </UL>
 * <P>
 * <b>Main Purpose: </b> is to provide an easy interface for <i>contact message
 * creation and sending </i> from within different OLAT bulding blocks.
 * <P>
 * <b>Responsabilites: </b> <br>
 * <UL>
 * <LI>supplies a workflow for creating and sending contact messages</LI>
 * <LI>works with the ContactList encapsulating the e-mail addresses in a
 * mailing list.</LI>
 * <LI>contact messages with pre-initialized subject and/or body</LI>
 * </UL>
 * <P>
 * @see org.olat.core.util.mail.ContactList
 * Initial Date: Jul 19, 2004
 * @author patrick
 */
public class ContactFormController extends BasicController {

	private Identity emailFrom;
	
	private ContactForm cntctForm;
	private DialogBoxController noUsersErrorCtr;
	private List<String> myButtons;
	
	@Autowired
	private MailManager mailService;
	@Autowired
	private UserManager userManager;
	@Autowired
	private RepositoryEntryDAO repositoryEntryDAO;

	/**
	 * 
	 * @param ureq
	 * @param windowControl
	 * @param isCanceable
	 * @param isReadonly
	 * @param hasRecipientsEditable
	 * @param cmsg
	 */
	public ContactFormController(UserRequest ureq, WindowControl windowControl, boolean isCanceable, boolean isReadonly, boolean hasRecipientsEditable, ContactMessage cmsg) {
		super(ureq, windowControl);
		
		//init email form
		emailFrom = cmsg.getFrom();
		
		cntctForm = new ContactForm(ureq, windowControl, emailFrom, isReadonly,isCanceable,hasRecipientsEditable);
		listenTo(cntctForm);
		
		List<ContactList> recipList = cmsg.getEmailToContactLists();
		boolean hasAtLeastOneAddress = hasAtLeastOneAddress(recipList);
		cntctForm.setBody(cmsg.getBodyText());
		cntctForm.setSubject(cmsg.getSubject());
		
		//init display component
		init(ureq, hasAtLeastOneAddress, cmsg.getDisabledIdentities());
	}
	
	private boolean hasAtLeastOneAddress(List<ContactList> recipList) {
		boolean hasAtLeastOneAddress = false;
		if (recipList != null && recipList.size() > 0 ) {
			for (ContactList cl: recipList) {
				if (!hasAtLeastOneAddress && cl != null && cl.getEmailsAsStrings().size() > 0) {
					hasAtLeastOneAddress = true;
				}
				if (cl.getEmailsAsStrings().size() > 0) cntctForm.addEmailTo(cl);
			}
		}
		return hasAtLeastOneAddress;
	}

	private void init(UserRequest ureq, boolean hasAtLeastOneAddress, List<Identity> disabledIdentities) {
		if (hasAtLeastOneAddress) {
			putInitialPanel(cntctForm.getInitialComponent());	
		} else {
			Controller mCtr = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, translate("error.msg.send.no.rcps"));
			listenTo(mCtr);// to be disposed as this controller gets disposed
			putInitialPanel(mCtr.getInitialComponent());
		}
		if(!hasAtLeastOneAddress | disabledIdentities.size() > 0){
			//show error that message can not be sent
			myButtons = new ArrayList<String>();
			myButtons.add(translate("back"));
			String title = "";
			String message = "";
			if(disabledIdentities.size() > 0) {
				title = MailHelper.getTitleForFailedUsersError(ureq.getLocale());
				message = MailHelper.getMessageForFailedUsersError(ureq.getLocale(), disabledIdentities);
			} else {
				title = translate("error.title.nousers");
				message = translate("error.msg.nousers");
			}
			noUsersErrorCtr = activateGenericDialog(ureq, title, message, myButtons, noUsersErrorCtr);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == noUsersErrorCtr) {
			if(event.equals(Event.CANCELLED_EVENT)) {
				// user has clicked the close button in the top-right corner
				fireEvent(ureq, Event.CANCELLED_EVENT);
			} else {
				// user has clicked the cancel button
				int pos = DialogBoxUIFactory.getButtonPos(event);
				if (pos == 0){
					// cancel button has been pressed, fire event to parent
					fireEvent(ureq, Event.CANCELLED_EVENT);
				}
			}
		} else if (source == cntctForm) {
			if (event == Event.DONE_EVENT) {
				doSend(ureq);
			} else if (event == Event.CANCELLED_EVENT) {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		}
	}
	
	private void doSend(UserRequest ureq) {
		MailerResult result = new MailerResult();
		try {
			File[] attachments = cntctForm.getAttachments();
			String businessPath = getWindowControl().getBusinessControl().getAsString();
			String repoEntryKey = getRepoEntryKey(businessPath);
			RepositoryEntry entry = repositoryEntryDAO.loadByKey(Long.parseLong(repoEntryKey));
			MailContext context = new MailContextImpl(businessPath);
			ContactMailTemplate template = new ContactMailTemplate(cntctForm.getSubject(), cntctForm.getBody(), entry);

			// prepare bundle
			String metaId = UUID.randomUUID().toString().replace("-", "");
			MailBundle bundle = new MailBundle();
			bundle.setContext(context);
			bundle.setMetaId(metaId);
			if (emailFrom == null) {
				// in case the user provides his own email in form						
				bundle.setFrom(cntctForm.getEmailFrom()); 
			} else {
				bundle.setFromId(emailFrom);
			}

			// TODO clean-up after the release using prepared patch (AZU)

			// send message to all defined recipients
			List<String> failedIdentities = new ArrayList<>();
			List<String> allIdentities = new ArrayList<>();
			List<ContactList> contactLists = cntctForm.getEmailToContactLists();
			for (ContactList contactList : contactLists) {
				//
				for (String email : contactList.getStringEmails().values()) {
					allIdentities.add(email);
					// can't process template because there's no Identity. Just send template as it is
					bundle.setTo(email);
					bundle.setContent(new SimpleMailContent(cntctForm.getSubject(), cntctForm.getBody(), attachments));
					result.append(mailService.sendMessage(bundle));
				}
				// clean up recipients before reusing bundle
				bundle.setTo(null);
				for (Identity toIdentity : contactList.getIdentiEmails().values()) {
					allIdentities.add(toIdentity.getKey().toString());
					bundle.setToId(toIdentity);
					MailContent msg = mailService.createContentFromTemplate(toIdentity, template, result);
					if (msg != null) {
                        msg.setAttachments(Arrays.asList(attachments));
						bundle.setContent(msg);
						result.append(mailService.sendMessage(bundle));
					} else {
						failedIdentities.add(toIdentity.getName());
					}
				}
				// clean up recipients before reusing bundle
				bundle.setToId(null);
			}

			// also send a copy to sender
			if (cntctForm.isTcpFrom()) {
				if (emailFrom == null) {
					// can't process template because there's no Identity. Just send template as it is
					bundle.setTo(cntctForm.getEmailFrom());
					bundle.setContent(new SimpleMailContent(cntctForm.getSubject(), cntctForm.getBody(), attachments));
					result.append(mailService.sendMessage(bundle));
				} else {
					bundle.setToId(emailFrom);
					MailContent msg = mailService.createContentFromTemplate(emailFrom, template, result);
					if (msg != null) {
                        msg.setAttachments(Arrays.asList(attachments));
						bundle.setContent(msg);
						result.append(mailService.sendMessage(bundle));
					}
				}
			}
			
			if (result.isSuccessful()) {
				if (failedIdentities.size() > 0) {
					int succeededIdentities = allIdentities.size() - failedIdentities.size();
					showWarning("msg.send.ok.some.failed", new String[] { String.valueOf(succeededIdentities), String.valueOf(failedIdentities.size()), String.join(", ", failedIdentities) });
				} else {
					showInfo("msg.send.ok");
				}
				// do logging
				ThreadLocalUserActivityLogger.log(MailLoggingAction.MAIL_SENT, getClass());
				fireEvent(ureq, Event.DONE_EVENT);
			} else {
				showError(result);
				fireEvent(ureq, Event.FAILED_EVENT);
			}
		} catch (Exception e) {
			logError("", e);
			showWarning("error.msg.send.nok");
		}
		cntctForm.setDisplayOnly(true);
	}

	// TODO consider creating globally reusable static method to pars repo entry key from any possible business path
	private String getRepoEntryKey(String businessPath) {
		String[] parts = businessPath.split(":");
		if (parts.length < 2) return "";
		return parts[1].substring(0, parts[1].lastIndexOf("]"));
	}

	private void showError(MailerResult result) {
		StringBuilder error = new StringBuilder(1024);
		error.append(translate("error.msg.send.nok"));
		if(result != null && (result.getFailedIdentites().size() > 0 || result.getInvalidAddresses().size() > 0)) {
			error.append("<br />");

			StringBuilder ids = new StringBuilder(1024);
			for(Identity identity:result.getFailedIdentites()) {
				if(ids.length() > 0) ids.append(", ");
				
				String fullname = userManager.getUserDisplayName(identity);
				if(StringHelper.containsNonWhitespace(fullname)) {
					ids.append(fullname);
				}
			}
			for(String invalidAddress:result.getInvalidAddresses()) {
				if(ids.length() > 0) ids.append(", ");
				ids.append(invalidAddress);
			}
			error.append(translate("error.msg.send.invalid.rcps", new String[]{ ids.toString() }));
		}
		getWindowControl().setError(error.toString());
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		//
	}

	@Override
	protected void doDispose() {
		//
	}

	// TODO consider making a reusable public class for a template that has a RepositoryEntry to add in the context
	private class ContactMailTemplate extends MailTemplate {

		private final RepositoryEntry repositoryEntry;

		public ContactMailTemplate(String subjectTemplate, String bodyTemplate, RepositoryEntry repositoryEntry) {
			super(subjectTemplate, bodyTemplate, null);
			this.repositoryEntry = repositoryEntry;
		}

		@Override
		public void putVariablesInMailContext(VelocityContext vContext, Identity recipient) {
			User user = recipient.getUser();
			vContext.put("firstname", user.getProperty(UserConstants.FIRSTNAME, null));
			vContext.put(UserConstants.FIRSTNAME, user.getProperty(UserConstants.FIRSTNAME, null));
			vContext.put("lastname", user.getProperty(UserConstants.LASTNAME, null));
			vContext.put(UserConstants.LASTNAME, user.getProperty(UserConstants.LASTNAME, null));
			String fullName = userManager.getUserDisplayName(recipient);
			vContext.put("fullname", fullName);
			vContext.put("fullName", fullName);
			vContext.put("mail", user.getProperty(UserConstants.EMAIL, null));
			vContext.put("email", user.getProperty(UserConstants.EMAIL, null));
			vContext.put("username", recipient.getName());
			// Put variables from greater context
			if(repositoryEntry != null) {
				String url = Settings.getServerContextPathURI() + "/url/RepositoryEntry/" + repositoryEntry.getKey();
				vContext.put("courseurl", url);
				vContext.put("coursename", repositoryEntry.getDisplayname());
				vContext.put("coursedescription", repositoryEntry.getDescription());
			}
		}
	}
}