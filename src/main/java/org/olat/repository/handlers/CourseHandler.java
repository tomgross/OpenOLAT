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

package org.olat.repository.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.control.generic.wizard.Step;
import org.olat.core.gui.control.generic.wizard.StepRunnerCallback;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.gui.media.CleanupAfterDeliveryFileMediaResource;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Formatter;
import org.olat.core.util.PathUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.nodes.INode;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.course.CorruptedCourseException;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.PersistingCourseImpl;
import org.olat.course.Structure;
import org.olat.course.assessment.AssessmentMode;
import org.olat.course.config.CourseConfig;
import org.olat.course.editor.CourseAccessAndProperties;
import org.olat.course.export.CourseEnvironmentMapper;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.groupsandrights.PersistingCourseGroupManager;
import org.olat.course.run.CourseRuntimeController;
import org.olat.course.run.RunMainController;
import org.olat.course.tree.CourseEditorTreeNode;
import org.olat.fileresource.types.GlossaryResource;
import org.olat.fileresource.types.ResourceEvaluation;
import org.olat.fileresource.types.SharedFolderFileResource;
import org.olat.modules.glossary.GlossaryManager;
import org.olat.modules.lecture.LectureService;
import org.olat.modules.reminder.Reminder;
import org.olat.modules.reminder.ReminderModule;
import org.olat.modules.reminder.ReminderRule;
import org.olat.modules.reminder.ReminderService;
import org.olat.modules.reminder.RuleSPI;
import org.olat.modules.reminder.model.ReminderRules;
import org.olat.modules.sharedfolder.SharedFolderManager;
import org.olat.repository.ErrorList;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryEntryImportExport.RepositoryEntryImport;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.model.RepositoryEntrySecurity;
import org.olat.repository.ui.RepositoryEntryRuntimeController.RuntimeControllerCreator;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.resource.references.ReferenceManager;
import org.olat.user.UserManager;

import de.tuchemnitz.wizard.workflows.coursecreation.CourseCreationHelper;
import de.tuchemnitz.wizard.workflows.coursecreation.CourseCreationMailHelper;
import de.tuchemnitz.wizard.workflows.coursecreation.model.CourseCreationConfiguration;
import de.tuchemnitz.wizard.workflows.coursecreation.steps.CcStep00;


/**
 * Initial Date: Apr 15, 2004
 *
 * @author 
 * 
 * Comment: Mike Stock
 * 
 */
public class CourseHandler implements RepositoryHandler {

	public static final String EDITOR_XML = "editortreemodel.xml";
	private static final OLog log = Tracing.createLoggerFor(CourseHandler.class);
	
	@Override
	public boolean isCreate() {
		return true;
	}
	
	@Override
	public RepositoryEntry createResource(Identity initialAuthor, String displayname, String description, Object createObject, Locale locale) {
		RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
		OLATResource resource = OLATResourceManager.getInstance().createOLATResourceInstance(CourseModule.class);
		RepositoryEntry re = repositoryService.create(initialAuthor, null, "", displayname, description, resource, RepositoryEntry.ACC_OWNERS);
		DBFactory.getInstance().commit();

		String shortDisplayname = Formatter.truncateOnly(displayname, 25);
		ICourse course = CourseFactory.createCourse(re, shortDisplayname, displayname, "");
		log.audit("Course created: " + course.getCourseTitle());
		return re;
	}
	
	@Override
	public boolean isPostCreateWizardAvailable() {
		return true;
	}

	@Override
	public String getCreateLabelI18nKey() {
		return "new.course";
	}

	@Override
	public ResourceEvaluation acceptImport(File file, String filename) {
		ResourceEvaluation eval = new ResourceEvaluation();
		try {
			IndexFileFilter visitor = new IndexFileFilter();
			Path fPath = PathUtils.visit(file, filename, visitor);
			
			if(visitor.isValid()) {
				Path repoXml = fPath.resolve("export/repo.xml");
				if(repoXml != null) {
					eval.setValid(true);
					
					RepositoryEntryImport re = RepositoryEntryImportExport.getConfiguration(repoXml);
					if(re != null) {
						eval.setDisplayname(re.getDisplayname());
						eval.setDescription(re.getDescription());
					}
					
					eval.setReferences(hasReferences(fPath));
				}
			}
			eval.setValid(visitor.isValid());
			
			PathUtils.closeSubsequentFS(fPath);
		} catch (IOException | IllegalArgumentException e) {
			log.error("", e);
		}
		return eval;
	}
	
