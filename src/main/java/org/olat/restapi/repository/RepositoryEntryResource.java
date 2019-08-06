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
package org.olat.restapi.repository;

import static org.olat.restapi.security.RestSecurityHelper.getIdentity;
import static org.olat.restapi.security.RestSecurityHelper.getUserRequest;
import static org.olat.restapi.security.RestSecurityHelper.isAdmin;
import static org.olat.restapi.security.RestSecurityHelper.isAuthor;
import static org.olat.restapi.security.RestSecurityHelper.isAuthorEditor;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.admin.securitygroup.gui.IdentitiesAddEvent;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.GroupRoles;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.logging.activity.LearningResourceLoggingAction;
import org.olat.core.logging.activity.OlatResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.FileUtils;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.mail.MailPackage;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.ImsCPFileResource;
import org.olat.modules.lecture.restapi.LectureBlocksWebService;
import org.olat.modules.reminder.restapi.RemindersWebService;
import org.olat.repository.ErrorList;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.repository.manager.RepositoryEntryLifecycleDAO;
import org.olat.repository.model.RepositoryEntryLifecycle;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.restapi.security.RestSecurityHelper;
import org.olat.restapi.support.MultipartReader;
import org.olat.restapi.support.ObjectFactory;
import org.olat.restapi.support.vo.RepositoryEntryLifecycleVO;
import org.olat.restapi.support.vo.RepositoryEntryVO;
import org.olat.user.restapi.UserVO;
import org.olat.user.restapi.UserVOFactory;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description:<br>
 * Repository entry resource
 *
 * <P>
 * Initial Date:  19.05.2009 <br>
 * @author patrickb, srosse, stephane.rosse@frentix.com
 */
public class RepositoryEntryResource {

  private static final OLog log = Tracing.createLoggerFor(RepositoryEntryResource.class);

  public static CacheControl cc = new CacheControl();

  static {
    cc.setMaxAge(-1);
  }
  
  private RepositoryManager repositoryManager;
  private RepositoryService repositoryService;
  private BaseSecurity securityManager;
  
  public RepositoryEntryResource(RepositoryManager repositoryManager, RepositoryService repositoryService, BaseSecurity securityManager) {
  	this.repositoryManager = repositoryManager;
  	this.repositoryService = repositoryService;
  	this.securityManager = securityManager;
  }

  /**
   * get a resource in the repository
   * @response.representation.200.qname {http://www.example.com}repositoryEntryVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc Get the repository resource
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_REPOENTRYVO}
   * @response.representation.404.doc The repository entry not found
   * @param repoEntryKey The key or soft key of the repository entry
   * @param request The REST request
   * @return
   */
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getById(@PathParam("repoEntryKey")String repoEntryKey, @Context Request request){
    RepositoryEntry re = lookupRepositoryEntry(repoEntryKey);
    if(re == null) {
      return Response.serverError().status(Status.NOT_FOUND).build();
    }

    Date lastModified = re.getLastModified();

    Response.ResponseBuilder response;
    if(lastModified == null) {
      EntityTag eTag = ObjectFactory.computeEtag(re);
      response = request.evaluatePreconditions(eTag);
      if(response == null) {
        RepositoryEntryVO vo = ObjectFactory.get(re);
        response = Response.ok(vo).tag(eTag).lastModified(lastModified);
      }
    } else {
      EntityTag eTag = ObjectFactory.computeEtag(re);
      response = request.evaluatePreconditions(lastModified, eTag);
      if(response == null) {
        RepositoryEntryVO vo = ObjectFactory.get(re);
        response = Response.ok(vo).tag(eTag).lastModified(lastModified);
      }
    }
    return response.build();
  }
  
