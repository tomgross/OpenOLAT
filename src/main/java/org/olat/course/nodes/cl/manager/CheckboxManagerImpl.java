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
package org.olat.course.nodes.cl.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.olat.basesecurity.Group;
import org.olat.basesecurity.IdentityImpl;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.nodes.CheckListCourseNode;
import org.olat.course.nodes.cl.CheckboxManager;
import org.olat.course.nodes.cl.model.AssessmentBatch;
import org.olat.course.nodes.cl.model.AssessmentData;
import org.olat.course.nodes.cl.model.Checkbox;
import org.olat.course.nodes.cl.model.CheckboxList;
import org.olat.course.nodes.cl.model.DBCheck;
import org.olat.course.nodes.cl.model.DBCheckbox;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.group.BusinessGroup;
import org.olat.modules.vitero.model.GroupRole;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 06.02.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class CheckboxManagerImpl implements CheckboxManager {
	
	@Autowired
	private DB dbInstance;
	
	@Override
	public DBCheckbox createDBCheckbox(String checkboxId, OLATResourceable ores, String resSubPath) {
		DBCheckbox checkbox = new DBCheckbox();
		checkbox.setCreationDate(new Date());
		checkbox.setLastModified(new Date());
		checkbox.setCheckboxId(checkboxId);
		checkbox.setResName(ores.getResourceableTypeName());
		checkbox.setResId(ores.getResourceableId());
		checkbox.setResSubPath(resSubPath);
		dbInstance.getCurrentEntityManager().persist(checkbox);
		return checkbox;
	}

	@Override
	public List<DBCheckbox> loadCheckbox(OLATResourceable ores, String resSubPath) {
		StringBuilder sb = new StringBuilder();
		sb.append("select box from clcheckbox box where box.resName=:resName and box.resId=:resId");
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			sb.append(" and box.resSubPath=:resSubPath");
		}
		
		TypedQuery<DBCheckbox> query = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), DBCheckbox.class)
			.setParameter("resName", ores.getResourceableTypeName())
			.setParameter("resId", ores.getResourceableId());
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			query.setParameter("resSubPath", resSubPath);
		}
		return query.getResultList();
	}
	
	@Override
	public DBCheckbox loadCheckbox(OLATResourceable ores, String resSubPath, String checkboxId) {
		StringBuilder sb = new StringBuilder();
		sb.append("select box from clcheckbox box")
		  .append(" where box.checkboxId=:checkboxId and box.resName=:resName and box.resId=:resId")
		  .append(" and box.resSubPath=:resSubPath");

		
		List<DBCheckbox> box = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), DBCheckbox.class)
			.setParameter("resName", ores.getResourceableTypeName())
			.setParameter("resId", ores.getResourceableId())
			.setParameter("resSubPath", resSubPath)
			.setParameter("checkboxId", checkboxId)
			.getResultList();
		if(box.isEmpty()) {
			return null;
		}
		return box.get(0);
	}
	
	private DBCheckbox loadForUpdate(DBCheckbox checkbox) {
		dbInstance.getCurrentEntityManager().detach(checkbox);

		StringBuilder sb = new StringBuilder();
		sb.append("select box from clcheckbox box where box.key=:key");

		
		List<DBCheckbox> box = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), DBCheckbox.class)
			.setParameter("key", checkbox.getKey())
			.getResultList();
		if(box.isEmpty()) {
			return null;
		}
		return box.get(0);
	}
	
	private List<DBCheckbox> loadCheckbox(OLATResourceable ores, String resSubPath, Collection<String> uuids) {
		if(uuids == null || uuids.isEmpty()) return Collections.emptyList();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select box from clcheckbox box")
		  .append(" where box.checkboxId in (:checkboxId) and box.resName=:resName and box.resId=:resId");
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			sb.append(" and box.resSubPath=:resSubPath");
		}
		
		TypedQuery<DBCheckbox> query = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), DBCheckbox.class)
			.setParameter("resName", ores.getResourceableTypeName())
			.setParameter("resId", ores.getResourceableId())
			.setParameter("checkboxId", uuids);
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			query.setParameter("resSubPath", resSubPath);
		}
		return query.getResultList();
	}

	@Override
	public void removeCheckbox(DBCheckbox checkbox) {
		DBCheckbox ref = dbInstance.getCurrentEntityManager()
				.getReference(DBCheckbox.class, checkbox.getKey());
		dbInstance.getCurrentEntityManager().remove(ref);
	}
	
	@Override
	public List<DBCheck> loadCheck(OLATResourceable ores, String resSubPath) {
		StringBuilder sb = new StringBuilder();
		sb.append("select check from clcheck check")
		  .append(" inner join fetch check.checkbox box")
		  .append(" where box.resName=:resName and box.resId=:resId");
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			sb.append(" and box.resSubPath=:resSubPath");
		}
		
		TypedQuery<DBCheck> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), DBCheck.class)
				.setParameter("resName", ores.getResourceableTypeName())
				.setParameter("resId", ores.getResourceableId());
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			query.setParameter("resSubPath", resSubPath);
		}
		return query.getResultList();
	}

	@Override
	public List<DBCheck> loadCheck(Identity identity, OLATResourceable ores, String resSubPath) {
		StringBuilder sb = new StringBuilder();
		sb.append("select check from clcheck check")
		  .append(" inner join fetch check.checkbox box")
		  .append(" where check.identity.key=:identityKey")
		  .append(" and box.resName=:resName and box.resId=:resId");
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			sb.append(" and box.resSubPath=:resSubPath");
		}
		
		TypedQuery<DBCheck> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), DBCheck.class)
				.setParameter("resName", ores.getResourceableTypeName())
				.setParameter("resId", ores.getResourceableId())
				.setParameter("identityKey", identity.getKey());
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			query.setParameter("resSubPath", resSubPath);
		}
		return query.getResultList();
	}

	@Override
	public void syncCheckbox(CheckboxList checkboxList, OLATResourceable ores, String resSubPath) {
		List<DBCheckbox> dbCheckboxList = loadCheckbox(ores, resSubPath);
		Map<String,DBCheckbox> uuids = new HashMap<>();
		for(DBCheckbox dbCheckbox:dbCheckboxList) {
			uuids.put(dbCheckbox.getCheckboxId(), dbCheckbox);
		}

		if(checkboxList != null && checkboxList.getList() != null) {
			List<Checkbox> resCheckboxList = checkboxList.getList();
			for(Checkbox resCheckbox:resCheckboxList) {
				String resUuid = resCheckbox.getCheckboxId();
				if(uuids.containsKey(resUuid)) {
					uuids.remove(resUuid);//already synched
				} else {
					createDBCheckbox(resUuid, ores, resSubPath);
				}
			}
		}
		
		for(DBCheckbox dbCheckbox:uuids.values()) {
			System.out.println("Remove them??? " + dbCheckbox.getCheckboxId());
		}
	}
	
	@Override
	public void deleteCheckbox(OLATResourceable ores, String resSubPath) {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from clcheck check")
		  .append(" where check.checkbox.key in (")
		  .append("   select box.key from clcheckbox box where box.resName=:resName and box.resId=:resId and box.resSubPath=:resSubPath")
		  .append(" )");
		dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString())
			.setParameter("resName", ores.getResourceableTypeName())
			.setParameter("resId", ores.getResourceableId())
			.setParameter("resSubPath", resSubPath)
			.executeUpdate();
		
		StringBuilder sb2 = new StringBuilder();
		sb2.append("delete from clcheckbox box")
		   .append(" where box.resName=:resName and box.resId=:resId and box.resSubPath=:resSubPath");
		dbInstance.getCurrentEntityManager()
			.createQuery(sb2.toString())
			.setParameter("resName", ores.getResourceableTypeName())
			.setParameter("resId", ores.getResourceableId())
			.setParameter("resSubPath", resSubPath)
			.executeUpdate();
	}

	@Override
	public void check(DBCheckbox checkbox, Identity owner, Float score, Boolean checked) {
		DBCheck currentCheck = loadCheck(checkbox, owner);
		if(currentCheck == null) {
			DBCheckbox lockedCheckbox = loadForUpdate(checkbox);
			if(lockedCheckbox != null) {
				//locked -> reload to make sure nobody create it
				DBCheck reloadedCheck = loadCheck(checkbox, owner);
				if(reloadedCheck == null) {
					createCheck(lockedCheckbox, owner, score, checked);
				} else {
					currentCheck = reloadedCheck;
				}
			}
		}
		if(currentCheck != null) {
			currentCheck.setScore(score);
			currentCheck.setChecked(checked);
		}
		dbInstance.commit();	
	}
	
	@Override
	public void check(OLATResourceable ores, String resSubPath, List<AssessmentBatch> batch) {
		Collections.sort(batch, new BatchComparator());
		EntityManager em = dbInstance.getCurrentEntityManager();
		
		Set<String> dbBoxUuids = new HashSet<>();
		for(AssessmentBatch row:batch) {
			dbBoxUuids.add(row.getCheckboxId());
		}
		List<DBCheckbox> boxes = loadCheckbox(ores, resSubPath, dbBoxUuids);
		Map<String, DBCheckbox> uuidToBox = new HashMap<>();
		for(DBCheckbox box:boxes) {
			uuidToBox.put(box.getCheckboxId(), box);
		}
		
		Identity currentIdentity = null;
		for(AssessmentBatch row:batch) {
			
			Long identityKey = row.getIdentityKey();
			if(currentIdentity == null || !identityKey.equals(currentIdentity.getKey())) {
				currentIdentity = em.getReference(IdentityImpl.class, identityKey);
			}
			
			boolean check = row.getCheck();
			DBCheckbox checkbox = uuidToBox.get(row.getCheckboxId());
			DBCheck currentCheck = loadCheck(checkbox, currentIdentity);
			if(check) {
				
				if(currentCheck == null) {
					DBCheckbox lockedCheckbox = loadForUpdate(checkbox);
					if(lockedCheckbox != null) {
						//locked -> reload to make sure nobody create it
						DBCheck reloaedCheck = loadCheck(checkbox, currentIdentity);
						if(reloaedCheck == null) {
							createCheck(lockedCheckbox, currentIdentity, row.getScore(), new Boolean(check));
						} else {
							currentCheck = reloaedCheck;
						}
					}
					dbInstance.commit();
				}
				
				if(currentCheck != null) {
					currentCheck.setScore(row.getScore());
					currentCheck.setChecked(new Boolean(check));
					em.merge(currentCheck);
				}
				
				//save check
			} else if(currentCheck != null) {
				currentCheck.setChecked(Boolean.FALSE);
				currentCheck.setScore(new Float(0f));
				em.merge(currentCheck);
			}	
		}
	}
	
	protected DBCheck loadCheck(DBCheckbox checkbox, Identity identity) {
		if(checkbox == null || identity == null) return null;
		
		StringBuilder sb = new StringBuilder();
		sb.append("select check from clcheck as check")
		  .append(" where check.identity.key=:identityKey and check.checkbox.key=:checkboxKey");
		
		List<DBCheck> checks = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), DBCheck.class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("checkboxKey", checkbox.getKey())
				.getResultList();
		
		if(checks.isEmpty()) {
			return null;
		}
		return checks.get(0);
	}

	protected DBCheck createCheck(DBCheckbox checkbox, Identity owner, Float score, Boolean checked) {
		DBCheck check = new DBCheck();
		check.setCreationDate(new Date());
		check.setLastModified(new Date());
		check.setIdentity(owner);
		check.setCheckbox(checkbox);
		check.setChecked(checked);
		check.setScore(score);
		dbInstance.getCurrentEntityManager().persist(check);
		return check;
	}
	
	@Override
	public int countChecks(OLATResourceable ores, String resSubPath) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(check) from clcheck check")
		  .append(" inner join check.checkbox box")
		  .append(" inner join check.identity ident")
		  .append(" where box.resName=:resName and box.resId=:resId");
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			sb.append(" and box.resSubPath=:resSubPath");
		}
		
		TypedQuery<Number> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("resName", ores.getResourceableTypeName())
				.setParameter("resId", ores.getResourceableId());
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			query.setParameter("resSubPath", resSubPath);
		}
		
		Number numOfChecks = query.getSingleResult();
		return numOfChecks.intValue();
	}

	@Override
	public int countChecked(Identity identity, OLATResourceable ores, String resSubPath) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(check) from clcheck check")
		  .append(" inner join check.checkbox box")
		  .append(" where check.identity.key=:identityKey and box.resName=:resName and box.resId=:resId")
		  .append("  and check.checked=true");
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			sb.append(" and box.resSubPath=:resSubPath");
		}
		
		TypedQuery<Number> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("resName", ores.getResourceableTypeName())
				.setParameter("resId", ores.getResourceableId())
				.setParameter("identityKey", identity.getKey());
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			query.setParameter("resSubPath", resSubPath);
		}
		
		Number numOfChecks = query.getSingleResult();
		return numOfChecks.intValue();
	}

	@Override
	public float calculateScore(Identity identity, OLATResourceable ores, String resSubPath) {
		StringBuilder sb = new StringBuilder();
		sb.append("select sum(check.score) from clcheck check")
		  .append(" inner join check.checkbox box")
		  .append(" where check.identity.key=:identityKey and check.checked=true and box.resName=:resName and box.resId=:resId");
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			sb.append(" and box.resSubPath=:resSubPath");
		}
		
		TypedQuery<Number> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("resName", ores.getResourceableTypeName())
				.setParameter("resId", ores.getResourceableId())
				.setParameter("identityKey", identity.getKey());
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			query.setParameter("resSubPath", resSubPath);
		}
		
		Number numOfChecks = query.getSingleResult();
		return numOfChecks == null ? 0.0f : numOfChecks.floatValue();
	}

	@Override
	public List<AssessmentData> getAssessmentDatas(OLATResourceable ores, String resSubPath, RepositoryEntry re, List<BusinessGroup> businessGroups) {
		StringBuilder sb = new StringBuilder();
		sb.append("select check from clcheck check")
		  .append(" inner join fetch check.checkbox box")
		  .append(" inner join fetch check.identity ident")
		  .append(" inner join fetch ident.user identUser")
		  .append(" where box.resName=:resName and box.resId=:resId");
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			sb.append(" and box.resSubPath=:resSubPath");
		}

		boolean hasBusinessGroups = businessGroups != null && businessGroups.size() > 0;
		if(hasBusinessGroups) {
			sb.append(" and ");
			if(re != null) {
				sb.append(" ( ");
			}
			
			sb.append(" check.identity.key in ( select membership.identity.key from bgroupmember membership ")
			  .append("   where membership.group in (:baseGroups) and membership.role='").append(GroupRole.participant).append("'")
			  .append(" )");
		}
		if(re != null) {
			if(hasBusinessGroups) {
				sb.append(" or ");
			} else {
				sb.append(" and ");
			}
			
			sb.append(" check.identity.key in ( select membership.identity.key from repoentrytogroup as rel, bgroup as reBaseGroup, bgroupmember membership ")
			  .append("   where rel.entry.key=:repoKey and rel.group=reBaseGroup and membership.group=reBaseGroup and membership.role='").append(GroupRole.participant).append("'")
			  .append(" )");

			if(hasBusinessGroups) {
				sb.append(" ) ");
			} 
		}

		TypedQuery<DBCheck> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), DBCheck.class)
				.setParameter("resName", ores.getResourceableTypeName())
				.setParameter("resId", ores.getResourceableId());
		if(StringHelper.containsNonWhitespace(resSubPath)) {
			query.setParameter("resSubPath", resSubPath);
		}

		if(hasBusinessGroups) {
			List<Group> groups = new ArrayList<>(businessGroups.size());
			for(BusinessGroup businessGroup:businessGroups) {
				groups.add(businessGroup.getBaseGroup());
			}
			query.setParameter("baseGroups", groups);
		}
		if(re != null) {
			query.setParameter("repoKey", re.getKey());
		}
		
		List<DBCheck> checks = query.getResultList();
		Map<Long, AssessmentData> identToBox = new HashMap<>();
		for(DBCheck check:checks) {
			AssessmentData data = identToBox.get(check.getIdentity().getKey());
			if(data == null) {
				data = new AssessmentData(check.getIdentity());
				identToBox.put(check.getIdentity().getKey(), data);
			}
			data.getChecks().add(check);
		}
		return new ArrayList<>(identToBox.values());
	}

	@Override
	public VFSContainer getFileContainer(CourseEnvironment courseEnv, CheckListCourseNode cNode) {
		String path = courseEnv.getCourseBaseContainer().getRelPath() + "/" + CheckListCourseNode.FOLDER_NAME + "/" + cNode.getIdent();
		return new OlatRootFolderImpl(path, null);
	}

	@Override
	public File getFileDirectory(CourseEnvironment courseEnv, CheckListCourseNode cNode) {
		String path = courseEnv.getCourseBaseContainer().getRelPath() + "/" + CheckListCourseNode.FOLDER_NAME + "/" + cNode.getIdent();
		return new File(FolderConfig.getCanonicalRoot(), path);
	}
	
	private static class BatchComparator implements Comparator<AssessmentBatch> {
		@Override
		public int compare(AssessmentBatch o1, AssessmentBatch o2) {
			Long id1 = o1.getIdentityKey();
			Long id2 = o2.getIdentityKey();
			return id1.compareTo(id2);
		}
	}
}