	/**
	 * Find references in the export folder with the repo.xml.
	 * @param fPath
	 * @return
	 */
	private boolean hasReferences(Path fPath) {
		boolean hasReferences = false;
		Path export = fPath.resolve("export");
		if(Files.isDirectory(export)) {
			try(DirectoryStream<Path> directory = Files.newDirectoryStream(export)) {
			    for (Path p : directory) {
			    	Path repoXml = p.resolve("repo.xml");
			    	if(Files.exists(repoXml)) {
			    		hasReferences = true;
			    		break;
			    	}
			    }
			} catch (IOException e) {
				log.error("", e);
			}
		}
		return hasReferences;
	}
	
	@Override
	public RepositoryEntry importResource(Identity initialAuthor, String initialAuthorAlt, String displayname,
			String description, boolean withReferences, Locale locale, File file, String filename) {

		OLATResource newCourseResource = OLATResourceManager.getInstance().createOLATResourceInstance(CourseModule.class);
		ICourse course = CourseFactory.importCourseFromZip(newCourseResource, file);
		// cfc.release();
		if (course == null) {
			return null;
		}

		RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
		RepositoryEntry re = repositoryService
				.create(initialAuthor, null, "", displayname, description, newCourseResource, RepositoryEntry.ACC_OWNERS);
		DBFactory.getInstance().commit();

		// create empty run structure
		course = CourseFactory.openCourseEditSession(course.getResourceableId());
		Structure runStructure = course.getRunStructure();
		runStructure.getRootNode().removeAllChildren();
		CourseFactory.saveCourse(course.getResourceableId());
		
		//import references
		CourseEditorTreeNode rootNode = (CourseEditorTreeNode)course.getEditorTreeModel().getRootNode();
		importReferences(rootNode, course, initialAuthor, locale, withReferences);
		if(withReferences && course.getCourseConfig().hasCustomSharedFolder()) {
			importSharedFolder(course, initialAuthor);
		}
		if(withReferences && course.getCourseConfig().hasGlossary()) {
			importGlossary(course, initialAuthor);
		}

		// create group management / import groups
		CourseGroupManager cgm = course.getCourseEnvironment().getCourseGroupManager();
		File fImportBaseDirectory = course.getCourseExportDataDir().getBasefile();
		CourseEnvironmentMapper envMapper = cgm.importCourseBusinessGroups(fImportBaseDirectory);
		envMapper.setAuthor(initialAuthor);
		//upgrade course
		course = CourseFactory.loadCourse(cgm.getCourseResource());
		course.postImport(fImportBaseDirectory, envMapper);
		
		//rename root nodes, but only when user modified the course title
		boolean doUpdateTitle = true;
		File repoConfigXml = new File(fImportBaseDirectory, "repo.xml");
		if (repoConfigXml.exists()) {
			RepositoryEntryImport importConfig;
			try {
				importConfig = RepositoryEntryImportExport.getConfiguration(new FileInputStream(repoConfigXml));
				if(importConfig != null) {
					if (displayname.equals(importConfig.getDisplayname())) {					
						// do not update if title was not modified during import
						// user does not expect to have an updated title and there is a chance
						// the root node title is not the same as the course title
						doUpdateTitle = false;
					}
				}
			} catch (FileNotFoundException e) {
				// ignore
			}
		}
		if (doUpdateTitle) {
			course.getRunStructure().getRootNode().setShortTitle(Formatter.truncateOnly(displayname, 25)); //do not use truncate!
			course.getRunStructure().getRootNode().setLongTitle(displayname);
		}
		//course.saveRunStructure();
		CourseEditorTreeNode editorRootNode = ((CourseEditorTreeNode)course.getEditorTreeModel().getRootNode());
		editorRootNode.getCourseNode().setShortTitle(Formatter.truncateOnly(displayname, 25)); //do not use truncate!
		editorRootNode.getCourseNode().setLongTitle(displayname);
	
		// mark entire structure as dirty/new so the user can re-publish
		markDirtyNewRecursively(editorRootNode);
		// root has already been created during export. Unmark it.
		editorRootNode.setNewnode(false);		
		
		//save and close edit session
		CourseFactory.saveCourse(course.getResourceableId());
		CourseFactory.closeCourseEditSession(course.getResourceableId(), true);
		
		RepositoryEntryImportExport imp = new RepositoryEntryImportExport(fImportBaseDirectory);
		if(imp.anyExportedPropertiesAvailable()) {
			re = imp.importContent(re, getMediaContainer(re));
		}
		
		//import reminders
		importReminders(re, fImportBaseDirectory, envMapper, initialAuthor);
		
		//clean up export folder
		cleanExportAfterImport(fImportBaseDirectory);
		
		return re;
	}
	
