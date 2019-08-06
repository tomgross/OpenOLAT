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

package org.olat.repository;

import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.olat.basesecurity.IdentityImpl;
import org.olat.core.id.CreateInfo;
import org.olat.core.id.Identity;
import org.olat.core.id.ModifiedInfo;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Persistable;
import org.olat.core.logging.AssertException;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.PersistingCourseImpl;
import org.olat.repository.model.RepositoryEntryLifecycle;
import org.olat.repository.model.RepositoryEntryStatistics;
import org.olat.repository.model.RepositoryEntryToGroupRelation;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceImpl;

/**
 *Represents a repository entry.
 */
@Entity(name="repositoryentry")
@Table(name="o_repositoryentry")
@NamedQueries({
	@NamedQuery(name="getRepositoryEntryRoleAndDefaults", query="select membership.role, relGroup.defaultGroup from repositoryentry as v inner join v.groups as relGroup inner join relGroup.group as baseGroup inner join baseGroup.members as membership where v.key=:repoKey and membership.identity.key=:identityKey"),
	@NamedQuery(name="filterRepositoryEntryMembership", query="select v.key, membership.identity.key from repositoryentry as v inner join v.groups as relGroup inner join relGroup.group as baseGroup inner join baseGroup.members as membership on membership.role in ('owner','coach','participant') where membership.identity.key=:identityKey and v.key in (:repositoryEntryKey)"),
	@NamedQuery(name="loadRepositoryEntryByKey", query="select v from repositoryentry as v inner join fetch v.olatResource as ores inner join fetch v.statistics as statistics left join fetch v.lifecycle as lifecycle where v.key = :repoKey"),
	@NamedQuery(name="loadRepositoryEntryByResourceKey", query="select v from repositoryentry as v inner join fetch v.olatResource as ores inner join fetch v.statistics as statistics left join fetch v.lifecycle as lifecycle where ores.key = :resourceKey"),
	@NamedQuery(name="loadRepositoryEntryByResId", query="select v from repositoryentry as v inner join fetch v.olatResource as ores inner join fetch v.statistics as statistics left join fetch v.lifecycle as lifecycle where ores.resId = :resId"),
	@NamedQuery(name="getDisplayNameByOlatResourceRedId", query="select v.displayname from repositoryentry v inner join v.olatResource as ores where ores.resId=:resid"),
	@NamedQuery(name="getDisplayNameByRepositoryEntryKey", query="select v.displayname from repositoryentry v where v.key=:reKey")

})
public class RepositoryEntry implements CreateInfo, Persistable , RepositoryEntryRef, ModifiedInfo, OLATResourceable {

	private static final long serialVersionUID = 5319576295875289054L;
	// IMPORTANT: Keep relation ACC_OWNERS < ACC_OWNERS_AUTHORS < ACC_USERS < ACC_USERS_GUESTS
	
	public static final int DELETED = 0;
	/**
	 * limit access to owners
	 */
	public static final int ACC_OWNERS = 1; // limit access to owners
	/**
	 * limit access to owners and authors
	 */
	public static final int ACC_OWNERS_AUTHORS = 2; // limit access to owners and authors
	/**
	 * limit access to owners, authors and users
	 */
	public static final int ACC_USERS = 3; // limit access to owners, authors and users
	/**
	 * no limits
	 */
	public static final int ACC_USERS_GUESTS = 4; // no limits
	
