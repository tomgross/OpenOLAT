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
package org.olat.core.commons.services.vfs.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.services.vfs.VFSMetadata;
import org.olat.core.commons.services.vfs.VFSMetadataRef;
import org.olat.core.commons.services.vfs.VFSRevision;
import org.olat.core.commons.services.vfs.model.VFSRevisionImpl;
import org.olat.core.id.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 18 mars 2019<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class VFSRevisionDAO {
	
	@Autowired
	private DB dbInstance;
	
	public VFSRevision createRevision(Identity author, String filename, int revisionNr, long size, Date fileLastModified,
			String revisionComment, VFSMetadata metadata) {
		VFSRevisionImpl rev = new VFSRevisionImpl();
		rev.setCreationDate(new Date());
		rev.setLastModified(rev.getCreationDate());
		rev.setRevisionNr(revisionNr);
		rev.setFilename(filename);
		if(fileLastModified == null) {
			rev.setFileLastModified(rev.getCreationDate());
		} else {
			rev.setFileLastModified(fileLastModified);
		}
		rev.setSize(size);
		rev.setRevisionComment(revisionComment);
		rev.copyValues(metadata);
		rev.setAuthor(author);
		rev.setMetadata(metadata);
		dbInstance.getCurrentEntityManager().persist(rev);
		return rev;
	}
	
	public VFSRevision createRevisionCopy(Identity author, String revisionComment, VFSRevision revisionToCopy, VFSMetadata metadata) {
		VFSRevisionImpl rev = new VFSRevisionImpl();
		VFSRevisionImpl revToCopy = (VFSRevisionImpl)revisionToCopy;
		rev.setCreationDate(new Date());
		rev.setLastModified(rev.getCreationDate());
		rev.setRevisionNr(revToCopy.getRevisionNr());
		rev.setFilename(revToCopy.getFilename());
		rev.setFileLastModified(revToCopy.getFileLastModified());
		rev.setSize(revToCopy.getSize());
		rev.setRevisionComment(revisionComment);
		rev.setAuthor(author);
		rev.copyValues(revToCopy);
		rev.setMetadata(metadata);
		dbInstance.getCurrentEntityManager().persist(rev);
		return rev;
	}
	
	public VFSRevision loadRevision(Long revisionKey) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select rev from vfsrevision rev")
		  .append(" left join fetch rev.author as author")
		  .append(" left join fetch author.user as authorUser")
		  .append(" inner join fetch rev.metadata meta")
		  .append(" where rev.key=:revisionKey");
		List<VFSRevision> revisions = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), VFSRevision.class)
			.setParameter("revisionKey", revisionKey)
			.getResultList();
		return revisions == null || revisions.isEmpty() ? null : revisions.get(0);
	}
	
	public List<VFSRevision> getRevisions(VFSMetadataRef metadata) {
		if(metadata == null) return new ArrayList<>();
		
		StringBuilder sb = new StringBuilder(256);
		sb.append("select rev from vfsrevision rev")
		  .append(" left join fetch rev.author as author")
		  .append(" left join fetch author.user as authorUser")
		  .append(" where rev.metadata.key=:metadataKey")
		  .append(" order by rev.revisionNr");
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), VFSRevision.class)
			.setParameter("metadataKey", metadata.getKey())
			.getResultList();
	}
	
	public List<VFSRevision> getRevisions(List<VFSMetadataRef> metadatas) {
		if(metadatas == null || metadatas.isEmpty()) return new ArrayList<>();
		
		List<Long> metadataKeys = metadatas.stream().map(VFSMetadataRef::getKey).collect(Collectors.toList());
		
		StringBuilder sb = new StringBuilder(256);
		sb.append("select rev from vfsrevision rev")
		  .append(" left join fetch rev.author as author")
		  .append(" left join fetch author.user as authorUser")
		  .append(" where rev.metadata.key in (:metadataKeys)");
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), VFSRevision.class)
			.setParameter("metadataKeys", metadataKeys)
			.getResultList();
	}
	
	public List<VFSMetadataRef> getMetadataWithMoreThan(long numOfRevisions) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select new org.olat.core.commons.services.vfs.model.VFSMetadataRefImpl(meta.key)")
		  .append(" from filemetadata meta")
		  .append(" where :numOfRevisions < (select count(rev.key) from vfsrevision rev where rev.metadata.key=meta.key)");
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), VFSMetadataRef.class)
			.setParameter("numOfRevisions", Long.valueOf(numOfRevisions))
			.getResultList();
	}
	
	public List<Long> getMetadataKeysOfDeletedFiles() {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select meta.key")
		  .append(" from filemetadata meta")
		  .append(" inner join vfsrevision rev on (rev.metadata.key=meta.key)")
		  .append(" where meta.deleted=:deleted");
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Long.class)
			.setParameter("deleted", Boolean.TRUE)
			.getResultList();
	}
	
	/**
	 * @return A list of metadata of deleted files with revisions.
	 */
	public List<VFSMetadataRef> getMetadataOfDeletedFiles() {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select new org.olat.core.commons.services.vfs.model.VFSMetadataRefImpl(meta.key)")
		  .append(" from vfsrevision rev")
		  .append(" inner join rev.metadata meta")
		  .append(" where meta.deleted=:deleted");
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), VFSMetadataRef.class)
			.setParameter("deleted", Boolean.TRUE)
			.getResultList();
	}
	
	/**
	 * @return A list of revisions of deleted files.
	 */
	public List<VFSRevision> getRevisionsOfDeletedFiles() {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select rev from vfsrevision rev")
		  .append(" inner join fetch rev.metadata meta")
		  .append(" where meta.deleted=:deleted");
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), VFSRevision.class)
			.setParameter("deleted", Boolean.TRUE)
			.getResultList();
	}
	
	public long getRevisionsSizeOfDeletedFiles() {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select sum(rev.size) from vfsrevision rev")
		  .append(" inner join rev.metadata meta")
		  .append(" where meta.deleted=:deleted");
		List<Long> size = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Long.class)
			.setParameter("deleted", Boolean.TRUE)
			.getResultList();
		return size == null || size.isEmpty() || size.get(0) == null ? 0 : size.get(0).longValue();
	}
	

	
	public long calculateRevisionsSize() {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select sum(rev.size) from vfsrevision rev");
		List<Long> size = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Long.class)
			.getResultList();
		return size == null || size.isEmpty() || size.get(0) == null ? -1l : size.get(0).longValue();
	}
	
	public VFSRevision updateRevision(VFSRevision revision) {
		return dbInstance.getCurrentEntityManager().merge(revision);
	}
	
	public void deleteRevision(VFSRevision revision) {
		VFSRevision reloadedRev = dbInstance.getCurrentEntityManager()
				.getReference(VFSRevisionImpl.class, ((VFSRevisionImpl)revision).getKey());
		dbInstance.getCurrentEntityManager().remove(reloadedRev);
	}

}