	private void cleanExportAfterImport(File fImportBaseDirectory) {
		try {
			Path exportDir = fImportBaseDirectory.toPath();
			FileUtils.deleteDirsAndFiles(exportDir);
		} catch (Exception e) {
			log.error("", e);
		}
	}
	
	private void importSharedFolder(ICourse course, Identity owner) {
		SharedFolderManager sfm = SharedFolderManager.getInstance();
		RepositoryEntryImportExport importExport = sfm.getRepositoryImportExport(course.getCourseExportDataDir().getBasefile());
		
		SharedFolderFileResource resource = sfm.createSharedFolder();
		if (resource == null) {
			log.error("Error adding file resource during repository reference import: " + importExport.getDisplayName());
		}

		// unzip contents
		VFSContainer sfContainer = sfm.getSharedFolder(resource);
		File fExportedFile = importExport.importGetExportedFile();
		if (fExportedFile.exists()) {
			ZipUtil.unzip(new LocalFileImpl(fExportedFile), sfContainer);
		} else {
			log.warn("The actual contents of the shared folder were not found in the export.");
		}
		// create repository entry
		RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
		OLATResource ores = OLATResourceManager.getInstance().findOrPersistResourceable(resource);
		RepositoryEntry importedRepositoryEntry = repositoryService.create(owner, null,
				importExport.getResourceName(), importExport.getDisplayName(), importExport.getDescription(), ores, RepositoryEntry.ACC_OWNERS);

		// set the new shared folder reference
		CourseConfig courseConfig = course.getCourseEnvironment().getCourseConfig();
		courseConfig.setSharedFolderSoftkey(importedRepositoryEntry.getSoftkey());
		
		CoreSpringFactory.getImpl(ReferenceManager.class)
			.addReference(importedRepositoryEntry.getOlatResource(), course, SharedFolderManager.SHAREDFOLDERREF);		
		CourseFactory.setCourseConfig(course.getResourceableId(), courseConfig);
	}
	
	private void importGlossary(ICourse course, Identity owner) {
		GlossaryManager gm = GlossaryManager.getInstance();
		RepositoryEntryImportExport importExport = gm.getRepositoryImportExport(course.getCourseExportDataDir().getBasefile());
		GlossaryResource resource = gm.createGlossary();
		if (resource == null) {
			log.error("Error adding glossary directry during repository reference import: " + importExport.getDisplayName());
			return;
		}

		// unzip contents
		VFSContainer glossaryContainer = gm.getGlossaryRootFolder(resource);
		File fExportedFile = importExport.importGetExportedFile();
		if (fExportedFile.exists()) {
			ZipUtil.unzip(new LocalFileImpl(fExportedFile), glossaryContainer);
		} else {
			log.warn("The actual contents of the glossary were not found in the export.");
		}

		// create repository entry
		RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
		OLATResource ores = OLATResourceManager.getInstance().findOrPersistResourceable(resource);
		
		RepositoryEntry importedRepositoryEntry = repositoryService.create(owner,
				null, importExport.getResourceName(), importExport.getDisplayName(), importExport.getDescription(), ores, RepositoryEntry.ACC_OWNERS);

			// set the new glossary reference
		CourseConfig courseConfig = course.getCourseEnvironment().getCourseConfig();
		courseConfig.setGlossarySoftKey(importedRepositoryEntry.getSoftkey());
		CoreSpringFactory.getImpl(ReferenceManager.class).addReference(course, importedRepositoryEntry.getOlatResource(), GlossaryManager.GLOSSARY_REPO_REF_IDENTIFYER);			
		CourseFactory.setCourseConfig(course.getResourceableId(), courseConfig);
	}
	
