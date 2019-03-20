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
package org.olat.course.nodes.gta.ui;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.olat.basesecurity.GroupRoles;
import org.olat.core.commons.services.notifications.PublisherData;
import org.olat.core.commons.services.notifications.SubscriptionContext;
import org.olat.core.commons.services.notifications.ui.ContextualSubscriptionController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.assessment.manager.UserCourseInformationsManager;
import org.olat.course.nodes.GTACourseNode;
import org.olat.course.nodes.gta.GTAManager;
import org.olat.course.nodes.gta.GTARelativeToDates;
import org.olat.course.nodes.gta.GTAType;
import org.olat.course.nodes.gta.Task;
import org.olat.course.nodes.gta.TaskList;
import org.olat.course.nodes.gta.TaskProcess;
import org.olat.course.nodes.gta.ui.events.TaskMultiUserEvent;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.modules.ModuleConfiguration;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryService;
import org.olat.repository.model.RepositoryEntryLifecycle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 20.03.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public abstract class GTAAbstractController extends BasicController implements GenericEventListener {
	
	protected VelocityContainer mainVC;

	protected Identity assessedIdentity;
	protected BusinessGroup assessedGroup;
	protected CourseEnvironment courseEnv;
	protected UserCourseEnvironment userCourseEnv;
	
	protected final TaskList taskList;
	protected final GTACourseNode gtaNode;
	protected final ModuleConfiguration config;
	protected final RepositoryEntry courseEntry;
	
	protected final boolean withSubscription;
	protected final PublisherData publisherData;
	protected final SubscriptionContext subsContext;

	protected final boolean withTitle;
	protected final boolean withGrading;
	
	protected final boolean businessGroupTask;
	
	protected final OLATResourceable taskListEventResource;
	
	protected GTAStepPreferences stepPreferences;
	
	private ContextualSubscriptionController contextualSubscriptionCtr;
	
	private DueDate assignmentDueDate;
	private DueDate submissionDueDate;
	private DueDate solutionDueDate;
	
	@Autowired
	protected GTAManager gtaManager;
	@Autowired
	protected RepositoryService repositoryService;
	@Autowired
	protected BusinessGroupService businessGroupService;
	@Autowired
	protected UserCourseInformationsManager userCourseInformationsManager;
	@Autowired
	protected CourseModule courseModule;
	
	public GTAAbstractController(UserRequest ureq, WindowControl wControl,
			GTACourseNode gtaNode, CourseEnvironment courseEnv, boolean withTitle, boolean withGrading, boolean withSubscription) {
		this(ureq, wControl, gtaNode, courseEnv, null, null, null, withTitle, withGrading, withSubscription);
	}
	
	public GTAAbstractController(UserRequest ureq, WindowControl wControl,
			GTACourseNode gtaNode, CourseEnvironment courseEnv, UserCourseEnvironment userCourseEnv, boolean withTitle, boolean withGrading, boolean withSubscription) {
		this(ureq, wControl, gtaNode, courseEnv, userCourseEnv, null, null, withTitle, withGrading, withSubscription);
	}

	public GTAAbstractController(UserRequest ureq, WindowControl wControl,
			GTACourseNode gtaNode, CourseEnvironment courseEnv, UserCourseEnvironment userCourseEnv,
			BusinessGroup assessedGroup, Identity assessedIdentity, boolean withTitle, boolean withGrading, boolean withSubscription) {
		super(ureq, wControl);
		
		this.withTitle = withTitle;
		this.withGrading = withGrading;
		this.withSubscription = withSubscription;
		
		this.gtaNode = gtaNode;
		this.config = gtaNode.getModuleConfiguration();

		this.userCourseEnv = userCourseEnv;
		this.courseEnv = courseEnv;
		this.courseEntry = courseEnv.getCourseGroupManager().getCourseEntry();
		
		this.assessedIdentity = assessedIdentity;
		this.assessedGroup = assessedGroup;
		
		businessGroupTask = GTAType.group.name().equals(config.getStringValue(GTACourseNode.GTASK_TYPE));
		
		taskList = gtaManager.createIfNotExists(courseEntry, gtaNode);
		publisherData = gtaManager.getPublisherData(courseEnv, gtaNode);
		subsContext = gtaManager.getSubscriptionContext(courseEnv, gtaNode);
		
		stepPreferences = (GTAStepPreferences)ureq.getUserSession()
				.getGuiPreferences()
				.get(GTAStepPreferences.class, taskList.getKey().toString());
		if(stepPreferences == null) {
			stepPreferences = new GTAStepPreferences();
		}
		
		taskListEventResource = OresHelper.createOLATResourceableInstance("GTaskList", taskList.getKey());
		CoordinatorManager.getInstance().getCoordinator()
			.getEventBus().registerFor(this, getIdentity(), taskListEventResource);
	}

	@Override
	protected void doDispose() {
		CoordinatorManager.getInstance().getCoordinator()
			.getEventBus().deregisterFor(this, taskListEventResource);
	}

	@Override
	public void event(Event event) {
		if(event instanceof TaskMultiUserEvent) {
			TaskMultiUserEvent ste = (TaskMultiUserEvent)event;
			if(!getIdentity().getKey().equals(ste.getEmitterKey())
					&& ((assessedGroup != null && assessedGroup.getKey().equals(ste.getForGroupKey()))
							|| (assessedIdentity != null && assessedIdentity.getKey().equals(ste.getForIdentityKey())))) {
				processEvent(ste);
			}
		}
	}
	
	protected abstract void processEvent(TaskMultiUserEvent event);

	protected abstract void initContainer(UserRequest ureq);
	
	protected final void process(UserRequest ureq) {
		Task task;
		if(businessGroupTask) {
			task = gtaManager.getTask(assessedGroup, taskList);
		} else {
			task = gtaManager.getTask(assessedIdentity, taskList);
		}
		
		if (withSubscription && subsContext != null) {
			contextualSubscriptionCtr = new ContextualSubscriptionController(ureq, getWindowControl(), subsContext, publisherData);
			listenTo(contextualSubscriptionCtr);
			mainVC.put("contextualSubscription", contextualSubscriptionCtr.getInitialComponent());
		}
		
		boolean assignment = config.getBooleanSafe(GTACourseNode.GTASK_ASSIGNMENT);
		mainVC.contextPut("assignmentEnabled", assignment);
		if(assignment) {
			task = stepAssignment(ureq, task);
		} else if(task == null) {
			TaskProcess firstStep = gtaManager.firstStep(gtaNode);
			task = gtaManager.createTask(null, taskList, firstStep, assessedGroup, assessedIdentity, gtaNode);
		}
		
		boolean submit = config.getBooleanSafe(GTACourseNode.GTASK_SUBMIT);
		mainVC.contextPut("submitEnabled", submit);
		if(submit) {
			task = stepSubmit(ureq, task);
		} else if(task != null && task.getTaskStatus() == TaskProcess.submit) {
			task = gtaManager.nextStep(task, gtaNode);
		}
		
		boolean reviewAndCorrection = config.getBooleanSafe(GTACourseNode.GTASK_REVIEW_AND_CORRECTION);
		mainVC.contextPut("reviewAndCorrectionEnabled", reviewAndCorrection);
		if(reviewAndCorrection) {
			task = stepReviewAndCorrection(ureq, task);
		} else if(task != null && task.getTaskStatus() == TaskProcess.review) {
			task = gtaManager.nextStep(task, gtaNode);
		}
		
		boolean revision = config.getBooleanSafe(GTACourseNode.GTASK_REVISION_PERIOD);
		mainVC.contextPut("revisionEnabled", reviewAndCorrection && revision);
		if(reviewAndCorrection && revision) {
			task = stepRevision(ureq, task);
		} else if(task != null && (task.getTaskStatus() == TaskProcess.revision || task.getTaskStatus() == TaskProcess.correction)) {
			task = gtaManager.nextStep(task, gtaNode);
		}
		
		boolean solution = config.getBooleanSafe(GTACourseNode.GTASK_SAMPLE_SOLUTION);
		mainVC.contextPut("solutionEnabled", solution);
		if(solution) {
			stepSolution(ureq, task);
		} else if(task != null && task.getTaskStatus() == TaskProcess.solution) {
			task = gtaManager.nextStep(task, gtaNode);
		}
		
		boolean grading = config.getBooleanSafe(GTACourseNode.GTASK_GRADING);
		mainVC.contextPut("gradingEnabled", grading);
		if(grading) {
			stepGrading(ureq, task);
		} else if(task != null && task.getTaskStatus() == TaskProcess.grading) {
			task = gtaManager.nextStep(task, gtaNode);
		}
		
		mainVC.contextPut("changelogconfig", courseModule.isDisplayChangeLog());
		Roles roles = ureq.getUserSession().getRoles();
		if (roles.isOLATAdmin() || roles.isAuthor()) {
			mainVC.contextPut("changelogconfig", true);
		}
		
		nodeLog();
		collapsedContents(task);
	}
	
	protected final void collapsedContents(Task currentTask) {
		TaskProcess status = null;
		TaskProcess previousStatus = null;
		if(currentTask != null) {
			status = currentTask.getTaskStatus();
			previousStatus = gtaManager.previousStep(status, gtaNode);
		}
		
		boolean assignment = Boolean.TRUE.equals(stepPreferences.getAssignement())
				|| TaskProcess.assignment.equals(status) || TaskProcess.assignment.equals(previousStatus);
		mainVC.contextPut("collapse_assignement", new Boolean(assignment));
		
		boolean submit = Boolean.TRUE.equals(stepPreferences.getSubmit())
				|| TaskProcess.submit.equals(status) || TaskProcess.submit.equals(previousStatus);
		mainVC.contextPut("collapse_submit", new Boolean(submit));
		
		boolean reviewAndCorrection = Boolean.TRUE.equals(stepPreferences.getReviewAndCorrection())
				|| TaskProcess.review.equals(status) || TaskProcess.review.equals(previousStatus);
		mainVC.contextPut("collapse_reviewAndCorrection", new Boolean(reviewAndCorrection));
		
		boolean revision = Boolean.TRUE.equals(stepPreferences.getRevision())
				|| TaskProcess.revision.equals(status) || TaskProcess.revision.equals(previousStatus)
				|| TaskProcess.correction.equals(status) || TaskProcess.correction.equals(previousStatus);
		mainVC.contextPut("collapse_revision", new Boolean(revision));
		
		boolean solution = Boolean.TRUE.equals(stepPreferences.getSolution())
				|| TaskProcess.solution.equals(status) || TaskProcess.solution.equals(previousStatus);
		mainVC.contextPut("collapse_solution", new Boolean(solution));
		
		boolean grading = Boolean.TRUE.equals(stepPreferences.getGrading())
				|| TaskProcess.grading.equals(status) || TaskProcess.grading.equals(previousStatus);
		mainVC.contextPut("collapse_grading", new Boolean(grading));
	}
	
	protected Task stepAssignment(@SuppressWarnings("unused") UserRequest ureq, Task assignedTask) {
		DueDate dueDate = getAssignementDueDate(assignedTask);
		if(dueDate != null) {
			if(dueDate.getDueDate() != null) {
				Date date = dueDate.getDueDate();
				String dateAsString = formatDueDate(dueDate, true);
				mainVC.contextPut("assignmentDueDate", dateAsString);
				mainVC.contextRemove("assignmentDueDateMsg");
				
				if(assignedTask != null && assignedTask.getTaskStatus() == TaskProcess.assignment
						&& date.compareTo(new Date()) < 0) {
					//push to the next step
					assignedTask = gtaManager.nextStep(assignedTask, gtaNode);
				}
			} else if(dueDate.getMessage() != null) {
				mainVC.contextPut("assignmentDueDateMsg", dueDate.getMessage());
				mainVC.contextRemove("assignmentDueDate");
			}
		}
		return assignedTask;
	}
	
	/**
	 * User friendly format, 2015-06-20 00:00 will be rendered as 2015-06-20
	 * if @param userDeadLine is false (for solution,e.g) and 2015-06-20
	 * if @param userDeadLine is true (meaning the user have the whole day
	 * to do the job until the deadline at midnight).
	 * @param dueDate
	 * @param user deadline
	 * @return
	 */
	protected String formatDueDate(DueDate dueDate, boolean userDeadLine) {
		Date date = dueDate.getDueDate();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		String formattedDate;
		if(cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0) {
			if(userDeadLine) {
				cal.add(Calendar.DATE, -1);
			}
			formattedDate = Formatter.getInstance(getLocale()).formatDate(cal.getTime());
		} else {
			formattedDate = Formatter.getInstance(getLocale()).formatDateAndTime(date);
		}
		return formattedDate;
	}
	
	protected void resetDueDates() {
		assignmentDueDate = null;
		submissionDueDate = null;
		solutionDueDate = null;
	}
	
	protected DueDate getAssignementDueDate(Task task) {
		if(assignmentDueDate == null) {
			Date dueDate = gtaNode.getModuleConfiguration().getDateValue(GTACourseNode.GTASK_ASSIGNMENT_DEADLINE);
			boolean relativeDate = gtaNode.getModuleConfiguration().getBooleanSafe(GTACourseNode.GTASK_RELATIVE_DATES);
			if(relativeDate) {
				int numOfDays = gtaNode.getModuleConfiguration().getIntegerSafe(GTACourseNode.GTASK_ASSIGNMENT_DEADLINE_RELATIVE, -1);
				String relativeTo = gtaNode.getModuleConfiguration().getStringValue(GTACourseNode.GTASK_ASSIGNMENT_DEADLINE_RELATIVE_TO);
				if(numOfDays >= 0 && StringHelper.containsNonWhitespace(relativeTo)) {
					assignmentDueDate = getReferenceDate(numOfDays, relativeTo, task);
				}
			} else if(dueDate != null) {
				assignmentDueDate = new DueDate(dueDate);
			}
		}
		return assignmentDueDate;
	}
	
	protected DueDate getReferenceDate(int numOfDays, String relativeTo, Task assignedTask) {
		DueDate dueDate = null;
		if(numOfDays >= 0 && StringHelper.containsNonWhitespace(relativeTo)) {
			GTARelativeToDates rel = GTARelativeToDates.valueOf(relativeTo);
			Date referenceDate = null;
			String message = null;
			switch(rel) {
				case courseStart: {
					RepositoryEntryLifecycle lifecycle = courseEntry.getLifecycle();
					if(lifecycle != null && lifecycle.getValidFrom() != null) {
						referenceDate = lifecycle.getValidFrom();
					}
					break;
				}
				case courseLaunch: {
					referenceDate = userCourseInformationsManager
							.getInitialLaunchDate(courseEnv.getCourseGroupManager().getCourseResource(), assessedIdentity);
					break;
				}
				case enrollment: {
					referenceDate = repositoryService
							.getEnrollmentDate(courseEntry, assessedIdentity);
					break;
				}
				case assignment: {
					if(assignedTask != null) {
						referenceDate = assignedTask.getAssignmentDate(); 
					} else {
						message = translate("relative.to.assignment.message", Integer.toString(numOfDays));
					}
					break;
				}
			}
			
			if(referenceDate != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(referenceDate);
				cal.add(Calendar.DATE, numOfDays);
				dueDate = new DueDate(cal.getTime());
			} else if(message != null) {
				dueDate = new DueDate(message);
			}
		}
		return dueDate;
	}
	
	protected Task stepSubmit(@SuppressWarnings("unused")UserRequest ureq, Task assignedTask) {
		DueDate dueDate = getSubmissionDueDate(assignedTask);
		if(dueDate != null) {
			if(dueDate.getDueDate() != null) {
				Date date = dueDate.getDueDate();
				String dateAsString = formatDueDate(dueDate, true);
				mainVC.contextPut("submitDueDate", dateAsString);
				mainVC.contextRemove("submitDueDateMsg");
				
				if(assignedTask != null && assignedTask.getTaskStatus() == TaskProcess.submit
						&& date.compareTo(new Date()) < 0) {
					//push to the next step
					assignedTask = gtaManager.nextStep(assignedTask, gtaNode);
					doUpdateAttempts();
				}
			} else if(dueDate.getMessage() != null) {
				mainVC.contextPut("submitDueDateMsg", dueDate.getMessage());
				mainVC.contextRemove("submitDueDate");
			}
		}
		
		return assignedTask;
	}
	
	protected DueDate getSubmissionDueDate(Task assignedTask) {
		if(submissionDueDate == null) {
			Date dueDate = gtaNode.getModuleConfiguration().getDateValue(GTACourseNode.GTASK_SUBMIT_DEADLINE);
			boolean relativeDate = gtaNode.getModuleConfiguration().getBooleanSafe(GTACourseNode.GTASK_RELATIVE_DATES);
			if(relativeDate) {
				int numOfDays = gtaNode.getModuleConfiguration().getIntegerSafe(GTACourseNode.GTASK_SUBMIT_DEADLINE_RELATIVE, -1);
				String relativeTo = gtaNode.getModuleConfiguration().getStringValue(GTACourseNode.GTASK_SUBMIT_DEADLINE_RELATIVE_TO);
				if(numOfDays >= 0 && StringHelper.containsNonWhitespace(relativeTo)) {
					submissionDueDate = getReferenceDate(numOfDays, relativeTo, assignedTask);
				}
			} else if(dueDate != null) {
				submissionDueDate = new DueDate(dueDate);
			}
		}
		return submissionDueDate;
	}
	
	protected Task stepReviewAndCorrection(@SuppressWarnings("unused")UserRequest ureq, Task assignedTask) {
		return assignedTask;
	}
	
	protected Task stepRevision(@SuppressWarnings("unused")UserRequest ureq, Task assignedTask) {
		return assignedTask;
	}
	
	protected Task stepSolution(@SuppressWarnings("unused")UserRequest ureq, Task assignedTask) {
		DueDate availableDate = getSolutionDueDate(assignedTask);
		if(availableDate != null) {
			if(availableDate.getDueDate() != null) {
				String date = formatDueDate(availableDate, false);
				mainVC.contextPut("solutionAvailableDate", date);
				mainVC.contextRemove("solutionAvailableDateMsg");
			} else if(availableDate.getMessage() != null) {
				mainVC.contextPut("solutionAvailableDateMsg", availableDate.getMessage());
				mainVC.contextRemove("solutionAvailableDate");
			}
		}
		return assignedTask;
	}
	
	protected DueDate getSolutionDueDate(Task assignedTask) {
		if(solutionDueDate == null) {
			boolean relativeDate = gtaNode.getModuleConfiguration().getBooleanSafe(GTACourseNode.GTASK_RELATIVE_DATES);
			Date dueDate = gtaNode.getModuleConfiguration().getDateValue(GTACourseNode.GTASK_SAMPLE_SOLUTION_VISIBLE_AFTER);
			if(relativeDate) {
				int numOfDays = gtaNode.getModuleConfiguration().getIntegerSafe(GTACourseNode.GTASK_SAMPLE_SOLUTION_VISIBLE_AFTER_RELATIVE, -1);
				String relativeTo = gtaNode.getModuleConfiguration().getStringValue(GTACourseNode.GTASK_SAMPLE_SOLUTION_VISIBLE_AFTER_RELATIVE_TO);
				if(numOfDays >= 0 && StringHelper.containsNonWhitespace(relativeTo)) {
					solutionDueDate = getReferenceDate(numOfDays, relativeTo, assignedTask);
				}
			} else if(dueDate != null) {
				solutionDueDate = new DueDate(dueDate);
			}
		}
		return solutionDueDate;
	}
	
	protected Task stepGrading(@SuppressWarnings("unused") UserRequest ureq, Task assignedTask) {
		return assignedTask;
	}
	
	protected void nodeLog() {
		if(businessGroupTask) {
			String groupLog = courseEnv.getAuditManager().getUserNodeLog(gtaNode, assessedGroup);
			if(StringHelper.containsNonWhitespace(groupLog)) {
				mainVC.contextPut("groupLog", groupLog);
			} else {
				mainVC.contextRemove("groupLog");
			}
		} else {
			String userLog = courseEnv.getAuditManager().getUserNodeLog(gtaNode, assessedIdentity);
			if(StringHelper.containsNonWhitespace(userLog)) {
				mainVC.contextPut("userLog", userLog);
			} else {
				mainVC.contextRemove("userLog");
			}
		}
	}
	
	protected void doUpdateAttempts() {
		if(businessGroupTask) {
			List<Identity> identities = businessGroupService.getMembers(assessedGroup, GroupRoles.participant.name());
			ICourse course = CourseFactory.loadCourse(courseEnv.getCourseGroupManager().getCourseEntry());
			for(Identity identity:identities) {
				UserCourseEnvironment uce = AssessmentHelper.createAndInitUserCourseEnvironment(identity, course);
				gtaNode.incrementUserAttempts(uce);
			}
		} else {
			UserCourseEnvironment assessedUserCourseEnv = getAssessedUserCourseEnvironment();
			gtaNode.incrementUserAttempts(assessedUserCourseEnv);
		}
	}
	
	protected UserCourseEnvironment getAssessedUserCourseEnvironment() {
		if(userCourseEnv == null) {
			ICourse course = CourseFactory.loadCourse(courseEnv.getCourseResourceableId());
			userCourseEnv = AssessmentHelper.createAndInitUserCourseEnvironment(assessedIdentity, course);
		}
		return userCourseEnv;
	}
	
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if("show".equals(event.getCommand())) {
			doShow(ureq);
		} else if("hide".equals(event.getCommand())) {
			doHide(ureq);
		}
	}
	
	private void doShow(UserRequest ureq) {
		String step = ureq.getParameter("step");
		doSaveStepPreferences(ureq, step, Boolean.TRUE);
	}
	
	private void doHide(UserRequest ureq) {
		String step = ureq.getParameter("step");
		doSaveStepPreferences(ureq, step, Boolean.FALSE);
	}

	private void doSaveStepPreferences(UserRequest ureq, String step, Boolean showHide) {
		if(step == null) return;
		switch(step) {
			case "assignment": stepPreferences.setAssignement(showHide); break;
			case "submit": stepPreferences.setSubmit(showHide); break;
			case "reviewAndCorrection": stepPreferences.setReviewAndCorrection(showHide); break;
			case "revision": stepPreferences.setRevision(showHide); break;
			case "solution": stepPreferences.setSolution(showHide); break;
			case "grading": stepPreferences.setGrading(showHide); break;
			default: {};
		}
		
		ureq.getUserSession().getGuiPreferences()
			.putAndSave(GTAStepPreferences.class, taskList.getKey().toString(), stepPreferences);
	}
	
	public static class DueDate {
		
		private final Date dueDate;
		private final String message;
		
		public DueDate(String message) {
			this(null, message);
		}
		
		public DueDate(Date dueDate) {
			this(dueDate, null);
		}
		
		public DueDate(Date dueDate, String message) {
			this.dueDate = dueDate;
			this.message = message;
		}

		public Date getDueDate() {
			return dueDate;
		}

		public String getMessage() {
			return message;
		}
	}
}