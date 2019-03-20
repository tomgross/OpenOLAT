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
package org.olat.modules.portfolio;

import org.olat.NewControllerFactory;
import org.olat.core.configuration.AbstractSpringModule;
import org.olat.core.configuration.ConfigOnOff;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.modules.portfolio.handler.BinderTemplateHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 06.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service("portfolioV2Module")
public class PortfolioV2Module extends AbstractSpringModule implements ConfigOnOff {
	
	public static final String ENTRY_POINT_TOC = "toc";
	public static final String ENTRY_POINT_ENTRIES = "entries";

	public static final String PORTFOLIO_ENABLED = "portfoliov2.enabled";
	public static final String PORTFOLIO_LEARNER_CAN_CREATE_BINDERS = "portfoliov2.learner.can.create.binders";
	public static final String PORTFOLIO_CAN_CREATE_BINDERS_FROM_TEMPLATE = "portfoliov2.can.create.binders.from.template";
	public static final String PORTFOLIO_CAN_CREATE_BINDERS_FROM_COURSE = "portfoliov2.can.create.binders.from.course";
	public static final String PORTFOLIO_BINDER_ENTRY_POINT = "portfoliov2.binder.entry.point";
	
	@Value("${portfoliov2.enabled:true}")
	private boolean enabled;
	@Value("${portfoliov2.learner.can.create.binders:true}")
	private boolean learnerCanCreateBinders;
	@Value("${portfoliov2.can.create.binders.from.template:true}")
	private boolean canCreateBindersFromTemplate;
	@Value("${portfoliov2.can.create.binders.from.course:true}")
	private boolean canCreateBindersFromCourse;
	@Value("${portfoliov2.binder.entry.point:toc}")
	private String binderEntryPoint;
	
	@Autowired
	public PortfolioV2Module(CoordinatorManager coordinatorManager) {
		super(coordinatorManager);
	}

	@Override
	public void init() {
		String enabledObj = getStringPropertyValue(PORTFOLIO_ENABLED, true);
		if(StringHelper.containsNonWhitespace(enabledObj)) {
			enabled = "true".equals(enabledObj);
		}
		
		String learnerCanCreateBindersObj = getStringPropertyValue(PORTFOLIO_LEARNER_CAN_CREATE_BINDERS, true);
		if(StringHelper.containsNonWhitespace(learnerCanCreateBindersObj)) {
			learnerCanCreateBinders = "true".equals(learnerCanCreateBindersObj);
		}
		
		RepositoryHandlerFactory.registerHandler(new BinderTemplateHandler(), 40);
		NewControllerFactory.getInstance().addContextEntryControllerCreator("BinderInvitation",
				new BinderInvitationContextEntryControllerCreator());	
		NewControllerFactory.getInstance().addContextEntryControllerCreator("Binder",
				new BinderContextEntryControllerCreator());
		NewControllerFactory.getInstance().addContextEntryControllerCreator("PortfolioV2",
				new BinderContextEntryControllerCreator());	
	}

	@Override
	protected void initFromChangedProperties() {
		init();
	}
	
	@Override
	public boolean isEnabled() {
		return false;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		setStringProperty(PORTFOLIO_ENABLED, Boolean.toString(enabled), true);
	}

	public boolean isLearnerCanCreateBinders() {
		return learnerCanCreateBinders;
	}

	public void setLearnerCanCreateBinders(boolean learnerCanCreateBinders) {
		this.learnerCanCreateBinders = learnerCanCreateBinders;
		setStringProperty(PORTFOLIO_LEARNER_CAN_CREATE_BINDERS, Boolean.toString(learnerCanCreateBinders), true);
	}
	
	public boolean isCanCreateBindersFromTemplate() {
		return canCreateBindersFromTemplate;
	}

	public void setCanCreateBindersFromTemplate(boolean canCreateBindersFromTemplate) {
		this.canCreateBindersFromTemplate = canCreateBindersFromTemplate;
		setStringProperty(PORTFOLIO_CAN_CREATE_BINDERS_FROM_TEMPLATE, Boolean.toString(canCreateBindersFromTemplate), true);
	}

	public boolean isCanCreateBindersFromCourse() {
		return canCreateBindersFromCourse;
	}

	public void setCanCreateBindersFromCourse(boolean canCreateBindersFromCourse) {
		this.canCreateBindersFromCourse = canCreateBindersFromCourse;
		setStringProperty(PORTFOLIO_CAN_CREATE_BINDERS_FROM_COURSE, Boolean.toString(canCreateBindersFromCourse), true);
	}

	public String getBinderEntryPoint() {
		return binderEntryPoint;
	}

	public void setBinderEntryPoint(String binderEntryPoint) {
		this.binderEntryPoint = binderEntryPoint;
		setStringProperty(PORTFOLIO_BINDER_ENTRY_POINT, binderEntryPoint, true);
	}
}