	private void importReferences(CourseEditorTreeNode node, ICourse course, Identity owner, Locale locale, boolean withReferences) {
		node.getCourseNode().importNode(course.getCourseExportDataDir().getBasefile(), course, owner, locale, withReferences);

		for (int i = 0; i<node.getChildCount(); i++) {
			INode child = node.getChildAt(i);
			if(child instanceof CourseEditorTreeNode) {
				importReferences((CourseEditorTreeNode)child, course, owner, locale, withReferences);
			}
		}
	}
	
	private void importReminders(RepositoryEntry re, File fImportBaseDirectory, CourseEnvironmentMapper envMapper, Identity initialAuthor) {
		ReminderModule reminderModule = CoreSpringFactory.getImpl(ReminderModule.class);
		ReminderService reminderService = CoreSpringFactory.getImpl(ReminderService.class);
		List<Reminder> reminders = reminderService.importRawReminders(initialAuthor, re, fImportBaseDirectory);
		if(reminders.size() > 0) {
			for(Reminder reminder:reminders) {
				ReminderRules clonedRules = new ReminderRules();
				String configuration = reminder.getConfiguration();
				ReminderRules rules = reminderService.toRules(configuration);
				for(ReminderRule rule:rules.getRules()) {
					RuleSPI ruleSpi = reminderModule.getRuleSPIByType(rule.getType());
					if(ruleSpi != null) {
						ReminderRule clonedRule = ruleSpi.clone(rule, envMapper);
						clonedRules.getRules().add(clonedRule);
					}
				}
				String convertedConfiguration = reminderService.toXML(clonedRules);
				reminder.setConfiguration(convertedConfiguration);
				reminderService.save(reminder);
			}
		}
	}
	
	private void markDirtyNewRecursively(CourseEditorTreeNode editorRootNode) {
		editorRootNode.setDirty(true);
		editorRootNode.setNewnode(true);
		if (editorRootNode.getChildCount() > 0) {
			for (int i = 0; i < editorRootNode.getChildCount(); i++) {
				markDirtyNewRecursively((CourseEditorTreeNode)editorRootNode.getChildAt(i));
			}
		}
	}
	
	@Override
	public RepositoryEntry copy(Identity author, RepositoryEntry source, RepositoryEntry target) {
		final OLATResource sourceResource = source.getOlatResource();
		final OLATResource targetResource = target.getOlatResource();
		
		CourseFactory.copyCourse(sourceResource, targetResource);
		 
		//transaction copied
		ICourse sourceCourse = CourseFactory.loadCourse(source);
		CourseGroupManager sourceCgm = sourceCourse.getCourseEnvironment().getCourseGroupManager();
		CourseEnvironmentMapper env = PersistingCourseGroupManager.getInstance(sourceResource).getBusinessGroupEnvironment();
			
		File fExportDir = new File(WebappHelper.getTmpDir(), UUID.randomUUID().toString());
		fExportDir.mkdirs();
		sourceCgm.exportCourseBusinessGroups(fExportDir, env, false, false);

		ICourse course = CourseFactory.loadCourse(target);
		CourseGroupManager cgm = course.getCourseEnvironment().getCourseGroupManager();
		// import groups
		CourseEnvironmentMapper envMapper = cgm.importCourseBusinessGroups(fExportDir);
		envMapper.setAuthor(author);
		//upgrade to the current version of the course
		course = CourseFactory.loadCourse(cgm.getCourseResource());
		course.postCopy(envMapper, sourceCourse);
		
		cloneReminders(author, envMapper, source, target);
		cloneLectureConfig(source, target);
		
		return target;
	}
	
	private void cloneLectureConfig(RepositoryEntry source, RepositoryEntry target) {
		LectureService lectureService = CoreSpringFactory.getImpl(LectureService.class);
		lectureService.copyRepositoryEntryLectureConfiguration(source, target);
	}
	
