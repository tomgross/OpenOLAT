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
package org.olat.admin.user.delete.service;

import java.io.File;

import org.olat.basesecurity.BaseSecurity;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.services.taskexecutor.LongRunnable;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.PersistingCourseImpl;
import org.olat.course.assessment.manager.CourseAssessmentManagerImpl;
import org.olat.course.nodes.ProjectBrokerCourseNode;
import org.olat.course.nodes.TACourseNode;
import org.olat.course.nodes.ta.DropboxController;
import org.olat.course.nodes.ta.ReturnboxController;
import org.olat.ims.qti.editor.QTIEditorPackageImpl;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

/**
 * 
 * Initial date: 02.07.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class DeleteUserDataTask implements LongRunnable {
	private static final long serialVersionUID = 4278304131373256050L;

	private static final OLog log = Tracing.createLoggerFor(DeleteUserDataTask.class);

	private final Long identityKey;
	private final String newDeletedUserName;//it's the used username, not the one (let the name because of XStream)
	
	public DeleteUserDataTask(Long identityKey, String deletedUserName) {
		this.identityKey = identityKey;
		this.newDeletedUserName = deletedUserName;
	}
	
	@Override
	public void run() {
		long startTime = System.currentTimeMillis();
		Identity identity = CoreSpringFactory.getImpl(BaseSecurity.class).loadIdentityByKey(identityKey);
		deleteHomesMetaAndVersionDataOf(identity);
		deleteAllTempQtiEditorFilesOf(identity);
		deleteAllCoursesUserFilesOf(identity);
		log.info("Finished UserFileDeletionManager thread for identity=" + identity + " in " + (System.currentTimeMillis() - startTime) + " (ms)");
	}
	
	/**
	 * Delete the temporary files of the QTI-editor File e.g. /usr/local/olatfs/olat/olatdata/tmp/qtieditor/schuessler
	 * @param identity
	 */
	private void deleteAllTempQtiEditorFilesOf(Identity identity) {
		File userTempQtiEditorDir = new File(QTIEditorPackageImpl.getQTIEditorBaseDir(), newDeletedUserName);
		if (userTempQtiEditorDir.exists()) {
			FileUtils.deleteDirsAndFiles(userTempQtiEditorDir, true, true); 
			log.audit("User-Deletion: identity=" + identity.getName() +" : QTI editor temp files deleted under dir=" + userTempQtiEditorDir.getAbsolutePath());
		}
	}

	/**
	 * Delete the meta and version data of the home.
	 * @param identity
	 */
	private void deleteHomesMetaAndVersionDataOf(Identity identity) {
		String userHome =  FolderConfig.getUserHomes().substring(1);
		File metaHomeDir = new File(FolderConfig.getCanonicalMetaRoot(), userHome);
		File metaHomeUserDir = new File(metaHomeDir, newDeletedUserName);
		if(metaHomeUserDir.exists()) {
			// the meta-data under home/<USER> can be deleted and must not be renamed
			FileUtils.deleteDirsAndFiles(metaHomeUserDir, true, true); 			
			log.audit("User-Deletion: Delete meta-data homes directory for identity=" + identity.getName()+ " directory=" + metaHomeUserDir.getAbsolutePath());
		}

		File versionHomeDir = new File(FolderConfig.getCanonicalVersionRoot(), userHome);
		File versionHomeUserDir = new File(versionHomeDir, newDeletedUserName);
		if(versionHomeUserDir.exists()) {
			// the meta-data under home/<USER> can be deleted and must not be renamed
			FileUtils.deleteDirsAndFiles(versionHomeUserDir, true, true); 			
			log.audit("User-Deletion: Delete meta-data homes directory for identity=" + identity.getName()+ " directory=" + metaHomeUserDir.getAbsolutePath());
		}
	}

	/**
	 * Delete all 'dropboxes' or 'returnboxes' directories for certain user in the course-file structure.
	 * 
	 * @param identity
	 */
	private void deleteAllCoursesUserFilesOf(Identity identity) {
		File coursesBaseDir = getCoursesBaseContainer();
		// loop over all courses path e.g. olatdata\bcroot\course\78931391428316\dropboxes\78933379704296\deltest 
		//                                                                       ^^^^^^^^^ dirTypeName
		String[] courseDirNames = coursesBaseDir.list();
		// 1. loop over all course-id e.g. 78931391428316
		for (String courseDirName:courseDirNames) {
			if(!StringHelper.isLong(courseDirName)) continue;
			
			File courseDir = new File(coursesBaseDir, courseDirName);
			if (courseDir.isDirectory()) {
				deleteAssessmentDocuments(identity, courseDir);
				deleteDropboxReturnbox(identity, courseDir);
			}
		}
	}

	private void deleteAssessmentDocuments(Identity identity, File courseDir) {
		File assessmentDocsDir = new File(courseDir, CourseAssessmentManagerImpl.ASSESSMENT_DOCS_DIR);
		if(assessmentDocsDir.exists()) {
			File[] nodeDirs = assessmentDocsDir.listFiles();
			for(File nodeDir:nodeDirs) {
				File userDir = new File(nodeDir, "person_" + identity.getKey());
				if(userDir.exists()) {
					FileUtils.deleteDirsAndFiles(userDir, true, true); 
					log.audit("User-Deletion: identity=" + identity.getName() +" : User file data deleted under dir=" + userDir.getAbsolutePath());
				}
			}
		}
	}
	
	private void deleteDropboxReturnbox(Identity identity, File courseDir) {
		File returnboxDir = new File(courseDir, ReturnboxController.RETURNBOX_DIR_NAME);
		File dropboxDir = new File(courseDir, DropboxController.DROPBOX_DIR_NAME);
		if(returnboxDir.exists() || dropboxDir.exists()) {
			ICourse currentCourse = null;
			File[] dropboxReturnboxDirs = new File[]{ returnboxDir, dropboxDir};
			for (File dropboxReturnboxDir: dropboxReturnboxDirs) {
				if(!dropboxReturnboxDir.exists()) continue;
				
				File[] nodeDirs = dropboxReturnboxDir.listFiles();
				// 3. loop over all node-id e.g. 78933379704296
				for (File nodeDir:nodeDirs) {
					String currentNodeId = nodeDir.getName();
					if(currentCourse == null) {
						currentCourse = loadCourse(courseDir);
					}
					
					if(currentCourse == null) {
						return;//corrupted course
					} else if (isTaskNode(currentCourse, currentNodeId)) {
						deleteUserDirectory(identity, nodeDir);
					} else if (isProjectBrokerNode(currentCourse, currentNodeId)) {
						// additional loop over project-id
						File[] projectDirs = nodeDir.listFiles();
						for (File projectDir:projectDirs) {
							deleteUserDirectory(identity, projectDir);
						}
					} else {
						log.warn("found dropbox or returnbox and node-type is NO Task- or ProjectBroker-Type courseId=" + courseDir.getName() + " nodeId=" + currentNodeId, null);
					}
				}
			}
		}
	}
	
	private ICourse loadCourse(File courseDir) {
		try {
			Long resId = Long.parseLong(courseDir.getName());
			//check if the course exists
			OLATResource resource = OLATResourceManager.getInstance().findResourceable(resId, "CourseModule");
			if(resource != null) {
				return CourseFactory.loadCourse(resId);
			} else {
				log.warn("course with resid=" + courseDir.getName() + " has a folder but no resource/repository entry", null);
			}
		} catch (Exception e) {
			log.error("could not load course with resid=" + courseDir.getName(), e);
		}
		return null;
	}
	
	private boolean isProjectBrokerNode(ICourse currentCourse, String currentNodeId) {
		return currentCourse.getRunStructure().getNode(currentNodeId) instanceof ProjectBrokerCourseNode;
	}

	private boolean isTaskNode(ICourse currentCourse, String currentNodeId) {
		return currentCourse.getRunStructure().getNode(currentNodeId) instanceof TACourseNode;
	}

	private void deleteUserDirectory(Identity identity, File directory) {
		File userDir = new File(directory, newDeletedUserName);
		// 4. loop over all user-dir e.g. deltest (only once)
		if (userDir.exists()) {
			// ok found a directory of a user => delete it
			FileUtils.deleteDirsAndFiles(userDir, true, true); 
			log.audit("User-Deletion: identity=" + identity.getName() +" : User file data deleted under dir=" + userDir.getAbsolutePath());
		}
	}
	
	/**
	 * 
	 * @return e.g. olatdata\bcroot\course\
	 */
	private File getCoursesBaseContainer() {
		OlatRootFolderImpl courseRootContainer = new OlatRootFolderImpl(File.separator + PersistingCourseImpl.COURSE_ROOT_DIR_NAME + File.separator, null);
		return courseRootContainer.getBasefile(); 
	}
}
