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

import static java.util.Collections.emptyList;
import static org.olat.core.gui.components.util.KeyValues.entry;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.util.KeyValues;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.course.nodes.appointments.Appointment;
import org.olat.course.nodes.appointments.AppointmentRef;
import org.olat.course.nodes.appointments.AppointmentSearchParams;
import org.olat.course.nodes.appointments.AppointmentsService;
import org.olat.course.nodes.appointments.Participation;
import org.olat.course.nodes.appointments.ParticipationRef;
import org.olat.course.nodes.appointments.ParticipationSearchParams;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 18 May 2020<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public abstract class AbstractRebookController extends FormBasicController {
	
	private static final String[] EMPTY = new String[0];

	private MultipleSelectionElement participationsEl;
	private SingleSelection appointmentsEl;
	private StaticTextElement noAppointmentsEl;
	
	private final DateFormat dateFormat;
	private final Appointment currentAppointment;
	private final List<Participation> participations;
	private final Configuration config;
	private String selectedAppointmentKey;
	
	@Autowired
	private AppointmentsService appointmentsService;
	@Autowired
	private UserManager userManager;

	public AbstractRebookController(UserRequest ureq, WindowControl wControl, Appointment appointment,
			Configuration config) {
		super(ureq, wControl);
		this.currentAppointment = appointment;
		this.config = config;
		
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale());
		
		ParticipationSearchParams params = new ParticipationSearchParams();
		params.setAppointments(Collections.singletonList(appointment));
		participations = appointmentsService.getParticipations(params);
		
		initForm(ureq);
	}
	
	abstract boolean isShowParticipations();
	
	abstract boolean isParticipationsReadOnly();
	
	abstract boolean isAllParticipationsSelected();
	
	abstract boolean isShowAppointments();
	
	abstract String getSubmitI18nKey();
	
	abstract void initFormTop(FormItemContainer formLayout, Controller listener, UserRequest ureq);
	
	abstract void onAfterRebooking();
	
	AppointmentsService getAppointmentsService() {
		return appointmentsService;
	}
	
	Appointment getCurrentAppointment() {
		return currentAppointment;
	}

	int getNumParticipations() {
		return participations.size();
	}
	
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		initFormTop(formLayout, listener, ureq);
		
		participationsEl = uifactory.addCheckboxesVertical("rebook.participation", formLayout,  EMPTY, EMPTY, 2);
		participationsEl.addActionListener(FormEvent.ONCHANGE);
				
		appointmentsEl = uifactory.addRadiosVertical("rebook.appointments", "rebook.appointments", formLayout, EMPTY, EMPTY);
		
		noAppointmentsEl = uifactory.addStaticTextElement("rebook.no.appointments",
				translate("rebook.no.appointments.text"), formLayout);
		
		FormLayoutContainer buttonCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		buttonCont.setRootForm(mainForm);
		formLayout.add(buttonCont);
		uifactory.addFormSubmitButton(getSubmitI18nKey(), buttonCont);
		uifactory.addFormCancelButton("cancel", buttonCont, ureq, getWindowControl());
		
		updateUI();
	}
	
	void updateUI(){
		if (participations.isEmpty()) {
			participationsEl.setVisible(false);
			appointmentsEl.setVisible(false);
			noAppointmentsEl.setVisible(false);
			return;
		}
		
		if (isShowParticipations()) {
			KeyValues participationsKV = new KeyValues();
			
			ParticipationSearchParams params = new ParticipationSearchParams();
			params.setAppointments(Collections.singletonList(currentAppointment));
			appointmentsService.getParticipations(params)
					.forEach(participation -> participationsKV.add(entry(
							participation.getKey().toString(),
							userManager.getUserDisplayName(participation.getIdentity().getKey()))));
			
			participationsKV.sort(KeyValues.VALUE_ASC);
			participationsEl.setKeysAndValues(participationsKV.keys(), participationsKV.values());
			if (isAllParticipationsSelected()) {
				participationsEl.selectAll();
			}
			
			participationsEl.setVisible(true);
		} else {
			participationsEl.setVisible(false);
		}
		participationsEl.setEnabled(!isParticipationsReadOnly());
		
		updateAppointmentsUI();
	}

	private void updateAppointmentsUI() {
		if (isShowAppointments()) {
			selectedAppointmentKey = appointmentsEl.isOneSelected()
					? appointmentsEl.getSelectedKey()
					: selectedAppointmentKey;
			int numParticipants = participationsEl.isAtLeastSelected(1)
					? participationsEl.getSelectedKeys().size()
					: 1;
			
			ParticipationSearchParams pParams = new ParticipationSearchParams();
			pParams.setTopic(currentAppointment.getTopic());
			List<Participation> participations = appointmentsService.getParticipations(pParams);
			Map<Long, List<Participation>> appointmentKeyToParticipations = participations.stream()
					.collect(Collectors.groupingBy(p -> p.getAppointment().getKey()));
			
			KeyValues freeKV = new KeyValues();
			AppointmentSearchParams aParams = new AppointmentSearchParams();
			aParams.setTopic(currentAppointment.getTopic());
			appointmentsService.getAppointments(aParams).stream()
					.filter(appointment -> hasFreeParticipations(appointment, appointmentKeyToParticipations, numParticipants))
					.sorted((a1, a2) -> a1.getStart().compareTo(a2.getStart()))
					.forEach(appointment -> freeKV.add(KeyValues.entry(appointment.getKey().toString(), formatDate(appointment))));
			
			if (freeKV.size() > 0) {
				appointmentsEl.setKeysAndValues(freeKV.keys(), freeKV.values(), null);
				if (Arrays.asList(appointmentsEl.getKeys()).contains(selectedAppointmentKey)) {
					appointmentsEl.select(selectedAppointmentKey, true);
				}
				
				appointmentsEl.setVisible(true);
				noAppointmentsEl.setVisible(false);
			} else {
				appointmentsEl.setVisible(false);
				noAppointmentsEl.setVisible(true);
			}
		} else {
			appointmentsEl.setVisible(false);
			noAppointmentsEl.setVisible(false);
		}
	}
	
	private boolean hasFreeParticipations(Appointment appointment,
			Map<Long, List<Participation>> appointmentKeyToParticipation, int numParticipants) {
		if (appointment.equals(currentAppointment)) return false;
		if (appointment.getMaxParticipations() == null) return true;
		
		List<Participation> participations = appointmentKeyToParticipation.getOrDefault(appointment.getKey(), emptyList());
		return appointment.getMaxParticipations().intValue() >= (participations.size() + numParticipants);
	}

	private String formatDate(Appointment appointment) {
		return new StringBuilder()
				.append(dateFormat.format(appointment.getStart()))
				.append(" - ")
				.append(dateFormat.format(appointment.getEnd()))
				.toString();
	}
	
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if (source == participationsEl) {
			updateAppointmentsUI();
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = super.validateFormLogic(ureq);
		
		participationsEl.clearError();
		if (participationsEl.isVisible()) {
			if (!participationsEl.isAtLeastSelected(1)) {
				participationsEl.setErrorKey("error.select.participant", null);
				allOk &= false;
			}
		}
		
		appointmentsEl.clearError();
		if (appointmentsEl.isVisible()) {
			if (!appointmentsEl.isOneSelected()) {
				appointmentsEl.setErrorKey("error.select.appointment", null);
				allOk &= false;
			}
		}
		
		return allOk;
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		if (appointmentsEl.isVisible()) {
			Collection<ParticipationRef> participationRefs = participationsEl.getSelectedKeys().stream()
					.map(Long::valueOf)
					.map(ParticipationRef::of)
					.collect(Collectors.toList());
			Long appointmentKey = Long.valueOf(appointmentsEl.getSelectedKey());
			appointmentsService.rebookParticipations(AppointmentRef.of(appointmentKey), participationRefs,
					!config.isConfirmation());
		}
		
		onAfterRebooking();
		
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void doDispose() {
		//
	}

}
