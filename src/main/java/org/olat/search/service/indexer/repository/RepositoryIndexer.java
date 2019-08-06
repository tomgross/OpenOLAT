/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.search.service.indexer.repository;


import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.util.resource.OresHelper;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.manager.RepositoryEntryDocumentFactory;
import org.olat.repository.model.SearchRepositoryEntryParameters;
import org.olat.resource.accesscontrol.ACService;
import org.olat.resource.accesscontrol.AccessResult;
import org.olat.resource.accesscontrol.OfferAccess;
import org.olat.resource.accesscontrol.provider.free.FreeAccessHandler;
import org.olat.resource.accesscontrol.provider.paypal.PaypalAccessHandler;
import org.olat.search.SearchModule;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.indexer.AbstractHierarchicalIndexer;
import org.olat.search.service.indexer.Indexer;
import org.olat.search.service.indexer.OlatFullIndexer;

/**
 * Index the whole OLAT-repository.
 * @author Christian Guretzki
 * 
 */
public class RepositoryIndexer extends AbstractHierarchicalIndexer {
	
	private static final int BATCH_SIZE = 100;
	
	private DB dbInstance;
	private RepositoryManager repositoryManager;
	private RepositoryEntryDocumentFactory documentFactory;
	
	private List<Long> repositoryBlackList;

	private RepositoryIndexer() {
		//
	}
	
	/**
	 * [used by spring]
	 */
	public void setSearchModule(SearchModule searchModule) {
		repositoryBlackList = searchModule.getRepositoryBlackList();
	}
	
	/**
	 * [used by spring]
	 */
	public void setRepositoryEntryDocumentFactory(RepositoryEntryDocumentFactory documentFactory) {
		this.documentFactory = documentFactory;
	}
	
