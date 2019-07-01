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
package org.olat.ims.qti21.ui;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.olat.core.commons.persistence.DB;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.EscapeMode;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.BooleanCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.TextFlexiCellRenderer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.nodes.IQTESTCourseNode;
import org.olat.course.nodes.iq.IQEditController;
import org.olat.course.nodes.iq.QTI21AssessmentRunController;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.fileresource.FileResourceManager;
import org.olat.ims.qti21.AssessmentSessionAuditLogger;
import org.olat.ims.qti21.AssessmentTestSession;
import org.olat.ims.qti21.QTI21AssessmentResultsOptions;
import org.olat.ims.qti21.QTI21DeliveryOptions;
import org.olat.ims.qti21.QTI21Module;
import org.olat.ims.qti21.QTI21Service;
import org.olat.ims.qti21.model.DigitalSignatureOptions;
import org.olat.ims.qti21.model.jpa.AssessmentTestSessionStatistics;
import org.olat.ims.qti21.ui.QTI21AssessmentTestSessionTableModel.TSCols;
import org.olat.ims.qti21.ui.assessment.IdentityAssessmentTestCorrectionController;
import org.olat.ims.qti21.ui.event.RetrieveAssessmentTestSessionEvent;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.AssessmentService;
import org.olat.modules.assessment.AssessmentToolOptions;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.model.RepositoryEntrySecurity;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ed.ph.jqtiplus.state.ItemSessionState;
import uk.ac.ed.ph.jqtiplus.state.TestPlan;
import uk.ac.ed.ph.jqtiplus.state.TestPlanNode;
import uk.ac.ed.ph.jqtiplus.state.TestPlanNode.TestNodeType;
import uk.ac.ed.ph.jqtiplus.state.TestPlanNodeKey;
import uk.ac.ed.ph.jqtiplus.state.TestSessionState;

