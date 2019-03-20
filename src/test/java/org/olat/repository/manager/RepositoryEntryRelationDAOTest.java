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
package org.olat.repository.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.olat.basesecurity.Group;
import org.olat.basesecurity.GroupRoles;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.group.manager.BusinessGroupRelationDAO;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRelationType;
import org.olat.repository.RepositoryService;
import org.olat.repository.model.RepositoryEntryToGroupRelation;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 26.02.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class RepositoryEntryRelationDAOTest extends OlatTestCase {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RepositoryEntryRelationDAO repositoryEntryRelationDao;
	@Autowired
	private BusinessGroupService businessGroupService;
	@Autowired
	private BusinessGroupRelationDAO businessGroupRelationDao;
	
	@Test
	public void getDefaultGroup() {
		RepositoryEntry re = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commitAndCloseSession();
		
		Group group = repositoryEntryRelationDao.getDefaultGroup(re);
		Assert.assertNotNull(group);
	}
	
	@Test
	public void addRole() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("add-role-2-");
		RepositoryEntry re = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commit();

		repositoryEntryRelationDao.addRole(id, re, GroupRoles.owner.name());
		dbInstance.commit();
	}
	
	@Test
	public void hasRole() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("add-role-3-");
		RepositoryEntry re = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commit();
		repositoryEntryRelationDao.addRole(id, re, GroupRoles.owner.name());
		dbInstance.commit();
		
		boolean owner = repositoryEntryRelationDao.hasRole(id, re, GroupRoles.owner.name());
		Assert.assertTrue(owner);
		boolean participant = repositoryEntryRelationDao.hasRole(id, re, GroupRoles.participant.name());
		Assert.assertFalse(participant);

		Group group = repositoryEntryRelationDao.getDefaultGroup(re);
		Assert.assertTrue(repositoryEntryRelationDao.hasRole(id, group, GroupRoles.owner.name()));
	}
	
	@Test
	public void getRoles() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("get-roles-1-");
		RepositoryEntry re = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commit();
		repositoryEntryRelationDao.addRole(id, re, GroupRoles.owner.name());
		dbInstance.commit();
		
		List<String> ownerRoles = repositoryEntryRelationDao.getRoles(id, re);
		Assert.assertNotNull(ownerRoles);
		Assert.assertEquals(1, ownerRoles.size());
		Assert.assertEquals(GroupRoles.owner.name(), ownerRoles.get(0));
	}
	
	@Test
	public void removeRole() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("add-role-4-");
		RepositoryEntry re = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commit();
		repositoryEntryRelationDao.addRole(id, re, GroupRoles.owner.name());
		dbInstance.commit();
		
		//check
		Assert.assertTrue(repositoryEntryRelationDao.hasRole(id, re, GroupRoles.owner.name()));
		
		//remove role
		int removeRoles = repositoryEntryRelationDao.removeRole(id, re, GroupRoles.owner.name());
		dbInstance.commitAndCloseSession();
		Assert.assertEquals(1, removeRoles);
		
		//check
		boolean owner = repositoryEntryRelationDao.hasRole(id, re, GroupRoles.owner.name());
		Assert.assertFalse(owner);
		boolean participant = repositoryEntryRelationDao.hasRole(id, re, GroupRoles.participant.name());
		Assert.assertFalse(participant);
	}
	
	@Test
	public void getMembersAndCountMembers() {
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsRndUser("member-1-");
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsRndUser("member-2-");
		RepositoryEntry re = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commit();
		repositoryEntryRelationDao.addRole(id1, re, GroupRoles.owner.name());
		repositoryEntryRelationDao.addRole(id2, re, GroupRoles.participant.name());
		dbInstance.commit();

		//all members
		List<Identity> members = repositoryEntryRelationDao.getMembers(re, RepositoryEntryRelationType.defaultGroup);
		int numOfMembers = repositoryEntryRelationDao.countMembers(re);
		Assert.assertNotNull(members);
		Assert.assertEquals(2, members.size());
		Assert.assertEquals(2, numOfMembers);
		Assert.assertTrue(members.contains(id1));
		Assert.assertTrue(members.contains(id2));
		
		//participant
		List<Identity> participants = repositoryEntryRelationDao.getMembers(re, RepositoryEntryRelationType.defaultGroup, GroupRoles.participant.name());
		int numOfParticipants = repositoryEntryRelationDao.countMembers(re, GroupRoles.participant.name());
		Assert.assertNotNull(participants);
		Assert.assertEquals(1, participants.size());
		Assert.assertEquals(1, numOfParticipants);
		Assert.assertTrue(members.contains(id2));
	}
	
	@Test
	public void countMembers_list() {
		//create a repository entry with a business group
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsRndUser("member-1-");
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsRndUser("member-2-");
		Identity id3 = JunitTestHelper.createAndPersistIdentityAsRndUser("member-3-");
		Identity id4 = JunitTestHelper.createAndPersistIdentityAsRndUser("member-4-");
		RepositoryEntry re = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commit();
		
		repositoryEntryRelationDao.addRole(id1, re, GroupRoles.owner.name());
		repositoryEntryRelationDao.addRole(id2, re, GroupRoles.participant.name());
		BusinessGroup group = businessGroupService.createBusinessGroup(null, "count relation 1", "tg", null, null, false, false, re);
	    businessGroupRelationDao.addRole(id2, group, GroupRoles.coach.name());
	    businessGroupRelationDao.addRole(id3, group, GroupRoles.coach.name());
	    businessGroupRelationDao.addRole(id4, group, GroupRoles.coach.name());
	    dbInstance.commitAndCloseSession();
	    
		//get the number of members
	    int numOfMembers = repositoryService.countMembers(Collections.singletonList(re), null);
		Assert.assertEquals(4, numOfMembers);
		
		//get the number of members without id1
	    int numOfMembersWithExclude = repositoryService.countMembers(Collections.singletonList(re), id1);
		Assert.assertEquals(3, numOfMembersWithExclude);
	}
	
	@Test
	public void getEnrollmentDate() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("enroll-date-1-");
		Identity wid = JunitTestHelper.createAndPersistIdentityAsRndUser("not-enroll-date-1-");
		RepositoryEntry re = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commit();
		repositoryEntryRelationDao.addRole(id, re, GroupRoles.owner.name());
		repositoryEntryRelationDao.addRole(id, re, GroupRoles.participant.name());
		dbInstance.commit();
		
		//enrollment date
		Date enrollmentDate = repositoryEntryRelationDao.getEnrollmentDate(re, id);
		Assert.assertNotNull(enrollmentDate);
		
		//this user isn't enrolled
		Date withoutEnrollmentDate = repositoryEntryRelationDao.getEnrollmentDate(re, wid);
		Assert.assertNull(withoutEnrollmentDate);
		
		//as participant
		Date participantEnrollmentDate = repositoryEntryRelationDao.getEnrollmentDate(re, id, GroupRoles.participant.name());
		Assert.assertNotNull(participantEnrollmentDate);
		//as owner
		Date ownerEnrollmentDate = repositoryEntryRelationDao.getEnrollmentDate(re, id, GroupRoles.owner.name());
		Assert.assertNotNull(ownerEnrollmentDate);
		//is not enrolled as coached
		Date coachEnrollmentDate = repositoryEntryRelationDao.getEnrollmentDate(re, id, GroupRoles.coach.name());
		Assert.assertNull(coachEnrollmentDate);
	}
	
	@Test
	public void getEnrollmentDates() {
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsRndUser("enroll-date-2-");
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsRndUser("enroll-date-3-");
		Identity wid = JunitTestHelper.createAndPersistIdentityAsRndUser("not-enroll-date-2-");
		RepositoryEntry re = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commit();
		repositoryEntryRelationDao.addRole(id1, re, GroupRoles.owner.name());
		repositoryEntryRelationDao.addRole(id2, re, GroupRoles.participant.name());
		dbInstance.commit();
		
		//enrollment date
		Map<Long,Date> enrollmentDates = repositoryEntryRelationDao.getEnrollmentDates(re);
		Assert.assertNotNull(enrollmentDates);
		Assert.assertEquals(2, enrollmentDates.size());
		Assert.assertTrue(enrollmentDates.containsKey(id1.getKey()));
		Assert.assertTrue(enrollmentDates.containsKey(id2.getKey()));
		Assert.assertFalse(enrollmentDates.containsKey(wid.getKey()));
	}

	@Test
	public void getEnrollmentDates_emptyCourse() {
		//enrollment of an empty course
		RepositoryEntry notEnrolledRe = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commit();
		Map<Long,Date> notEnrollmentDates = repositoryEntryRelationDao.getEnrollmentDates(notEnrolledRe);
		Assert.assertNotNull(notEnrollmentDates);
		Assert.assertEquals(0, notEnrollmentDates.size());
	}
	
	@Test
	public void getAuthorKeys() {
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsRndUser("auth-1-");
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsRndUser("part-2-");
		RepositoryEntry re = repositoryService.create("Rei Ayanami", "rel", "rel", null, null);
		dbInstance.commit();
		repositoryEntryRelationDao.addRole(id1, re, GroupRoles.owner.name());
		repositoryEntryRelationDao.addRole(id2, re, GroupRoles.participant.name());
		dbInstance.commit();
		
		List<Long> authorKeys = repositoryEntryRelationDao.getAuthorKeys(re);
		Assert.assertNotNull(authorKeys);
		Assert.assertEquals(1, authorKeys.size());
		Assert.assertEquals(id1.getKey(), authorKeys.get(0));
	}
	
	@Test
	public void isMember() {
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser("re-member-lc-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser("re-member-lc-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		BusinessGroup group = businessGroupService.createBusinessGroup(null, "memberg", "tg", null, null, false, false, re);
	    businessGroupRelationDao.addRole(id1, group, GroupRoles.coach.name());
		dbInstance.commitAndCloseSession();

		//id1 is member
		boolean member1 = repositoryEntryRelationDao.isMember(id1, re);
		Assert.assertTrue(member1);
		//id2 is not member
		boolean member2 = repositoryEntryRelationDao.isMember(id2, re);
		Assert.assertFalse(member2);
	}
	
	@Test
	public void isMember_v2() {
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser("re-is-member-1-lc-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser("re-is-member-2-lc-" + UUID.randomUUID().toString());
		Identity id3 = JunitTestHelper.createAndPersistIdentityAsUser("re-is-member-3-lc-" + UUID.randomUUID().toString());
		Identity id4 = JunitTestHelper.createAndPersistIdentityAsUser("re-is-member-4-lc-" + UUID.randomUUID().toString());
		Identity id5 = JunitTestHelper.createAndPersistIdentityAsUser("re-is-member-5-lc-" + UUID.randomUUID().toString());
		Identity id6 = JunitTestHelper.createAndPersistIdentityAsUser("re-is-member-6-lc-" + UUID.randomUUID().toString());
		Identity idNull = JunitTestHelper.createAndPersistIdentityAsUser("re-is-member-null-lc-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		BusinessGroup group1 = businessGroupService.createBusinessGroup(null, "member-1-g", "tg", null, null, false, false, re);
		BusinessGroup group2 = businessGroupService.createBusinessGroup(null, "member-2-g", "tg", null, null, false, false, re);
		BusinessGroup group3 = businessGroupService.createBusinessGroup(null, "member-3-g", "tg", null, null, true, false, re);
		BusinessGroup groupNull = businessGroupService.createBusinessGroup(null, "member-null-g", "tg", null, null, true, false, null);
		repositoryEntryRelationDao.addRole(id1, re, GroupRoles.owner.name());
		repositoryEntryRelationDao.addRole(id2, re, GroupRoles.coach.name());
		repositoryEntryRelationDao.addRole(id3, re, GroupRoles.participant.name());
	    businessGroupRelationDao.addRole(id4, group1, GroupRoles.coach.name());
	    businessGroupRelationDao.addRole(id5, group2, GroupRoles.participant.name());
	    businessGroupRelationDao.addRole(id6, group3, GroupRoles.waiting.name());
	    businessGroupRelationDao.addRole(idNull, groupNull, GroupRoles.participant.name());
		dbInstance.commitAndCloseSession();

		//id1 is owner
		boolean member1 = repositoryEntryRelationDao.isMember(id1, re);
		Assert.assertTrue(member1);
		//id2 is tutor
		boolean member2 = repositoryEntryRelationDao.isMember(id2, re);
		Assert.assertTrue(member2);
		//id3 is repo participant
		boolean member3 = repositoryEntryRelationDao.isMember(id3, re);
		Assert.assertTrue(member3);
		//id4 is group coach
		boolean member4= repositoryEntryRelationDao.isMember(id4, re);
		Assert.assertTrue(member4);
		//id5 is group participant
		boolean member5 = repositoryEntryRelationDao.isMember(id5, re);
		Assert.assertTrue(member5);
		//id6 is waiting
		boolean member6 = repositoryEntryRelationDao.isMember(id6, re);
		Assert.assertFalse(member6);
		//idNull is not member
		boolean memberNull = repositoryEntryRelationDao.isMember(idNull, re);
		Assert.assertFalse(memberNull);
	}
	
	@Test
	public void filterMembership() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-member-lc-" + UUID.randomUUID().toString());
		RepositoryEntry re1 = JunitTestHelper.createAndPersistRepositoryEntry();
		BusinessGroup group = businessGroupService.createBusinessGroup(null, "memberg", "tg", null, null, false, false, re1);
	    businessGroupRelationDao.addRole(id, group, GroupRoles.coach.name());
	    RepositoryEntry re2 = JunitTestHelper.createAndPersistRepositoryEntry();
		repositoryEntryRelationDao.addRole(id, re2, GroupRoles.owner.name());
	    RepositoryEntry re3 = JunitTestHelper.createAndPersistRepositoryEntry();
		repositoryEntryRelationDao.addRole(id, re3, GroupRoles.waiting.name());
		dbInstance.commitAndCloseSession();

		//id is member
		List<Long> entries = new ArrayList<>();
		entries.add(re1.getKey());
		entries.add(re2.getKey());
		entries.add(re3.getKey());
		entries.add(502l);
		repositoryEntryRelationDao.filterMembership(id, entries);

		Assert.assertTrue(entries.contains(re1.getKey()));
		Assert.assertTrue(entries.contains(re2.getKey()));
		//waiting list
		Assert.assertFalse(entries.contains(re3.getKey()));
		//unkown
		Assert.assertFalse(entries.contains(502l));
		
		//check against empty value
		List<Long> empyEntries = new ArrayList<>();
		repositoryEntryRelationDao.filterMembership(id, empyEntries);
		Assert.assertTrue(empyEntries.isEmpty());
	}
	
	@Test
	public void countRelations() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-member-lc-" + UUID.randomUUID().toString());
		RepositoryEntry re1 = JunitTestHelper.createAndPersistRepositoryEntry();
		RepositoryEntry re2 = JunitTestHelper.createAndPersistRepositoryEntry();

		BusinessGroup group = businessGroupService.createBusinessGroup(null, "count relation 1", "tg", null, null, false, false, re1);
	    businessGroupRelationDao.addRole(id, group, GroupRoles.coach.name());
	    businessGroupService.addResourceTo(group, re2);
	    dbInstance.commitAndCloseSession();

	    int numOfRelations = repositoryEntryRelationDao.countRelations(group.getBaseGroup());
	    Assert.assertEquals(2, numOfRelations);
	}
	
	@Test
	public void getRelations() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-member-lc-" + UUID.randomUUID().toString());
		RepositoryEntry re1 = JunitTestHelper.createAndPersistRepositoryEntry();
		RepositoryEntry re2 = JunitTestHelper.createAndPersistRepositoryEntry();

		BusinessGroup group = businessGroupService.createBusinessGroup(null, "get relations", "tg", null, null, false, false, re1);
	    businessGroupRelationDao.addRole(id, group, GroupRoles.coach.name());
	    businessGroupService.addResourceTo(group, re2);
	    dbInstance.commitAndCloseSession();
	    
	    //get the relations from the business group's base group to the 2 repository entries
	    List<Group> groups = Collections.singletonList(group.getBaseGroup());
	    List<RepositoryEntryToGroupRelation> relations = repositoryEntryRelationDao.getRelations(groups);
	    Assert.assertNotNull(relations);
	    Assert.assertEquals(2, relations.size());
		Assert.assertTrue(relations.get(0).getEntry().equals(re1) || relations.get(0).getEntry().equals(re2));
		Assert.assertTrue(relations.get(1).getEntry().equals(re1) || relations.get(1).getEntry().equals(re2));
	}
	
	@Test
	public void getIdentitiesWithRole() {
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsRndUser("id-role-1-");
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsRndUser("id-role-2-");
		Identity id3 = JunitTestHelper.createAndPersistIdentityAsRndUser("id-role-3-");
		Identity id4 = JunitTestHelper.createAndPersistIdentityAsRndUser("id-role-4-");
		
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		repositoryEntryRelationDao.addRole(id1, re, GroupRoles.coach.name());
		repositoryEntryRelationDao.addRole(id2, re, GroupRoles.participant.name());

		BusinessGroup group = businessGroupService.createBusinessGroup(null, "get relations", "tg", null, null, false, false, re);
	    businessGroupRelationDao.addRole(id3, group, GroupRoles.coach.name());
	    businessGroupRelationDao.addRole(id4, group, GroupRoles.participant.name());
	    businessGroupService.addResourceTo(group, re);
	    dbInstance.commitAndCloseSession();

	    List<Identity> relations = repositoryEntryRelationDao.getIdentitiesWithRole(GroupRoles.coach.name());
	    Assert.assertNotNull(relations);
	    Assert.assertTrue(relations.contains(id1));
	    Assert.assertFalse(relations.contains(id2));
	    Assert.assertTrue(relations.contains(id3));
	    Assert.assertFalse(relations.contains(id4));
	}
	
	@Test
	public void removeMembers() {
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser("re-member-rm-1-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser("re-member-rm-2-" + UUID.randomUUID().toString());
		Identity id3 = JunitTestHelper.createAndPersistIdentityAsUser("re-member-rm-3-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		repositoryEntryRelationDao.addRole(id1, re, GroupRoles.owner.name());
		repositoryEntryRelationDao.addRole(id2, re, GroupRoles.participant.name());
		repositoryEntryRelationDao.addRole(id3, re, GroupRoles.owner.name());
	    dbInstance.commitAndCloseSession();
	    
	    List<Identity> membersToRemove = new ArrayList<>(2);
	    membersToRemove.add(id2);
	    membersToRemove.add(id3);
		boolean removed = repositoryEntryRelationDao.removeMembers(re, membersToRemove);
		Assert.assertTrue(removed);
		dbInstance.commitAndCloseSession();
		
		List<Identity> members = repositoryEntryRelationDao.getMembers(re, RepositoryEntryRelationType.defaultGroup);
		Assert.assertNotNull(members);
	    Assert.assertEquals(1, members.size());
	    Assert.assertTrue(members.contains(id1));
	}
	
	@Test
	public void removeRelation_specificOne() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-member-lc-" + UUID.randomUUID().toString());
		RepositoryEntry re1 = JunitTestHelper.createAndPersistRepositoryEntry();
		RepositoryEntry re2 = JunitTestHelper.createAndPersistRepositoryEntry();

		BusinessGroup group = businessGroupService.createBusinessGroup(null, "remove relation", "tg", null, null, false, false, re1);
	    businessGroupRelationDao.addRole(id, group, GroupRoles.coach.name());
	    businessGroupService.addResourceTo(group, re2);
	    dbInstance.commitAndCloseSession();
	    
	    int numOfRelations = repositoryEntryRelationDao.removeRelation(group.getBaseGroup(), re2);
	    Assert.assertEquals(1, numOfRelations);
	    dbInstance.commitAndCloseSession();
	    
	    List<Group> groups = Collections.singletonList(group.getBaseGroup());
	    List<RepositoryEntryToGroupRelation> relations = repositoryEntryRelationDao.getRelations(groups);
	    Assert.assertEquals(1, relations.size());
	    RepositoryEntry relationRe1 = relations.get(0).getEntry();
	    Assert.assertNotNull(relationRe1);
	    Assert.assertEquals(re1, relationRe1);
	}
	
	@Test
	public void removeRelations_repositoryEntrySide() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-member-lc-" + UUID.randomUUID().toString());
		RepositoryEntry re1 = JunitTestHelper.createAndPersistRepositoryEntry();
		RepositoryEntry re2 = JunitTestHelper.createAndPersistRepositoryEntry();

		BusinessGroup group = businessGroupService.createBusinessGroup(null, "remove all relations", "tg", null, null, false, false, re1);
	    businessGroupRelationDao.addRole(id, group, GroupRoles.coach.name());
	    businessGroupService.addResourceTo(group, re2);
	    dbInstance.commitAndCloseSession();
	    
	    int numOfRelations = repositoryEntryRelationDao.removeRelations(re2);
	    Assert.assertEquals(2, numOfRelations);//default relation + relation to group
	    dbInstance.commitAndCloseSession();
	    
	    List<Group> groups = Collections.singletonList(group.getBaseGroup());
	    List<RepositoryEntryToGroupRelation> relations = repositoryEntryRelationDao.getRelations(groups);
	    Assert.assertEquals(1, relations.size());
	    RepositoryEntry relationRe1 = relations.get(0).getEntry();
	    Assert.assertNotNull(relationRe1);
	    Assert.assertEquals(re1, relationRe1);
	}
	
	@Test
	public void removeRelation_byGroup() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-member-lc-" + UUID.randomUUID().toString());
		RepositoryEntry re1 = JunitTestHelper.createAndPersistRepositoryEntry();
		RepositoryEntry re2 = JunitTestHelper.createAndPersistRepositoryEntry();

		BusinessGroup group = businessGroupService.createBusinessGroup(null, "remove relation by group", "tg", null, null, false, false, re1);
	    businessGroupRelationDao.addRole(id, group, GroupRoles.coach.name());
	    businessGroupService.addResourceTo(group, re2);
	    dbInstance.commitAndCloseSession();
	    
	    int numOfRelations = repositoryEntryRelationDao.removeRelation(group.getBaseGroup());
	    Assert.assertEquals(2, numOfRelations);
	    dbInstance.commitAndCloseSession();
	    
	    List<Group> groups = Collections.singletonList(group.getBaseGroup());
	    List<RepositoryEntryToGroupRelation> relations = repositoryEntryRelationDao.getRelations(groups);
	    Assert.assertEquals(0, relations.size());
	}
}
