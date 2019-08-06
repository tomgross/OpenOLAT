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

package org.olat.commons.info.ui;

import org.olat.core.util.StringHelper;

import java.io.File;

/**
 * 
 * Description:<br>
 * A small wrapper for all informations about an info message.
 * 
 * <P>
 * Initial Date:  14 déc. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoMessageForDisplay {
	
	private final Long key;
	private final String title;
	private final String message;
	private final String infos;
	private final String modifier;
	private final File[] attachments;

	public InfoMessageForDisplay(Long key, String title, String message, File[] attachments, String infos, String modifier) {
		this.key = key;
		this.title = title;
		this.infos = infos;
		this.attachments = attachments;
		this.message = message;
		this.modifier = modifier;
	}

	public Long getKey() {
		return key;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public File[] getAttachments() {
		return attachments;
	}

	public boolean hasAttachments() {
		return attachments.length > 0;
	}

	public String getInfos() {
		return infos;
	}

	public boolean isModified() {
		return StringHelper.containsNonWhitespace(modifier);
	}

	public String getModifier() {
		return modifier;
	}
}