/**
 * This controller is used by the assessment tools of the course and
 * of the test resource. The assessment tool of the resource doesn't
 * provide any user course environment or course node. Be aware!
 * 
 * 
 * Initial date: 28.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QTI21AssessmentDetailsController extends FormBasicController {

	private Component resetToolCmp;
	private FlexiTableElement tableEl;
	private QTI21AssessmentTestSessionTableModel tableModel;
	
	private RepositoryEntry entry;
	private RepositoryEntry testEntry;
	private final String subIdent;
	private final boolean manualCorrections;
	private final Identity assessedIdentity;
	
	private final boolean readOnly;
	private final IQTESTCourseNode courseNode;
	private final RepositoryEntrySecurity reSecurity;
	private final UserCourseEnvironment assessedUserCourseEnv;
	
	private CloseableModalController cmc;
	private AssessmentResultController resultCtrl;
	private QTI21ResetToolController resetToolCtrl;
	private DialogBoxController retrieveConfirmationCtr;
	private IdentityAssessmentTestCorrectionController correctionCtrl;
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private UserManager userManager;
	@Autowired
	private QTI21Module qtiModule;
	@Autowired
	protected QTI21Service qtiService;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private AssessmentService assessmentService;
	
	/**
	 * The constructor used by the assessment tool of the course.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param assessableEntry
	 * @param courseNode
	 * @param coachCourseEnv
	 * @param assessedUserCourseEnv
	 */
	public QTI21AssessmentDetailsController(UserRequest ureq, WindowControl wControl,
			RepositoryEntry assessableEntry, IQTESTCourseNode courseNode,
			UserCourseEnvironment coachCourseEnv, UserCourseEnvironment assessedUserCourseEnv) {
		super(ureq, wControl, "assessment_details");
		entry = assessableEntry;
		this.courseNode = courseNode;
		subIdent = courseNode.getIdent();
		readOnly = coachCourseEnv.isCourseReadOnly();
		this.assessedUserCourseEnv = assessedUserCourseEnv;
		testEntry = courseNode.getReferencedRepositoryEntry();
		assessedIdentity = assessedUserCourseEnv.getIdentityEnvironment().getIdentity();
		manualCorrections = qtiService.needManualCorrection(testEntry)
				|| IQEditController.CORRECTION_MANUAL.equals(courseNode.getModuleConfiguration().getStringValue(IQEditController.CONFIG_CORRECTION_MODE));
		
		RepositoryEntry courseEntry = assessedUserCourseEnv.getCourseEnvironment().getCourseGroupManager().getCourseEntry();
		reSecurity = repositoryManager.isAllowed(ureq, courseEntry);

		initForm(ureq);
		updateModel();
	}
	
	/**
	 * The constructor used by the assessment tool of the test resource itself.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param assessableEntry
	 * @param assessedIdentity
	 */
	public QTI21AssessmentDetailsController(UserRequest ureq, WindowControl wControl,
			RepositoryEntry assessableEntry, Identity assessedIdentity) {
		super(ureq, wControl, "assessment_details");
		entry = assessableEntry;
		testEntry = assessableEntry;
		subIdent = null;
		readOnly = false;
		courseNode = null;
		assessedUserCourseEnv = null;
		this.assessedIdentity = assessedIdentity;
		manualCorrections = qtiService.needManualCorrection(assessableEntry);
		reSecurity = repositoryManager.isAllowed(ureq, assessableEntry);

		initForm(ureq);
		updateModel();
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.terminationTime));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.lastModified));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.duration, new TextFlexiCellRenderer(EscapeMode.none)));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.numOfItemSessions, new TextFlexiCellRenderer(EscapeMode.none)));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.responded, new TextFlexiCellRenderer(EscapeMode.none)));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.corrected, new TextFlexiCellRenderer(EscapeMode.none)));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.score, new TextFlexiCellRenderer(EscapeMode.none)));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.manualScore, new TextFlexiCellRenderer(EscapeMode.none)));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.finalScore, new TextFlexiCellRenderer(EscapeMode.none)));
		
		if(readOnly) {
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel("select", translate("select"), "open"));
		} else {
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.open.i18nHeaderKey(), TSCols.open.ordinal(), "open",
					new BooleanCellRenderer(new StaticFlexiCellRenderer(translate("select"), "open"),
							new StaticFlexiCellRenderer(translate("pull"), "open"))));
		}
		if(manualCorrections && !readOnly) {
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(TSCols.correction.i18nHeaderKey(), TSCols.correction.ordinal(), "correction",
					new BooleanCellRenderer(new StaticFlexiCellRenderer(translate("correction"), "correction"), null)));
		}
	

		tableModel = new QTI21AssessmentTestSessionTableModel(columnsModel, getTranslator());
		tableEl = uifactory.addTableElement(getWindowControl(), "sessions", tableModel, 20, false, getTranslator(), formLayout);
		tableEl.setEmtpyTableMessageKey("results.empty");

		if(reSecurity.isEntryAdmin() && !readOnly) {
			AssessmentToolOptions asOptions = new AssessmentToolOptions();
			asOptions.setAdmin(reSecurity.isEntryAdmin());
			asOptions.setIdentities(Collections.singletonList(assessedIdentity));
			if(courseNode != null) {
				resetToolCtrl = new QTI21ResetToolController(ureq, getWindowControl(),
						assessedUserCourseEnv.getCourseEnvironment(), asOptions, courseNode);
			} else {
				resetToolCtrl = new QTI21ResetToolController(ureq, getWindowControl(), entry, asOptions);
			}
			listenTo(resetToolCtrl);
			resetToolCmp = resetToolCtrl.getInitialComponent();	
		}
	} 

	@Override
	protected void doDispose() {
		//
	}
	
	protected void updateModel() {
		List<AssessmentTestSessionStatistics> sessionsStatistics = qtiService.getAssessmentTestSessionsStatistics(entry, subIdent, assessedIdentity);
		List<QTI21AssessmentTestSessionDetails> infos = new ArrayList<>();
		for(AssessmentTestSessionStatistics sessionStatistics:sessionsStatistics) {
			AssessmentTestSession testSession = sessionStatistics.getTestSession();
			TestSessionState testSessionState = qtiService.loadTestSessionState(testSession);
			TestPlan testPlan = testSessionState.getTestPlan();
			List<TestPlanNode> nodes = testPlan.getTestPlanNodeList();
			
			int responded = 0;
			int numOfItems = 0;
			for(TestPlanNode node:nodes) {
				TestNodeType testNodeType = node.getTestNodeType();
				ItemSessionState itemSessionState = testSessionState.getItemSessionStates().get(node.getKey());

				TestPlanNodeKey testPlanNodeKey = node.getKey();
				if(testPlanNodeKey != null && testPlanNodeKey.getIdentifier() != null
						&& testNodeType == TestNodeType.ASSESSMENT_ITEM_REF) {
					numOfItems++;
					if(itemSessionState.isResponded()) {
						responded++;
					}
				}
			}

			infos.add(new QTI21AssessmentTestSessionDetails(testSession,
					numOfItems, responded, sessionStatistics.getNumOfCorrectedItems()));
		}
		
		
		Collections.sort(infos, new AssessmentTestSessionComparator());
		tableModel.setObjects(infos);
		tableEl.reloadData();
		tableEl.reset();
			
		if(resetToolCmp != null) {
			if(sessionsStatistics.size() > 0) {
				flc.getFormItemComponent().put("reset.tool", resetToolCmp);
			} else {
				flc.getFormItemComponent().remove(resetToolCmp);
			}
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(cmc == source) {
			cleanUp();
		} else if(correctionCtrl == source) {
			if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				if(courseNode != null) {
					doUpdateCourseNode(correctionCtrl.getAssessmentTestSession());
				} else {
					doUpdateEntry(correctionCtrl.getAssessmentTestSession());
				}
				updateModel();
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
			cmc.deactivate();
			cleanUp();
		} else if(retrieveConfirmationCtr == source) {
			if(DialogBoxUIFactory.isYesEvent(event) || DialogBoxUIFactory.isOkEvent(event)) {
				doPullSession((AssessmentTestSession)retrieveConfirmationCtr.getUserObject());
				updateModel();
			}
		} else if(resetToolCtrl == source) {
			if(event == Event.DONE_EVENT) {
				updateModel();
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(correctionCtrl);
		removeAsListenerAndDispose(resultCtrl);
		removeAsListenerAndDispose(cmc);
		correctionCtrl = null;
		resultCtrl = null;
		cmc = null;
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(tableEl == source) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				String cmd = se.getCommand();
				QTI21AssessmentTestSessionDetails row = tableModel.getObject(se.getIndex());
				AssessmentTestSession testSession = qtiService.getAssessmentTestSession(row.getTestSession().getKey());
				if("open".equals(cmd)) {
					if(testSession.getFinishTime() == null) {
						doConfirmPullSession(ureq, testSession);
					} else {
						doOpenResult(ureq, testSession);
					}
				} else if("correction".equals(cmd)) {
					doCorrection(ureq, testSession);
				}
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	private void doCorrection(UserRequest ureq, AssessmentTestSession session) {
		correctionCtrl = new IdentityAssessmentTestCorrectionController(ureq, getWindowControl(), session);
		listenTo(correctionCtrl);
		cmc = new CloseableModalController(getWindowControl(), "close", correctionCtrl.getInitialComponent(),
				true, translate("correction"));
		cmc.activate();
		listenTo(cmc);
	}
	
	private void doUpdateCourseNode(AssessmentTestSession session) {
		ScoreEvaluation scoreEval = courseNode.getUserScoreEvaluation(assessedUserCourseEnv);
		BigDecimal finalScore = calculateFinalScore(session);
		Float score = finalScore == null ? null : finalScore.floatValue();
		ScoreEvaluation manualScoreEval = new ScoreEvaluation(score, scoreEval.getPassed(),
				scoreEval.getAssessmentStatus(), null, scoreEval.getFullyAssessed(), session.getKey());
		courseNode.updateUserScoreEvaluation(manualScoreEval, assessedUserCourseEnv, getIdentity(), false);
	}
	
	private void doUpdateEntry(AssessmentTestSession session) {
		AssessmentEntry assessmentEntry = assessmentService.loadAssessmentEntry(assessedIdentity, entry, null, entry);
		BigDecimal finalScore = calculateFinalScore(session);
		assessmentEntry.setScore(finalScore);
		assessmentEntry.setAssessmentId(session.getKey());
		assessmentService.updateAssessmentEntry(assessmentEntry);
	}
	
	private BigDecimal calculateFinalScore(AssessmentTestSession session) {
		BigDecimal finalScore = session.getScore();
		if(finalScore == null) {
			finalScore = session.getManualScore();
		} else if(session.getManualScore() != null) {
			finalScore = finalScore.add(session.getManualScore());
		}
		return finalScore;
	}

	private void doConfirmPullSession(UserRequest ureq, AssessmentTestSession session) {
		String title = translate("pull");
		String fullname = userManager.getUserDisplayName(session.getIdentity());
		String text = translate("retrievetest.confirm.text", new String[]{ fullname });
		retrieveConfirmationCtr = activateOkCancelDialog(ureq, title, text, retrieveConfirmationCtr);
		retrieveConfirmationCtr.setUserObject(session);
	}
	
	private void doPullSession(AssessmentTestSession session) {
		session = qtiService.getAssessmentTestSession(session.getKey());
		
		if(session.getFinishTime() == null) {
			if(qtiModule.isDigitalSignatureEnabled()) {
				qtiService.signAssessmentResult(session, getSignatureOptions(session), session.getIdentity());
			}
			session.setFinishTime(new Date());
		}
		session.setTerminationTime(new Date());
		session = qtiService.updateAssessmentTestSession(session);
		dbInstance.commit();//make sure that the changes committed before sending the event
		
		AssessmentSessionAuditLogger candidateAuditLogger = qtiService.getAssessmentSessionAuditLogger(session, false);
		candidateAuditLogger.logTestRetrieved(session, getIdentity());
		
		OLATResourceable sessionOres = OresHelper.createOLATResourceableInstance(AssessmentTestSession.class, session.getKey());
		CoordinatorManager.getInstance().getCoordinator().getEventBus()
			.fireEventToListenersOf(new RetrieveAssessmentTestSessionEvent(session.getKey()), sessionOres);
	}
	
	private DigitalSignatureOptions getSignatureOptions(AssessmentTestSession session) {
		RepositoryEntry sessionTestEntry = session.getTestEntry();
		QTI21DeliveryOptions deliveryOptions = qtiService.getDeliveryOptions(sessionTestEntry);
		
		boolean digitalSignature = deliveryOptions.isDigitalSignature();
		boolean sendMail = deliveryOptions.isDigitalSignatureMail();
		if(courseNode != null) {
			ModuleConfiguration config = courseNode.getModuleConfiguration();
			digitalSignature = config.getBooleanSafe(IQEditController.CONFIG_DIGITAL_SIGNATURE,
					deliveryOptions.isDigitalSignature());
			sendMail = config.getBooleanSafe(IQEditController.CONFIG_DIGITAL_SIGNATURE_SEND_MAIL,
					deliveryOptions.isDigitalSignatureMail());
		}
		
		DigitalSignatureOptions options = new DigitalSignatureOptions(digitalSignature, sendMail, entry, testEntry);
		if(digitalSignature) {
			if(courseNode == null) {
				 AssessmentEntryOutcomesListener.decorateResourceConfirmation(session, options, null, getLocale());
			} else {
				CourseEnvironment courseEnv = CourseFactory.loadCourse(entry).getCourseEnvironment();
				QTI21AssessmentRunController.decorateCourseConfirmation(session, options, courseEnv, courseNode, sessionTestEntry, null, getLocale());
			}
		}
		return options;
	}

	private void doOpenResult(UserRequest ureq, AssessmentTestSession session) {
		if(resultCtrl != null) return;

		FileResourceManager frm = FileResourceManager.getInstance();
		File fUnzippedDirRoot = frm.unzipFileResource(session.getTestEntry().getOlatResource());
		URI assessmentObjectUri = qtiService.createAssessmentTestUri(fUnzippedDirRoot);
		File submissionDir = qtiService.getSubmissionDirectory(session);
		String mapperUri = registerCacheableMapper(ureq, "QTI21DetailsResources::" + session.getKey(),
				new ResourcesMapper(assessmentObjectUri, submissionDir));
		
		resultCtrl = new AssessmentResultController(ureq, getWindowControl(), assessedIdentity, false, session,
				fUnzippedDirRoot, mapperUri, null, QTI21AssessmentResultsOptions.allOptions(), true, true);
		listenTo(resultCtrl);
		cmc = new CloseableModalController(getWindowControl(), "close", resultCtrl.getInitialComponent(),
				true, translate("table.header.results"));
		cmc.activate();
		listenTo(cmc);
	}
	
	public static class AssessmentTestSessionComparator implements Comparator<QTI21AssessmentTestSessionDetails> {

		@Override
		public int compare(QTI21AssessmentTestSessionDetails q1, QTI21AssessmentTestSessionDetails q2) {
			AssessmentTestSession a1 = q1.getTestSession();
			AssessmentTestSession a2 = q2.getTestSession();
			
			Date t1 = a1.getTerminationTime();
			if(t1 == null) {
				t1 = a1.getFinishTime();
			}
			Date t2 = a2.getTerminationTime();
			if(t2 == null) {
				t2 = a2.getFinishTime();
			}
			
			int c;
			if(t1 == null && t2 == null) {
				c = 0;
			} else if(t2 == null) {
				return 1;
			} else if(t1 == null) {
				return -1;
			} else {
				c = t1.compareTo(t2);
			}
			
			if(c == 0) {
				Date c1 = a1.getCreationDate();
				Date c2 = a2.getCreationDate();
				if(c1 == null && c2 == null) {
					c = 0;
				} else if(c2 == null) {
					return -1;
				} else if(c1 == null) {
					return 1;
				} else {
					c = c1.compareTo(c2);
				}
			}
			return -c;
		}
	}
}