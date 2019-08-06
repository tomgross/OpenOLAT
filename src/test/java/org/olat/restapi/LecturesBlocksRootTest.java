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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Assert;
import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.course.ICourse;
import org.olat.modules.lecture.LectureBlock;
import org.olat.modules.lecture.LectureService;
import org.olat.modules.lecture.restapi.LectureBlockVO;
import org.olat.repository.RepositoryEntry;
import org.olat.restapi.repository.course.CoursesWebService;
import org.olat.restapi.support.vo.CourseConfigVO;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatJerseyTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 13 sept. 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LecturesBlocksRootTest extends OlatJerseyTestCase {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private LectureService lectureService;
	
	@Test
	public void getLecturesBlock()
	throws IOException, URISyntaxException {
		Identity author = JunitTestHelper.createAndPersistIdentityAsAuthor("lect-root-all");
		ICourse course = CoursesWebService.createEmptyCourse(author, "Course with absence", "Course with absence", new CourseConfigVO());
		RepositoryEntry entry = course.getCourseEnvironment().getCourseGroupManager().getCourseEntry();
		LectureBlock block = createLectureBlock(entry);
		dbInstance.commit();
		lectureService.addTeacher(block, author);
		dbInstance.commit();

		RestConnection conn = new RestConnection();
		Assert.assertTrue(conn.login("administrator", "openolat"));

		URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("lectures").build();
		HttpGet method = conn.createGet(uri, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		List<LectureBlockVO> voList = parseLectureBlockArray(response.getEntity().getContent());
		Assert.assertNotNull(voList);
		Assert.assertFalse(voList.isEmpty());
		
		LectureBlockVO lectureBlockVo = null;
		for(LectureBlockVO vo:voList) {
			if(vo.getKey().equals(block.getKey())) {
				lectureBlockVo = vo;
			}
		}
		
		Assert.assertNotNull(lectureBlockVo);
		Assert.assertEquals(block.getKey(), lectureBlockVo.getKey());
		Assert.assertEquals(entry.getKey(), lectureBlockVo.getRepoEntryKey());
	}
	
	@Test
	public void getLecturesBlock_date()
	throws IOException, URISyntaxException {
		Identity author = JunitTestHelper.createAndPersistIdentityAsAuthor("lect-root-1");
		ICourse course = CoursesWebService.createEmptyCourse(author, "Course with absence", "Course with absence", new CourseConfigVO());
		RepositoryEntry entry = course.getCourseEnvironment().getCourseGroupManager().getCourseEntry();
		LectureBlock block = createLectureBlock(entry);
		dbInstance.commit();
		lectureService.addTeacher(block, author);
		dbInstance.commit();

		RestConnection conn = new RestConnection();
		Assert.assertTrue(conn.login("administrator", "openolat"));

		String date = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").format(new Date());
		URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("lectures").queryParam("date", date).build();
		HttpGet method = conn.createGet(uri, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		List<LectureBlockVO> voList = parseLectureBlockArray(response.getEntity().getContent());
		Assert.assertNotNull(voList);
		Assert.assertFalse(voList.isEmpty());
		
		LectureBlockVO lectureBlockVo = null;
		for(LectureBlockVO vo:voList) {
			if(vo.getKey().equals(block.getKey())) {
				lectureBlockVo = vo;
			}
		}
		
		Assert.assertNotNull(lectureBlockVo);
		Assert.assertEquals(block.getKey(), lectureBlockVo.getKey());
		Assert.assertEquals(entry.getKey(), lectureBlockVo.getRepoEntryKey());
	}
	
	private LectureBlock createLectureBlock(RepositoryEntry entry) {
		LectureBlock lectureBlock = lectureService.createLectureBlock(entry);
		lectureBlock.setStartDate(new Date());
		lectureBlock.setEndDate(new Date());
		lectureBlock.setTitle("Hello lecturers");
		lectureBlock.setPlannedLecturesNumber(4);
		return lectureService.save(lectureBlock, null);
	}
	
	protected List<LectureBlockVO> parseLectureBlockArray(InputStream body) {
		try {
			ObjectMapper mapper = new ObjectMapper(jsonFactory); 
			return mapper.readValue(body, new TypeReference<List<LectureBlockVO>>(){/* */});
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