	/**
	 * To get the web service for the lecture blocks of a specific learning resource.
	 * @response.representation.200.doc A web service to manage the lecture blocks
	 * @param repoEntryKey The primary key of the learning resource 
	 * @return The web service for lecture blocks.
	 */
	@Path("lectureblocks")
	public LectureBlocksWebService getLectureBlocksWebService(@PathParam("repoEntryKey")String repoEntryKey) {
	    RepositoryEntry re = lookupRepositoryEntry(repoEntryKey);
	    if(re == null) return null;
		LectureBlocksWebService service = new LectureBlocksWebService(re);
		CoreSpringFactory.autowireObject(service);
		return service;
	}
	
	/**
	 * To get the web service for the reminders of a specific learning resource.
	 * @response.representation.200.doc A web service to manage the reminders
	 * @param repoEntryKey The primary key of the learning resource 
	 * @return The web service for reminders.
	 */
	@Path("reminders")
	public RemindersWebService getRemindersWebService(@PathParam("repoEntryKey")String repoEntryKey) {
	    RepositoryEntry re = lookupRepositoryEntry(repoEntryKey);
	    if(re == null) return null;
	    RemindersWebService service = new RemindersWebService(re);
		CoreSpringFactory.autowireObject(service);
		return service;
	}
  
	/**
	 * Returns the list of owners of the repository entry specified by the groupKey.
	 * @response.representation.200.qname {http://www.example.com}userVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc Owners of the repository entry
	 * @response.representation.200.example {@link org.olat.user.restapi.Examples#SAMPLE_USERVOes}
	 * @response.representation.404.doc The repository entry cannot be found
	 * @param repoEntryKey The key of the repository entry
	 * @param request The HTTP Request
	 * @return
	 */
	@GET
	@Path("owners")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getOwners(@PathParam("repoEntryKey") String repoEntryKey, @Context HttpServletRequest request) {
		RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
		if(repoEntry == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if(!isAuthorEditor(repoEntry, request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		return getIdentityInSecurityGroup(repoEntry, GroupRoles.owner.name());
	}
	
	/**
	 * Adds an owner to the repository entry.
	 * @response.representation.200.doc The user is added as owner of the repository entry
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The repository entry or the user cannot be found
	 * @param repoEntryKey The key of the repository entry 
	 * @param identityKey The user's id
	 * @param request The HTTP request
	 * @return
	 */
	@PUT
	@Path("owners/{identityKey}")
	public Response addOwner(@PathParam("repoEntryKey") String repoEntryKey, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest request) {
		try {
			RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
			if(repoEntry == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			} else if(!isAuthorEditor(repoEntry, request)) {
				return Response.serverError().status(Status.UNAUTHORIZED).build();
			}
			
			Identity identityToAdd = securityManager.loadIdentityByKey(identityKey);
			if(identityToAdd == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			}

			UserRequest ureq = RestSecurityHelper.getUserRequest(request);
			IdentitiesAddEvent iae = new IdentitiesAddEvent(identityToAdd);
			repositoryManager.addOwners(ureq.getIdentity(), iae, repoEntry, new MailPackage(false));
			return Response.ok().build();
		} catch (Exception e) {
			log.error("Trying to add an owner to a repository entry", e);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@PUT
	@Path("owners")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response addOwners(UserVO[] owners, @PathParam("repoEntryKey") String repoEntryKey,
			@Context HttpServletRequest request) {
		try {
			RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
			if(repoEntry == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			} else if(!isAuthorEditor(repoEntry, request)) {
				return Response.serverError().status(Status.UNAUTHORIZED).build();
			}
			
			List<Identity> identityToAdd = loadIdentities(owners);
			UserRequest ureq = RestSecurityHelper.getUserRequest(request);
			IdentitiesAddEvent iae = new IdentitiesAddEvent(identityToAdd);
			repositoryManager.addOwners(ureq.getIdentity(), iae, repoEntry, new MailPackage(false));
			return Response.ok().build();
		} catch (Exception e) {
			log.error("Trying to add an owner to a repository entry", e);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Removes the owner from the repository entry.
	 * @response.representation.200.doc The user is removed as owner from the repository entry
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The repository entry or the user cannot be found
	 * @param repoEntryKey The key of the repository entry
	 * @param identityKey The user's id
	 * @param request The HTTP request
	 * @return
	 */
	@DELETE
	@Path("owners/{identityKey}")
	public Response removeOwner(@PathParam("repoEntryKey") String repoEntryKey, @PathParam("identityKey") Long identityKey, @Context HttpServletRequest request) {
		try {
			RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
			if(repoEntry == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			} else if (!isAuthorEditor(repoEntry, request)) {
				return Response.serverError().status(Status.UNAUTHORIZED).build();
			}

			Identity identityToRemove = securityManager.loadIdentityByKey(identityKey);
			if(identityToRemove == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			}

			final UserRequest ureq = RestSecurityHelper.getUserRequest(request);
			repositoryManager.removeOwners(ureq.getIdentity(), Collections.singletonList(identityToRemove), repoEntry, new MailPackage(false));
			return Response.ok().build();
		} catch (Exception e) {
			log.error("Trying to remove an owner to a repository entry", e);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Returns the list of coaches of the repository entry.
	 * @response.representation.200.qname {http://www.example.com}userVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc Coaches of the repository entry
	 * @response.representation.200.example {@link org.olat.user.restapi.Examples#SAMPLE_USERVOes}
	 * @response.representation.404.doc The repository entry cannot be found
	 * @param repoEntryKey The key of the repository entry
	 * @param request The HTTP Request
	 * @return
	 */
	@GET
	@Path("coaches")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getCoaches(@PathParam("repoEntryKey") String repoEntryKey, @Context HttpServletRequest request) {
		RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
		if(repoEntry == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if(!isAuthorEditor(repoEntry, request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		return getIdentityInSecurityGroup(repoEntry, GroupRoles.coach.name());
	}
	
	/**
	 * Adds a coach to the repository entry.
	 * @response.representation.200.doc The user is added as coach of the repository entry
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The repository entry or the user cannot be found
	 * @param repoEntryKey The key of the repository entry 
	 * @param identityKey The user's id
	 * @param request The HTTP request
	 * @return
	 */
	@PUT
	@Path("coaches/{identityKey}")
	public Response addCoach(@PathParam("repoEntryKey") String repoEntryKey, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest request) {
		try {
			RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
			if(repoEntry == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			} else if(!isAuthorEditor(repoEntry, request)) {
				return Response.serverError().status(Status.UNAUTHORIZED).build();
			}
			
			Identity identityToAdd = securityManager.loadIdentityByKey(identityKey);
			if(identityToAdd == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			}

			UserRequest ureq = RestSecurityHelper.getUserRequest(request);
			IdentitiesAddEvent iae = new IdentitiesAddEvent(identityToAdd);
			repositoryManager.addTutors(ureq.getIdentity(), ureq.getUserSession().getRoles(), iae, repoEntry, null);
			return Response.ok().build();
		} catch (Exception e) {
			log.error("Trying to add a coach to a repository entry", e);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PUT
	@Path("coaches")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response addCoach(UserVO[] coaches, @PathParam("repoEntryKey") String repoEntryKey,
			@Context HttpServletRequest request) {
		try {
			RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
			if(repoEntry == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			} else if(!isAuthorEditor(repoEntry, request)) {
				return Response.serverError().status(Status.UNAUTHORIZED).build();
			}
			
			List<Identity> identityToAdd = loadIdentities(coaches);
			UserRequest ureq = RestSecurityHelper.getUserRequest(request);
			IdentitiesAddEvent iae = new IdentitiesAddEvent(identityToAdd);
			repositoryManager.addTutors(ureq.getIdentity(), ureq.getUserSession().getRoles(), iae, repoEntry, null);
			return Response.ok().build();
		} catch (Exception e) {
			log.error("Trying to add a coach to a repository entry", e);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Removes the coach from the repository entry.
	 * @response.representation.200.doc The user is removed as coach from the repository entry
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The repository entry or the user cannot be found
	 * @param repoEntryKey The key of the repository entry
	 * @param identityKey The user's id
	 * @param request The HTTP request
	 * @return
	 */
	@DELETE
	@Path("coaches/{identityKey}")
	public Response removeCoach(@PathParam("repoEntryKey") String repoEntryKey, @PathParam("identityKey") Long identityKey, @Context HttpServletRequest request) {
		try {
			RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
			if(repoEntry == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			} else if (!isAuthorEditor(repoEntry, request)) {
				return Response.serverError().status(Status.UNAUTHORIZED).build();
			}

			Identity identityToRemove = securityManager.loadIdentityByKey(identityKey);
			if(identityToRemove == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			}

			final UserRequest ureq = RestSecurityHelper.getUserRequest(request);
			repositoryManager.removeTutors(ureq.getIdentity(), Collections.singletonList(identityToRemove), repoEntry, new MailPackage(false));
			return Response.ok().build();
		} catch (Exception e) {
			log.error("Trying to remove a coach from a repository entry", e);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Returns the list of participants of the repository entry.
	 * @response.representation.200.qname {http://www.example.com}userVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc Coaches of the repository entry
   * @response.representation.200.example {@link org.olat.user.restapi.Examples#SAMPLE_USERVOes}
	 * @response.representation.404.doc The repository entry cannot be found
	 * @param repoEntryKey The key of the repository entry
	 * @param request The HTTP Request
	 * @return
	 */
	@GET
	@Path("participants")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getParticipants(@PathParam("repoEntryKey") String repoEntryKey, @Context HttpServletRequest request) {
		RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
		if(repoEntry == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if(!isAuthorEditor(repoEntry, request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		return getIdentityInSecurityGroup(repoEntry, GroupRoles.participant.name());
	}
	
	/**
	 * Adds a participant to the repository entry.
	 * @response.representation.200.doc The user is added as participant of the repository entry
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The repository entry or the user cannot be found
	 * @param repoEntryKey The key of the repository entry 
	 * @param identityKey The user's id
	 * @param request The HTTP request
	 * @return
	 */
	@PUT
	@Path("participants/{identityKey}")
	public Response addParticipant(@PathParam("repoEntryKey") String repoEntryKey, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest request) {
		try {
			RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
			if(repoEntry == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			} else if(!isAuthorEditor(repoEntry, request)) {
				return Response.serverError().status(Status.UNAUTHORIZED).build();
			}
			
			Identity identityToAdd = securityManager.loadIdentityByKey(identityKey);
			if(identityToAdd == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			}

			UserRequest ureq = RestSecurityHelper.getUserRequest(request);
			IdentitiesAddEvent iae = new IdentitiesAddEvent(identityToAdd);
			repositoryManager.addParticipants(ureq.getIdentity(), ureq.getUserSession().getRoles(), iae, repoEntry, null);
			return Response.ok().build();
		} catch (Exception e) {
			log.error("Trying to add a participant to a repository entry", e);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@PUT
	@Path("participants")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response addParticipants(UserVO[] participants, @PathParam("repoEntryKey") String repoEntryKey,
			@Context HttpServletRequest request) {
		try {
			RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
			if(repoEntry == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			} else if(!isAuthorEditor(repoEntry, request)) {
				return Response.serverError().status(Status.UNAUTHORIZED).build();
			}
			
			List<Identity> participantList = loadIdentities(participants);
			UserRequest ureq = RestSecurityHelper.getUserRequest(request);
			IdentitiesAddEvent iae = new IdentitiesAddEvent(participantList);
			repositoryManager.addParticipants(ureq.getIdentity(), ureq.getUserSession().getRoles(), iae, repoEntry, null);
			return Response.ok().build();
		} catch (Exception e) {
			log.error("Trying to add a participant to a repository entry", e);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	private List<Identity> loadIdentities(UserVO[] users) {
		List<Long> identityKeys = new ArrayList<>();
		for(UserVO user:users) {
			identityKeys.add(user.getKey());
		}
		return securityManager.loadIdentityByKeys(identityKeys);
	}
	
	/**
	 * Removes the participant from the repository entry.
	 * @response.representation.200.doc The user is removed as participant from the repository entry
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The repository entry or the user cannot be found
	 * @param repoEntryKey The key of the repository entry
	 * @param identityKey The user's id
	 * @param request The HTTP request
	 * @return
	 */
	@DELETE
	@Path("participants/{identityKey}")
	public Response removeParticipant(@PathParam("repoEntryKey") String repoEntryKey, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest request) {
		try {
			RepositoryEntry repoEntry = lookupRepositoryEntry(repoEntryKey);
			if(repoEntry == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			} else if (!isAuthorEditor(repoEntry, request)) {
				return Response.serverError().status(Status.UNAUTHORIZED).build();
			}

			Identity identityToRemove = securityManager.loadIdentityByKey(identityKey);
			if(identityToRemove == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			}

			final UserRequest ureq = RestSecurityHelper.getUserRequest(request);
			repositoryManager.removeParticipants(ureq.getIdentity(), Collections.singletonList(identityToRemove), repoEntry, null, false);
			return Response.ok().build();
		} catch (Exception e) {
			log.error("Trying to remove a participant from a repository entry", e);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

  /**
   * Download the export zip file of a repository entry.
   * @response.representation.mediaType multipart/form-data
   * @response.representation.doc Download the resource file
   * @response.representation.200.mediaType application/zip
   * @response.representation.200.doc Download the repository entry as export zip file
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_REPOENTRYVO}
   * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The resource could not found
   * @response.representation.406.doc Download of this resource is not possible
   * @response.representation.409.doc The resource is locked
   * @param repoEntryKey
   * @param request The HTTP request
   * @return
   */
	@GET
	@Path("file")
	@Produces({ "application/zip", MediaType.APPLICATION_OCTET_STREAM })
	public Response getRepoFileById(@PathParam("repoEntryKey") String repoEntryKey,
			@Context HttpServletRequest request, @Context HttpServletResponse response) {
		RepositoryEntry re = lookupRepositoryEntry(repoEntryKey);
		if (re == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}

		RepositoryHandler typeToDownload = RepositoryHandlerFactory.getInstance().getRepositoryHandler(re);
		if (typeToDownload == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}

		OLATResource ores = OLATResourceManager.getInstance().findResourceable(re.getOlatResource());
		if (ores == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}

		Identity identity = getIdentity(request);
		boolean canDownload = re.getCanDownload() && typeToDownload.supportsDownload();
		if (isAdmin(request) || RepositoryManager.getInstance().isOwnerOfRepositoryEntry(identity, re)) {
			canDownload = true;
		} else if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		if (!canDownload) {
			return Response.serverError().status(Status.NOT_ACCEPTABLE).build();
		}

		boolean isAlreadyLocked = typeToDownload.isLocked(ores);
		LockResult lockResult = null;
		try {
			lockResult = typeToDownload.acquireLock(ores, identity);
			if (lockResult == null || (lockResult.isSuccess() && !isAlreadyLocked)) {
				MediaResource mr = typeToDownload.getAsMediaResource(ores, false);
				if (mr != null) {
					repositoryService.incrementDownloadCounter(re);
					InputStream in = mr.getInputStream();
					if(in == null) {
						mr.prepare(response);
						return null;
					} else {
						return Response.ok(in).cacheControl(cc).build(); // success
					}
				} else {
					return Response.serverError().status(Status.NO_CONTENT).build();
				}
			} else {
				return Response.serverError().status(Status.CONFLICT).build();
			}
		} finally {
			if ((lockResult != null && lockResult.isSuccess() && !isAlreadyLocked)) {
				typeToDownload.releaseLock(lockResult);
			}
		}
	}
  
  @POST
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response updateEntry(@PathParam("repoEntryKey") String repoEntryKey,
      RepositoryEntryVO vo, @Context HttpServletRequest request) {
    if(!RestSecurityHelper.isAuthor(request)) {
      return Response.serverError().status(Status.UNAUTHORIZED).build();
    }
    
    final RepositoryEntry re = lookupRepositoryEntry(repoEntryKey);
    if(re == null) {
      return Response.serverError().status(Status.NOT_FOUND).build();
    }
    
    RepositoryEntryLifecycle lifecycle = null;
    RepositoryEntryLifecycleVO lifecycleVo = vo.getLifecycle();
    if(lifecycleVo != null) {
    	RepositoryEntryLifecycleDAO lifecycleDao = CoreSpringFactory.getImpl(RepositoryEntryLifecycleDAO.class);
    	if(lifecycleVo.getKey() != null) {
    		lifecycle = lifecycleDao.loadById(lifecycleVo.getKey());
    		if(lifecycle.isPrivateCycle()) {
    			//check date
      		String fromStr = lifecycleVo.getValidFrom();
      		String toStr = lifecycleVo.getValidTo();
      		String label = lifecycleVo.getLabel();
      		String softKey = lifecycleVo.getSoftkey();
    			Date from = ObjectFactory.parseDate(fromStr);
      		Date to = ObjectFactory.parseDate(toStr);
      		lifecycle.setLabel(label);
      		lifecycle.setSoftKey(softKey);
      		lifecycle.setValidFrom(from);
      		lifecycle.setValidTo(to);
    		}
    	} else {
    		String fromStr = lifecycleVo.getValidFrom();
    		String toStr = lifecycleVo.getValidTo();
    		String label = lifecycleVo.getLabel();
    		String softKey = lifecycleVo.getSoftkey();
    		Date from = ObjectFactory.parseDate(fromStr);
    		Date to = ObjectFactory.parseDate(toStr);
    		lifecycle = lifecycleDao.create(label, softKey, true, from, to);
    	}
    }

    RepositoryEntry reloaded = repositoryManager.setDescriptionAndName(re, vo.getDisplayname(), vo.getDescription(),
    		vo.getLocation(), vo.getAuthors(), vo.getExternalId(), vo.getExternalRef(), vo.getManagedFlags(),
    		lifecycle);
    RepositoryEntryVO rvo = ObjectFactory.get(reloaded);
    return Response.ok(rvo).build();
  }

  /**
   * Replace a resource in the repository and update its display name. The implementation is
   * limited to CP.
   * @response.representation.mediaType multipart/form-data
   * @response.representation.doc Import the resource file
   * @response.representation.200.qname {http://www.example.com}repositoryEntryVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc Replace the resource and return the updated repository entry
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_REPOENTRYVO}
   * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @param repoEntryKey The key or soft key of the repository entry
   * @param filename The name of the file
   * @param file The file input stream
   * @param displayname The display name
   * @param request The HTTP request
   * @return
   */
  @POST
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  public Response replaceResource(@PathParam("repoEntryKey") String repoEntryKey,
      @Context HttpServletRequest request) {
    if(!RestSecurityHelper.isAuthor(request)) {
      return Response.serverError().status(Status.UNAUTHORIZED).build();
    }

    MultipartReader reader = null;
    try {
      final RepositoryEntry re = lookupRepositoryEntry(repoEntryKey);
      if(re == null) {
        return Response.serverError().status(Status.NOT_FOUND).build();
      }
      
      reader = new MultipartReader(request);
      File tmpFile = reader.getFile();
      String displayname = reader.getValue("displayname");
      String location = reader.getValue("location");
      String authors = reader.getValue("authors");
      String description = reader.getValue("description");
      String externalId = reader.getValue("externalId");
      String externalRef = reader.getValue("externalRef");
      String managedFlags = reader.getValue("managedFlags");

      Identity identity = RestSecurityHelper.getUserRequest(request).getIdentity();
      RepositoryEntry replacedRe;
      if(tmpFile == null) {
      	replacedRe = repositoryManager.setDescriptionAndName(re, displayname, description,
      			location, authors, externalId, externalRef, managedFlags, re.getLifecycle());
      } else {
	      long length = tmpFile.length();
	      if(length == 0) {
	        return Response.serverError().status(Status.NO_CONTENT).build();
	      }
	      replacedRe = replaceFileResource(identity, re, tmpFile);
	      if(replacedRe == null) {
	        return Response.serverError().status(Status.NOT_FOUND).build();
	      } else {
	      	replacedRe = repositoryManager.setDescriptionAndName(replacedRe, displayname, description,
	      			location, authors, externalId, externalRef, managedFlags, replacedRe.getLifecycle());
	      }
      }
      RepositoryEntryVO vo = ObjectFactory.get(replacedRe);
      return Response.ok(vo).build();
    } catch (Exception e) {
      log.error("Error while importing a file",e);
    } finally {
    	MultipartReader.closeQuietly(reader);
    }
    return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
  }

	private RepositoryEntry replaceFileResource(Identity identity, RepositoryEntry re, File fResource) {
		if (re == null) throw new NullPointerException("RepositoryEntry cannot be null");

		FileResourceManager frm = FileResourceManager.getInstance();
		File currentResource = frm.getFileResource(re.getOlatResource());
		if (currentResource == null || !currentResource.exists()) {
			log.debug("Current resource file doesn't exist");
			return null;
		}

		String typeName = re.getOlatResource().getResourceableTypeName();
		if (typeName.equals(ImsCPFileResource.TYPE_NAME)) {
			if (currentResource.delete()) {
				FileUtils.copyFileToFile(fResource, currentResource, false);

				String repositoryHome = FolderConfig.getCanonicalRepositoryHome();
				String relUnzipDir = frm.getUnzippedDirRel(re.getOlatResource());
				File unzipDir = new File(repositoryHome, relUnzipDir);
				if (unzipDir != null && unzipDir.exists()) {
					FileUtils.deleteDirsAndFiles(unzipDir, true, true);
				}
				frm.unzipFileResource(re.getOlatResource());
			}
			log.audit("Resource: " + re.getOlatResource() + " replaced by " + identity.getKey());
			return re;
		}

		log.debug("Cannot replace a resource of the type: " + typeName);
		return null;
	}
  
    /**
	 * Delete a resource by id
	 * 
	 * @response.representation.200.doc The metadatas of the deleted resource
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param request The HTTP request
	 * @return It returns the XML representation of the <code>Structure</code>
	 *         object representing the course.
	 */
	@DELETE
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response deleteCourse(@PathParam("repoEntryKey") String repoEntryKey, @Context HttpServletRequest request) {
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		RepositoryEntry re = lookupRepositoryEntry(repoEntryKey);
		if(re == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(re, request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		UserRequest ureq = getUserRequest(request);
		RepositoryService rs = CoreSpringFactory.getImpl(RepositoryService.class);
		ErrorList errors = rs.deletePermanently(re, ureq.getIdentity(), ureq.getUserSession().getRoles(), ureq.getLocale());
		if(errors.hasErrors()) {
			return Response.serverError().status(500).build();
		}
		ThreadLocalUserActivityLogger.log(LearningResourceLoggingAction.LEARNING_RESOURCE_DELETE, getClass(),
				LoggingResourceable.wrap(re, OlatResourceableType.genRepoEntry));
		return Response.ok().build();
	}
	
	/**
	 * Change the status of a course by id. The possible status are:
	 * <ul>
	 * 	<li>closed</li>
	 * 	<li>unclosed</li>
	 * 	<li>unpublished</li>
	 * 	<li>deleted</li>
	 * 	<li>restored</li>
	 * </ul>
	 * 
	 * @response.representation.200.doc The metadatas of the deleted course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param request The HTTP request
	 * @return It returns the XML representation of the <code>Structure</code>
	 *         object representing the course.
	 */
	@POST
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Path("status")
	public Response deleteCoursePermanently(@PathParam("repoEntryKey") String repoEntryKey,
			@FormParam("newStatus") String newStatus, @Context HttpServletRequest request) {
		
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		RepositoryEntry re = lookupRepositoryEntry(repoEntryKey);
		if(re == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(re, request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		RepositoryService rs = CoreSpringFactory.getImpl(RepositoryService.class);
		if("closed".equals(newStatus)) {
			rs.closeRepositoryEntry(re, null, false);
			log.audit("REST closing course: " + re.getDisplayname() + " [" + re.getKey() + "]");
			ThreadLocalUserActivityLogger.log(LearningResourceLoggingAction.LEARNING_RESOURCE_CLOSE, getClass(),
					LoggingResourceable.wrap(re, OlatResourceableType.genRepoEntry));
		} else if("unclosed".equals(newStatus)) {
			rs.uncloseRepositoryEntry(re);
			log.audit("REST unclosing course: " + re.getDisplayname() + " [" + re.getKey() + "]");
			ThreadLocalUserActivityLogger.log(LearningResourceLoggingAction.LEARNING_RESOURCE_UPDATE, getClass(),
					LoggingResourceable.wrap(re, OlatResourceableType.genRepoEntry));
		} else if("unpublished".equals(newStatus)) {
			rs.unpublishRepositoryEntry(re);
			log.audit("REST unpublishing course: " + re.getDisplayname() + " [" + re.getKey() + "]");
			ThreadLocalUserActivityLogger.log(LearningResourceLoggingAction.LEARNING_RESOURCE_DEACTIVATE, getClass(),
					LoggingResourceable.wrap(re, OlatResourceableType.genRepoEntry));
		} else if("deleted".equals(newStatus)) {
			Identity identity = getIdentity(request);
			rs.deleteSoftly(re, identity, true, false);
			log.audit("REST deleting (soft) course: " + re.getDisplayname() + " [" + re.getKey() + "]");
			ThreadLocalUserActivityLogger.log(LearningResourceLoggingAction.LEARNING_RESOURCE_TRASH, getClass(),
					LoggingResourceable.wrap(re, OlatResourceableType.genRepoEntry));
		} else if("restored".equals(newStatus)) {
			rs.restoreRepositoryEntry(re);
			log.audit("REST restoring course: " + re.getDisplayname() + " [" + re.getKey() + "]");
			ThreadLocalUserActivityLogger.log(LearningResourceLoggingAction.LEARNING_RESOURCE_RESTORE, getClass(),
					LoggingResourceable.wrap(re, OlatResourceableType.genRepoEntry));
		}
		return Response.ok().build();
	}
	
	private Response getIdentityInSecurityGroup(RepositoryEntry re, String role) {
		List<Identity> identities = repositoryService.getMembers(re, role);
		
		int count = 0;
		UserVO[] ownerVOs = new UserVO[identities.size()];
		for(Identity identity:identities) {
			ownerVOs[count++] = UserVOFactory.get(identity, true, true);
		}
		return Response.ok(ownerVOs).build();
	}

  private RepositoryEntry lookupRepositoryEntry(String key) {
    Long repoEntryKey = longId(key);
    RepositoryEntry re = null;
    if(repoEntryKey != null) {//looks like a primary key
      re = repositoryManager.lookupRepositoryEntry(repoEntryKey);
    }
    if(re == null) {// perhaps a soft key
      re = repositoryManager.lookupRepositoryEntryBySoftkey(key, false);
    }
    return re;
  }

  private Long longId(String key) {
    try {
      for(int i=key.length(); i-->0; ) {
        if(!Character.isDigit(key.charAt(i))) {
          return null;
        }
      }
      return new Long(key);
    } catch(NumberFormatException ex) {
      return null;
    }
  }
}
