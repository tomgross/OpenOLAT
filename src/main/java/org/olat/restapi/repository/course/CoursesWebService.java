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
package org.olat.restapi.repository.course;

import static org.olat.restapi.security.RestSecurityHelper.getIdentity;
import static org.olat.restapi.security.RestSecurityHelper.getRoles;
import static org.olat.restapi.security.RestSecurityHelper.getUserRequest;
import static org.olat.restapi.security.RestSecurityHelper.isAuthor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.config.CourseConfig;
import org.olat.course.nodes.CourseNode;
import org.olat.course.tree.CourseEditorTreeNode;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryAllowToLeaveOptions;
import org.olat.repository.RepositoryEntryManagedFlag;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.repository.model.SearchRepositoryEntryParameters;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.resource.accesscontrol.ACService;
import org.olat.resource.accesscontrol.AccessResult;
import org.olat.restapi.security.RestSecurityHelper;
import org.olat.restapi.support.MediaTypeVariants;
import org.olat.restapi.support.MultipartReader;
import org.olat.restapi.support.ObjectFactory;
import org.olat.restapi.support.vo.CourseConfigVO;
import org.olat.restapi.support.vo.CourseVO;
import org.olat.restapi.support.vo.CourseVOes;

/**
 *
 * Description:<br>
 * This web service handles the courses.
 *
 * <P>
 * Initial Date:  27 apr. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com
 */
@Path("repo/courses")
public class CoursesWebService {

	private static final OLog log = Tracing.createLoggerFor(CoursesWebService.class);

	private static final String VERSION = "1.0";

	/**
	 * The version of the Course Web Service
	 * @response.representation.200.mediaType text/plain
	 * @response.representation.200.doc The version of this specific Web Service
	 * @response.representation.200.example 1.0
	 * @return
	 */
	@GET
	@Path("version")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getVersion() {
		return Response.ok(VERSION).build();
	}

	/**
	 * Get all courses viewable by the authenticated user
	 * @response.representation.200.qname {http://www.example.com}courseVO
	 * @response.representation.200.mediaType application/xml, application/json, application/json;pagingspec=1.0
	 * @response.representation.200.doc List of visible courses
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSEVOes}
	 * @param start
	 * @param limit
	 * @param externalId Search with an external ID
	 * @param externalRef Search with an external reference
	 * @param managed (true / false) Search only managed / not managed groups
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return
	 */
	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getCourseList(@QueryParam("start") @DefaultValue("0") Integer start,
			@QueryParam("limit") @DefaultValue("25") Integer limit,
			@QueryParam("managed") Boolean managed,
			@QueryParam("externalId") String externalId, @QueryParam("externalRef") String externalRef,
			@QueryParam("repositoryEntryKey") String repositoryEntryKey,
			@Context HttpServletRequest httpRequest, @Context Request request) {
		RepositoryManager rm = RepositoryManager.getInstance();

		Roles roles = getRoles(httpRequest);
		Identity identity = getIdentity(httpRequest);
		SearchRepositoryEntryParameters params = new SearchRepositoryEntryParameters(identity, roles, CourseModule.getCourseTypeName());
		params.setManaged(managed);

		if(StringHelper.containsNonWhitespace(externalId)) {
			params.setExternalId(externalId);
		}
		if(StringHelper.containsNonWhitespace(externalRef)) {
			params.setExternalRef(externalRef);
		}
		if(StringHelper.containsNonWhitespace(repositoryEntryKey) && StringHelper.isLong(repositoryEntryKey)) {
			try {
				params.setRepositoryEntryKeys(Collections.singletonList(new Long(repositoryEntryKey)));
			} catch (NumberFormatException e) {
				log.error("Cannot parse the following repository entry key: " + repositoryEntryKey);
			}
		}

		if(MediaTypeVariants.isPaged(httpRequest, request)) {
			int totalCount = rm.countGenericANDQueryWithRolesRestriction(params);
			List<RepositoryEntry> repoEntries = rm.genericANDQueryWithRolesRestriction(params, start, limit, true);
			CourseVO[] vos = toCourseVo(repoEntries);
			CourseVOes voes = new CourseVOes();
			voes.setCourses(vos);
			voes.setTotalCount(totalCount);
			return Response.ok(voes).build();
		} else {
			List<RepositoryEntry> repoEntries = rm.genericANDQueryWithRolesRestriction(params, 0, -1, false);
			CourseVO[] vos = toCourseVo(repoEntries);
			return Response.ok(vos).build();
		}
	}