	private void cloneReminders(Identity author, CourseEnvironmentMapper envMapper, RepositoryEntry source, RepositoryEntry target) {
		ReminderModule reminderModule = CoreSpringFactory.getImpl(ReminderModule.class);
		ReminderService reminderService = CoreSpringFactory.getImpl(ReminderService.class);
		List<Reminder> reminders = reminderService.getReminders(source);
		
		for(Reminder reminder:reminders) {
			String configuration = reminder.getConfiguration();
			ReminderRules rules = reminderService.toRules(configuration);
			ReminderRules clonedRules = new ReminderRules();
			for(ReminderRule rule:rules.getRules()) {
				RuleSPI ruleSpi = reminderModule.getRuleSPIByType(rule.getType());
				if(ruleSpi != null) {
					ReminderRule clonedRule = ruleSpi.clone(rule, envMapper);
					if (clonedRule != null) 
						clonedRules.getRules().add(clonedRule);
				}
			}

			Reminder clonedReminder = reminderService.createReminder(target, author);
			clonedReminder.setDescription(reminder.getDescription());
			clonedReminder.setEmailSubject(reminder.getEmailSubject());
			clonedReminder.setEmailBody(reminder.getEmailBody());
			clonedReminder.setConfiguration(reminderService.toXML(clonedRules));
			reminderService.save(clonedReminder);
		}
	}

	@Override
	public String getSupportedType() {
		return CourseModule.getCourseTypeName();
	}

	@Override
	public boolean supportsDownload() {
		return true;
	}
	
	@Override
	public EditionSupport supportsEdit(OLATResourceable resource) {
		return EditionSupport.yes;
	}
	
	@Override
	public boolean supportsAssessmentDetails() {
		return false;
	}

	@Override
	public MainLayoutController createLaunchController(RepositoryEntry re, RepositoryEntrySecurity reSecurity, UserRequest ureq, WindowControl wControl) {
		return new CourseRuntimeController(ureq, wControl, re, reSecurity,
				new RuntimeControllerCreator() {
					@Override
					public Controller create(UserRequest uureq, WindowControl wwControl, TooledStackedPanel toolbarPanel,
							RepositoryEntry entry, RepositoryEntrySecurity security, AssessmentMode assessmentMode) {
						ICourse course = CourseFactory.loadCourse(entry);
						return new RunMainController(uureq, wwControl, toolbarPanel, course, entry, security, assessmentMode);
					}
			}, true, true);
	}

	@Override
	public Controller createAssessmentDetailsController(RepositoryEntry re, UserRequest ureq, WindowControl wControl, TooledStackedPanel toolbar, Identity assessedIdentity) {
		return null;
	}

	@Override
	public MediaResource getAsMediaResource(OLATResourceable res, boolean backwardsCompatible) {
		RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(res, true);
		String exportFileName = StringHelper.transformDisplayNameToFileSystemName(re.getDisplayname()) + ".zip";
		File fExportZIP = new File(WebappHelper.getTmpDir(), exportFileName);
		CourseFactory.exportCourseToZIP(res, fExportZIP, false, backwardsCompatible);
		return new CleanupAfterDeliveryFileMediaResource(fExportZIP);
	}
	
	@Override
	public VFSContainer getMediaContainer(RepositoryEntry repoEntry) {
		OLATResource resource = repoEntry.getOlatResource();
		String relPath = File.separator + PersistingCourseImpl.COURSE_ROOT_DIR_NAME + File.separator + resource.getResourceableId();
		VFSContainer rootFolder = new OlatRootFolderImpl(relPath, null);
		VFSItem item = rootFolder.resolve("media");
		VFSContainer mediaContainer;
		if(item == null) {
			mediaContainer = rootFolder.createChildContainer("media");
		} else if(item instanceof VFSContainer) {
			mediaContainer = (VFSContainer)item;
		} else {
			log.error("media folder is not a container", null);
			mediaContainer = null;
		}
		return mediaContainer;
	}

	@Override
	public Controller createEditorController(RepositoryEntry re, UserRequest ureq, WindowControl wControl, TooledStackedPanel toolbar) {
		return CourseFactory.createEditorController(ureq, wControl, toolbar, re, null);
	}

