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
package org.olat.commons.calendar.ui.components;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.core.CoreSpringFactory;
import org.olat.core.dispatcher.mapper.Mapper;
import org.olat.core.gui.media.JSONMediaResource;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;

/**
 * 
 * Initial date: 09.04.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class FullCalendarMapper implements Mapper {
	
	private static final OLog log = Tracing.createLoggerFor(FullCalendarMapper.class);
	private static final DateFormat formatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	private final FullCalendarComponent fcC;
	private final CalendarManager calendarManager;
	
	public FullCalendarMapper(FullCalendarComponent fcC) {
		this.fcC = fcC;
		calendarManager = CoreSpringFactory.getImpl(CalendarManager.class);
	}

	/**
	 * [{
	 * 	"id":111,
	 * 	"title":"Event1",
	 *  "start":"2013-03-10",
	 *  "url":"http:\/\/yahoo.com\/"
	 * },{
	 *  "id":222,
	 *  "title":"Event2",
	 *  "start":"2013-03-20",
	 *  "end":"2013-03-22",
	 *  "url":"http:\/\/yahoo.com\/"
	 * }]
	 */
	@Override
	public MediaResource handle(String relPath, HttpServletRequest request) {
		try {
			JSONArray ja = new JSONArray();
			
			String calendarId = getCalendarID(request);
			String start = request.getParameter("start");
			String end = request.getParameter("end");
			
			Date startDate = null;
			if(StringHelper.isLong(start)) {
				long startTime = Long.parseLong(start);
				startDate = new Date(startTime * 1000);
			}
			Date endDate = null;
			if(StringHelper.isLong(end)) {
				long time = Long.parseLong(end);
				endDate = new Date(time * 1000);
			}
			
			collectKalendarEvents(ja, calendarId, startDate, endDate);
			return new JSONMediaResource(ja, "UTF-8");
		} catch (JSONException e) {
			log.error("", e);
			return null;
		}
	}
	
	private String getCalendarID(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String calendarId = uri;
		int index = calendarId.lastIndexOf('/');
		if(index > 0) {
			calendarId = calendarId.substring(index + 1);
		}
		if(calendarId.endsWith(".json")) {
			calendarId = calendarId.substring(0, calendarId.length() - 5);
		}
		return calendarId;
	}
	
	private void collectKalendarEvents(JSONArray ja, String calendarId, Date from, Date to) throws JSONException {
		KalendarRenderWrapper cal =  fcC.getCalendar(calendarId);
		if(cal != null) {
			boolean privateEventsVisible = cal.isPrivateEventsVisible();
			List<KalendarEvent> events = calendarManager.getEvents(cal.getKalendar(), from, to, privateEventsVisible);

			for(KalendarEvent event:events) {
				if(!privateEventsVisible && event.getClassification() == KalendarEvent.CLASS_PRIVATE) {
					continue;
				}
				
				boolean timeOnly = !privateEventsVisible && event.getClassification() == KalendarEvent.CLASS_X_FREEBUSY;
				JSONObject jsonEvent = getJSONEvent(event, cal, timeOnly);
				ja.put(jsonEvent);
			}
		}
	}
	
	private JSONObject getJSONEvent(KalendarEvent event, KalendarRenderWrapper cal, boolean timeOnly)
	throws JSONException {
		JSONObject jsonEvent = new JSONObject();
		String id = FullCalendarComponent.normalizeId(event);
		jsonEvent.put("id", id);
		if(timeOnly) {
			jsonEvent.put("title", "");
		} else {
			jsonEvent.put("title", event.getSubject());
		}
		jsonEvent.put("allDay", Boolean.valueOf(event.isAllDayEvent()));
		
		if(fcC.isDifferentiateManagedEvents()) {
			applyManagedClassNames(jsonEvent, event, cal);
		} else if(StringHelper.containsNonWhitespace(cal.getCssClass())) {
			applyClassNames(jsonEvent, cal);
		}
		
		jsonEvent.put("editable", Boolean.valueOf(cal.getAccess() == KalendarRenderWrapper.ACCESS_READ_WRITE));
		
		if(event.getBegin() != null) {
			jsonEvent.put("start", formatDate(event.getBegin()));
		}
		if(event.getEnd() != null) {
			jsonEvent.put("end", formatDate(event.getEnd()));
		}
		return jsonEvent;
	}
	
	private String getColor(String cssClass) {
		if(StringHelper.containsNonWhitespace(cssClass) && cssClass.startsWith("o_cal_")) {
			return cssClass.substring(6, cssClass.length());
		}
		return null;
	}
	
	private void applyClassNames(JSONObject jsonEvent, KalendarRenderWrapper cal)
	throws JSONException {
		jsonEvent.put("className", cal.getCssClass());
		String color = getColor(cal.getCssClass());
		if(StringHelper.containsNonWhitespace(color)) {
			jsonEvent.put("color", color);
		}
	}
	
	private void applyManagedClassNames(JSONObject jsonEvent, KalendarEvent event, KalendarRenderWrapper cal)
	throws JSONException {
		StringBuilder classNames = new StringBuilder(32);
		if(StringHelper.containsNonWhitespace(cal.getCssClass())) {
			if(cal.getKalendar().hasManagedEvents()) {
				if(!event.isManaged()) {
					jsonEvent.put("color", "steelblue");
				} else {
					classNames.append(cal.getCssClass());
					String color = getColor(cal.getCssClass());
					if(StringHelper.containsNonWhitespace(color)) {
						jsonEvent.put("color", color);
					}
				}
			} else {
				classNames.append(cal.getCssClass());
				String color = getColor(cal.getCssClass());
				if(StringHelper.containsNonWhitespace(color)) {
					jsonEvent.put("color", color);
				}
			}
		} 

		if(event.isManaged()) {
			classNames.append(" o_cal_event_managed");
		} else {
			classNames.append(" o_cal_event_not_managed");
		}
		jsonEvent.put("className", classNames.toString());
	}
	
	private String formatDate(Date date) {
		synchronized(formatDateTime) {
			return formatDateTime.format(date);
		}
	}
}
