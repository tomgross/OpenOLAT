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

package org.olat.course.nodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.services.taskexecutor.TaskExecutorManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.BreadcrumbPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.ExportUtil;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.io.ShieldOutputStream;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSManager;
import org.olat.course.ICourse;
import org.olat.course.archiver.ScoreAccountingHelper;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.assessment.bulk.BulkAssessmentToolController;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionExpression;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.export.CourseEnvironmentMapper;
import org.olat.course.nodes.ms.MSEditFormController;
import org.olat.course.nodes.ta.BulkDownloadToolController;
import org.olat.course.nodes.ta.ConvertToGTACourseNode;
import org.olat.course.nodes.ta.DropboxController;
import org.olat.course.nodes.ta.DropboxScoringViewController;
import org.olat.course.nodes.ta.ReturnboxController;
import org.olat.course.nodes.ta.TACourseNodeEditController;
import org.olat.course.nodes.ta.TACourseNodeRunController;
import org.olat.course.nodes.ta.TaskController;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.properties.PersistingCoursePropertyManager;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.scoring.AssessmentEvaluation;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.AssessmentToolOptions;
import org.olat.properties.Property;
import org.olat.repository.RepositoryEntry;
import org.olat.resource.OLATResource;

/**
 *   Initial Date:  30.08.2004
 *  
 *   @author Mike Stock
 * 	 @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */

public class TACourseNode extends GenericCourseNode implements PersistentAssessableCourseNode {
	private static final long serialVersionUID = -7266553843441305310L;

	private static final String PACKAGE_TA = Util.getPackageName(TACourseNodeRunController.class);

	private static final String PACKAGE = Util.getPackageName(TACourseNode.class);

	private static final String TYPE = "ta";
	
	// NLS support:
	
	private static final String NLS_GUESTNOACCESS_TITLE = "guestnoaccess.title";
	private static final String NLS_GUESTNOACCESS_MESSAGE = "guestnoaccess.message";
	private static final String NLS_ERROR_MISSINGSCORECONFIG_SHORT = "error.missingscoreconfig.short";
	private static final String NLS_WARN_NODEDELETE = "warn.nodedelete";
	
	
	private static final int CURRENT_CONFIG_VERSION = 2;

	/** CONF_TASK_ENABLED configuration parameter key. */
	public static final String CONF_TASK_ENABLED = "task_enabled";
	/** CONF_TASK_TYPE configuration parameter key. */
	public static final String CONF_TASK_TYPE = "task_type";

	/** CONF_TASK_TEXT configuration parameter key. */
	public static final String CONF_TASK_TEXT = "task_text";
	/** CONF_TASK_SAMPLING_WITH_REPLACEMENT configuration parameter key. */
	public static final String CONF_TASK_SAMPLING_WITH_REPLACEMENT = "task_sampling";
	/** CONF_TASK_FOLDER_REL_PATH configuration parameter key. */
	public static final String CONF_TASK_FOLDER_REL_PATH = "task_folder_rel";

	/** CONF_DROPBOX_ENABLED configuration parameter key. */
	public static final String CONF_DROPBOX_ENABLED = "dropbox_enabled";
	/** CONF_DROPBOX_ENABLEMAIL configuration parameter key. */
	public static final String CONF_DROPBOX_ENABLEMAIL = "dropbox_enablemail";
	/** CONF_DROPBOX_CONFIRMATION configuration parameter key. */
	public static final String CONF_DROPBOX_CONFIRMATION = "dropbox_confirmation";
	/** CONF_DROPBOX_CONFIRMATION_LINK configuration parameter key. */
	public static final String CONF_DROPBOX_CONFIRMATION_LINK = "dropbox_confirmation_link";

	/** CONF_RETURNBOX_ENABLED configuration parameter key. */
	public static final String CONF_RETURNBOX_ENABLED = "returnbox_enabled";

	/** CONF_SCORING_ENABLED configuration parameter key. */
	public static final String CONF_SCORING_ENABLED = "scoring_enabled";

	/** ACCESS_SCORING configuration parameter key. */
	public static final String ACCESS_SCORING = "scoring";
	/** ACCESS_DROPBOX configuration parameter key. */
	public static final String ACCESS_DROPBOX = "dropbox";
	/** ACCESS_RETURNBOX configuration parameter key. */
	public static final String ACCESS_RETURNBOX = "returnbox";	
	/** ACCESS_TASK configuration parameter key. */
	public static final String ACCESS_TASK = "task";
	/** ACCESS_SOLUTION configuration parameter key. */
	public static final String ACCESS_SOLUTION = "solution";

	/** CONF_SOLUTION_ENABLED configuration parameter key. */
	public static final String CONF_SOLUTION_ENABLED = "solution_enabled";

	/** Solution folder-name in the file-system. */
	public static final String SOLUTION_FOLDER_NAME = "solutions";

