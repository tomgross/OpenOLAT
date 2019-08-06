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
package org.olat.modules.lecture.manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.velocity.VelocityContext;
import org.olat.basesecurity.Group;
import org.olat.basesecurity.IdentityImpl;
import org.olat.basesecurity.IdentityRef;
import org.olat.basesecurity.manager.GroupDAO;
import org.olat.commons.calendar.CalendarManagedFlag;
import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.model.Kalendar;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.core.commons.persistence.DB;
import org.olat.core.gui.translator.Translator;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.mail.MailBundle;
import org.olat.core.util.mail.MailContext;
import org.olat.core.util.mail.MailContextImpl;
import org.olat.core.util.mail.MailManager;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.group.BusinessGroup;
import org.olat.group.DeletableGroupData;
import org.olat.modules.lecture.LectureBlock;
import org.olat.modules.lecture.LectureBlockAuditLog;
import org.olat.modules.lecture.LectureBlockAuditLog.Action;
import org.olat.modules.lecture.LectureBlockRef;
import org.olat.modules.lecture.LectureBlockRollCall;
import org.olat.modules.lecture.LectureBlockRollCallRef;
import org.olat.modules.lecture.LectureBlockRollCallSearchParameters;
import org.olat.modules.lecture.LectureBlockStatus;
import org.olat.modules.lecture.LectureBlockToGroup;
import org.olat.modules.lecture.LectureModule;
import org.olat.modules.lecture.LectureParticipantSummary;
import org.olat.modules.lecture.LectureRollCallStatus;
import org.olat.modules.lecture.LectureService;
import org.olat.modules.lecture.Reason;
import org.olat.modules.lecture.RepositoryEntryLectureConfiguration;
import org.olat.modules.lecture.model.AggregatedLectureBlocksStatistics;
import org.olat.modules.lecture.model.LectureBlockAndRollCall;
import org.olat.modules.lecture.model.LectureBlockIdentityStatistics;
import org.olat.modules.lecture.model.LectureBlockImpl;
import org.olat.modules.lecture.model.LectureBlockStatistics;
import org.olat.modules.lecture.model.LectureBlockToTeacher;
import org.olat.modules.lecture.model.LectureBlockWithTeachers;
import org.olat.modules.lecture.model.LectureStatisticsSearchParameters;
import org.olat.modules.lecture.model.LecturesBlockSearchParameters;
import org.olat.modules.lecture.model.ParticipantAndLectureSummary;
import org.olat.modules.lecture.ui.ConfigurationHelper;
import org.olat.modules.lecture.ui.LectureAdminController;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.olat.repository.manager.RepositoryEntryDAO;
import org.olat.user.UserDataDeletable;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 17 mars 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class LectureServiceImpl implements LectureService, UserDataDeletable, DeletableGroupData {
	private static final OLog log = Tracing.createLoggerFor(LectureServiceImpl.class);
	private static final CalendarManagedFlag[] CAL_MANAGED_FLAGS = new CalendarManagedFlag[] { CalendarManagedFlag.all };

	@Autowired
	private DB dbInstance;
	@Autowired
	private GroupDAO groupDao;
	@Autowired
	private ReasonDAO reasonDao;
	@Autowired
	private UserManager userManager;
	@Autowired
	private I18nManager i18nManager;
	@Autowired
	private MailManager mailManager;
	@Autowired
	private LectureModule lectureModule;
	@Autowired
	private CalendarManager calendarMgr;
	@Autowired
	private LectureBlockDAO lectureBlockDao;
	@Autowired
	private LectureBlockAuditLogDAO auditLogDao;
	@Autowired
	private RepositoryEntryDAO repositoryEntryDao;
	@Autowired
	private LectureBlockToGroupDAO lectureBlockToGroupDao;
	@Autowired
	private LectureBlockRollCallDAO lectureBlockRollCallDao;
	@Autowired
	private LectureBlockReminderDAO lectureBlockReminderDao;
	@Autowired
	private LectureParticipantSummaryDAO lectureParticipantSummaryDao;
	@Autowired
	private RepositoryEntryLectureConfigurationDAO lectureConfigurationDao;
	
	
	@Override
	public RepositoryEntryLectureConfiguration getRepositoryEntryLectureConfiguration(RepositoryEntry entry) {
		RepositoryEntryLectureConfiguration config = lectureConfigurationDao.getConfiguration(entry);
		if(config == null) {
			RepositoryEntry reloadedEntry = repositoryEntryDao.loadForUpdate(entry);
			config = lectureConfigurationDao.getConfiguration(entry);
			if(config == null) {
				config = lectureConfigurationDao.createConfiguration(reloadedEntry);
			}
			dbInstance.commit();
		}
		return config;
	}
	
	@Override
	public boolean isRepositoryEntryLectureEnabled(RepositoryEntryRef entry) {
		if(!lectureModule.isEnabled()) {
			return false;
		}
		return lectureConfigurationDao.isConfigurationEnabledFor(entry);
	}

	@Override
	public RepositoryEntryLectureConfiguration copyRepositoryEntryLectureConfiguration(RepositoryEntry sourceEntry, RepositoryEntry targetEntry) {
		RepositoryEntryLectureConfiguration config = lectureConfigurationDao.getConfiguration(sourceEntry);
		if(config != null) {
			config = lectureConfigurationDao.cloneConfiguration(config, targetEntry);
		}
		return config;
	}

	@Override
	public RepositoryEntryLectureConfiguration updateRepositoryEntryLectureConfiguration(RepositoryEntryLectureConfiguration config) {
		return lectureConfigurationDao.update(config);
	}

	@Override
	public LectureBlock createLectureBlock(RepositoryEntry entry) {
		return lectureBlockDao.createLectureBlock(entry);
	}

	@Override
	public LectureBlock save(LectureBlock lectureBlock, List<Group> groups) {
		LectureBlockImpl block = (LectureBlockImpl)lectureBlockDao.update(lectureBlock);
		if(groups != null) {
			List<LectureBlockToGroup> lectureToGroups = lectureBlockToGroupDao.getLectureBlockToGroups(block);
			for(Group group:groups) {
				boolean found = false;
				for(LectureBlockToGroup lectureToGroup:lectureToGroups) {
					if(lectureToGroup.getGroup().equals(group)) {
						found = true;
						break;
					}
				}
				
				if(!found) {
					LectureBlockToGroup blockToGroup = lectureBlockToGroupDao.createAndPersist(block, group);
					lectureToGroups.add(blockToGroup);
				}
			}
			
			for(Iterator<LectureBlockToGroup> lectureToGroupIt=lectureToGroups.iterator(); lectureToGroupIt.hasNext(); ) {
				LectureBlockToGroup lectureBlockToGroup = lectureToGroupIt.next();
				if(!groups.contains(lectureBlockToGroup.getGroup())) {
					lectureBlockToGroupDao.remove(lectureBlockToGroup);
				}
			}
		}
		block.getTeacherGroup().getKey();
		return block;
	}
	
	@Override
	public LectureBlock close(LectureBlock lectureBlock, Identity author) {
		lectureBlock.setStatus(LectureBlockStatus.done);
		lectureBlock.setRollCallStatus(LectureRollCallStatus.closed);
		LectureBlockImpl block = (LectureBlockImpl)lectureBlockDao.update(lectureBlock);
		
		int numOfLectures = block.getEffectiveLecturesNumber();
		if(numOfLectures <= 0 && block.getStatus() != LectureBlockStatus.cancelled) {
			numOfLectures = block.getPlannedLecturesNumber();
		}
		
		List<LectureBlockRollCall> rollCallList = lectureBlockRollCallDao.getRollCalls(lectureBlock);
		for(LectureBlockRollCall rollCall:rollCallList) {
			lectureBlockRollCallDao.adaptLecture(block, rollCall, numOfLectures, author);
		}
		dbInstance.commit();
		recalculateSummary(block.getEntry());
		return block;
	}

	@Override
	public LectureBlock cancel(LectureBlock lectureBlock) {
		lectureBlock.setStatus(LectureBlockStatus.cancelled);
		lectureBlock.setRollCallStatus(LectureRollCallStatus.closed);
		lectureBlock.setEffectiveLecturesNumber(0);
		LectureBlockImpl block = (LectureBlockImpl)lectureBlockDao.update(lectureBlock);
		dbInstance.commit();
		recalculateSummary(block.getEntry());
		return block;
	}

	@Override
	public String toAuditXml(LectureBlock lectureBlock) {
		return auditLogDao.toXml(lectureBlock);
	}
	
	@Override
	public String toAuditXml(LectureBlockRollCall rollCall) {
		return auditLogDao.toXml(rollCall);
	}
	
	@Override
	public LectureBlock toAuditLectureBlock(String xml) {
		return auditLogDao.lectureBlockFromXml(xml);
	}

	@Override
	public LectureBlockRollCall toAuditLectureBlockRollCall(String xml) {
		return auditLogDao.rollCallFromXml(xml);
	}

	@Override
	public String toAuditXml(LectureParticipantSummary summary) {
		return auditLogDao.toXml(summary);
	}

	@Override
	public LectureParticipantSummary toAuditLectureParticipantSummary(String xml) {
		return auditLogDao.summaryFromXml(xml);
	}

	@Override
	public void auditLog(LectureBlockAuditLog.Action action, String before, String after, String message,
			LectureBlockRef lectureBlock, LectureBlockRollCall rollCall,
			RepositoryEntryRef entry, IdentityRef assessedIdentity, IdentityRef author) {
		auditLogDao.auditLog(action, before, after, message, lectureBlock, rollCall, entry, assessedIdentity, author);
	}

	@Override
	public List<LectureBlockAuditLog> getAuditLog(LectureBlockRef lectureBlock) {
		return auditLogDao.getAuditLog(lectureBlock);
	}

	@Override
	public List<LectureBlockAuditLog> getAuditLog(IdentityRef assessedIdentity) {
		return auditLogDao.getAuditLog(assessedIdentity);
	}
	@Override
	public List<LectureBlockAuditLog> getAuditLog(RepositoryEntryRef entry, IdentityRef assessedIdentity, Action action) {
		return auditLogDao.getAuditLog(entry, assessedIdentity, action);
	}

	@Override
	public List<LectureBlockAuditLog> getAuditLog(RepositoryEntryRef entry) {
		return auditLogDao.getAuditLog(entry);
	}

	@Override
	public LectureBlock copyLectureBlock(String newTitle, LectureBlock block) {
		LectureBlock copy = lectureBlockDao.createLectureBlock(block.getEntry());
		copy.setTitle(newTitle);
		copy.setDescription(block.getDescription());
		copy.setPreparation(block.getPreparation());
		copy.setLocation(block.getLocation());
		copy.setRollCallStatus(LectureRollCallStatus.open);
		copy.setEffectiveLecturesNumber(block.getEffectiveLecturesNumber());
		copy.setPlannedLecturesNumber(block.getPlannedLecturesNumber());
		copy.setStartDate(block.getStartDate());
		copy.setEndDate(block.getEndDate());
		copy = lectureBlockDao.update(copy);
		return copy;
	}

	@Override
	public void deleteLectureBlock(LectureBlock lectureBlock) {
		//first remove events
		LectureBlock reloadedBlock = lectureBlockDao.loadByKey(lectureBlock.getKey());
		RepositoryEntry entry = reloadedBlock.getEntry();
		RepositoryEntryLectureConfiguration config = getRepositoryEntryLectureConfiguration(entry);
		if(ConfigurationHelper.isSyncCourseCalendarEnabled(config, lectureModule)) {
			unsyncCourseCalendar(lectureBlock, entry);
		}
		if(ConfigurationHelper.isSyncTeacherCalendarEnabled(config, lectureModule)) {
			List<Identity> teachers = getTeachers(reloadedBlock);
			unsyncInternalCalendar(reloadedBlock, teachers);
		}
		lectureBlockDao.delete(reloadedBlock);
	}

	@Override
	public int delete(RepositoryEntry entry) {
		int rows = 0;
		List<LectureBlock> blocksToDelete = lectureBlockDao.getLectureBlocks(entry);
		for(LectureBlock blockToDelete:blocksToDelete) {
			rows += lectureBlockDao.delete(blockToDelete);
		}
		rows += lectureConfigurationDao.deleteConfiguration(entry);
		rows += lectureParticipantSummaryDao.deleteSummaries(entry);
		return rows;
	}

	@Override
	public void deleteUserData(Identity identity, String newDeletedUserName) {
		lectureParticipantSummaryDao.deleteSummaries(identity);
		lectureBlockRollCallDao.deleteRollCalls(identity);
		lectureBlockReminderDao.deleteReminders(identity);
	}

	@Override
	public boolean deleteGroupDataFor(BusinessGroup group) {
		lectureBlockToGroupDao.deleteLectureBlockToGroup(group.getBaseGroup());
		return true;
	}

	@Override
	public LectureBlock getLectureBlock(LectureBlockRef block) {
		return lectureBlockDao.loadByKey(block.getKey());
	}

	@Override
	public List<Reason> getAllReasons() {
		return reasonDao.getReasons();
	}

	@Override
	public Reason getReason(Long key) {
		return reasonDao.loadReason(key);
	}	

	@Override
	public boolean isReasonInUse(Reason reason) {
		return reasonDao.isReasonInUse(reason);
	}

	@Override
	public boolean deleteReason(Reason reason) {
		return reasonDao.delete(reason);
	}

	@Override
	public Reason createReason(String title, String description) {
		return reasonDao.createReason(title, description);
	}

	@Override
	public Reason updateReason(Reason reason) {
		return reasonDao.updateReason(reason);
	}

	@Override
	public List<Group> getLectureBlockToGroups(LectureBlockRef block) {
		return lectureBlockToGroupDao.getGroups(block);
	}
	
	@Override
	public List<Identity> getParticipants(LectureBlockRef block) {
		return lectureBlockDao.getParticipants(block);
	}

	@Override
	public List<Identity> getParticipants(RepositoryEntry entry) {
		return lectureBlockDao.getParticipants(entry);
	}

	@Override
	public List<Identity> getParticipants(RepositoryEntry entry, Identity teacher) {
		return lectureBlockDao.getParticipants(entry, teacher);
	}

	@Override
	public List<Identity> startLectureBlock(Identity teacher, LectureBlock lectureBlock) {
		RepositoryEntry entry = lectureBlock.getEntry();
		Date now = new Date();

		List<ParticipantAndLectureSummary> participantsAndSummaries = lectureParticipantSummaryDao.getLectureParticipantSummaries(lectureBlock);
		Set<Identity> participants = new HashSet<>();
		for(ParticipantAndLectureSummary participantAndSummary:participantsAndSummaries) {
			if(participants.contains(participantAndSummary.getIdentity())) {
				continue;
			}
			if(participantAndSummary.getSummary() == null) {
				lectureParticipantSummaryDao.createSummary(entry, participantAndSummary.getIdentity(), now);
			}
			participants.add(participantAndSummary.getIdentity());
		}
		return new ArrayList<>(participants);
	}
	
	@Override
	public List<Identity> syncParticipantSummaries(LectureBlock lectureBlock) {
		RepositoryEntry entry = lectureBlock.getEntry();
		Date now = new Date();

		List<ParticipantAndLectureSummary> participantsAndSummaries = lectureParticipantSummaryDao.getLectureParticipantSummaries(lectureBlock);
		Set<Identity> participants = new HashSet<>();
		for(ParticipantAndLectureSummary participantAndSummary:participantsAndSummaries) {
			if(participants.contains(participantAndSummary.getIdentity())) {
				continue;
			}
			if(participantAndSummary.getSummary() == null) {
				lectureParticipantSummaryDao.createSummary(entry, participantAndSummary.getIdentity(), now);
			}
			participants.add(participantAndSummary.getIdentity());
		}
		return new ArrayList<>(participants);
	}

	@Override
	public List<LectureBlockRollCall> getRollCalls(LectureBlockRef block) {
		return lectureBlockRollCallDao.getRollCalls(block);
	}

	@Override
	public List<LectureBlockRollCall> getRollCalls(LectureBlockRollCallSearchParameters searchParams) {
		return lectureBlockRollCallDao.getRollCalls(searchParams);
	}

	@Override
	public LectureBlockRollCall getOrCreateRollCall(Identity identity, LectureBlock lectureBlock,
			Boolean authorizedAbsence, String reasonAbsence) {
		LectureBlockRollCall rollCall = lectureBlockRollCallDao.getRollCall(lectureBlock, identity);
		if(rollCall == null) {//reload in case of concurrent usage
			rollCall = lectureBlockRollCallDao.createAndPersistRollCall(lectureBlock, identity,
					authorizedAbsence, reasonAbsence, null, null);
		} else if(authorizedAbsence != null) {
			rollCall.setAbsenceAuthorized(authorizedAbsence);
			rollCall.setAbsenceReason(reasonAbsence);
			rollCall = lectureBlockRollCallDao.update(rollCall);
		}
		return rollCall;
	}
	
	@Override
	public LectureBlockRollCall getRollCall(LectureBlockRollCallRef rollCall) {
		if(rollCall == null) return null;
		return lectureBlockRollCallDao.loadByKey(rollCall.getKey());
	}

	@Override
	public LectureBlockRollCall updateRollCall(LectureBlockRollCall rollCall) {
		return lectureBlockRollCallDao.update(rollCall);
	}

	@Override
	public LectureBlockRollCall addRollCall(Identity identity, LectureBlock lectureBlock, LectureBlockRollCall rollCall, List<Integer> absences) {
		if(rollCall == null) {//reload in case of concurrent usage
			rollCall = lectureBlockRollCallDao.getRollCall(lectureBlock, identity);
		}
		
		boolean checkAuthorized = lectureModule.isAuthorizedAbsenceEnabled() &&  lectureModule.isAbsenceDefaultAuthorized();
		if(rollCall == null) {
			Boolean authorized = null;
			if(checkAuthorized && absences != null && absences.size() > 0) {
				authorized = Boolean.TRUE;
			}
			rollCall = lectureBlockRollCallDao.createAndPersistRollCall(lectureBlock, identity, authorized, null, null, absences);
		} else {
			if(checkAuthorized && absences != null && absences.size() > 0 && rollCall.getAbsenceAuthorized() == null) {
				rollCall.setAbsenceAuthorized(Boolean.TRUE);
			}
			rollCall = lectureBlockRollCallDao.addLecture(lectureBlock, rollCall, absences);
		}
		return rollCall;
	}
	
	@Override
	public LectureBlockRollCall addRollCall(Identity identity, LectureBlock lectureBlock, LectureBlockRollCall rollCall, String comment, List<Integer> absences) {
		if(rollCall == null) {//reload in case of concurrent usage
			rollCall = lectureBlockRollCallDao.getRollCall(lectureBlock, identity);
		}
		boolean checkAuthorized = lectureModule.isAuthorizedAbsenceEnabled() &&  lectureModule.isAbsenceDefaultAuthorized();
		if(rollCall == null) {
			Boolean authorized = null;
			if(checkAuthorized && absences != null && absences.size() > 0) {
				authorized = Boolean.TRUE;
			}
			rollCall = lectureBlockRollCallDao.createAndPersistRollCall(lectureBlock, identity, authorized, null, comment, absences);
		} else {
			if(comment != null) {
				rollCall.setComment(comment);
			}
			if(checkAuthorized && absences != null && absences.size() > 0 && rollCall.getAbsenceAuthorized() == null) {
				rollCall.setAbsenceAuthorized(Boolean.TRUE);
			}
			rollCall = lectureBlockRollCallDao.addLecture(lectureBlock, rollCall, absences);
		}
		return rollCall;
	}

	@Override
	public LectureBlockRollCall removeRollCall(Identity identity, LectureBlock lectureBlock, LectureBlockRollCall rollCall, List<Integer> absences) {
		if(rollCall == null) {//reload in case of concurrent usage
			rollCall = lectureBlockRollCallDao.getRollCall(lectureBlock, identity);
		}
		if(rollCall == null) {
			rollCall = lectureBlockRollCallDao.createAndPersistRollCall(lectureBlock, identity, null, null, null, absences);
		} else {
			rollCall = lectureBlockRollCallDao.removeLecture(lectureBlock, rollCall, absences);
		}
		return rollCall;
	}

	@Override
	public void adaptRollCalls(LectureBlock lectureBlock) {
		LectureBlockStatus status = lectureBlock.getStatus();
		LectureRollCallStatus rollCallStatus = lectureBlock.getRollCallStatus();
		if(status == LectureBlockStatus.done || rollCallStatus == LectureRollCallStatus.closed || rollCallStatus == LectureRollCallStatus.autoclosed) {
			log.warn("Try to adapt roll call of a closed lecture block: " + lectureBlock.getKey());
			return;
		}

		List<LectureBlockRollCall> rollCallList = lectureBlockRollCallDao.getRollCalls(lectureBlock);
		for(LectureBlockRollCall rollCall:rollCallList) {
			int numOfLectures = lectureBlock.getEffectiveLecturesNumber();
			if(numOfLectures <= 0 && lectureBlock.getStatus() != LectureBlockStatus.cancelled) {
				numOfLectures = lectureBlock.getPlannedLecturesNumber();
			}
			lectureBlockRollCallDao.adaptLecture(lectureBlock, rollCall, numOfLectures, null);
		}
	}
	
	@Override
	public void adaptAll(Identity author) {
		List<LectureBlock> lectureBlocks = lectureBlockDao.getLectureBlocks();
		for(LectureBlock lectureBlock:lectureBlocks) {
			List<LectureBlockRollCall> rollCallList = lectureBlockRollCallDao.getRollCalls(lectureBlock);
			for(LectureBlockRollCall rollCall:rollCallList) {
				int numOfLectures = lectureBlock.getEffectiveLecturesNumber();
				if(numOfLectures <= 0 && lectureBlock.getStatus() != LectureBlockStatus.cancelled) {
					numOfLectures = lectureBlock.getPlannedLecturesNumber();
				}
				lectureBlockRollCallDao.adaptLecture(lectureBlock, rollCall, numOfLectures, author);
			}
			dbInstance.commitAndCloseSession();
		}
	}

	@Override
	public void recalculateSummary(RepositoryEntry entry) {
		List<LectureBlockStatistics> statistics = getParticipantsLecturesStatistics(entry);
		int count = 0;
		for(LectureBlockStatistics statistic:statistics) {
			if(lectureParticipantSummaryDao.updateStatistics(statistic) == 0) {
				Identity identity = dbInstance.getCurrentEntityManager()
						.getReference(IdentityImpl.class, statistic.getIdentityKey());
				lectureParticipantSummaryDao.createSummary(entry, identity, new Date(), statistic);
			}
			if(++count % 20 == 0) {
				dbInstance.commitAndCloseSession();
			}
		}
	}

	@Override
	public void recalculateSummary(RepositoryEntry entry, Identity identity) {
		List<LectureBlockStatistics> statistics = getParticipantsLecturesStatistics(entry);
		for(LectureBlockStatistics statistic:statistics) {
			if(identity.getKey().equals(statistic.getIdentityKey())) {
				if(lectureParticipantSummaryDao.updateStatistics(statistic) == 0) {
					lectureParticipantSummaryDao.createSummary(entry, identity, new Date(), statistic);
				}
			}
		}
	}

	@Override
	public void autoCloseRollCall() {
		int period = lectureModule.getRollCallAutoClosePeriod();
		if(period > 0) {
			Date now = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(now);
			cal.add(Calendar.DATE, -period);
			Date endDate = cal.getTime();
			List<LectureBlockImpl> blocks = lectureBlockDao.loadOpenBlocksBefore(endDate);
			for(LectureBlockImpl block:blocks) {
				autoClose(block);
				dbInstance.commitAndCloseSession();
			}
		}
	}
	
	private void autoClose(LectureBlockImpl lectureBlock) {
		String blockBefore = auditLogDao.toXml(lectureBlock);
		lectureBlock.setStatus(LectureBlockStatus.done);
		lectureBlock.setRollCallStatus(LectureRollCallStatus.autoclosed);
		if(lectureBlock.getEffectiveLecturesNumber() <= 0 && lectureBlock.getStatus() != LectureBlockStatus.cancelled) {
			lectureBlock.setEffectiveLecturesNumber(lectureBlock.getPlannedLecturesNumber());
		}
		lectureBlock.setAutoClosedDate(new Date());
		lectureBlock = (LectureBlockImpl)lectureBlockDao.update(lectureBlock);
		dbInstance.commit();
		
		List<LectureBlockRollCall> rollCalls = lectureBlockRollCallDao.getRollCalls(lectureBlock);
		Map<Identity,LectureBlockRollCall> rollCallMap = rollCalls.stream().collect(Collectors.toMap(r -> r.getIdentity(), r -> r));
		List<ParticipantAndLectureSummary> participantsAndSummaries = lectureParticipantSummaryDao.getLectureParticipantSummaries(lectureBlock);
		Set<Identity> participants = new HashSet<>();
		for(ParticipantAndLectureSummary participantAndSummary:participantsAndSummaries) {
			if(participants.contains(participantAndSummary.getIdentity())) {
				continue;
			}
			if(participantAndSummary.getSummary() != null) {
				LectureBlockRollCall rollCall = rollCallMap.get(participantAndSummary.getIdentity());
				
				String before = auditLogDao.toXml(rollCall);
				if(rollCall == null) {
					rollCall = lectureBlockRollCallDao.createAndPersistRollCall(lectureBlock, participantAndSummary.getIdentity(), null, null, null, new ArrayList<>());
				} else if(rollCall.getLecturesAbsentList().isEmpty() && rollCall.getLecturesAttendedList().isEmpty()) {
					rollCall = lectureBlockRollCallDao.addLecture(lectureBlock, rollCall, new ArrayList<>());
				}

				String after = auditLogDao.toXml(rollCall);
				auditLogDao.auditLog(LectureBlockAuditLog.Action.autoclose, before, after, null,
						lectureBlock, rollCall, lectureBlock.getEntry(), participantAndSummary.getIdentity(), null);
			}
		}

		String blockAfter = auditLogDao.toXml(lectureBlock);
		auditLogDao.auditLog(LectureBlockAuditLog.Action.autoclose, blockBefore, blockAfter, null, lectureBlock, null, lectureBlock.getEntry(), null, null);
		dbInstance.commit();
		
		recalculateSummary(lectureBlock.getEntry());
	}

	@Override
	public void sendReminders() {
		int reminderPeriod = lectureModule.getRollCallReminderPeriod();
		if(reminderPeriod > 0) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -reminderPeriod);
			Date endDate = cal.getTime();

			boolean reminderEnabled = lectureModule.isRollCallReminderEnabled();
			List<LectureBlockToTeacher> toRemindList = lectureBlockReminderDao.getLectureBlockTeachersToReminder(endDate);
			for(LectureBlockToTeacher toRemind:toRemindList) {
				Identity teacher = toRemind.getTeacher();
				LectureBlock lectureBlock = toRemind.getLectureBlock();
				if(reminderEnabled) {
					sendReminder(teacher, lectureBlock);
				} else {
					lectureBlockReminderDao.createReminder(lectureBlock, teacher, "disabled");
				}
			}
		}
	}
	
	private void sendReminder(Identity teacher, LectureBlock lectureBlock) {
		RepositoryEntry entry = lectureBlock.getEntry();
		String language = teacher.getUser().getPreferences().getLanguage();
		Locale locale = i18nManager.getLocaleOrDefault(language);
		String startDate = Formatter.getInstance(locale).formatDate(lectureBlock.getStartDate());
		
		MailContext context = new MailContextImpl("[RepositoryEntry:" + entry.getKey() + "]");
		String url = Settings.getServerContextPathURI() + "/url/RepositoryEntry/" + entry.getKey() + "/LectureBlock/" + lectureBlock.getKey();
		String[] args = new String[]{
				lectureBlock.getTitle(),					//{0}
				startDate,									//{1}
				entry.getDisplayname(),						//{2}
				url,										//{3}
				userManager.getUserDisplayName(teacher) 	//{4}	
		};
		
		Translator trans = Util.createPackageTranslator(LectureAdminController.class, locale);
		String subject = trans.translate("lecture.teacher.reminder.subject", args);
		String body = trans.translate("lecture.teacher.reminder.body", args);

		LectureReminderTemplate template = new LectureReminderTemplate(subject, body);
		MailerResult result = new MailerResult();
		MailBundle bundle = mailManager.makeMailBundle(context, teacher, template, null, null, result);
		MailerResult sendResult = mailManager.sendMessage(bundle);
		result.append(sendResult);

		String status;
		List<Identity> failedIdentities = result.getFailedIdentites();
		if(failedIdentities != null && failedIdentities.contains(teacher)) {
			status = "error";
		} else {
			status = "ok";
		}
		
		lectureBlockReminderDao.createReminder(lectureBlock, teacher, status);
	}

	@Override
	public List<LectureBlock> getLectureBlocks(RepositoryEntryRef entry) {
		return lectureBlockDao.getLectureBlocks(entry);
	}
	
	@Override
	public List<LectureBlock> getLectureBlocks(LecturesBlockSearchParameters searchParams) {
		return lectureBlockDao.searchLectureBlocks(searchParams);
	}

	@Override
	public List<LectureBlock> getLectureBlocks(IdentityRef teacher, LecturesBlockSearchParameters searchParams) {
		return lectureBlockDao.loadByTeacher(teacher, searchParams);
	}

	@Override
	public List<LectureBlockWithTeachers> getLectureBlocksWithTeachers(RepositoryEntryRef entry) {
		return lectureBlockDao.getLecturesBlockWithTeachers(entry);
	}

	/**
	 * 
	 */
	@Override
	public List<LectureBlockWithTeachers> getLectureBlocksWithTeachers(RepositoryEntryRef entry,
			IdentityRef teacher, LecturesBlockSearchParameters searchParams) {
		return lectureBlockDao.getLecturesBlockWithTeachers(entry, teacher, searchParams);
	}

	@Override
	public List<Identity> getTeachers(LectureBlock lectureBlock) {
		LectureBlockImpl block = (LectureBlockImpl)lectureBlock;
		return groupDao.getMembers(block.getTeacherGroup(), "teacher");
	}
	
	@Override
	public List<Identity> getTeachers(RepositoryEntry entry) {
		return lectureBlockDao.getTeachers(entry);
	}

	@Override
	public List<LectureBlock> getLectureBlocks(RepositoryEntryRef entry, IdentityRef teacher) {
		return lectureBlockDao.getLecturesAsTeacher(entry, teacher);
	}

	@Override
	public boolean hasLecturesAsTeacher(RepositoryEntryRef entry, Identity identity) {
		return lectureBlockDao.hasLecturesAsTeacher(entry, identity);
	}

	@Override
	public List<LectureBlock> getRollCallAsTeacher(Identity identity) {
		return lectureBlockDao.getRollCallAsTeacher(identity);
	}

	@Override
	public void addTeacher(LectureBlock lectureBlock, Identity teacher) {
		LectureBlockImpl block = (LectureBlockImpl)lectureBlock;
		if(!groupDao.hasRole(block.getTeacherGroup(), teacher, "teacher")) {
			groupDao.addMembershipOneWay(block.getTeacherGroup(), teacher, "teacher");
		}
	}

	@Override
	public void removeTeacher(LectureBlock lectureBlock, Identity teacher) {
		LectureBlockImpl block = (LectureBlockImpl)lectureBlock;
		groupDao.removeMembership(block.getTeacherGroup(), teacher);
	}

	@Override
	public LectureParticipantSummary getOrCreateParticipantSummary(RepositoryEntry entry, Identity identity) {
		LectureParticipantSummary summary = lectureParticipantSummaryDao.getSummary(entry, identity);
		if(summary == null) {
			summary = lectureParticipantSummaryDao.createSummary(entry, identity, null);
		}
		return summary;
	}

	@Override
	public LectureParticipantSummary saveParticipantSummary(LectureParticipantSummary summary) {
		return lectureParticipantSummaryDao.update(summary);
	}

	@Override
	public List<LectureBlockIdentityStatistics> groupByIdentity(List<LectureBlockIdentityStatistics> statistics) {
		Map<Long,LectureBlockIdentityStatistics> groupBy = new HashMap<>();
		for(LectureBlockIdentityStatistics statistic:statistics) {
			if(groupBy.containsKey(statistic.getIdentityKey())){
				groupBy.get(statistic.getIdentityKey()).aggregate(statistic);
			} else {
				groupBy.put(statistic.getIdentityKey(), statistic.cloneForAggregation());
			}
		}

		boolean countAuthorizedAbsenceAsAttendant = lectureModule.isCountAuthorizedAbsenceAsAttendant();
		List<LectureBlockIdentityStatistics> aggregatedStatistics = new ArrayList<>(groupBy.values());
		for(LectureBlockIdentityStatistics statistic:aggregatedStatistics) {
			lectureBlockRollCallDao.calculateAttendanceRate(statistic, countAuthorizedAbsenceAsAttendant);
		}
		return aggregatedStatistics;
	}

	@Override
	public List<LectureBlockStatistics> getParticipantLecturesStatistics(IdentityRef identity) {
		boolean authorizedAbsenceEnabled = lectureModule.isAuthorizedAbsenceEnabled();
		boolean calculateAttendanceRate = lectureModule.isRollCallCalculateAttendanceRateDefaultEnabled();
		boolean absenceDefaultAuthorized = lectureModule.isAbsenceDefaultAuthorized();
		boolean countAuthorizedAbsenceAsAttendant = lectureModule.isCountAuthorizedAbsenceAsAttendant();
		double defaultRequiredAttendanceRate = lectureModule.getRequiredAttendanceRateDefault();
		return lectureBlockRollCallDao.getStatistics(identity, authorizedAbsenceEnabled,
				absenceDefaultAuthorized, countAuthorizedAbsenceAsAttendant,
				calculateAttendanceRate, defaultRequiredAttendanceRate);
	}

	@Override
	public List<LectureBlockStatistics> getParticipantsLecturesStatistics(RepositoryEntry entry) {
		boolean authorizedAbsenceEnabled = lectureModule.isAuthorizedAbsenceEnabled();
		boolean calculateAttendanceRate = lectureModule.isRollCallCalculateAttendanceRateDefaultEnabled();
		boolean absenceDefaultAuthorized = lectureModule.isAbsenceDefaultAuthorized();
		boolean countAuthorizedAbsenceAsAttendant = lectureModule.isCountAuthorizedAbsenceAsAttendant();
		double defaultRequiredAttendanceRate = lectureModule.getRequiredAttendanceRateDefault();
		RepositoryEntryLectureConfiguration config = getRepositoryEntryLectureConfiguration(entry);
		return lectureBlockRollCallDao.getStatistics(entry, config, authorizedAbsenceEnabled,
				absenceDefaultAuthorized, countAuthorizedAbsenceAsAttendant,
				calculateAttendanceRate, defaultRequiredAttendanceRate);
	}
	
	@Override
	public List<LectureBlockIdentityStatistics> getLecturesStatistics(LectureStatisticsSearchParameters params,
			List<UserPropertyHandler> userPropertyHandlers, Identity identity, boolean admin) {
		boolean authorizedAbsenceEnabled = lectureModule.isAuthorizedAbsenceEnabled();
		boolean calculateAttendanceRate = lectureModule.isRollCallCalculateAttendanceRateDefaultEnabled();
		boolean absenceDefaultAuthorized = lectureModule.isAbsenceDefaultAuthorized();
		boolean countAuthorizedAbsenceAsAttendant = lectureModule.isCountAuthorizedAbsenceAsAttendant();
		double defaultRequiredAttendanceRate = lectureModule.getRequiredAttendanceRateDefault();
		return lectureBlockRollCallDao.getStatistics(params, userPropertyHandlers, identity, admin, 
				authorizedAbsenceEnabled, absenceDefaultAuthorized, countAuthorizedAbsenceAsAttendant,
				calculateAttendanceRate, defaultRequiredAttendanceRate);
	}

	@Override
	public AggregatedLectureBlocksStatistics aggregatedStatistics(List<? extends LectureBlockStatistics> statistics) {
		boolean countAuthorizedAbsenceAsAttendant = lectureModule.isCountAuthorizedAbsenceAsAttendant();
		return lectureBlockRollCallDao.aggregatedStatistics(statistics, countAuthorizedAbsenceAsAttendant);
	}

	@Override
	public List<LectureBlockAndRollCall> getParticipantLectureBlocks(RepositoryEntryRef entry, IdentityRef participant,
			String teacherSeaparator) {
		return lectureBlockRollCallDao.getParticipantLectureBlockAndRollCalls(entry, participant, teacherSeaparator);
	}

	@Override
	public void syncCalendars(RepositoryEntry entry) {
		RepositoryEntryLectureConfiguration config = getRepositoryEntryLectureConfiguration(entry);
		if(ConfigurationHelper.isSyncTeacherCalendarEnabled(config, lectureModule)) {
			List<LectureBlock> blocks = getLectureBlocks(entry);
			for(LectureBlock block:blocks) {
				List<Identity> teachers = getTeachers(block);
				syncInternalCalendar(block, teachers);
			}
		} else {
			unsyncTeachersCalendar(entry);
		}
		
		if(ConfigurationHelper.isSyncCourseCalendarEnabled(config, lectureModule)) {
			fullSyncCourseCalendar(config.getEntry());
		} else {
			unsyncInternalCalendar(config.getEntry());
		}
		
		/*if(ConfigurationHelper.isSyncParticipantCalendarEnabled(config, lectureModule)) {
			List<LectureBlock> blocks = getLectureBlocks(entry);
			for(LectureBlock block:blocks) {
				List<Identity> participants = getParticipants(block);
				syncInternalCalendar(block, participants);
			}
		} else {
			unsyncParticipantsCalendar(entry);
		}*/
	}

	@Override
	public void syncCalendars(LectureBlock lectureBlock) {
		RepositoryEntryLectureConfiguration config = lectureConfigurationDao.getConfiguration(lectureBlock);
		if(ConfigurationHelper.isSyncTeacherCalendarEnabled(config, lectureModule)) {
			List<Identity> teachers = getTeachers(lectureBlock);
			syncInternalCalendar(lectureBlock, teachers);
		} else {
			List<Identity> teachers = getTeachers(lectureBlock);
			unsyncInternalCalendar(lectureBlock, teachers);
		}

		if(ConfigurationHelper.isSyncCourseCalendarEnabled(config, lectureModule)) {
			syncCourseCalendar(lectureBlock, config.getEntry());
		} else {
			unsyncCourseCalendar(lectureBlock, config.getEntry());
		}
		
		/*if(ConfigurationHelper.isSyncParticipantCalendarEnabled(config, lectureModule)) {
			List<Identity> participants = getParticipants(lectureBlock);
			syncInternalCalendar(lectureBlock, participants);
		} else {
			List<Identity> participants = getParticipants(lectureBlock);
			unsyncInternalCalendar(lectureBlock, participants);
		}*/
	}
	

	private void syncCourseCalendar(LectureBlock lectureBlock, RepositoryEntry entry) {
		Kalendar cal = calendarMgr.getCalendar(CalendarManager.TYPE_COURSE, entry.getOlatResource().getResourceableId().toString());
		syncEvent(lectureBlock, entry, cal);
	}
	
	private void unsyncCourseCalendar(LectureBlock lectureBlock, RepositoryEntry entry) {
		Kalendar cal = calendarMgr.getCalendar(CalendarManager.TYPE_COURSE, entry.getOlatResource().getResourceableId().toString());
		unsyncEvent(lectureBlock, entry, cal);
	}
	
	private void fullSyncCourseCalendar(RepositoryEntry entry) {
		List<LectureBlock> blocks = getLectureBlocks(entry);
		Map<String, LectureBlock> externalIds = blocks.stream()
				.collect(Collectors.toMap(b -> generateExternalId(b, entry), b -> b));
		
		Kalendar cal = calendarMgr.getCalendar(CalendarManager.TYPE_COURSE, entry.getOlatResource().getResourceableId().toString());
		String prefix = generateExternalIdPrefix(entry);
		
		List<KalendarEvent> events = new ArrayList<>(cal.getEvents());
		for(KalendarEvent event:events) {
			String externalId = event.getExternalId();
			if(StringHelper.containsNonWhitespace(externalId) && externalId.startsWith(prefix)) {
				if(externalIds.containsKey(externalId)) {
					if(updateEvent(externalIds.get(externalId), event)) {
						calendarMgr.updateEventFrom(cal, event);
					}
					externalIds.remove(externalId);
				} else {
					calendarMgr.removeEventFrom(cal, event);
				}
			}
		}
		
		// add new calendar events
		List<KalendarEvent> eventsToAdd = new ArrayList<>();
		for(Map.Entry<String, LectureBlock> entryToAdd:externalIds.entrySet()) {
			eventsToAdd.add(createEvent(entryToAdd.getValue(), entry));
		}
		if(eventsToAdd.size() > 0) {
			calendarMgr.addEventTo(cal, eventsToAdd);
		}
	}
	
	private void unsyncInternalCalendar(RepositoryEntry entry) {
		Kalendar cal = calendarMgr.getCalendar(CalendarManager.TYPE_COURSE, entry.getOlatResource().getResourceableId().toString());
		String prefix = generateExternalIdPrefix(entry);
		List<KalendarEvent> events = new ArrayList<>(cal.getEvents());
		for(KalendarEvent event:events) {
			String externalId = event.getExternalId();
			if(StringHelper.containsNonWhitespace(externalId) && externalId.startsWith(prefix)) {
				calendarMgr.removeEventFrom(cal, event);
			}
		}
	}
	
	private void syncInternalCalendar(LectureBlock lectureBlock, List<Identity> identities) {
		RepositoryEntry entry = lectureBlock.getEntry();
		for(Identity identity:identities) {
			Kalendar cal = calendarMgr.getCalendar(CalendarManager.TYPE_USER, identity.getName());
			syncEvent(lectureBlock, entry, cal);
			lectureParticipantSummaryDao.updateCalendarSynchronization(entry, identity);
		}
	}
	
	/**
	 * Try to update the vent. If there is not an event with
	 * the right external identifier, it returns false and do nothing.
	 * 
	 * @param lectureBlock
	 * @param eventExternalId
	 * @param cal
	 * @return
	 */
	private boolean syncEvent(LectureBlock lectureBlock, RepositoryEntry entry, Kalendar cal) {
		boolean updated = false;
		String eventExternalId = generateExternalId(lectureBlock, entry);
		
		for(KalendarEvent event:cal.getEvents()) {
			if(eventExternalId.equals(event.getExternalId())) {
				if(updateEvent(lectureBlock, event)) {
					calendarMgr.updateEventFrom(cal, event);
				}
				return true;
			}
		}
		
		if(!updated) {
			KalendarEvent newEvent = createEvent(lectureBlock, entry);
			calendarMgr.addEventTo(cal, newEvent);
		}
		
		return true;
	}
	
	private void unsyncInternalCalendar(LectureBlock lectureBlock, List<Identity> identities) {
		RepositoryEntry entry = lectureBlock.getEntry();
		for(Identity identity:identities) {
			Kalendar cal = calendarMgr.getCalendar(CalendarManager.TYPE_USER, identity.getName());
			unsyncEvent(lectureBlock, entry, cal);
			lectureParticipantSummaryDao.updateCalendarSynchronization(entry, identity);
		}
	}
	
	private void unsyncEvent(LectureBlock lectureBlock, RepositoryEntry entry, Kalendar cal) {
		String externalId = generateExternalId(lectureBlock, entry);
		List<KalendarEvent> events = new ArrayList<>(cal.getEvents());
		for(KalendarEvent event:events) {
			if(externalId.equals(event.getExternalId())) {
				calendarMgr.removeEventFrom(cal, event);
			}
		}
	}
	
	private void unsyncTeachersCalendar(RepositoryEntry entry) {
		List<Identity> teachers = getTeachers(entry);
		unsyncInternalCalendar(entry, teachers);
	}
	
	/*
	private void unsyncParticipantsCalendar(RepositoryEntry entry) {
		List<Identity> participants = getParticipants(entry);
		unsyncInternalCalendar(entry, participants);
	}
	*/
	
	private void unsyncInternalCalendar(RepositoryEntry entry, List<Identity> identities) {
		String prefix = generateExternalIdPrefix(entry);
		for(Identity identity:identities) {
			Kalendar cal = calendarMgr.getCalendar(CalendarManager.TYPE_USER, identity.getName());
			List<KalendarEvent> events = new ArrayList<>(cal.getEvents());
			for(KalendarEvent event:events) {
				if(event.getExternalId() != null && event.getExternalId().startsWith(prefix)) {
					calendarMgr.removeEventFrom(cal, event);
				}
			}
			lectureParticipantSummaryDao.updateCalendarSynchronization(entry, identity);
		}
	}
	
	private KalendarEvent createEvent(LectureBlock lectureBlock, RepositoryEntry entry) {
		String eventId = UUID.randomUUID().toString();
		String title = lectureBlock.getTitle();
		KalendarEvent event = new KalendarEvent(eventId, null, title, lectureBlock.getStartDate(), lectureBlock.getEndDate());
		event.setExternalId(generateExternalId(lectureBlock, entry));
		event.setLocation(lectureBlock.getLocation());
		updateEventDescription(lectureBlock, event);
		event.setManagedFlags(CAL_MANAGED_FLAGS);
		return event;
	}
	
	private boolean updateEvent(LectureBlock lectureBlock, KalendarEvent event) {
		event.setSubject(lectureBlock.getTitle());
		event.setLocation(lectureBlock.getLocation());
		updateEventDescription(lectureBlock, event);
		event.setBegin(lectureBlock.getStartDate());
		event.setEnd(lectureBlock.getEndDate());
		event.setManagedFlags(CAL_MANAGED_FLAGS);
		return true;
	}
	
	private void updateEventDescription(LectureBlock lectureBlock, KalendarEvent event) {
		StringBuilder descr = new StringBuilder();
		if(StringHelper.containsNonWhitespace(lectureBlock.getDescription())) {
			descr.append(lectureBlock.getDescription());
		}
		if(StringHelper.containsNonWhitespace(lectureBlock.getPreparation())) {
			if(descr.length() > 0) descr.append("\n");
			descr.append(lectureBlock.getPreparation());
		}
		event.setDescription(descr.toString());
	}
	
	private String generateExternalIdPrefix(RepositoryEntry entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("lecture-block-").append(entry.getKey()).append("-");
		return sb.toString();
	}
	
	private String generateExternalId(LectureBlock lectureBlock, RepositoryEntry entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("lecture-block-").append(entry.getKey()).append("-").append(lectureBlock.getKey());
		return sb.toString();
	}
	
	public class LectureReminderTemplate extends MailTemplate {
		
		public LectureReminderTemplate(String subjectTemplate, String bodyTemplate) {
			super(subjectTemplate, bodyTemplate, null);
		}

		@Override
		public void putVariablesInMailContext(VelocityContext vContext, Identity recipient) {
			//
		}
	}
}