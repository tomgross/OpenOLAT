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
package org.olat.modules.lecture;

/**
 * 
 * Initial date: 28 août 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LectureBlockRollCallSearchParameters {
	
	private Boolean closed;
	private Boolean hasAbsence;
	private Boolean hasSupervisorNotificationDate;
	
	private Long rollCallKey;
	private Long lectureBlockKey;

	public Boolean getClosed() {
		return closed;
	}

	public void setClosed(Boolean closed) {
		this.closed = closed;
	}

	public Boolean getHasAbsence() {
		return hasAbsence;
	}

	public void setHasAbsence(Boolean hasAbsence) {
		this.hasAbsence = hasAbsence;
	}

	public Boolean getHasSupervisorNotificationDate() {
		return hasSupervisorNotificationDate;
	}

	public void setHasSupervisorNotificationDate(Boolean hasSupervisorNotificationDate) {
		this.hasSupervisorNotificationDate = hasSupervisorNotificationDate;
	}

	public Long getRollCallKey() {
		return rollCallKey;
	}

	public void setRollCallKey(Long rollCallKey) {
		this.rollCallKey = rollCallKey;
	}

	public Long getLectureBlockKey() {
		return lectureBlockKey;
	}

	public void setLectureBlockKey(Long lectureBlockKey) {
		this.lectureBlockKey = lectureBlockKey;
	}
}