	/** CONF_TASK_PREVIEW configuration parameter key used for task-form. */
	public static final String CONF_TASK_PREVIEW = "task_preview";
	
	/** CONF_TASK_DESELECT configuration parameter key used for task-form. */
	public static final String CONF_TASK_DESELECT = "task_deselect";

	private Condition conditionTask, conditionDrop, conditionReturnbox, conditionScoring, conditionSolution;

	private static final OLog log = Tracing.createLoggerFor(TACourseNode.class);

	/**
	 * Default constructor.
	 */
	public TACourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl, BreadcrumbPanel stackPanel, ICourse course, UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		TACourseNodeEditController childTabCntrllr = new TACourseNodeEditController(ureq, wControl, course, this, euce);
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, euce, childTabCntrllr);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {
		updateModuleConfigDefaults(false);
		Controller controller;
		// Do not allow guests to access tasks
		Roles roles = ureq.getUserSession().getRoles();
		if (roles.isGuestOnly()) {
			Translator trans = new PackageTranslator(PACKAGE, ureq.getLocale());
			String title = trans.translate(NLS_GUESTNOACCESS_TITLE);
			String message = trans.translate(NLS_GUESTNOACCESS_MESSAGE);
			controller = MessageUIFactory.createInfoMessage(ureq, wControl, title, message);
		} else {
			controller = new TACourseNodeRunController(ureq, wControl, userCourseEnv, this, ne, false);
		}
		Controller ctrl = TitledWrapperHelper.getWrapper(ureq, wControl, controller, this, "o_ta_icon");
		return new NodeRunConstructionResult(ctrl);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPreviewController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPreviewController(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv, NodeEvaluation ne) {
		return new TACourseNodeRunController(ureq, wControl, userCourseEnv, this, ne, true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return false;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	@Override
	public StatusDescription isConfigValid() {
		/*
		 * first check the one click cache
		 */
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		boolean isValid = true;
		Boolean hasScoring = (Boolean) getModuleConfiguration().get(CONF_SCORING_ENABLED);
		if (hasScoring.booleanValue()) {
			if (!MSEditFormController.isConfigValid(getModuleConfiguration())) isValid = false;
		}
		StatusDescription sd = StatusDescription.NOERROR;
		if (!isValid) {
			// FIXME: refine statusdescriptions by moving the statusdescription
			String shortKey = NLS_ERROR_MISSINGSCORECONFIG_SHORT;
			String longKey = NLS_ERROR_MISSINGSCORECONFIG_SHORT;
			String[] params = new String[] { this.getShortTitle() };
			String translPackage = Util.getPackageName(MSEditFormController.class);
			sd = new StatusDescription(StatusDescription.ERROR, shortKey, longKey, params, translPackage);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(TACourseNodeEditController.PANE_TAB_CONF_SCORING);
		}
		// Check if any group exist make sense only with dropbox, scoring or solution
		Boolean hasDropbox  = (Boolean) getModuleConfiguration().get(CONF_DROPBOX_ENABLED);
	  if (hasDropbox == null) {
	    hasDropbox = new Boolean(false);
	   }
	  Boolean hasReturnbox = (Boolean) getModuleConfiguration().get(CONF_RETURNBOX_ENABLED);
	  if(hasReturnbox == null) {
	  	hasReturnbox = hasDropbox;
	  }
		Boolean hasSolution = (Boolean) getModuleConfiguration().get(CONF_SOLUTION_ENABLED);
	  if (hasSolution == null) {
	    hasSolution = new Boolean(false);
	  }
	  
	  //remove the error handling for missing groups as you can use the course members
		return sd;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		oneClickStatusCache = null;
		// only here we know which translator to take for translating condition
		// error messages
		String translatorStr = Util.getPackageName(TACourseNodeEditController.class);
		// check if group-manager is already initialized
		List<StatusDescription> sds = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}

	@Override
	protected void calcAccessAndVisibility(ConditionInterpreter ci, NodeEvaluation nodeEval) {
		if (ci == null) throw new OLATRuntimeException("no condition interpreter <" + getIdent() + " " + getShortName() + ">",
				new IllegalArgumentException());
		if (nodeEval == null) throw new OLATRuntimeException("node Evaluationt is null!! for <" + getIdent() + " " + getShortName() + ">",
				new IllegalArgumentException());
		// evaluate the preconditions
		boolean task = (getConditionTask().getConditionExpression() == null ? true : ci.evaluateCondition(conditionTask));
		nodeEval.putAccessStatus(ACCESS_TASK, task);
		boolean dropbox = (getConditionDrop().getConditionExpression() == null ? true : ci.evaluateCondition(conditionDrop));
		nodeEval.putAccessStatus(ACCESS_DROPBOX, dropbox);		
		boolean returnbox = (getConditionReturnbox().getConditionExpression() == null ? true : ci.evaluateCondition(conditionReturnbox));
		nodeEval.putAccessStatus(ACCESS_RETURNBOX, returnbox);
		boolean scoring = (getConditionScoring().getConditionExpression() == null ? true : ci.evaluateCondition(conditionScoring));
		nodeEval.putAccessStatus(ACCESS_SCORING, scoring);
		boolean solution = (getConditionSolution().getConditionExpression() == null ? true : ci.evaluateCondition(conditionSolution));
		nodeEval.putAccessStatus(ACCESS_SOLUTION, solution);

		boolean visible = (getPreConditionVisibility().getConditionExpression() == null ? true : ci
				.evaluateCondition(getPreConditionVisibility()));
		nodeEval.setVisible(visible);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#informOnDelete(org.olat.core.gui.UserRequest,
	 *      org.olat.course.ICourse)
	 */
	@Override
	public String informOnDelete(Locale locale, ICourse course) {
		Translator trans = new PackageTranslator(PACKAGE_TA, locale);
		CoursePropertyManager cpm = PersistingCoursePropertyManager.getInstance(course);
		List<Property> list = cpm.listCourseNodeProperties(this, null, null, null);
		if (list.size() != 0) return trans.translate("warn.nodedelete"); // properties exist
		File fTaskFolder = new File(FolderConfig.getCanonicalRoot() + TACourseNode.getTaskFolderPathRelToFolderRoot(course, this));
		if (fTaskFolder.exists() && fTaskFolder.list().length > 0) return trans.translate(NLS_WARN_NODEDELETE); // task folder contains files
		return null; // no data yet.
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#cleanupOnDelete(
	 *      org.olat.course.ICourse)
	 */
	@Override
	public void cleanupOnDelete(ICourse course) {
		CoursePropertyManager pm = course.getCourseEnvironment().getCoursePropertyManager();
		// Delete all properties...
		pm.deleteNodeProperties(this, null);
		File fTaskFolder = new File(FolderConfig.getCanonicalRoot() + TACourseNode.getTaskFolderPathRelToFolderRoot(course, this));
		if (fTaskFolder.exists()) {
			FileUtils.deleteDirsAndFiles(fTaskFolder, true, true);
		}
		File fDropBox = new File(FolderConfig.getCanonicalRoot() + DropboxController.getDropboxPathRelToFolderRoot(course.getCourseEnvironment(), this));
		if (fDropBox.exists()) {
			FileUtils.deleteDirsAndFiles(fDropBox, true, true);
		}
		
		OLATResource resource = course.getCourseEnvironment().getCourseGroupManager().getCourseResource();
		CoreSpringFactory.getImpl(TaskExecutorManager.class).delete(resource, getIdent());
	}

	/**
	 * @return dropbox condition
	 */
	public Condition getConditionDrop() {
		if (conditionDrop == null) {
			conditionDrop = new Condition();
		}
		conditionDrop.setConditionId("drop");
		return conditionDrop;
	}
	
	/**
	 * 
	 * @return Returnbox condition
	 */
	public Condition getConditionReturnbox() {
		if (conditionReturnbox == null) {
			conditionReturnbox = new Condition();
		}
		conditionReturnbox.setConditionId(ACCESS_RETURNBOX);
		return conditionReturnbox;
	}

	/**
	 * @return scoring condition
	 */
	public Condition getConditionScoring() {
		if (conditionScoring == null) {
			conditionScoring = new Condition();
		}
		conditionScoring.setConditionId("scoring");
		return conditionScoring;
	}

	/**
	 * @return task condition
	 */
	public Condition getConditionTask() {
		if (conditionTask == null) {
			conditionTask = new Condition();
		}
		conditionTask.setConditionId("task");
		return conditionTask;
	}

	/**
	 * @return scoring condition
	 */
	public Condition getConditionSolution() {
		if (conditionSolution == null) {
			conditionSolution = new Condition();
		}
		conditionSolution.setConditionId("solution");
		return conditionSolution;
	}

	/**
	 * @param conditionDrop
	 */
	public void setConditionDrop(Condition conditionDrop) {
		if (conditionDrop == null) {
			conditionDrop = getConditionDrop();
		}
		conditionDrop.setConditionId("drop");
		this.conditionDrop = conditionDrop;
	}
	
	/**
	 * 
	 * @param condition
	 */
	public void setConditionReturnbox(Condition condition) {
		if (condition == null) {
			condition = getConditionReturnbox();
		}
		condition.setConditionId(ACCESS_RETURNBOX);
		this.conditionReturnbox = condition;
	}

	/**
	 * @param conditionScoring
	 */
	public void setConditionScoring(Condition conditionScoring) {
		if (conditionScoring == null) {
			conditionScoring = getConditionScoring();
		}
		conditionScoring.setConditionId("scoring");
		this.conditionScoring = conditionScoring;
	}

	/**
	 * @param conditionTask
	 */
	public void setConditionTask(Condition conditionTask) {
		if (conditionTask == null) {
			conditionTask = getConditionTask();
		}
		conditionTask.setConditionId("task");
		this.conditionTask = conditionTask;
	}

	/**
	 * @param conditionScoring
	 */
	public void setConditionSolution(Condition conditionSolution) {
		if (conditionSolution == null) {
			conditionSolution = getConditionSolution();
		}
		conditionSolution.setConditionId("solution");
		this.conditionSolution = conditionSolution;
	}
	
	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserScoreEvaluation(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public AssessmentEvaluation getUserScoreEvaluation(UserCourseEnvironment userCourseEnv) {
		if(hasPassedConfigured() || hasScoreConfigured()) {
			return getUserScoreEvaluation(getUserAssessmentEntry(userCourseEnv));
		}
		return AssessmentEvaluation.EMPTY_EVAL;
	}
	
	@Override
	public AssessmentEvaluation getUserScoreEvaluation(AssessmentEntry entry) {
		return AssessmentEvaluation.toAssessmentEvalutation(entry, this);
	}

	@Override
	public AssessmentEntry getUserAssessmentEntry(UserCourseEnvironment userCourseEnv) {
		AssessmentManager am = userCourseEnv.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnv.getIdentityEnvironment().getIdentity();
		return am.getAssessmentEntry(this, mySelf);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasCommentConfigured()
	 */
	@Override
	public boolean hasCommentConfigured() {
		Boolean hasScoring = (Boolean) getModuleConfiguration().get(CONF_SCORING_ENABLED);
		if (hasScoring) {
			ModuleConfiguration config = getModuleConfiguration();
			Boolean comment = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_COMMENT_FIELD);
			if (comment != null) {
				return comment.booleanValue();
			}
		}
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasPassedConfigured()
	 */
	@Override
	public boolean hasPassedConfigured() {
		Boolean hasScoring = (Boolean) getModuleConfiguration().get(CONF_SCORING_ENABLED);
		if (hasScoring) {
			ModuleConfiguration config = getModuleConfiguration();
			Boolean passed = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD);
			if (passed != null) {
				return passed.booleanValue();
			}
		}
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasScoreConfigured()
	 */
	@Override
	public boolean hasScoreConfigured() {
		Boolean hasScoring = (Boolean) getModuleConfiguration().get(CONF_SCORING_ENABLED);
		if (hasScoring) {
			ModuleConfiguration config = getModuleConfiguration();
			Boolean score = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD);
			if (score != null) {
				return score.booleanValue();
			}
		}
		return false;
	}
	

	@Override
	public boolean isAssessedBusinessGroups() {
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasStatusConfigured()
	 */
	@Override
	public boolean hasStatusConfigured() {
		return true; // Task Course node has always a status-field
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getMaxScoreConfiguration()
	 */
	@Override
	public Float getMaxScoreConfiguration() {
		if (!hasScoreConfigured()) { throw new OLATRuntimeException(TACourseNode.class, "getMaxScore not defined when hasScore set to false", null); }
		ModuleConfiguration config = getModuleConfiguration();
		Float max = (Float) config.get(MSCourseNode.CONFIG_KEY_SCORE_MAX);
		return max;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getMinScoreConfiguration()
	 */
	@Override
	public Float getMinScoreConfiguration() {
		if (!hasScoreConfigured()) { throw new OLATRuntimeException(TACourseNode.class, "getMinScore not defined when hasScore set to false", null); }
		ModuleConfiguration config = getModuleConfiguration();
		Float min = (Float) config.get(MSCourseNode.CONFIG_KEY_SCORE_MIN);
		return min;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getCutValueConfiguration()
	 */
	@Override
	public Float getCutValueConfiguration() {
		if (!hasPassedConfigured()) { throw new OLATRuntimeException(TACourseNode.class, "getCutValue not defined when hasPassed set to false", null); }
		ModuleConfiguration config = getModuleConfiguration();
		Float cut = (Float) config.get(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE);
		return cut;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserCoachComment(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getUserCoachComment(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		String coachCommentValue = am.getNodeCoachComment(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return coachCommentValue;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserUserComment(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getUserUserComment(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		String userCommentValue = am.getNodeComment(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return userCommentValue;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserLog(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getUserLog(UserCourseEnvironment userCourseEnvironment) {
		UserNodeAuditManager am = userCourseEnvironment.getCourseEnvironment().getAuditManager();
		String logValue = am.getUserNodeLog(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return logValue;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#isEditableConfigured()
	 */
	@Override
	public boolean isEditableConfigured() {
		// always true
		return true;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserCoachComment(java.lang.String,
	 *      org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public void updateUserCoachComment(String coachComment, UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		if (coachComment != null) {
			am.saveNodeCoachComment(this, mySelf, coachComment);
		}
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserScoreEvaluation(org.olat.course.run.scoring.ScoreEvaluation,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.core.id.Identity)
	 */
	@Override
	public void updateUserScoreEvaluation(ScoreEvaluation scoreEval, UserCourseEnvironment userCourseEnvironment,
			Identity coachingIdentity, boolean incrementAttempts) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		ScoreEvaluation newScoreEval = new ScoreEvaluation(scoreEval.getScore(), scoreEval.getPassed(), scoreEval.getAssessmentStatus(), scoreEval.getUserVisible(), null, null);
		am.saveScoreEvaluation(this, coachingIdentity, mySelf, newScoreEval, userCourseEnvironment, incrementAttempts);		
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserUserComment(java.lang.String,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.core.id.Identity)
	 */
	@Override
	public void updateUserUserComment(String userComment, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		if (userComment != null) {
			am.saveNodeComment(this, coachingIdentity, mySelf, userComment);
		}
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserAttempts(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public Integer getUserAttempts(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		Integer userAttemptsValue = am.getNodeAttempts(this, mySelf);
		return userAttemptsValue;

	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasAttemptsConfigured()
	 */
	@Override
	public boolean hasAttemptsConfigured() {
		return true;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserAttempts(java.lang.Integer,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.core.id.Identity)
	 */
	@Override
	public void updateUserAttempts(Integer userAttempts, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		if (userAttempts != null) {
			AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
			Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
			am.saveNodeAttempts(this, coachingIdentity, mySelf, userAttempts);
		}
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#incrementUserAttempts(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public void incrementUserAttempts(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.incrementNodeAttempts(this, mySelf, userCourseEnvironment);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsEditController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public Controller getDetailsEditController(UserRequest ureq, WindowControl wControl, BreadcrumbPanel stackPanel,
			UserCourseEnvironment coachCourseEnv, UserCourseEnvironment assessedUserCourseEnv) {
		// prepare file component
		return new DropboxScoringViewController(ureq, wControl, this, assessedUserCourseEnv);
	}

	/**
	 * Factory method to launch course element assessment tools. limitToGroup is optional to skip he the group choose step
	 */
	@Override
	public List<Controller> createAssessmentTools(UserRequest ureq, WindowControl wControl, TooledStackedPanel stackPanel,
			UserCourseEnvironment coachCourseEnv, AssessmentToolOptions options) {
		List<Controller> tools = new ArrayList<Controller>(1);
		CourseEnvironment courseEnv = coachCourseEnv.getCourseEnvironment();
		if(!coachCourseEnv.isCourseReadOnly()) {
			tools.add(new BulkAssessmentToolController(ureq, wControl, courseEnv, this));
		}
		tools.add(new BulkDownloadToolController(ureq, wControl, courseEnv, options, this));
		return tools;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsListView(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getDetailsListView(UserCourseEnvironment userCourseEnvironment) {
		Identity identity = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		CoursePropertyManager propMgr = userCourseEnvironment.getCourseEnvironment().getCoursePropertyManager();
		List<Property> samples = propMgr.findCourseNodeProperties(this, identity, null, TaskController.PROP_ASSIGNED);
		if (samples.size() == 0) return null; // no sample assigned yet
		return samples.get(0).getStringValue();
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsListViewHeaderKey()
	 */
	@Override
	public String getDetailsListViewHeaderKey() {
		return "table.header.details.ta";
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasDetails()
	 */
	@Override
	public boolean hasDetails() {
		ModuleConfiguration modConfig = getModuleConfiguration();
		Boolean hasTask = (Boolean) modConfig.get(TACourseNode.CONF_TASK_ENABLED);
		if (hasTask == null) hasTask = Boolean.FALSE;
		Boolean hasDropbox = (Boolean) modConfig.get(TACourseNode.CONF_DROPBOX_ENABLED);
		if (hasDropbox == null) hasDropbox = Boolean.FALSE;		
		Boolean hasReturnbox = (Boolean) modConfig.get(TACourseNode.CONF_RETURNBOX_ENABLED);
		if (hasReturnbox == null) hasReturnbox = hasDropbox;
		
		return (hasTask.booleanValue() || hasDropbox.booleanValue() || hasReturnbox.booleanValue());
	}

	@Override
	public void copyConfigurationTo(CourseNode courseNode, ICourse course) {
		if(courseNode instanceof GTACourseNode) {
			ConvertToGTACourseNode convert = new ConvertToGTACourseNode();
			convert.convert(this, (GTACourseNode)courseNode, course);
		}
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#exportNode(java.io.File,
	 *      org.olat.course.ICourse)
	 */
	@Override
	public void exportNode(File fExportDirectory, ICourse course) {
		// export the tasks
		File fTaskFolder = new File(FolderConfig.getCanonicalRoot(), TACourseNode.getTaskFolderPathRelToFolderRoot(course, this));
		File fNodeExportDir = new File(fExportDirectory, getIdent());
		fNodeExportDir.mkdirs();
		FileUtils.copyDirContentsToDir(fTaskFolder, fNodeExportDir, false, "export task course node");
		
		//export thes solutions
		File fSolutionDir = new File(FolderConfig.getCanonicalRoot(), TACourseNode.getFoldernodesPathRelToFolderBase(course.getCourseEnvironment()) + "/" + getIdent());
		File fSolExportDir = new File(new File(fExportDirectory, "solutions"), getIdent());
		fSolExportDir.mkdirs();
		FileUtils.copyDirContentsToDir(fSolutionDir, fSolExportDir, false, "export task course node solutions");
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#importNode(java.io.File,
	 *      org.olat.course.ICourse, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, boolean)
	 */
	@Override
	public void importNode(File importDirectory, ICourse course, Identity owner, Locale locale, boolean withReferences) {
		//import tasks
		File fNodeImportDir = new File(importDirectory, getIdent());
		File fTaskfolderDir = new File(FolderConfig.getCanonicalRoot() + getTaskFolderPathRelToFolderRoot(course, this));
		FileUtils.copyDirContentsToDir(fNodeImportDir, fTaskfolderDir, false, "import task course node");
	
		File fSolutionDir = new File(FolderConfig.getCanonicalRoot(), getFoldernodesPathRelToFolderBase(course.getCourseEnvironment()) + "/" + getIdent());
		fSolutionDir.mkdirs();
		File fSolImportDir = new File(new File(importDirectory, "solutions"), getIdent());
		FileUtils.copyDirContentsToDir(fSolImportDir, fSolutionDir, false, "import task course node solutions");
	}

	@Override
	public boolean archiveNodeData(Locale locale, ICourse course, ArchiveOptions options, ZipOutputStream exportStream, String charset) {
		boolean dataFound = false;
		String dropboxPath = DropboxController.getDropboxPathRelToFolderRoot(course.getCourseEnvironment(), this);
		OlatRootFolderImpl dropboxDir = new OlatRootFolderImpl(dropboxPath, null);
		String solutionsPath = TACourseNode.getFoldernodesPathRelToFolderBase(course.getCourseEnvironment()) + "/" + this.getIdent();
		OlatRootFolderImpl solutionDir = new OlatRootFolderImpl(solutionsPath, null);
		String returnboxPath = ReturnboxController.getReturnboxPathRelToFolderRoot(course.getCourseEnvironment(), this);
		OlatRootFolderImpl returnboxDir = new OlatRootFolderImpl(returnboxPath, null);
		
		Boolean hasTask = (Boolean) getModuleConfiguration().get(TACourseNode.CONF_TASK_ENABLED);
		
		String dirName = "task_"
				+ StringHelper.transformDisplayNameToFileSystemName(getShortName())
				+ "_" + Formatter.formatDatetimeFilesystemSave(new Date(System.currentTimeMillis()));

		if (dropboxDir.exists() || solutionDir.exists() || returnboxDir.exists() || hasTask.booleanValue()){	
			// prepare writing course results overview table
			List<Identity> users = ScoreAccountingHelper.loadUsers(course.getCourseEnvironment(), options);
			Set<String> dropboxNames = null;
			if(options != null && (options.getGroup() != null || options.getIdentities() != null)) {
				dropboxNames = new HashSet<String>();
				for(Identity user:users) {
					dropboxNames.add(user.getName());
				}
			}
			
			String courseTitle = course.getCourseTitle();
			String fileName = ExportUtil.createFileNameWithTimeStamp(courseTitle, "xlsx");
			List<AssessableCourseNode> nodes = Collections.<AssessableCourseNode>singletonList(this);
			// write course results overview table to filesystem
			try {
				exportStream.putNextEntry(new ZipEntry(dirName + "/" + fileName));
				ScoreAccountingHelper.createCourseResultsOverviewXMLTable(users, nodes, course, locale,
						new ShieldOutputStream(exportStream));
				exportStream.closeEntry();
			} catch (IOException e) {
				log.error("", e);
			}

			// copy solutions to tmp dir
			if (solutionDir.exists()) {
				for(VFSItem child:solutionDir.getItems()) {
					dataFound = true;
					ZipUtil.addToZip(child, dirName + "/solutions", exportStream);
				}
			}
				
			// copy dropboxes to tmp dir
			if (dropboxDir.exists()) {
				//OLAT-6362 archive only dropboxes of users that handed in at least one file -> prevent empty folders in archive
				List<VFSItem> dropBoxContent = dropboxDir.getItems();
				for (VFSItem file:dropBoxContent) {
					if((dropboxNames == null || dropboxNames.contains(file.getName())) && VFSManager.isDirectoryAndNotEmpty(file)){
						dataFound = true;
						ZipUtil.addToZip(file, dirName + "/dropboxes", exportStream);
					}
				}
			}
			
			// copy only the choosen task to user taskfolder, loop over all users
			String taskfolderPath = TACourseNode.getTaskFolderPathRelToFolderRoot(course.getCourseEnvironment(),this);
			OlatRootFolderImpl taskfolderDir = new OlatRootFolderImpl(taskfolderPath, null);
			for(Identity identity:users) {
				// check if user already chose a task
				String assignedTask = TaskController.getAssignedTask(identity, course.getCourseEnvironment(), this);
				if (assignedTask != null) {
					VFSItem item = taskfolderDir.resolve(assignedTask);
					if(item != null) {
						// copy choosen task to user folder
						ZipUtil.addToZip(item, dirName + "/taskfolders/" + identity.getName(), exportStream);
						dataFound = true;
					}
				}
			}
	
			// copy returnboxes
			if (returnboxDir.exists()) {
				//OLAT-6362 archive only existing returnboxes -> prevent empty folders in archive
				List<VFSItem> returnBoxContent = returnboxDir.getItems();
				for (VFSItem file : returnBoxContent) {
					if((dropboxNames == null || dropboxNames.contains(file.getName())) && VFSManager.isDirectoryAndNotEmpty(file)){
						dataFound = true;
						ZipUtil.addToZip(file, dirName + "/returnboxes", exportStream);
					}
				}
			}
		}	
		return dataFound;
	}

	/**
	 * Get the the place where all task folders are stored. Path relative to the
	 * folder root.
	 * 
	 * @param courseEnv
	 * @return the task folders path relative to the folder root.
	 */
	public static String getTaskFoldersPathRelToFolderRoot(CourseEnvironment courseEnv) {
		return courseEnv.getCourseBaseContainer().getRelPath() + "/taskfolders";
	}

	/**
	 * Get the task folder path relative to the folder root for a specific node.
	 * 
	 * @param courseEnv
	 * @param cNode
	 * @return the task folder path relative to the folder root.
	 */
	public static String getTaskFolderPathRelToFolderRoot(CourseEnvironment courseEnv, CourseNode cNode) {
		return getTaskFoldersPathRelToFolderRoot(courseEnv) + "/" + cNode.getIdent();
	}

	/**
	 * Get the task folder path relative to the folder root for a specific node.
	 * 
	 * @param course
	 * @param cNode
	 * @return the task folder path relative to the folder root.
	 */
	public static String getTaskFolderPathRelToFolderRoot(ICourse course, CourseNode cNode) {
		return getTaskFolderPathRelToFolderRoot(course.getCourseEnvironment(), cNode);
	}

	/**
	 * Get the the place where all dropboxes are stored. Path relative to the
	 * folder root.
	 * 
	 * @param courseEnv
	 * @return the dropboxes path relative to the folder root.
	 */
	public static String getDropBoxesPathRelToFolderRoot(CourseEnvironment courseEnv) {
		return courseEnv.getCourseBaseContainer().getRelPath() + "/dropboxes";
	}

	/**
	 * Get the dropbox path relative to the folder root for a specific node.
	 * 
	 * @param courseEnv
	 * @param cNode
	 * @return the dropbox path relative to the folder root.
	 */
	public static String getDropBoxPathRelToFolderRoot(CourseEnvironment courseEnv, CourseNode cNode) {
		return getDropBoxesPathRelToFolderRoot(courseEnv) + "/" + cNode.getIdent();
	}

	/**
	 * Get the dropbox path relative to the folder root for a specific node.
	 * 
	 * @param course
	 * @param cNode
	 * @return the dropbox path relative to the folder root.
	 */
	public static String getDropBoxPathRelToFolderRoot(ICourse course, CourseNode cNode) {
		return getDropBoxPathRelToFolderRoot(course.getCourseEnvironment(), cNode);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#getConditionExpressions()
	 */
	@Override
	public List<ConditionExpression> getConditionExpressions() {
		List<ConditionExpression> retVal;
		List<ConditionExpression> parentsConditions = super.getConditionExpressions();
		if (parentsConditions.size() > 0) {
			retVal = new ArrayList<ConditionExpression>(parentsConditions);
		} else {
			retVal = new ArrayList<ConditionExpression>();
		}
		//
		String coS = getConditionDrop().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			ConditionExpression ce = new ConditionExpression(getConditionDrop().getConditionId());
			ce.setExpressionString(getConditionDrop().getConditionExpression());
			retVal.add(ce);
		}
		coS = getConditionReturnbox().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			ConditionExpression ce = new ConditionExpression(getConditionReturnbox().getConditionId());
			ce.setExpressionString(getConditionReturnbox().getConditionExpression());
			retVal.add(ce);
		}	else if(coS == null && getConditionDrop().getConditionExpression()!=null && !getConditionDrop().getConditionExpression().equals("")) {
			//old courses that had dropbox but no returnbox: use for returnbox the conditionExpression from dropbox
			ConditionExpression ce = new ConditionExpression(getConditionReturnbox().getConditionId());
			ce.setExpressionString(getConditionDrop().getConditionExpression());
			retVal.add(ce);
		}
		coS = getConditionScoring().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			ConditionExpression ce = new ConditionExpression(getConditionScoring().getConditionId());
			ce.setExpressionString(getConditionScoring().getConditionExpression());
			retVal.add(ce);
		}
		coS = getConditionTask().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			ConditionExpression ce = new ConditionExpression(getConditionTask().getConditionId());
			ce.setExpressionString(getConditionTask().getConditionExpression());
			retVal.add(ce);
		}
		//
		return retVal;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	public static OlatNamedContainerImpl getNodeFolderContainer(TACourseNode node, CourseEnvironment courseEnvironment) {
		String path = getFoldernodePathRelToFolderBase(courseEnvironment, node);
		OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(path, null);
		OlatNamedContainerImpl namedFolder = new OlatNamedContainerImpl(TACourseNode.SOLUTION_FOLDER_NAME, rootFolder);
		return namedFolder;
	}

	/**
	 * @param courseEnv
	 * @param node
	 * @return the relative folder base path for this folder node
	 */
	private static String getFoldernodePathRelToFolderBase(CourseEnvironment courseEnvironment, TACourseNode node) {
		return getFoldernodesPathRelToFolderBase(courseEnvironment) + "/" + node.getIdent();
	}

	/**
	 * @param courseEnv
	 * @return the relative folder base path for folder nodes
	 */
	public static String getFoldernodesPathRelToFolderBase(CourseEnvironment courseEnv) {
		return courseEnv.getCourseBaseContainer().getRelPath() + "/" + TACourseNode.SOLUTION_FOLDER_NAME;
	}

	/**
	 * Init config parameter with default values for a new course node.
	 */
	@Override
	public void updateModuleConfigDefaults(boolean isNewNode) {
		ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			// use defaults for new course building blocks
			// task defaults
			config.set(CONF_TASK_ENABLED, Boolean.TRUE);
			config.set(CONF_TASK_TYPE, TaskController.TYPE_MANUAL);
			config.set(CONF_TASK_TEXT, "");
			config.set(CONF_TASK_SAMPLING_WITH_REPLACEMENT, Boolean.TRUE);
			// dropbox defaults
			config.set(CONF_DROPBOX_ENABLED, Boolean.TRUE);
			config.set(CONF_RETURNBOX_ENABLED, Boolean.TRUE);
			config.set(CONF_DROPBOX_ENABLEMAIL, Boolean.FALSE);
			config.set(CONF_DROPBOX_CONFIRMATION, "");
			// scoring defaults
			config.set(CONF_SCORING_ENABLED, Boolean.TRUE);
			// New config parameter version 2
			config.setBooleanEntry(CONF_TASK_PREVIEW, false);
			// solution defaults
			config.set(CONF_SOLUTION_ENABLED, Boolean.TRUE);
			MSCourseNode.initDefaultConfig(config);
	    config.setConfigurationVersion(CURRENT_CONFIG_VERSION);
		} else {
			int version = config.getConfigurationVersion();
			if (version < CURRENT_CONFIG_VERSION) {
				// Loaded config is older than current config version => migrate
				if (version == 1) {
					// migrate V1 => V2
					config.setBooleanEntry(CONF_TASK_PREVIEW, false);
					// solution defaults
					config.set(CONF_SOLUTION_ENABLED, Boolean.FALSE);
					MSCourseNode.initDefaultConfig(config);
					version = 2;
				}
				config.setConfigurationVersion(CURRENT_CONFIG_VERSION);
			}
		}
	}
	
	@Override
	protected void postImportCopyConditions(CourseEnvironmentMapper envMapper) {
		super.postImportCopyConditions(envMapper);
		postImportCondition(conditionTask, envMapper);
		postImportCondition(conditionDrop, envMapper);
		postImportCondition(conditionReturnbox, envMapper);
		postImportCondition(conditionScoring, envMapper);
		postImportCondition(conditionSolution, envMapper);
	}

	@Override
	public void postExport(CourseEnvironmentMapper envMapper, boolean backwardsCompatible) {
		super.postExport(envMapper, backwardsCompatible);
		postExportCondition(conditionTask, envMapper, backwardsCompatible);
		postExportCondition(conditionDrop, envMapper, backwardsCompatible);
		postExportCondition(conditionReturnbox, envMapper, backwardsCompatible);
		postExportCondition(conditionScoring, envMapper, backwardsCompatible);
		postExportCondition(conditionSolution, envMapper, backwardsCompatible);
	}
}