	public static final String MEMBERS_ONLY =  "membersonly";
	
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
	@Column(name="repositoryentry_id", nullable=false, unique=true, insertable=true, updatable=false)
	private Long key;
	@Version
	private int version = 0;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creationdate", nullable=false, insertable=true, updatable=false)
	private Date creationDate;
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="lastmodified", nullable=false, insertable=true, updatable=true)
	private Date lastModified;

	@Column(name="softkey", nullable=false, insertable=true, updatable=true)
	private String softkey;
	
	@ManyToOne(targetEntity=OLATResourceImpl.class,fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="fk_olatresource", nullable=false, insertable=true, updatable=false)
	private OLATResource olatResource;
	
	@OneToMany(targetEntity=RepositoryEntryToGroupRelation.class, fetch=FetchType.LAZY,
			orphanRemoval=true, cascade={CascadeType.PERSIST, CascadeType.REMOVE})
	@JoinColumn(name="fk_entry_id")
	private Set<RepositoryEntryToGroupRelation> groups;
	
	@Column(name="resourcename", nullable=false, insertable=true, updatable=true)
	private String resourcename; // mandatory
	@Column(name="displayname", nullable=false, insertable=true, updatable=true)
	private String displayname; // mandatory
	@Column(name="description", nullable=true, insertable=true, updatable=true)
	private String description; // mandatory
	@Column(name="initialauthor", nullable=false, insertable=true, updatable=true)
	private String initialAuthor; // mandatory // login of the author of the first version
	@Column(name="authors", nullable=true, insertable=true, updatable=true)
	private String authors;

	@Column(name="mainlanguage", nullable=true, insertable=true, updatable=true)
	private String mainLanguage;
	@Column(name="location", nullable=true, insertable=true, updatable=true)
	private String location;
	@Column(name="objectives", nullable=true, insertable=true, updatable=true)
	private String objectives;
	@Column(name="requirements", nullable=true, insertable=true, updatable=true)
	private String requirements;
	@Column(name="credits", nullable=true, insertable=true, updatable=true)
	private String credits;
	@Column(name="expenditureofwork", nullable=true, insertable=true, updatable=true)
	private String expenditureOfWork;
	
	@Column(name="external_id", nullable=true, insertable=true, updatable=true)
	private String externalId;
	@Column(name="external_ref", nullable=true, insertable=true, updatable=true)
	private String externalRef;
	@Column(name="managed_flags", nullable=true, insertable=true, updatable=true)
	private String managedFlagsString;
	
	@ManyToOne(targetEntity=RepositoryEntryLifecycle.class,fetch=FetchType.LAZY, optional=true)
	@JoinColumn(name="fk_lifecycle", nullable=true, insertable=true, updatable=true)
	private RepositoryEntryLifecycle lifecycle;
	
	@ManyToOne(targetEntity=RepositoryEntryStatistics.class,fetch=FetchType.LAZY, optional=false, cascade={CascadeType.PERSIST, CascadeType.REMOVE})
	@JoinColumn(name="fk_stats", nullable=false, insertable=true, updatable=false)
	private RepositoryEntryStatistics statistics;

	@Column(name="accesscode", nullable=false, insertable=true, updatable=true)
	private int access;
	@Column(name="cancopy", nullable=false, insertable=true, updatable=true)
	private boolean canCopy;
	@Column(name="canreference", nullable=false, insertable=true, updatable=true)
	private boolean canReference;
	@Column(name="canlaunch", nullable=false, insertable=true, updatable=true)
	private boolean canLaunch;
	@Column(name="candownload", nullable=false, insertable=true, updatable=true)
	private boolean canDownload;
	@Column(name="membersonly", nullable=false, insertable=true, updatable=true)
	private boolean membersOnly;
	@Column(name="statuscode", nullable=false, insertable=true, updatable=true)
	private int statusCode;
	@Column(name="allowToLeave", nullable=true, insertable=true, updatable=true)
	private String allowToLeave;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="deletiondate", nullable=true, insertable=true, updatable=true)
	private Date deletionDate;
	@ManyToOne(targetEntity=IdentityImpl.class,fetch=FetchType.LAZY, optional=true)
	@JoinColumn(name="fk_deleted_by", nullable=true, insertable=true, updatable=true)
	private Identity deletedBy;

	
	/**
	 * Default constructor.
	 */
	public RepositoryEntry() {
		softkey = CodeHelper.getGlobalForeverUniqueID();
		access = ACC_OWNERS;
	}

	@Override
	public Long getKey() {
		return key;
	}

	/**
	 * Key means id.
	 */
	public void setKey(Long key) {
		this.key = key;
	}

	@Override
	public Date getCreationDate() {
		return creationDate;
	}



	/**
	 * @return The softkey associated with this repository entry.
	 */
	public String getSoftkey() {
		return softkey;
	}
	
	/**
	 * Set the softkey of this repository entry.
	 * @param softkey
	 */
	public void setSoftkey(String softkey) {
		if (softkey.length() > 36) {
			throw new AssertException("Trying to set a softkey which is too long...");
		}
		this.softkey = softkey;
	}
	
	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getMainLanguage() {
		return mainLanguage;
	}

	public void setMainLanguage(String mainLanguage) {
		this.mainLanguage = mainLanguage;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getObjectives() {
		return objectives;
	}

	public void setObjectives(String objectives) {
		this.objectives = objectives;
	}

	public String getRequirements() {
		return requirements;
	}

	public void setRequirements(String requirements) {
		this.requirements = requirements;
	}

	public String getCredits() {
		return credits;
	}

	public void setCredits(String credits) {
		this.credits = credits;
	}

	public String getExpenditureOfWork() {
		return expenditureOfWork;
	}

	public void setExpenditureOfWork(String expenditureOfWork) {
		this.expenditureOfWork = expenditureOfWork;
	}

	/**
	 * @return description as HTML snippet
	 */
	public String getFormattedDescription() {
		String descr = Formatter.formatLatexFormulas(getDescription());
		return descr;		
	}
	
	/**
	 * @return Returns the initialAuthor.
	 */
	public String getInitialAuthor() {
		return initialAuthor;
	}
	/**
	 * @param initialAuthor The initialAuthor to set.
	 */
	public void setInitialAuthor(String initialAuthor) {
		if (initialAuthor == null) initialAuthor = "";
		if (initialAuthor.length() > IdentityImpl.NAME_MAXLENGTH)
			throw new AssertException("initialAuthor is limited to "+IdentityImpl.NAME_MAXLENGTH+" characters.");
		this.initialAuthor = initialAuthor;
	}

	public String getAuthors() {
		return authors;
	}

	public void setAuthors(String authors) {
		this.authors = authors;
	}

	/**
	 * @return Returns the statusCode.
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * @param statusCode The statusCode to set.
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	@Transient
	public RepositoryEntryStatus getRepositoryEntryStatus() {
		return new RepositoryEntryStatus(statusCode);
	}
	
	/**
	 * @return Returns the name.
	 */
	public String getResourcename() {
		return resourcename;
	}
	/**
	 * @param name The name to set.
	 */
	public void setResourcename(String name) {
		/*
		 * TODO sev26
		 * The size is now equal to the actual reasonable/possible (see
		 * varchar index issue of MySQL) database table attribute size.
		 */
		if (name.length() > 255)
			throw new AssertException("resourcename is limited to 255 characters.");
		this.resourcename = name;
	}

	/**
	 * @return Returns the olatResource.
	 */
	public OLATResource getOlatResource() {
		return olatResource;
	}
	/**
	 * @param olatResource The olatResource to set.
	 */
	public void setOlatResource(OLATResource olatResource) {
		this.olatResource = olatResource;
	}

	public Set<RepositoryEntryToGroupRelation> getGroups() {
		return groups;
	}

	public void setGroups(Set<RepositoryEntryToGroupRelation> groups) {
		this.groups = groups;
	}

	/**
	 * @return Wether this repo entry can be copied.
	 */
	public boolean getCanCopy() {
		return canCopy;
	}

	/**
	 * @return Wether this repo entry can be referenced by other people.
	 */
	public boolean getCanReference() {
		return canReference;
	}

	/**
	 * @return Wether this repo entry can be downloaded.
	 */
	public boolean getCanDownload() {
		return canDownload;
	}

	/**
	 * @return Wether this repo entry can be launched.
	 */
	public boolean getCanLaunch() {
		return canLaunch;
	}

	/**
	 * @return Access restrictions.
	 */
	public int getAccess() {
		return access;
	}
	
	/**
	 * Is the repository entry exclusive
	 * @return
	 */
	public boolean isMembersOnly() {
		return membersOnly;
	}

	/**
	 * @param b
	 */
	public void setCanCopy(boolean b) {
		canCopy = b;
	}

	/**
	 * @param b
	 */
	public void setCanReference(boolean b) {
		canReference = b;
	}

	/**
	 * @param b
	 */
	public void setCanDownload(boolean b) {
		canDownload = b;
	}

	/**
	 * @param b
	 */
	public void setCanLaunch(boolean b) {
		canLaunch = b;
	}

	/**
	 * Set access restrictions.
	 * @param i
	 */
	public void setAccess(int i) {
		access = i;
	}
	
	/**
	 * Set if the repository entry is exclusive 
	 * @param membersOnly
	 */
	public void setMembersOnly(boolean membersOnly) {
		this.membersOnly = membersOnly;
	}

	public String getAllowToLeave() {
		return allowToLeave;
	}

	public void setAllowToLeave(String allowToLeave) {
		this.allowToLeave = allowToLeave;
	}
	
	public RepositoryEntryAllowToLeaveOptions getAllowToLeaveOption() {
		RepositoryEntryAllowToLeaveOptions setting;
		if(StringHelper.containsNonWhitespace(allowToLeave)) {
			setting = RepositoryEntryAllowToLeaveOptions.valueOf(allowToLeave);
		} else if(RepositoryEntryManagedFlag.isManaged(this, RepositoryEntryManagedFlag.membersmanagement)) {
			setting = RepositoryEntryAllowToLeaveOptions.never;
		} else {
			setting = RepositoryEntryAllowToLeaveOptions.atAnyTime;
		}
		return setting;
	}

	public void setAllowToLeaveOption(RepositoryEntryAllowToLeaveOptions setting) {
		if(setting == null) {
			allowToLeave = null;
		} else {
			allowToLeave = setting.name();
		}
	}

	/**
	 * @return Returns the displayname.
	 */
	public String getDisplayname() {
		return displayname;
	}
	/**
	 * @param displayname The displayname to set.
	 */
	public void setDisplayname(String displayname) {
		/*
		 * See comment of #setResourcename(String)
		 */
		if (displayname.length() > 255)
			throw new AssertException("DisplayName is limited to 255 characters.");
		this.displayname = displayname;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getExternalRef() {
		return externalRef;
	}

	public void setExternalRef(String externalRef) {
		this.externalRef = externalRef;
	}

	public RepositoryEntryManagedFlag[] getManagedFlags() {
		return RepositoryEntryManagedFlag.toEnum(managedFlagsString);
	}


	public String getManagedFlagsString() {
		return managedFlagsString;
	}

	public void setManagedFlagsString(String managedFlagsString) {
		this.managedFlagsString = managedFlagsString;
	}

	public RepositoryEntryLifecycle getLifecycle() {
		return lifecycle;
	}

	public void setLifecycle(RepositoryEntryLifecycle lifecycle) {
		this.lifecycle = lifecycle;
	}

	public RepositoryEntryStatistics getStatistics() {
		return statistics;
	}

	public void setStatistics(RepositoryEntryStatistics statistics) {
		this.statistics = statistics;
	}

	public boolean exceedsSizeLimit() {
		final OLATResource sourceResource = getOlatResource();
		if (!sourceResource.getResourceableTypeName().equals("CourseModule")) {
			// only check size of courses, other resourceables are considered small enough
			return false;
		}
		try {
			PersistingCourseImpl sourceCourse = (PersistingCourseImpl) CourseFactory.loadCourse(sourceResource);
			return sourceCourse.exceedsSizeLimit(); // possible NullPointerException is caught on the spot
		} catch (Exception $ex) {
			return true;
		}
	}

	/**
	 * @see org.olat.core.id.OLATResourceablegetResourceableTypeName()
	 */
	public String getResourceableTypeName() { 
		return OresHelper.calculateTypeName(RepositoryEntry.class); 
	}

	/**
	 * @see org.olat.core.id.OLATResourceablegetResourceableId()
	 */
	public Long getResourceableId() {
		return getKey();
	}

	public int getVersion() {
		return version;
	}
	
	public void setVersion(int v) {
		version = v;
	}

	/**
	 * 
	 * @see org.olat.core.id.ModifiedInfo#getLastModified()
	 */
	@Override
	public Date getLastModified() {
		return lastModified;
	}

	/**
	 * 
	 * @see org.olat.core.id.ModifiedInfo#setLastModified(java.util.Date)
	 */
	@Override
	public void setLastModified(Date date) {
		this.lastModified = date;
	}
	
	public void setCreationDate(Date date) {
		this.creationDate = date;
	}
	
	public Date getDeletionDate() {
		return deletionDate;
	}

	public void setDeletionDate(Date deletionDate) {
		this.deletionDate = deletionDate;
	}

	public Identity getDeletedBy() {
		return deletedBy;
	}

	public void setDeletedBy(Identity deletedBy) {
		this.deletedBy = deletedBy;
	}

	@Override
	public int hashCode() {
		return getKey() == null ? 293485 : getKey().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if(obj instanceof RepositoryEntry) {
			RepositoryEntry re = (RepositoryEntry)obj;
			return getKey() != null && getKey().equals(re.getKey());
		}
		return false;
	}
	
	@Override
	public boolean equalsByPersistableKey(Persistable persistable) {
		return equals(persistable);
	}

	@Override
	public String toString() {
		return super.toString()+" [resourcename="+resourcename+", version="+version+", description="+description+"]";
	}
}