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

import static org.olat.restapi.security.RestSecurityHelper.isAuthorEditor;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.GroupRoles;
import org.olat.core.CoreSpringFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.IQTESTCourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.scoring.ScoreAccounting;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.ims.qti.QTIResultSet;
import org.olat.ims.qti.container.AssessmentContext;
import org.olat.ims.qti.container.HttpItemInput;
import org.olat.ims.qti.container.ItemContext;
import org.olat.ims.qti.container.ItemInput;
import org.olat.ims.qti.container.ItemsInput;
import org.olat.ims.qti.container.SectionContext;
import org.olat.ims.qti.navigator.Info;
import org.olat.ims.qti.navigator.MenuItemNavigator;
import org.olat.ims.qti.navigator.Navigator;
import org.olat.ims.qti.process.AssessmentFactory;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.assessment.Role;
import org.olat.modules.iq.IQManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.restapi.security.RestSecurityHelper;
import org.olat.restapi.support.vo.AssessableResultsVO;

/**
 * 
 * Description:<br>
 * Retrieve and import course assessments
 * 
 * <P>
 * Initial Date:  7 apr. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com
 */
@Path("repo/courses/{courseId}/assessments")
public class CourseAssessmentWebService {
	
	private static final OLog log = Tracing.createLoggerFor(CourseAssessmentWebService.class);
	
	private static final String VERSION  = "1.0";
	
	private static final CacheControl cc = new CacheControl();
	static {
		cc.setMaxAge(-1);
	}
	
