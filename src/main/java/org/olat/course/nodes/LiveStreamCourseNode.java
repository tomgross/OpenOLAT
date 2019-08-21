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
package org.olat.course.nodes;

import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.BreadcrumbPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.course.ICourse;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.cal.CourseCalendars;
import org.olat.course.nodes.livestream.LiveStreamModule;
import org.olat.course.nodes.livestream.LiveStreamSecurityCallback;
import org.olat.course.nodes.livestream.LiveStreamSecurityCallbackFactory;
import org.olat.course.nodes.livestream.ui.LiveStreamEditController;
import org.olat.course.nodes.livestream.ui.LiveStreamPeekviewController;
import org.olat.course.nodes.livestream.ui.LiveStreamRunController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.repository.RepositoryEntry;
import org.olat.resource.OLATResource;

/**
 * 
 * Initial date: 22 May 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class LiveStreamCourseNode extends AbstractAccessableCourseNode {

	private static final long serialVersionUID = 1948699450468358698L;
	
	private static final String TYPE = "livestream";
	
	public static final int CURRENT_VERSION = 1;
	public static final String CONFIG_BUFFER_BEFORE_MIN = "bufferBeforeMin";
	public static final String CONFIG_BUFFER_AFTER_MIN = "bufferBeforeAfter";
	public static final String CONFIG_COACH_CAN_EDIT = "coachCanEdit";

	public LiveStreamCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	@Override
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl, BreadcrumbPanel stackPanel,
			ICourse course, UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		LiveStreamEditController editCtrl = new LiveStreamEditController(ureq, wControl, this, course, euce);
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, euce, editCtrl);
	}

	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {
		updateModuleConfigDefaults(false);
		
		Controller runCtrl;
		if (userCourseEnv.isCourseReadOnly()) {
			Translator trans = Util.createPackageTranslator(Card2BrainCourseNode.class, ureq.getLocale());
			String title = trans.translate("freezenoaccess.title");
			String message = trans.translate("freezenoaccess.message");
			runCtrl = MessageUIFactory.createInfoMessage(ureq, wControl, title, message);
		} else {
			CourseCalendars calendars = CourseCalendars.createCourseCalendarsWrapper(ureq, wControl, userCourseEnv, ne);
			LiveStreamSecurityCallback secCallback = LiveStreamSecurityCallbackFactory
					.createSecurityCallback(userCourseEnv, this.getModuleConfiguration());
			OLATResource courseOres = userCourseEnv.getCourseEnvironment().getCourseGroupManager().getCourseResource();
			runCtrl = new LiveStreamRunController(ureq, wControl, this.getModuleConfiguration(), courseOres, secCallback, calendars);
		}
		Controller ctrl = TitledWrapperHelper.getWrapper(ureq, wControl, runCtrl, this, "o_livestream_icon");
		return new NodeRunConstructionResult(ctrl);
	}
	
	@Override
	public Controller createPeekViewRunController(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment userCourseEnv, NodeEvaluation ne) {
		CourseCalendars calendars = CourseCalendars.createCourseCalendarsWrapper(ureq, wControl, userCourseEnv, ne);
		return new LiveStreamPeekviewController(ureq, wControl, getIdent(), this.getModuleConfiguration(), calendars);
	}

	@SuppressWarnings("deprecation")
	@Override
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		String translatorStr = Util.getPackageName(ConditionEditController.class);
		List<StatusDescription> statusDescs = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		return StatusDescriptionHelper.sort(statusDescs);
	}
	
	@Override
	public StatusDescription isConfigValid() {
		return  StatusDescription.NOERROR;
	}
	
	@Override
	public void updateModuleConfigDefaults(boolean isNewNode) {
		ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			LiveStreamModule liveStreamModule = CoreSpringFactory.getImpl(LiveStreamModule.class);
			config.setIntValue(CONFIG_BUFFER_BEFORE_MIN, liveStreamModule.getBufferBeforeMin());
			config.setIntValue(CONFIG_BUFFER_AFTER_MIN, liveStreamModule.getBufferAfterMin());
			config.setBooleanEntry(CONFIG_COACH_CAN_EDIT, liveStreamModule.isEditCoach());
		}
		config.setConfigurationVersion(CURRENT_VERSION);
	}
	
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		return null;
	}

	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return false;
	}

}
