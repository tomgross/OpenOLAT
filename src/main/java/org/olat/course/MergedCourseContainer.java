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
package org.olat.course;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.MergeSource;
import org.olat.core.util.vfs.NamedContainerImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.callbacks.ReadOnlyCallback;
import org.olat.course.config.CourseConfig;
import org.olat.course.folder.MergedCourseElementDataContainer;
import org.olat.modules.sharedfolder.SharedFolderManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.model.RepositoryEntrySecurity;
import org.olat.resource.OLATResource;

/**
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class MergedCourseContainer extends MergeSource {
	
	private static final OLog log = Tracing.createLoggerFor(MergedCourseContainer.class);
	
	private final Long courseId;
	private boolean courseReadOnly = false;
	private boolean overrideReadOnly = false;
	private final IdentityEnvironment identityEnv;
	
	public MergedCourseContainer(Long courseId, String name) {
		this(courseId, name, null, false);
	}
	
	public MergedCourseContainer(Long courseId, String name, IdentityEnvironment identityEnv) {
		this(courseId, name, identityEnv, false);
	}
	
	public MergedCourseContainer(Long courseId, String name, IdentityEnvironment identityEnv, boolean overrideReadOnly) {
		super(null, name);
		this.courseId = courseId;
		this.identityEnv = identityEnv;
		this.overrideReadOnly = overrideReadOnly;
	}
	
	@Override
	protected void init() {
		ICourse course = CourseFactory.loadCourse(courseId);
		if(course instanceof PersistingCourseImpl) {
			init((PersistingCourseImpl)course);
		}
	}
	
	protected void init(PersistingCourseImpl persistingCourse) {
		super.init();
		
		RepositoryEntry courseRe = persistingCourse.getCourseEnvironment().getCourseGroupManager().getCourseEntry();
		courseReadOnly = !overrideReadOnly && (courseRe.getRepositoryEntryStatus().isClosed() || courseRe.getRepositoryEntryStatus().isUnpublished());
		if(courseReadOnly) {
			setLocalSecurityCallback(new ReadOnlyCallback());
		}

		if(identityEnv == null || identityEnv.getRoles().isOLATAdmin()) {
			VFSContainer courseContainer = persistingCourse.getIsolatedCourseFolder();
			if(courseReadOnly) {
				courseContainer.setLocalSecurityCallback(new ReadOnlyCallback());
			}
			addContainersChildren(courseContainer, true);
		} else {
			RepositoryEntry re = persistingCourse.getCourseEnvironment().getCourseGroupManager().getCourseEntry();
			RepositoryEntrySecurity reSecurity = RepositoryManager.getInstance()
					.isAllowed(identityEnv.getIdentity(), identityEnv.getRoles(), re);
			if(reSecurity.isEntryAdmin()) {
				VFSContainer courseContainer = persistingCourse.getIsolatedCourseFolder();
				if(courseReadOnly) {
					courseContainer.setLocalSecurityCallback(new ReadOnlyCallback());
				}
				addContainersChildren(courseContainer, true);
			}
		}
		
		initSharedFolder(persistingCourse);
			
		// add all course building blocks of type BC to a virtual folder
		MergedCourseElementDataContainer nodesContainer = new MergedCourseElementDataContainer(courseId, identityEnv);
		if (!nodesContainer.isEmpty()) {
			addContainer(nodesContainer);
		}
	}
	
	/**
	 * Grab any shared folder that is configured, but only when in unchecked
	 * security mode (no identity environment) or when the user has course
	 * admin rights
	 * 
	 * @param persistingCourse
	 */
	private void initSharedFolder(PersistingCourseImpl persistingCourse) {
		CourseConfig courseConfig = persistingCourse.getCourseConfig();
		String sfSoftkey = courseConfig.getSharedFolderSoftkey();
		if (StringHelper.containsNonWhitespace(sfSoftkey) && !CourseConfig.VALUE_EMPTY_SHAREDFOLDER_SOFTKEY.equals(sfSoftkey)) {
			RepositoryEntry re = persistingCourse.getCourseEnvironment().getCourseGroupManager().getCourseEntry();
			if(identityEnv == null || identityEnv.getRoles().isOLATAdmin() || RepositoryManager.getInstance().isOwnerOfRepositoryEntry(identityEnv.getIdentity(), re)) {
				OLATResource sharedResource = CoreSpringFactory.getImpl(RepositoryService.class).loadRepositoryEntryResourceBySoftKey(sfSoftkey);
				if (sharedResource != null) {
					OlatRootFolderImpl sharedFolder = SharedFolderManager.getInstance().getSharedFolder(sharedResource);
					if (sharedFolder != null) {
						if(courseConfig.isSharedFolderReadOnlyMount() || courseReadOnly) {
							sharedFolder.setLocalSecurityCallback(new ReadOnlyCallback());
						}
						//add local course folder's children as read/write source and any sharedfolder as subfolder
						addContainer(new NamedContainerImpl("_sharedfolder", sharedFolder));
					}
				}
			}
		}
	}

	private Object readResolve() {
		try {
			init();
			return this;
		} catch (Exception e) {
			log.error("Cannot init the merged container of a course after deserialization", e);
			return null;
		}
	}
}
