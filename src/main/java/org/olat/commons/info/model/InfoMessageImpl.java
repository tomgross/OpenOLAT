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

package org.olat.commons.info.model;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.*;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.olat.basesecurity.IdentityImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFileImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.id.CreateInfo;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Persistable;
import org.olat.core.util.FileUtils;
import org.olat.core.util.vfs.VFSItem;

/**
 * Initial Date: 17.03.2017
 * @author fkiefer, fabian.kiefer@frentix.com, www.frentix.com
 */
@Entity(name="infomessage")
@Table(name="o_info_message")
public class InfoMessageImpl implements InfoMessage, CreateInfo, Persistable {

	private static final long serialVersionUID = 6373476657660866469L;
	
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "enhanced-sequence", parameters={
		@Parameter(name="sequence_name", value="hibernate_unique_key"),
		@Parameter(name="force_table_use", value="true"),
		@Parameter(name="optimizer", value="legacy-hilo"),
		@Parameter(name="value_column", value="next_hi"),
		@Parameter(name="increment_size", value="32767"),
		@Parameter(name="initial_value", value="32767")
	})
	@Column(name="info_id", nullable=false, unique=true, insertable=true, updatable=false)
	private Long key;
	@Column(name="version", nullable=false, insertable=true, updatable=false)
	private int version = 0;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creationdate", nullable=false, insertable=true, updatable=false)
	private Date creationDate;
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="modificationdate", nullable=true, insertable=true, updatable=true)
	private Date modificationDate;
	
	@Column(name="title", nullable=true, insertable=true, updatable=true)
	private String title;
	@Column(name="message", nullable=true, insertable=true, updatable=true)
	private String message;
	
	@Column(name="resid", nullable=false, insertable=true, updatable=false)
	private Long resId;
	@Column(name="resname", nullable=false, insertable=true, updatable=false)
	private String resName;
	@Column(name="ressubpath", nullable=true, insertable=true, updatable=false)
	private String resSubPath;
	@Column(name="businesspath", nullable=true, insertable=true, updatable=false)
	private String businessPath;
	
	@ManyToOne(targetEntity=IdentityImpl.class, fetch=FetchType.LAZY, optional=true)
	@JoinColumn(name="fk_author_id", nullable=true, insertable=true, updatable=true)
	private Identity author;
	@ManyToOne(targetEntity=IdentityImpl.class, fetch=FetchType.LAZY, optional=true)
	@JoinColumn(name="fk_modifier_id", nullable=true, insertable=true, updatable=true)
	private Identity modifier;

	@Transient
	private File[] attachments = null;

	public InfoMessageImpl() {
		//
	}
	
	public Date getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(Date modificationDate) {
		this.modificationDate = modificationDate;
	}
	
	public void setCreationDate(Date creationDate) {
		this.creationDate= creationDate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public File[] getAttachments() {
		if (attachments == null) { // lazy loading
			List<File> files = new ArrayList<>();
			for (Object fileItem : getMediaFolder().getItems().toArray()) {
				if (fileItem instanceof VFSItem) {
					files.add(((OlatRootFileImpl) fileItem).getBasefile());
				}
			}
			attachments = files.toArray(new File[0]);
		}
		return attachments;
	}

	@Override
	public void setAttachments(File[] attachments) {
		this.attachments = attachments;
	}

	public Long getResId() {
		return resId;
	}

	public void setResId(Long resId) {
		this.resId = resId;
	}

	public String getResName() {
		return resName;
	}

	public void setResName(String resName) {
		this.resName = resName;
	}

	public String getResSubPath() {
		return resSubPath;
	}

	public void setResSubPath(String subPath) {
		this.resSubPath = subPath;
	}

	public String getBusinessPath() {
		return businessPath;
	}

	public void setBusinessPath(String businessPath) {
		this.businessPath = businessPath;
	}

	public Identity getAuthor() {
		return author;
	}

	public void setAuthor(Identity author) {
		this.author = author;
	}

	public Identity getModifier() {
		return modifier;
	}

	public void setModifier(Identity modifier) {
		this.modifier = modifier;
	}

	public OLATResourceable getOLATResourceable() {
		final String name = resName;
		final Long id = resId;
		return new OLATResourceable() {
			@Override
			public String getResourceableTypeName() {
				return name;
			}
			@Override
			public Long getResourceableId() {
				return id;
			}
		};
	}
	
	@Override
	public int hashCode() {
		return getKey() == null ? 8225 : getKey().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj instanceof InfoMessage) {
			InfoMessage info = (InfoMessage)obj;
			return getKey() != null && getKey().equals(info.getKey());
		}
		return false;
	}

	@Override
	public boolean equalsByPersistableKey(Persistable persistable) {
		return equals(persistable);
	}

	@Override
	public Long getKey() {
		return key;
	}

	
	@Override
	public Date getCreationDate() {
		return creationDate;
	}

	@Override
	public OlatRootFolderImpl getMediaFolder() {
		return new OlatRootFolderImpl("/repository/" + getOLATResourceable().getResourceableId() + "/" + getKey().toString() + "/attachments",	null);
	}

	@Override
	public boolean copyAttachmentToMediaFolder(File attachment) {
		try(
				InputStream in = new FileInputStream(attachment);
				OutputStream out = getMediaFolder().createChildLeaf(UUID.randomUUID().toString() + "." + attachment.getName()).getOutputStream(false);
		) {
			return FileUtils.copy(in, out);
		} catch (IOException e) {
			return false;
		}
	}



}
