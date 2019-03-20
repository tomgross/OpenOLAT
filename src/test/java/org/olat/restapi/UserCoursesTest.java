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
package org.olat.restapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.olat.basesecurity.GroupRoles;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.services.mark.MarkManager;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.course.ICourse;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.restapi.repository.course.CoursesWebService;
import org.olat.restapi.support.vo.CourseVO;
import org.olat.restapi.support.vo.CourseVOes;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatJerseyTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Ignore
public class UserCoursesTest extends OlatJerseyTestCase {
	
	private static final OLog log = Tracing.createLoggerFor(UserCoursesTest.class);
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private MarkManager markManager;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private RepositoryService repositoryService;
	
	@Test
	public void testMyCourses() throws IOException, URISyntaxException {
		//prepare a course with a participant
		Identity user = JunitTestHelper.createAndPersistIdentityAsUser("My-course-" + UUID.randomUUID().toString());
		ICourse course = CoursesWebService.createEmptyCourse(user, "My course 1", "My course", null);
		RepositoryEntry courseRe = repositoryManager.lookupRepositoryEntry(course, true);
		repositoryManager.setAccess(courseRe, RepositoryEntry.ACC_OWNERS, true);
		repositoryService.addRole(user, courseRe, GroupRoles.participant.name());
		dbInstance.commitAndCloseSession();
		
		RestConnection conn = new RestConnection();
		Assert.assertTrue(conn.login(user.getName(), JunitTestHelper.PWD));
		
		//without paging
		URI request = UriBuilder.fromUri(getContextURI()).path("/users").path(user.getKey().toString()).path("/courses/my").build();
		HttpGet method = conn.createGet(request, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream body = response.getEntity().getContent();
		List<CourseVO> courses = parseCourseArray(body);
		Assert.assertNotNull(courses);
		Assert.assertEquals(1, courses.size());

		//with paging
		URI pagedRequest = UriBuilder.fromUri(getContextURI()).path("/users").path(user.getKey().toString()).path("/courses/my")
				.queryParam("start", "0").queryParam("limit", "10").build();
		HttpGet pagedMethod = conn.createGet(pagedRequest, MediaType.APPLICATION_JSON + ";pagingspec=1.0", true);
		HttpResponse pagedResponse = conn.execute(pagedMethod);
		Assert.assertEquals(200, pagedResponse.getStatusLine().getStatusCode());
		InputStream pagedBody = pagedResponse.getEntity().getContent();
		CourseVOes pagedCourses = conn.parse(pagedBody, CourseVOes.class);
		Assert.assertNotNull(pagedCourses);
		Assert.assertEquals(1, pagedCourses.getTotalCount());
		Assert.assertNotNull(pagedCourses.getCourses());
		Assert.assertEquals(1, pagedCourses.getCourses().length);

		conn.shutdown();
	}
	
	@Test
	public void testTeachedCourses() throws IOException, URISyntaxException {
		//prepare a course with a tutor
		Identity teacher = JunitTestHelper.createAndPersistIdentityAsUser("Course-teacher-" + UUID.randomUUID().toString());
		ICourse course = CoursesWebService.createEmptyCourse(teacher, "A course to teach", "A course to teach", null);
		RepositoryEntry courseRe = repositoryManager.lookupRepositoryEntry(course, true);
		repositoryManager.setAccess(courseRe, RepositoryEntry.ACC_OWNERS, true);
		repositoryService.addRole(teacher, courseRe, GroupRoles.coach.name());
		dbInstance.commitAndCloseSession();
		
		RestConnection conn = new RestConnection();
		Assert.assertTrue(conn.login(teacher.getName(), JunitTestHelper.PWD));
		
		//without paging
		URI request = UriBuilder.fromUri(getContextURI()).path("/users").path(teacher.getKey().toString()).path("/courses/teached").build();
		HttpGet method = conn.createGet(request, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream body = response.getEntity().getContent();
		List<CourseVO> courses = parseCourseArray(body);
		Assert.assertNotNull(courses);
		Assert.assertEquals(1, courses.size());

		//with paging
		URI pagedRequest = UriBuilder.fromUri(getContextURI()).path("/users").path(teacher.getKey().toString()).path("/courses/teached")
				.queryParam("start", "0").queryParam("limit", "10").build();
		HttpGet pagedMethod = conn.createGet(pagedRequest, MediaType.APPLICATION_JSON + ";pagingspec=1.0", true);
		HttpResponse pagedResponse = conn.execute(pagedMethod);
		Assert.assertEquals(200, pagedResponse.getStatusLine().getStatusCode());
		InputStream pagedBody = pagedResponse.getEntity().getContent();
		CourseVOes pagedCourses = conn.parse(pagedBody, CourseVOes.class);
		Assert.assertNotNull(pagedCourses);
		Assert.assertEquals(1, pagedCourses.getTotalCount());
		Assert.assertNotNull(pagedCourses.getCourses());
		Assert.assertEquals(1, pagedCourses.getCourses().length);

		conn.shutdown();
	}
	
	@Test
	public void testFavoritCourses() throws IOException, URISyntaxException {
		//prepare a course with a tutor
		Identity me = JunitTestHelper.createAndPersistIdentityAsUser("Course-teacher-" + UUID.randomUUID().toString());
		ICourse course = CoursesWebService.createEmptyCourse(me, "A course to teach", "A course to teach", null);
		RepositoryEntry courseRe = repositoryManager.lookupRepositoryEntry(course, true);
		repositoryManager.setAccess(courseRe, RepositoryEntry.ACC_USERS, false);
		markManager.setMark(courseRe, me, null, "[RepositoryEntry:" + courseRe.getKey() + "]");	
		dbInstance.commitAndCloseSession();

		RestConnection conn = new RestConnection();
		Assert.assertTrue(conn.login(me.getName(), JunitTestHelper.PWD));
		
		//without paging
		URI request = UriBuilder.fromUri(getContextURI()).path("/users").path(me.getKey().toString()).path("/courses/favorite").build();
		HttpGet method = conn.createGet(request, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream body = response.getEntity().getContent();
		List<CourseVO> courses = parseCourseArray(body);
		Assert.assertNotNull(courses);
		Assert.assertEquals(1, courses.size());
		
		//with paging
		URI pagedRequest = UriBuilder.fromUri(getContextURI()).path("/users").path(me.getKey().toString()).path("/courses/favorite")
				.queryParam("start", "0").queryParam("limit", "10").build();
		HttpGet pagedMethod = conn.createGet(pagedRequest, MediaType.APPLICATION_JSON + ";pagingspec=1.0", true);
		HttpResponse pagedResponse = conn.execute(pagedMethod);
		Assert.assertEquals(200, pagedResponse.getStatusLine().getStatusCode());
		InputStream pagedBody = pagedResponse.getEntity().getContent();
		CourseVOes pagedCourses = conn.parse(pagedBody, CourseVOes.class);
		Assert.assertNotNull(pagedCourses);
		Assert.assertEquals(1, pagedCourses.getTotalCount());
		Assert.assertNotNull(pagedCourses.getCourses());
		Assert.assertEquals(1, pagedCourses.getCourses().length);
		

		conn.shutdown();
	}
	
	protected List<CourseVO> parseCourseArray(InputStream body) {
		try {
			ObjectMapper mapper = new ObjectMapper(jsonFactory);
			return mapper.readValue(body, new TypeReference<List<CourseVO>>(){/* */});
		} catch (Exception e) {
			log.error("", e);
			return null;
		}
	}
}