	@Override
	public StepsMainRunController createWizardController(OLATResourceable res, UserRequest ureq, WindowControl wControl) {
		// load the course structure
		final RepositoryEntry repoEntry = (RepositoryEntry) res;
		ICourse course = CourseFactory.loadCourse(repoEntry);
		Translator cceTranslator = Util.createPackageTranslator(CourseCreationHelper.class, ureq.getLocale());
		final CourseCreationConfiguration courseConfig = new CourseCreationConfiguration(course.getCourseTitle(), Settings.getServerContextPathURI() + "/url/RepositoryEntry/" + repoEntry.getKey());
		// wizard finish callback called after "finish" is called
		final CourseCreationHelper ccHelper = new CourseCreationHelper(ureq.getLocale(), repoEntry, courseConfig , course);
		StepRunnerCallback finishCallback = new StepRunnerCallback() {
			public Step execute(UserRequest uureq, WindowControl control, StepsRunContext runContext) {
				// retrieve access and properties
				CourseAccessAndProperties accessAndProps = (CourseAccessAndProperties) runContext.get("accessAndProperties");
				courseConfig.setAccessAndProperties(accessAndProps);
				
				// here goes the code which reads out the wizards data from the runContext and then does some wizardry
				ccHelper.finalizeWorkflow(uureq);
				control.setInfo(CourseCreationMailHelper.getSuccessMessageString(uureq));
				// send notification mail
				final MailerResult mr = CourseCreationMailHelper.sentNotificationMail(uureq, ccHelper.getConfiguration());
				MailHelper.printErrorsAndWarnings(mr, control, uureq.getUserSession().getRoles().isOLATAdmin(), uureq.getLocale());
				return StepsMainRunController.DONE_MODIFIED;
			}
		};
		Step start  = new CcStep00(ureq, courseConfig, repoEntry);
		StepsMainRunController ccSMRC = new StepsMainRunController(ureq, wControl, start, finishCallback, null, cceTranslator.translate("coursecreation.title"), "o_sel_course_create_wizard");
		return ccSMRC;
	}

	@Override
	public boolean cleanupOnDelete(RepositoryEntry entry, OLATResourceable res) {
		// notify all current users of this resource (course) that it will be deleted now.
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new OLATResourceableJustBeforeDeletedEvent(res), res);
		//archiving is done within readyToDelete		
		OLATResourceManager rm = OLATResourceManager.getInstance();
		OLATResource resource = rm.findResourceable(res);
		CourseFactory.deleteCourse(entry, resource);
		return true;
	}

	@Override
	public boolean readyToDelete(RepositoryEntry entry, Identity identity, Roles roles, Locale locale, ErrorList errors) {
		ReferenceManager refM = CoreSpringFactory.getImpl(ReferenceManager.class);
		String referencesSummary = refM.getReferencesToSummary(entry.getOlatResource(), locale);
		if (referencesSummary != null) {
			Translator translator = Util.createPackageTranslator(RepositoryManager.class, locale);
			errors.setError(translator.translate("details.delete.error.references",
					new String[] { referencesSummary, entry.getDisplayname() }));
			return false;
		}
		/*
		 * make an archive of the course nodes with valuable data
		 */
		UserManager um = UserManager.getInstance();
		String charset = um.getUserCharset(identity);
		try {
			CourseFactory.archiveCourse(entry.getOlatResource(),charset, locale, identity, roles);
		} catch (CorruptedCourseException e) {
			log.error("The course is corrupted, cannot archive it: " + entry, e);
		}
		return true;
	}

	@Override
	public LockResult acquireLock(OLATResourceable ores, Identity identity) {
		return CoordinatorManager.getInstance().getCoordinator().getLocker().acquireLock(ores, identity, CourseFactory.COURSE_EDITOR_LOCK);
	}

	@Override
	public void releaseLock(LockResult lockResult) {
		if(lockResult!=null) {
		  CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lockResult);
		}
	}

	@Override
	public boolean isLocked(OLATResourceable ores) {
		return CoordinatorManager.getInstance().getCoordinator().getLocker().isLocked(ores, CourseFactory.COURSE_EDITOR_LOCK);
	}
	
	private static class IndexFileFilter extends SimpleFileVisitor<Path> {
		private boolean editorFile;

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
		throws IOException {

			String filename = file.getFileName().toString();
			if(EDITOR_XML.equals(filename)) {
				editorFile = true;
			}
			return editorFile ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
		}
		
		public boolean isValid() {
			return editorFile;
		}
	}
}
