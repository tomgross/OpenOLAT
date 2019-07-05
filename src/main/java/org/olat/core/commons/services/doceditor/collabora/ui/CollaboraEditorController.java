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
package org.olat.core.commons.services.doceditor.collabora.ui;

import org.olat.core.commons.services.doceditor.DocEditor.Mode;
import org.olat.core.commons.services.doceditor.DocEditorSecurityCallback;
import org.olat.core.commons.services.doceditor.DocEditorSecurityCallbackBuilder;
import org.olat.core.commons.services.doceditor.collabora.CollaboraService;
import org.olat.core.commons.services.doceditor.wopi.Access;
import org.olat.core.commons.services.vfs.VFSMetadata;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.lock.LockResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 10 Feb 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class CollaboraEditorController extends BasicController {
	
	private final VFSLeaf vfsLeaf;
	private DocEditorSecurityCallback secCallback;
	private LockResult lock;
	private Access access;

	@Autowired
	private CollaboraService collaboraService;

	public CollaboraEditorController(UserRequest ureq, WindowControl wControl, VFSLeaf vfsLeaf,
			final DocEditorSecurityCallback securityCallback) {
		super(ureq, wControl);
		this.vfsLeaf = vfsLeaf;
		this.secCallback = securityCallback;
	
		if (collaboraService.isLockNeeded(securityCallback.getMode())) {
			if (collaboraService.isLockedForMe(vfsLeaf, getIdentity())) {
				this.secCallback = DocEditorSecurityCallbackBuilder.clone(securityCallback)
						.withMode(Mode.VIEW)
						.build();
				showWarning("editor.warning.locked");
			} else {
				lock = collaboraService.lock(vfsLeaf, getIdentity());
			}
		}
		VelocityContainer mainVC = createVelocityContainer("collabora");

		VFSMetadata vfsMetadata = vfsLeaf.getMetaInfo();
		if (vfsMetadata == null) {
			mainVC.contextPut("warning", translate("editor.warning.no.metadata"));
		} else {
			this.access = collaboraService.createAccess(vfsMetadata, getIdentity(), secCallback);
			String url = CollaboraEditorUrlBuilder
					.builder(access)
					.withLang(ureq.getLocale().getLanguage())
					.withCloseButton(access.isCanClose())
					.build();
			
			mainVC.contextPut("id", "o_" + CodeHelper.getRAMUniqueID());
			mainVC.contextPut("url", url);
		}
		
		putInitialPanel(mainVC);
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if ("close".equals(event.getCommand())) {
			// Suppress close event, because we can not hide close button
			if (access.isCanClose()) {
				fireEvent(ureq, Event.DONE_EVENT);
			}
		}
	}

	@Override
	protected void doDispose() {
		collaboraService.unlock(vfsLeaf, lock);
		collaboraService.deleteAccess(access);
	}

}