	/**
	 * [used by spring]
	 */
	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}
	
	/**
	 * [used by spring]
	 */
	public void setDbInstance(DB dbInstance) {
		this.dbInstance = dbInstance;
	}

	/**
    * Loops over all repository-entries. Index repository meta data. 
    * Go further with repository-indexer for certain type if available. 
    * @see org.olat.search.service.indexer.Indexer#doIndex(org.olat.search.service.SearchResourceContext, java.lang.Object, org.olat.search.service.indexer.OlatFullIndexer)
    */
	@Override
	public void doIndex(SearchResourceContext parentResourceContext, Object businessObj, OlatFullIndexer indexWriter) throws IOException,InterruptedException {
		final Roles roles = new Roles(true, true, true, true, false, true, false);

		final SearchRepositoryEntryParameters params = new SearchRepositoryEntryParameters();
		params.setRoles(roles);
		boolean debug = isLogDebugEnabled();

		
		// loop over all repository-entries
		// committing here to make sure the loadBusinessGroup below does actually
		// reload from the database and not only use the session cache 
		// (see org.hibernate.Session.get(): 
		//  If the instance, or a proxy for the instance, is already associated with the session, return that instance or proxy.)
		dbInstance.commitAndCloseSession();
		
		int counter = 0;
		List<RepositoryEntry> repositoryList;
		do {
			repositoryList = repositoryManager.genericANDQueryWithRolesRestriction(params, counter, BATCH_SIZE, true);
	
			for(RepositoryEntry repositoryEntry:repositoryList) {
				try {
					// reload the repositoryEntry here before indexing it to make sure it has not been deleted in the meantime
					RepositoryEntry reloadedRepositoryEntry = repositoryManager.lookupRepositoryEntry(repositoryEntry.getKey());
					if (reloadedRepositoryEntry==null) {
						logInfo("doIndex: repositoryEntry was deleted while we were indexing. The deleted repositoryEntry was: "+repositoryEntry);
						continue;
					}
					if(repositoryEntry.getAccess() == RepositoryEntry.DELETED) {
						continue;
					}
					
					repositoryEntry = reloadedRepositoryEntry;
					if (debug) {
						logDebug("Index repositoryEntry=" + repositoryEntry + "  counter=" + counter++ + " with ResourceableId=" + repositoryEntry.getOlatResource().getResourceableId());
					}
					if (!isOnBlacklist(repositoryEntry.getOlatResource().getResourceableId()) ) {
						SearchResourceContext searchResourceContext = new SearchResourceContext(parentResourceContext);
						searchResourceContext.setBusinessControlFor(repositoryEntry);
						searchResourceContext.setTitle(repositoryEntry.getDisplayname());
						searchResourceContext.setDescription(repositoryEntry.getDescription());
						Document document = documentFactory.createDocument(searchResourceContext, repositoryEntry);
						indexWriter.addDocument(document);
						// Pass created-date & modified-date in context to child indexer because the child have no dates
						searchResourceContext.setLastModified(repositoryEntry.getLastModified());
						searchResourceContext.setCreatedDate(repositoryEntry.getCreationDate());
						// go further with resource
						Indexer repositoryEntryIndexer = getRepositoryEntryIndexer(repositoryEntry);
						if (repositoryEntryIndexer != null) {
							repositoryEntryIndexer.doIndex(searchResourceContext, repositoryEntry, indexWriter);
						} else if (debug) {
							logDebug("No RepositoryEntryIndexer for " + repositoryEntry.getOlatResource()); // e.g. RepositoryEntry				
						}
					} else {
						logWarn("RepositoryEntry is on black-list and excluded from search-index, repositoryEntry=" + repositoryEntry, null);
					}
				} catch (Throwable ex) {
					// create meaninfull debugging output to find repo entry that is somehow broken
					String entryDebug = "NULL";
					if (repositoryEntry != null) {
						entryDebug = "resId::" + repositoryEntry.getResourceableId() + " resTypeName::" + repositoryEntry.getResourceableTypeName() + " resName::" + repositoryEntry.getResourcename();
					}
					logWarn("Exception=" + ex.getMessage() + " for repo entry " + entryDebug, ex);
					dbInstance.rollbackAndCloseSession();
				}
				dbInstance.commitAndCloseSession();
			}
			counter += repositoryList.size();
			
		} while(repositoryList.size() == BATCH_SIZE);
		if (debug) {
			logDebug("RepositoryIndexer finished.  counter=" + counter);
		}
	}

	private boolean isOnBlacklist(Long key) {
		return repositoryBlackList.contains(key);
	}

	/**
	 * 
	 * @see org.olat.search.service.indexer.Indexer#getSupportedTypeName()
	 */
	public String getSupportedTypeName() {
		return OresHelper.calculateTypeName(RepositoryEntry.class);
	}

	/**
	 * 
	 * @see org.olat.search.service.indexer.Indexer#checkAccess(org.olat.core.id.context.ContextEntry, org.olat.core.id.context.BusinessControl, org.olat.core.id.Identity, org.olat.core.id.Roles)
	 */
	@Override
	public boolean checkAccess(ContextEntry contextEntry, BusinessControl businessControl, Identity identity, Roles roles) {
		boolean debug = isLogDebugEnabled();
		if (debug) logDebug("checkAccess for businessControl=" + businessControl + "  identity=" + identity + "  roles=" + roles);
		Long repositoryKey = contextEntry.getOLATResourceable().getResourceableId();
		RepositoryEntry repositoryEntry = repositoryManager.lookupRepositoryEntry(repositoryKey);
		if (repositoryEntry == null) {
			return false;
		}
		if(roles.isGuestOnly()) {
			if(repositoryEntry.getAccess() != RepositoryEntry.ACC_USERS_GUESTS) {
				return false;
			}
		}
			
		boolean isOwner = repositoryManager.isOwnerOfRepositoryEntry(identity,repositoryEntry);
		boolean isAllowedToLaunch = false;
		if (!isOwner) {
			isAllowedToLaunch = repositoryManager.isAllowedToLaunch(identity, roles, repositoryEntry);
			if(isAllowedToLaunch) {
				List<ContextEntry> entries = businessControl.getEntriesDownTheControls();
				if(entries.size() > 1) {
					boolean hasAccess = false;
					ACService acService = CoreSpringFactory.getImpl(ACService.class);
					AccessResult acResult = acService.isAccessible(repositoryEntry, identity, false); 
					if (acResult.isAccessible()) {
						hasAccess = true;
					} else if (!acResult.getAvailableMethods().isEmpty()) {
						for(OfferAccess offer:acResult.getAvailableMethods()) {
							String type = offer.getMethod().getType();
							if (type.equals(FreeAccessHandler.METHOD_TYPE) || type.equals(PaypalAccessHandler.METHOD_TYPE)) {
								hasAccess = true;
							}
						}
					}
					isAllowedToLaunch = hasAccess;
				}
			}
		}
			
		if (debug) logDebug("isOwner=" + isOwner + "  isAllowedToLaunch=" + isAllowedToLaunch);
		if (isOwner || isAllowedToLaunch) {
			Indexer repositoryEntryIndexer = getRepositoryEntryIndexer(repositoryEntry);
			if (debug) logDebug("repositoryEntryIndexer=" + repositoryEntryIndexer);
			if (repositoryEntryIndexer != null) {
			  return super.checkAccess(contextEntry, businessControl, identity, roles)
			  		&& repositoryEntryIndexer.checkAccess(contextEntry, businessControl, identity, roles);
			}
		}
		return false;
	}
	
	/**
	 * Get the repository handler for this repository entry.
	 * @param re
	 * @return the handler or null if no appropriate handler could be found
	 */
	public Indexer getRepositoryEntryIndexer(RepositoryEntry re) {
		OLATResourceable ores = re.getOlatResource();
		if (ores == null) throw new AssertException("RepositoryEntry has no OlatResource [re.getOlatResource()==null].");
		return getRepositoryEntryIndexer(ores.getResourceableTypeName());
	}
	
	/**
	 * Get a repository handler which supports the given resourceable type.
	 * @param resourceableTypeName
	 * @return the handler or null if no appropriate handler could be found
	 */
	public Indexer getRepositoryEntryIndexer(String resourceableTypeName) {
		List<Indexer> indexers = getIndexerByType(resourceableTypeName);
		if(indexers != null && !indexers.isEmpty()) {
			return indexers.get(0);
		}
		return null;
	}
}