	/**
	 * Retireves the version of the Course Assessment Web Service.
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
	 * Returns the results of the course.
	 * @response.representation.200.qname {http://www.example.com}assessableResultsVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc Array of results for the whole the course
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_ASSESSABLERESULTSVOes}
   * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return
	 */
	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getCourseResults(@PathParam("courseId") Long courseId, @Context HttpServletRequest httpRequest, @Context Request request) {
		if(!RestSecurityHelper.isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		ICourse course = CoursesWebService.loadCourse(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}
		//fxdiff VCRP-1,2: access control of resources
		List<Identity> courseUsers = loadUsers(course);
		int i=0;
		
		Date lastModified = null;
		AssessableResultsVO[] results = new AssessableResultsVO[courseUsers.size()];
		for(Identity courseUser:courseUsers) {
			AssessableResultsVO result = getRootResult(courseUser, course);
			if(lastModified == null || (result.getLastModifiedDate() != null && lastModified.before(result.getLastModifiedDate()))) {
				lastModified = result.getLastModifiedDate();
			}
			results[i++] = result;
		}
		
		if(lastModified != null) {
			Response.ResponseBuilder response = request.evaluatePreconditions(lastModified);
			if(response != null) {
				return response.build();
			}
			return Response.ok(results).lastModified(lastModified).cacheControl(cc).build();
		}
		return Response.ok(results).build();
	}
	
	/**
	 * Returns the results of the course.
	 * @response.representation.200.qname {http://www.example.com}assessableResultsVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The result of the course
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_ASSESSABLERESULTSVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The identity or the course not found
	 * @param courseId The course resourceable's id
	 * @param identityKey The id of the user
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return
	 */
	@GET
	@Path("users/{identityKey}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getCourseResultsOf(@PathParam("courseId") Long courseId, @PathParam("identityKey") Long identityKey, @Context HttpServletRequest httpRequest, @Context Request request) {
		if(!RestSecurityHelper.isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		try {
			Identity userIdentity = BaseSecurityManager.getInstance().loadIdentityByKey(identityKey, false);
			if(userIdentity == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			}

			ICourse course = CoursesWebService.loadCourse(courseId);
			if(course == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			}
				
			AssessableResultsVO results = getRootResult(userIdentity, course);
			if(results.getLastModifiedDate() != null) {
				Response.ResponseBuilder response = request.evaluatePreconditions(results.getLastModifiedDate());
				if (response != null) {
					return response.build();
			  }
			}

			ResponseBuilder response = Response.ok(results);
			if(results.getLastModifiedDate() != null) {
				response = response.lastModified(results.getLastModifiedDate()).cacheControl(cc);
			}
			return response.build();
		} catch (Throwable e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Exports results for an assessable course node for all students.
	 * @response.representation.200.qname {http://www.example.com}assessableResultsVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc Export all results of all user of the course
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_ASSESSABLERESULTSVOes}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param nodeId The id of the course building block
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return
	 */
	@GET
	@Path("{nodeId}")
	@Produces( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response getAssessableResults(@PathParam("courseId") Long courseId, @PathParam("nodeId") Long nodeId,
			@Context HttpServletRequest httpRequest, @Context Request request) {
		if(!RestSecurityHelper.isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		ICourse course = CoursesWebService.loadCourse(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		//fxdiff VCRP-1,2: access control of resources
		List<Identity> courseUsers = loadUsers(course);
		int i=0;
		Date lastModified = null;
		AssessableResultsVO[] results = new AssessableResultsVO[courseUsers.size()];
		for(Identity courseUser:courseUsers) {
			AssessableResultsVO result = getNodeResult(courseUser, course, nodeId);
			if(lastModified == null || (result.getLastModifiedDate() != null && lastModified.before(result.getLastModifiedDate()))) {
				lastModified = result.getLastModifiedDate();
			}
			results[i++] = result;
		}
		
		if(lastModified != null) {
			Response.ResponseBuilder response = request.evaluatePreconditions(lastModified);
			if(response != null) {
				return response.build();
			}
			return Response.ok(results).lastModified(lastModified).cacheControl(cc).build();
		}
		
		return Response.ok(results).build();
	}
	
	/**
	 * Imports results for an assessable course node for the authenticated student.
	 * @response.representation.qname {http://www.example.com}assessableResultsVO
   * @response.representation.mediaType application/xml, application/json
   * @response.representation.doc A result to import
   * @response.representation.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_ASSESSABLERESULTSVO}
   * @response.representation.200.doc Import successful
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The identity not found
	 * @param courseId The resourceable id of the course
	 * @param nodeId The id of the course building block
	 * @param resultsVO The results
	 * @param request The HTTP request
	 * @return
	 */
	@POST
	@Path("{nodeId}")
	@Consumes( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response postAssessableResults(@PathParam("courseId") Long courseId, @PathParam("nodeId") String nodeId,
			AssessableResultsVO resultsVO, @Context HttpServletRequest request) {
		if(!RestSecurityHelper.isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		Identity identity = RestSecurityHelper.getUserRequest(request).getIdentity();
		if(identity == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}
		attachAssessableResults(courseId, nodeId, identity, resultsVO);
		return Response.ok().build();
	}
	
	private void attachAssessableResults(Long courseResourceableId, String nodeKey, Identity requestIdentity, AssessableResultsVO resultsVO) {
		try {
			ICourse course = CourseFactory.openCourseEditSession(courseResourceableId);
			CourseNode node = getParentNode(course, nodeKey);
			if (!(node instanceof AssessableCourseNode)) { throw new IllegalArgumentException(
					"The supplied node key does not refer to an AssessableCourseNode"); }
			BaseSecurity securityManager = BaseSecurityManager.getInstance();
			Identity userIdentity = securityManager.loadIdentityByKey(resultsVO.getIdentityKey());

			// create an identenv with no roles, no attributes, no locale
			IdentityEnvironment ienv = new IdentityEnvironment();
			ienv.setIdentity(userIdentity);
			UserCourseEnvironment userCourseEnvironment = new UserCourseEnvironmentImpl(ienv, course.getCourseEnvironment());

			// Fetch all score and passed and calculate score accounting for the
			// entire course
			userCourseEnvironment.getScoreAccounting().evaluateAll();

			if (node instanceof IQTESTCourseNode) {
				importTestItems(course, nodeKey, requestIdentity, resultsVO);
			} else {
				AssessableCourseNode assessableNode = (AssessableCourseNode) node;
				ScoreEvaluation scoreEval = new ScoreEvaluation(resultsVO.getScore(), Boolean.TRUE, Boolean.TRUE, new Long(nodeKey));//not directly pass this key
				assessableNode.updateUserScoreEvaluation(scoreEval, userCourseEnvironment, requestIdentity, true, Role.coach);
			}

			CourseFactory.saveCourseEditorTreeModel(course.getResourceableId());
			CourseFactory.closeCourseEditSession(course.getResourceableId(), true);
		} catch (Throwable e) {
			throw new WebApplicationException(e);
		}
	}
	


	private void importTestItems(ICourse course, String nodeKey, Identity identity, AssessableResultsVO resultsVO) {
		try {
			IQManager iqManager = CoreSpringFactory.getImpl(IQManager.class);

			// load the course and the course node
			CourseNode courseNode = getParentNode(course, nodeKey);
			ModuleConfiguration modConfig = courseNode.getModuleConfiguration();

			// check if the result set is already saved
			QTIResultSet set = iqManager.getLastResultSet(identity, course.getResourceableId(), courseNode.getIdent());
			if (set == null) {
				String resourcePathInfo = course.getResourceableId() + File.separator + courseNode.getIdent();

				// The use of these classes AssessmentInstance, AssessmentContext and
				// Navigator
				// allow the use of the persistence mechanism of OLAT without
				// duplicating the code.
				// The consequence is that we must loop on section and items and set the
				// navigator on
				// the right position before submitting the inputs.
				AssessmentInstance ai = AssessmentFactory.createAssessmentInstance(identity, "", modConfig, false, course.getResourceableId(), courseNode.getIdent(), resourcePathInfo, null);
				Navigator navigator = ai.getNavigator();
				navigator.startAssessment();
				// The type of the navigator depends on the setting of the course node
				boolean perItem = (navigator instanceof MenuItemNavigator);

				Map<String, ItemInput> datas = convertToHttpItemInput(resultsVO.getResults());

				AssessmentContext ac = ai.getAssessmentContext();
				int sectioncnt = ac.getSectionContextCount();
				// loop on the sections
				for (int i = 0; i < sectioncnt; i++) {
					SectionContext sc = ac.getSectionContext(i);
					navigator.goToSection(i);

					ItemsInput iips = new ItemsInput();
					int itemcnt = sc.getItemContextCount();
					// loop on the items
					for (int j = 0; j < itemcnt; j++) {

						ItemContext it = sc.getItemContext(j);
						if (datas.containsKey(it.getIdent())) {

							if (perItem) {
								// save the datas on a per item base
								navigator.goToItem(i, j);

								// the navigator can give informations on its current status
								Info info = navigator.getInfo();
								if (info.containsError()) {
									// some items cannot processed twice
								} else {
									iips.addItemInput(datas.get(it.getIdent()));
									navigator.submitItems(iips);
									iips = new ItemsInput();
								}
							} else {
								// put for a section
								iips.addItemInput(datas.get(it.getIdent()));
							}
						}
					}

					if (!perItem) {
						// save the inputs of the section. In a section based navigation,
						// we must saved the inputs of the whole section at once
						navigator.submitItems(iips);
					}
				}

				navigator.submitAssessment();

				// persist the QTIResultSet (o_qtiresultset and o_qtiresult) on the
				// database
				// TODO iqManager.persistResults(ai, course.getResourceableId(),
				// courseNode.getIdent(), identity, "127.0.0.1");

				// write the reporting file on the file system
				// The path is <olatdata> / resreporting / <username> / Assessment /
				// <assessId>.xml
				// TODO Document docResReporting = iqManager.getResultsReporting(ai,
				// identity, Locale.getDefault());
				// TODO FilePersister.createResultsReporting(docResReporting, identity,
				// ai.getFormattedType(), ai.getAssessID());

				// prepare all instances needed to save the score at the course node
				// level
				CourseEnvironment cenv = course.getCourseEnvironment();
				IdentityEnvironment identEnv = new IdentityEnvironment();
				identEnv.setIdentity(identity);
				UserCourseEnvironment userCourseEnv = new UserCourseEnvironmentImpl(identEnv, cenv);

				// update scoring overview for the user in the current course
				Float score = ac.getScore();
				Boolean passed = ac.isPassed();
				ScoreEvaluation sceval = new ScoreEvaluation(score, passed, passed, new Long(nodeKey));//perhaps don't pass this key directly
				AssessableCourseNode acn = (AssessableCourseNode) courseNode;
				// assessment nodes are assessable
				boolean incrementUserAttempts = true;
				acn.updateUserScoreEvaluation(sceval, userCourseEnv, identity, incrementUserAttempts, Role.coach);
			} else {
				log.error("Result set already saved");
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	private Map<String, ItemInput> convertToHttpItemInput(Map<Long, String> results) {
		Map<String, ItemInput> datas = new HashMap<String, ItemInput>();
		for (Long key : results.keySet()) {
			HttpItemInput iip = new HttpItemInput(results.get(key));
			iip.putSingle(key.toString(), results.get(key));
			//TODO somehow obtain answer from value
			datas.put(iip.getIdent(), iip);
		}
		return datas;
	}

	private CourseNode getParentNode(ICourse course, String parentNodeId) {
		if (parentNodeId == null) {
			return course.getRunStructure().getRootNode();
		} else {
			return course.getEditorTreeModel().getCourseNode(parentNodeId);
		}
	}

	/**
	 * Returns the results of a student at a specific assessable node
	 * @response.representation.200.qname {http://www.example.com}assessableResultsVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The result of a user at a specific node
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_ASSESSABLERESULTSVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The identity or the course not found
	 * @param courseId The course resourceable's id
	 * @param nodeId The ident of the course building block
	 * @param identityKey The id of the user
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return
	 */
	@GET
	@Path("{nodeId}/users/{identityKey}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getCourseNodeResultsForNode(@PathParam("courseId") Long courseId, @PathParam("nodeId") Long nodeId, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest httpRequest, @Context Request request) {
		if(!RestSecurityHelper.isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		try {
			Identity userIdentity = BaseSecurityManager.getInstance().loadIdentityByKey(identityKey, false);
			if(userIdentity == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			}

			ICourse course = CoursesWebService.loadCourse(courseId);
			if(course == null) {
				return Response.serverError().status(Status.NOT_FOUND).build();
			}

			AssessableResultsVO results = getNodeResult(userIdentity, course, nodeId);
			if(results.getLastModifiedDate() != null) {
				Response.ResponseBuilder response = request.evaluatePreconditions(results.getLastModifiedDate());
				if(response != null) {
					return response.build();
				}
				return Response.ok(results).lastModified(results.getLastModifiedDate()).cacheControl(cc).build();
			}
			return Response.ok(results).build();
		} catch (Throwable e) {
			throw new WebApplicationException(e);
		}
	}
	
	private AssessableResultsVO getRootResult(Identity identity, ICourse course) {
		CourseNode rootNode = course.getRunStructure().getRootNode();
		return getRootResult(identity, course, rootNode);
	}
	
	private AssessableResultsVO getNodeResult(Identity identity, ICourse course, Long nodeId) {
		CourseNode courseNode = course.getRunStructure().getNode(nodeId.toString());
		return getRootResult(identity, course, courseNode);
	}
	
	private AssessableResultsVO getRootResult(Identity identity, ICourse course, CourseNode courseNode) {
		AssessableResultsVO results = new AssessableResultsVO();
		results.setIdentityKey(identity.getKey());
		
		// create an identenv with no roles, no attributes, no locale
		IdentityEnvironment ienv = new IdentityEnvironment();
		ienv.setIdentity(identity);
		UserCourseEnvironment userCourseEnvironment = new UserCourseEnvironmentImpl(ienv, course.getCourseEnvironment());
		
		// Fetch all score and passed and calculate score accounting for the entire course
		ScoreAccounting scoreAccounting = userCourseEnvironment.getScoreAccounting();
		scoreAccounting.evaluateAll();
		
		if(courseNode instanceof AssessableCourseNode) {
			AssessableCourseNode assessableRootNode = (AssessableCourseNode)courseNode;
			ScoreEvaluation scoreEval = scoreAccounting.evalCourseNode(assessableRootNode);
			results.setScore(scoreEval.getScore());
			results.setPassed(scoreEval.getPassed());
			results.setLastModifiedDate(getLastModificationDate(identity, course, courseNode));
		}
		
		return results;
	}
	
	private Date getLastModificationDate(Identity assessedIdentity, ICourse course, CourseNode courseNode) {
		AssessmentManager am = course.getCourseEnvironment().getAssessmentManager();
		return am.getScoreLastModifiedDate(courseNode, assessedIdentity);
	}

	private List<Identity> loadUsers(ICourse course) {
		List<Identity> identities = new ArrayList<Identity>();
		List<BusinessGroup> groups = course.getCourseEnvironment().getCourseGroupManager().getAllBusinessGroups();

		Set<Long> check = new HashSet<Long>();
		BusinessGroupService businessGroupService = CoreSpringFactory.getImpl(BusinessGroupService.class);
		List<Identity> participants = businessGroupService.getMembers(groups, GroupRoles.participant.name());
		for(Identity participant:participants) {
			if(!check.contains(participant.getKey())) {
				identities.add(participant);
				check.add(participant.getKey());
			}
		}
		
		RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
		RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(course, false);
		if(re != null) {
			List<Identity> ids = repositoryService.getMembers(re, GroupRoles.participant.name());
			for(Identity id:ids) {
				if(!check.contains(id.getKey())) {
					identities.add(id);
					check.add(id.getKey());
				}
			}
		}

		return identities;
	}
}