	public static CourseVO[] toCourseVo(List<RepositoryEntry> repoEntries) {
		List<CourseVO> voList = new ArrayList<CourseVO>();

		int count=0;
		for (RepositoryEntry repoEntry : repoEntries) {
			try {
				ICourse course = loadCourse(repoEntry.getOlatResource().getResourceableId());
				voList.add(ObjectFactory.get(repoEntry, course));
				if(count % 33 == 0) {
					DBFactory.getInstance().commitAndCloseSession();
				}
			} catch (Exception e) {
				log.error("Cannot load the course with this repository entry: " + repoEntry, e);
			}
		}

		CourseVO[] vos = new CourseVO[voList.size()];
		voList.toArray(vos);
		return vos;
	}

	@Path("{courseId}")
	public CourseWebService getCourse(@PathParam("courseId") Long courseId) {
		ICourse course = loadCourse(courseId);
		if(course == null) {
			return null;
		}
		OLATResource ores = course.getCourseEnvironment().getCourseGroupManager().getCourseResource();
		return new CourseWebService(ores, course);
	}

	/**
	 * Creates an empty course, or a copy from a course if the parameter copyFrom is set.
	 * @response.representation.200.qname {http://www.example.com}courseVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The metadatas of the created course
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @param shortTitle The short title
	 * @param title The title
	 * @param sharedFolderSoftKey The repository entry key of a shared folder (optional)
	 * @param copyFrom The course primary key key to make a copy from (optional)
	 * @param initialAuthor The primary key of the initial author (optional)
	 * @param noAuthor True to create a course without the author
	 * @param request The HTTP request
	 * @return It returns the id of the newly created Course
	 */
	@PUT
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response createEmptyCourse(@QueryParam("shortTitle") String shortTitle, @QueryParam("title") String title,
			@QueryParam("displayName") String displayName, @QueryParam("description") String description,
			@QueryParam("softKey") String softKey, @QueryParam("access") Integer access, @QueryParam("membersOnly") Boolean membersOnly,
			@QueryParam("externalId") String externalId, @QueryParam("externalRef") String externalRef,
			@QueryParam("authors") String authors, @QueryParam("location") String location,
			@QueryParam("managedFlags") String managedFlags, @QueryParam("sharedFolderSoftKey") String sharedFolderSoftKey,
			@QueryParam("copyFrom") Long copyFrom, @QueryParam("initialAuthor") Long initialAuthor,
			@QueryParam("setAuthor")  @DefaultValue("true") Boolean setAuthor, @Context HttpServletRequest request) {
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		CourseConfigVO configVO = new CourseConfigVO();
		configVO.setSharedFolderSoftKey(sharedFolderSoftKey);

		int accessInt = (access == null ? RepositoryEntry.ACC_OWNERS : access.intValue());
		boolean membersOnlyBool = (membersOnly == null ? false : membersOnly.booleanValue());
		if(!StringHelper.containsNonWhitespace(displayName)) {
			displayName = shortTitle;
		}

		ICourse course;
		UserRequest ureq = getUserRequest(request);
		Identity id = null;
		if(setAuthor != null && setAuthor.booleanValue()) {
			if (initialAuthor != null) {
				id = getIdentity(initialAuthor);
			}
			if (id == null) {
				id = ureq.getIdentity();
			}
		}
		if(copyFrom != null) {
			course = copyCourse(copyFrom, ureq, id, shortTitle, title, displayName, description, softKey, accessInt, membersOnlyBool, authors, location, externalId, externalRef, managedFlags, configVO);
		} else {
			course = createEmptyCourse(id, shortTitle, title, displayName, description, softKey, accessInt, membersOnlyBool, authors, location, externalId, externalRef, managedFlags, configVO);
		}
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}
		CourseVO vo = ObjectFactory.get(course);
		return Response.ok(vo).build();
	}

	/**
	 * Creates an empty course
	 * @response.representation.200.qname {http://www.example.com}courseVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The metadatas of the created course
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @param courseVo The course
	 * @param request The HTTP request
	 * @return It returns the newly created course
	 */
	@PUT
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response createEmptyCourse(CourseVO courseVo, @Context HttpServletRequest request) {
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		UserRequest ureq = getUserRequest(request);

		CourseConfigVO configVO = new CourseConfigVO();
		ICourse course = createEmptyCourse(ureq.getIdentity(),
				courseVo.getTitle(), courseVo.getTitle(), courseVo.getTitle(), courseVo.getDescription(),
				courseVo.getSoftKey(), RepositoryEntry.ACC_OWNERS, false,
				courseVo.getAuthors(), courseVo.getLocation(),
				courseVo.getExternalId(), courseVo.getExternalRef(), courseVo.getManagedFlags(),
				configVO);
		CourseVO vo = ObjectFactory.get(course);
		return Response.ok(vo).build();
	}

	/**
	 * Imports a course from a course archive zip file
	 * @response.representation.200.qname {http://www.example.com}courseVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The metadatas of the imported course
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @param ownerUsername set the owner of the imported course to the user of this username.
	 * @param request The HTTP request
	 * @return It returns the imported course
	 */
	@POST
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	public Response importCourse(@QueryParam("ownerUsername") String ownerUsername, @Context HttpServletRequest request) {
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		UserRequest ureq = RestSecurityHelper.getUserRequest(request);

		Identity identity = null;
		// Set the owner of the imported course to the user defined in the parameter
		if (ownerUsername != null && !ownerUsername.isEmpty() && isAuthor(request)) {
			identity = BaseSecurityManager.getInstance().findIdentityByName(ownerUsername);
			if(identity == null) {
				return Response.serverError().status(Status.BAD_REQUEST).build();
			}
		}
		if (identity == null) {
			identity = ureq.getIdentity();
		}

		MultipartReader partsReader = null;
		try {
			partsReader = new MultipartReader(request);
			File tmpFile = partsReader.getFile();
			long length = tmpFile.length();
			if(length > 0) {
				Long accessRaw = partsReader.getLongValue("access");
				int access = accessRaw != null ? accessRaw.intValue() : RepositoryEntry.ACC_OWNERS;
				String membersOnlyRaw = partsReader.getValue("membersOnly");
				boolean membersonly = "true".equals(membersOnlyRaw);
				String softKey = partsReader.getValue("softkey");
				String displayName = partsReader.getValue("displayname");
				ICourse course = importCourse(ureq, identity, tmpFile, displayName, softKey, access, membersonly);
				CourseVO vo = ObjectFactory.get(course);
				return Response.ok(vo).build();
			}
			return Response.serverError().status(Status.NO_CONTENT).build();
		} catch (Exception e) {
			log.error("Error while importing a file",e);
		} finally {
			MultipartReader.closeQuietly(partsReader);
		}

		CourseVO vo = null;
		return Response.ok(vo).build();
	}

	public static boolean isCourseAccessible(ICourse course, boolean authorRightsMandatory, HttpServletRequest request) {
		if(authorRightsMandatory && !isAuthor(request)) {
			return false;
		}

		Identity identity = getIdentity(request);
		RepositoryEntry entry = RepositoryManager.getInstance().lookupRepositoryEntry(course, true);
		ACService acManager = CoreSpringFactory.getImpl(ACService.class);
		AccessResult result = acManager.isAccessible(entry, identity, false);
		if(result.isAccessible()) {
			return true;
		}
		return false;
	}

	public static ICourse loadCourse(Long courseId) {
		try {
			ICourse course = CourseFactory.loadCourse(courseId);
			return course;
		} catch(Exception ex) {
			log.error("cannot load course with id: " + courseId, ex);
			return null;
		}
	}

	public static ICourse importCourse(UserRequest ureq, Identity identity, File fCourseImportZIP,
			String displayName, String softKey, int access, boolean membersOnly) {

		log.info("REST Import course " + displayName + " START");
		if(!StringHelper.containsNonWhitespace(displayName)) {
			displayName = "import-" + UUID.randomUUID().toString();
		}

		RepositoryHandler handler = RepositoryHandlerFactory.getInstance().getRepositoryHandler(CourseModule.getCourseTypeName());
		RepositoryEntry re = handler.importResource(identity, null, displayName, null, true, Locale.ENGLISH, fCourseImportZIP, null);

		if(StringHelper.containsNonWhitespace(softKey)) {
			re.setSoftkey(softKey);
		}
		//make the repository
		if(membersOnly) {
			re.setMembersOnly(true);
			re.setAccess(RepositoryEntry.ACC_OWNERS);
		} else {
			re.setAccess(access);
		}
		CoreSpringFactory.getImpl(RepositoryService.class).update(re);
		log.info("REST Import course " + displayName + " END");

		//publish
		log.info("REST Publish course " + displayName + " START");
		ICourse course = CourseFactory.loadCourse(re);
		CourseFactory.publishCourse(course, RepositoryEntry.ACC_USERS, false,  identity, ureq.getLocale());
		log.info("REST Publish course " + displayName + " END");
		return course;
	}

	private static ICourse copyCourse(Long copyFrom, UserRequest ureq, Identity initialAuthor, String shortTitle, String longTitle, String displayName,
			String description, String softKey, int access, boolean membersOnly, String authors, String location, String externalId, String externalRef,
			String managedFlags, CourseConfigVO courseConfigVO) {

		//String learningObjectives = name + " (Example of creating a new course)";

		OLATResourceable originalOresTrans = OresHelper.createOLATResourceableInstance(CourseModule.class, copyFrom);
		RepositoryEntry src = RepositoryManager.getInstance().lookupRepositoryEntry(originalOresTrans, false);
		if(src == null) {
			src = RepositoryManager.getInstance().lookupRepositoryEntry(copyFrom, false);
		}
		if(src == null) {
			log.warn("Cannot find course to copy from: " + copyFrom);
			return null;
		}
		OLATResource originalOres = OLATResourceManager.getInstance().findResourceable(src.getOlatResource());
		boolean isAlreadyLocked = RepositoryHandlerFactory.getInstance().getRepositoryHandler(src).isLocked(originalOres);
		LockResult lockResult = RepositoryHandlerFactory.getInstance().getRepositoryHandler(src).acquireLock(originalOres, ureq.getIdentity());

		//check range of access
		if(access < 1 || access > RepositoryEntry.ACC_USERS_GUESTS) {
			access = RepositoryEntry.ACC_OWNERS;
		}

		if(lockResult == null || (lockResult != null && lockResult.isSuccess()) && !isAlreadyLocked) {
			RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);

			//create new repo entry
			String name;
			if(description == null || description.trim().length() == 0) {
				description = src.getDescription();
			}

			if (courseConfigVO != null && StringHelper.containsNonWhitespace(displayName)) {
				name = displayName;
			} else {
				name = "Copy of " + src.getDisplayname();
			}

			String resName = src.getResourcename();
			if (resName == null) {
				resName = "";
			}

			OLATResource sourceResource = src.getOlatResource();
			OLATResource copyResource = OLATResourceManager.getInstance().createOLATResourceInstance(sourceResource.getResourceableTypeName());
			RepositoryEntry preparedEntry = repositoryService.create(initialAuthor, null, resName, name,
					description, copyResource, RepositoryEntry.ACC_OWNERS);

			RepositoryHandler handler = RepositoryHandlerFactory.getInstance().getRepositoryHandler(src);
			preparedEntry = handler.copy(initialAuthor, src, preparedEntry);

			preparedEntry.setCanDownload(src.getCanDownload());
			if(StringHelper.containsNonWhitespace(softKey)) {
				preparedEntry.setSoftkey(softKey);
			}
			if(StringHelper.containsNonWhitespace(externalId)) {
				preparedEntry.setExternalId(externalId);
			}
			if(StringHelper.containsNonWhitespace(externalRef)) {
				preparedEntry.setExternalRef(externalRef);
			}
			if(StringHelper.containsNonWhitespace(authors)) {
				preparedEntry.setAuthors(authors);
			}
			if(StringHelper.containsNonWhitespace(location)) {
				preparedEntry.setLocation(location);
			}
			if(StringHelper.containsNonWhitespace(managedFlags)) {
				preparedEntry.setManagedFlagsString(managedFlags);
			}
			if(membersOnly) {
				preparedEntry.setMembersOnly(true);
				preparedEntry.setAccess(RepositoryEntry.ACC_OWNERS);
			} else {
				preparedEntry.setAccess(access);
			}
			preparedEntry.setAllowToLeaveOption(src.getAllowToLeaveOption());

			repositoryService.update(preparedEntry);

			// copy image if available
			RepositoryManager.getInstance().copyImage(src, preparedEntry);

			ICourse course = prepareCourse(preparedEntry,shortTitle, longTitle, courseConfigVO);
			RepositoryHandlerFactory.getInstance().getRepositoryHandler(src).releaseLock(lockResult);
			return course;
		}

		return null;
	}

	/**
	 * Create an empty course with some defaults settings
	 * @param initialAuthor Author
	 * @param shortTitle Title of the course
	 * @param longTitle Long title of the course
	 * @param courseConfigVO Can be null
	 * @return
	 */
	public static ICourse createEmptyCourse(Identity initialAuthor, String shortTitle, String longTitle, CourseConfigVO courseConfigVO) {
		return createEmptyCourse(initialAuthor, shortTitle, longTitle, shortTitle, null, null, RepositoryEntry.ACC_OWNERS, false, null, null, null, null, null, courseConfigVO);
	}

	/**
	 * Create an empty course with some settings
	 * @param initialAuthor
	 * @param shortTitle
	 * @param longTitle
	 * @param softKey
	 * @param externalId
	 * @param externalRef
	 * @param managedFlags
	 * @param courseConfigVO
	 * @return
	 */
	public static ICourse createEmptyCourse(Identity initialAuthor, String shortTitle, String longTitle, String reDisplayName,
			String description, String softKey, int access, boolean membersOnly, String authors, String location,
			String externalId, String externalRef, String managedFlags, CourseConfigVO courseConfigVO) {

		if(!StringHelper.containsNonWhitespace(reDisplayName)) {
			reDisplayName = shortTitle;
		}

		try {
			// create a repository entry
			RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
			OLATResource resource = OLATResourceManager.getInstance().createOLATResourceInstance(CourseModule.class);
			RepositoryEntry addedEntry = repositoryService.create(initialAuthor, null, "-", reDisplayName, null, resource, 0);
			if(StringHelper.containsNonWhitespace(softKey) && softKey.length() <= 30) {
				addedEntry.setSoftkey(softKey);
			}
			addedEntry.setLocation(location);
			addedEntry.setAuthors(authors);
			addedEntry.setExternalId(externalId);
			addedEntry.setExternalRef(externalRef);
			addedEntry.setManagedFlagsString(managedFlags);
			addedEntry.setDescription(description);
			if(RepositoryEntryManagedFlag.isManaged(addedEntry, RepositoryEntryManagedFlag.membersmanagement)) {
				addedEntry.setAllowToLeaveOption(RepositoryEntryAllowToLeaveOptions.never);
			} else {
				addedEntry.setAllowToLeaveOption(RepositoryEntryAllowToLeaveOptions.atAnyTime);//default
			}
			if(membersOnly) {
				addedEntry.setMembersOnly(true);
				addedEntry.setAccess(RepositoryEntry.ACC_OWNERS);
			} else {
				addedEntry.setAccess(access);
			}
			addedEntry = repositoryService.update(addedEntry);

			// create an empty course
			CourseFactory.createCourse(addedEntry, shortTitle, longTitle, "");

			return prepareCourse(addedEntry, shortTitle, longTitle, courseConfigVO);
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	private static ICourse prepareCourse(RepositoryEntry addedEntry, String shortTitle, String longTitle, CourseConfigVO courseConfigVO) {
		// set root node title
		String courseShortTitle = addedEntry.getDisplayname();
		if(StringHelper.containsNonWhitespace(shortTitle)) {
			courseShortTitle = shortTitle;
		}
		String courseLongTitle = addedEntry.getDisplayname();
		if(StringHelper.containsNonWhitespace(longTitle)) {
			courseLongTitle = longTitle;
		}

		ICourse course = CourseFactory.openCourseEditSession(addedEntry.getOlatResource().getResourceableId());
		course.getRunStructure().getRootNode().setShortTitle(Formatter.truncate(courseShortTitle, 25));
		course.getRunStructure().getRootNode().setLongTitle(courseLongTitle);

		CourseNode rootNode = ((CourseEditorTreeNode) course.getEditorTreeModel().getRootNode()).getCourseNode();
		rootNode.setShortTitle(Formatter.truncate(courseShortTitle, 25));
		rootNode.setLongTitle(courseLongTitle);

		if(courseConfigVO != null) {
			CourseConfig courseConfig = course.getCourseEnvironment().getCourseConfig();
			if(StringHelper.containsNonWhitespace(courseConfigVO.getSharedFolderSoftKey())) {
				courseConfig.setSharedFolderSoftkey(courseConfigVO.getSharedFolderSoftKey());
			}
			if(courseConfigVO.getCalendar() != null) {
				courseConfig.setCalendarEnabled(courseConfigVO.getCalendar().booleanValue());
			}
			if(courseConfigVO.getChat() != null) {
				courseConfig.setChatIsEnabled(courseConfigVO.getChat().booleanValue());
			}
			if(courseConfigVO.getEfficencyStatement() != null) {
				courseConfig.setEfficencyStatementIsEnabled(courseConfigVO.getEfficencyStatement().booleanValue());
			}
			if(StringHelper.containsNonWhitespace(courseConfigVO.getCssLayoutRef())) {
				courseConfig.setCssLayoutRef(courseConfigVO.getCssLayoutRef());
			}
			if(StringHelper.containsNonWhitespace(courseConfigVO.getGlossarySoftkey())) {
				courseConfig.setGlossarySoftKey(courseConfigVO.getGlossarySoftkey());
			}
			CourseFactory.setCourseConfig(course.getResourceableId(), courseConfig);
		}

		CourseFactory.saveCourse(course.getResourceableId());
		CourseFactory.closeCourseEditSession(course.getResourceableId(), true);
		return CourseFactory.loadCourse(addedEntry);
	}
}
