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
package org.olat.course.nodes.members;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.olat.basesecurity.GroupRoles;
import org.olat.core.id.Identity;
import org.olat.core.util.StringHelper;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.MembersCourseNode;
import org.olat.group.BusinessGroupService;
import org.olat.modules.ModuleConfiguration;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryService;

/**
 * 
 * <p>Initial date: May 20, 2016
 * @author lmihalkovic, http://www.frentix.com
 */
public class MembersHelpers {
	private MembersHelpers() {
		// CANNOT CREATE
	}

	// -----------------------------------------------------
	
	public static List<Identity> getOwners(RepositoryService repositoryService, RepositoryEntry courseRepositoryEntry) {
		return repositoryService.getMembers(courseRepositoryEntry, GroupRoles.owner.name());
	}

	// -----------------------------------------------------

	public static void addCoaches(ModuleConfiguration moduleConfiguration, CourseGroupManager cgm, BusinessGroupService bgs, List<Identity> list) {
	
		if(moduleConfiguration.has(MembersCourseNode.CONFIG_KEY_COACHES_GROUP)) {
			String coachGroupNames = moduleConfiguration.getStringValue(MembersCourseNode.CONFIG_KEY_COACHES_GROUP);
			List<Long> coachGroupKeys = moduleConfiguration.getList(MembersCourseNode.CONFIG_KEY_COACHES_GROUP_ID, Long.class);
			if(coachGroupKeys == null && StringHelper.containsNonWhitespace(coachGroupNames)) {
				coachGroupKeys = bgs.toGroupKeys(coachGroupNames, cgm.getCourseEntry());
			}
			list.addAll(retrieveCoachesFromGroups(coachGroupKeys, cgm));
		}

		if(moduleConfiguration.has(MembersCourseNode.CONFIG_KEY_COACHES_AREA)) {
			String coachAreaNames = moduleConfiguration.getStringValue(MembersCourseNode.CONFIG_KEY_COACHES_AREA);
			List<Long> coachAreaKeys = moduleConfiguration.getList(MembersCourseNode.CONFIG_KEY_COACHES_AREA_IDS, Long.class);
			if(coachAreaKeys == null && StringHelper.containsNonWhitespace(coachAreaNames)) {
				coachAreaKeys = bgs.toGroupKeys(coachAreaNames, cgm.getCourseEntry());
			}
			list.addAll(retrieveCoachesFromAreas(coachAreaKeys, cgm));
		}
		
		if(moduleConfiguration.anyTrue(MembersCourseNode.CONFIG_KEY_COACHES_COURSE
				, MembersCourseNode.CONFIG_KEY_COACHES_ALL)) {
			list.addAll(retrieveCoachesFromCourse(cgm));
		}
		if(moduleConfiguration.anyTrue(MembersCourseNode.CONFIG_KEY_COACHES_ALL)) {
			list.addAll(retrieveCoachesFromCourseGroups(cgm));
		}
	}
	
	public static List<Identity> retrieveCoachesFromAreas(List<Long> areaKeys, CourseGroupManager cgm) {
		List<Identity> coaches = cgm.getCoachesFromAreas(areaKeys);
		Set<Identity> coachesWithoutDuplicates = new HashSet<Identity>(coaches);
		coaches = new ArrayList<Identity>(coachesWithoutDuplicates);
		return coaches;
	}
	
	public static List<Identity> retrieveCoachesFromGroups(List<Long> groupKeys, CourseGroupManager cgm) {
		List<Identity> coaches = new ArrayList<Identity>(new HashSet<Identity>(cgm.getCoachesFromBusinessGroups(groupKeys)));
		return coaches;
	}
	
	public static List<Identity> retrieveCoachesFromCourse(CourseGroupManager cgm) {
		List<Identity> coaches = cgm.getCoaches();
		return coaches;
	}

	public static List<Identity> retrieveCoachesFromCourseGroups(CourseGroupManager cgm) {
		Set<Identity> uniq = new HashSet<Identity>();
		{
			List<Identity> coaches = cgm.getCoachesFromAreas();
			uniq.addAll(coaches);
		}
		{
			List<Identity> coaches = cgm.getCoachesFromBusinessGroups();
			uniq.addAll(coaches);
		}
		return new ArrayList<Identity>(uniq);
	}
	
	// -----------------------------------------------------
	
	public static void addParticipants(ModuleConfiguration moduleConfiguration, CourseGroupManager cgm, BusinessGroupService bgs, List<Identity> list) {

		if(moduleConfiguration.has(MembersCourseNode.CONFIG_KEY_PARTICIPANTS_GROUP)) {
			String participantGroupNames = moduleConfiguration.getStringValue(MembersCourseNode.CONFIG_KEY_PARTICIPANTS_GROUP);
			List<Long> participantGroupKeys = moduleConfiguration.getList(MembersCourseNode.CONFIG_KEY_PARTICIPANTS_GROUP_ID, Long.class);
			if(participantGroupKeys == null && StringHelper.containsNonWhitespace(participantGroupNames)) {
				participantGroupKeys = bgs.toGroupKeys(participantGroupNames, cgm.getCourseEntry());
			}
			list.addAll(retrieveParticipantsFromGroups(participantGroupKeys, cgm));
		}
		
		if(moduleConfiguration.has(MembersCourseNode.CONFIG_KEY_PARTICIPANTS_AREA)) {
			String participantAreaNames = moduleConfiguration.getStringValue(MembersCourseNode.CONFIG_KEY_PARTICIPANTS_AREA);
			List<Long> participantAreaKeys = moduleConfiguration.getList(MembersCourseNode.CONFIG_KEY_PARTICIPANTS_AREA_ID, Long.class);
			if(participantAreaKeys == null && StringHelper.containsNonWhitespace(participantAreaNames)) {
				participantAreaKeys = bgs.toGroupKeys(participantAreaNames, cgm.getCourseEntry());
			}
			list.addAll(retrieveParticipantsFromAreas(participantAreaKeys, cgm));
		}
		
		if(moduleConfiguration.anyTrue(MembersCourseNode.CONFIG_KEY_PARTICIPANTS_COURSE
				, MembersCourseNode.CONFIG_KEY_PARTICIPANTS_ALL)) {
			list.addAll(retrieveParticipantsFromCourse(cgm));
		}
		if(moduleConfiguration.anyTrue(MembersCourseNode.CONFIG_KEY_PARTICIPANTS_ALL)) {
			list.addAll(retrieveParticipantsFromCourseGroups(cgm));
		}
	}
	
	public static List<Identity> retrieveParticipantsFromAreas(List<Long> areaKeys, CourseGroupManager cgm) {
		List<Identity> participiants = cgm.getParticipantsFromAreas(areaKeys);
		return participiants;
	}
	
	public static List<Identity> retrieveParticipantsFromGroups(List<Long> groupKeys, CourseGroupManager cgm) {
		List<Identity> participiants = cgm.getParticipantsFromBusinessGroups(groupKeys);
		return participiants;
	}
	
	public static List<Identity> retrieveParticipantsFromCourse(CourseGroupManager cgm) {
		List<Identity> participiants = cgm.getParticipants();
		return participiants;
	}
	
	public static List<Identity> retrieveParticipantsFromCourseGroups(CourseGroupManager cgm) {
		Set<Identity> uniq = new HashSet<Identity>();
		{
			List<Identity> participiants = cgm.getParticipantsFromAreas();
			uniq.addAll(participiants);
		}
		{
			List<Identity> participiants = cgm.getParticipantsFromBusinessGroups();
			uniq.addAll(participiants);
		}
		return new ArrayList<Identity>(uniq);
	}
	
}
