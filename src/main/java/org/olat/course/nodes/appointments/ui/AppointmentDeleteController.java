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
package org.olat.course.nodes.appointments.ui;

import static org.olat.core.gui.translator.TranslatorHelper.translateAll;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.course.nodes.appointments.Appointment;

/**
 * 
 * Initial date: 18 May 2020<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class AppointmentDeleteController extends AbstractRebookController {
	
	private static final String DELETE = "appointment.delete.no";
	private static final String CHANGE = "appointment.delete.yes";
	private static final String[] PARTICIPATIONS = { DELETE, CHANGE };
	
	private SingleSelection changeEl;

	public AppointmentDeleteController(UserRequest ureq, WindowControl wControl, Appointment appointment,
			Configuration config) {
		super(ureq, wControl, appointment, config);
	}
	
	@Override
	boolean isShowParticipations() {
		return getNumParticipations() > 0;
	}

	@Override
	boolean isParticipationsReadOnly() {
		return true;
	}

	@Override
	boolean isAllParticipationsSelected() {
		return true;
	}

	@Override
	boolean isShowAppointments() {
		return changeEl != null && changeEl.isOneSelected() && CHANGE.equals(changeEl.getSelectedKey());
	}

	@Override
	String getSubmitI18nKey() {
		return "delete";
	}
	
	@Override
	protected void initFormTop(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if (getNumParticipations() > 0) {
			setFormDescription("appointment.delete.participations", new String[] { String.valueOf(getNumParticipations()) } );
		
			changeEl = uifactory.addRadiosHorizontal("appointment.delete.rebook", formLayout, PARTICIPATIONS,
					translateAll(getTranslator(), PARTICIPATIONS));
			changeEl.addActionListener(FormEvent.ONCHANGE);
			changeEl.select(DELETE, true);
		} else {
			setFormDescription("confirm.appointment.delete");
		}
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if (source == changeEl) {
			super.updateUI();
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	void onAfterRebooking() {
		getAppointmentsService().deleteAppointment(getCurrentAppointment());
	}
	
}
