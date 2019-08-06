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
package org.olat.modules.lecture.restapi;

import static org.olat.restapi.security.RestSecurityHelper.getIdentity;
import static org.olat.restapi.security.RestSecurityHelper.getRoles;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.core.id.Roles;
import org.olat.modules.lecture.LectureBlock;
import org.olat.modules.lecture.LectureBlockStatus;
import org.olat.modules.lecture.LectureRollCallStatus;
import org.olat.modules.lecture.LectureService;
import org.olat.modules.lecture.RepositoryEntryLectureConfiguration;
import org.olat.modules.lecture.model.LectureBlockRefImpl;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 8 juin 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LectureBlocksWebService {
	
	private final RepositoryEntry entry;
	
	@Autowired
	private LectureService lectureService;
	
	public LectureBlocksWebService(RepositoryEntry entry) {
		this.entry = entry;
	}
	
	/**
	 * Return the lecture blocks of the specified course or repository entry.
	 * @response.representation.200.qname {http://www.example.com}lectureBlocksVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc An array of lecture blocks
	 * @response.representation.200.example {@link org.olat.modules.lecture.restapi.Examples#SAMPLE_LECTUREBLOCKVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param httpRequest The HTTP request
	 * @return The lecture blocks
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response getLectureBlocks(@Context HttpServletRequest httpRequest) {
		Roles roles = getRoles(httpRequest);
		if(!roles.isOLATAdmin()) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		List<LectureBlock> blockList = lectureService.getLectureBlocks(entry);
		List<LectureBlockVO> voList = new ArrayList<>(blockList.size());
		for(LectureBlock block:blockList) {
			voList.add(new LectureBlockVO(block, entry.getKey()));
		}
		LectureBlockVO[] voes = voList.toArray(new LectureBlockVO[voList.size()]);
		return Response.ok(voes).build();
	}

	/**
	 * Create or update a lecture block.
	 * @response.representation.200.qname {http://www.example.com}lectureBlocksVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The updated lecture block
	 * @response.representation.200.example {@link org.olat.modules.lecture.restapi.Examples#SAMPLE_LECTUREBLOCKVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param block The lecture block
	 * @param request The HTTP request
	 * @return It returns the updated / created lecture block.
	 */
	@PUT
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response putLectureBlocks(LectureBlockVO block, @Context HttpServletRequest httpRequest) {
		Roles roles = getRoles(httpRequest);
		if(!roles.isOLATAdmin()) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		LectureBlock updatedBlock = saveLectureBlock(block);
		return Response.ok(new LectureBlockVO(updatedBlock, entry.getKey())).build();
	}
	
	/**
	 * Create or update a lecture block. The status of the blocks will be set to
	 * autoclose only for newly created blocks. By update, the states of the
	 * block and the roll call will not be updated.
	 * 
	 * @response.representation.200.qname {http://www.example.com}lectureBlocksVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The updated configuration
	 * @response.representation.200.example {@link org.olat.modules.lecture.restapi.Examples#SAMPLE_LECTUREBLOCKVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param block The lecture block
	 * @param request The HTTP request
	 * @return It returns the updated / created lecture block.
	 */
	@POST
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response postLectureBlocks(LectureBlockVO block, @Context HttpServletRequest httpRequest) {
		Roles roles = getRoles(httpRequest);
		if(!roles.isOLATAdmin()) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		LectureBlock updatedBlock = saveLectureBlock(block);
		return Response.ok(new LectureBlockVO(updatedBlock, entry.getKey())).build();
	}
	
	private LectureBlock saveLectureBlock(LectureBlockVO blockVo) {
		LectureBlock block;
		int currentPlannedLectures;
		boolean syncParticipants = false;
		if(blockVo.getKey() != null && blockVo.getKey() > 0) {
			block = lectureService.getLectureBlock(blockVo);
			currentPlannedLectures = block.getPlannedLecturesNumber();
		} else {
			block = lectureService.createLectureBlock(entry);
			currentPlannedLectures = -1;
			if("autoclosed".equals(blockVo.getRollCallStatus())) {
				block.setStatus(LectureBlockStatus.done);
				block.setRollCallStatus(LectureRollCallStatus.autoclosed);
				syncParticipants = true;
			}
		}
		
		if(blockVo.getExternalId() != null) {
			block.setExternalId(blockVo.getExternalId());
		}
		if(blockVo.getTitle() != null) {
			block.setTitle(blockVo.getTitle());
		}
		if(blockVo.getDescription() != null) {
			block.setDescription(blockVo.getDescription());
		}
		if(blockVo.getPreparation() != null) {
			block.setPreparation(blockVo.getPreparation());
		}
		if(blockVo.getLocation() != null) {
			block.setLocation(blockVo.getLocation());
		}
		if(blockVo.getComment() != null) {
			block.setComment(blockVo.getComment());
		}
		if(blockVo.getStartDate() != null) {
			block.setStartDate(blockVo.getStartDate());
		}
		if(blockVo.getEndDate() != null) {
			block.setEndDate(blockVo.getEndDate());
		}
		if(blockVo.getCompulsory() != null) {
			block.setCompulsory(blockVo.getCompulsory().booleanValue());
		}
		if(blockVo.getManagedFlagsString() != null) {
			block.setManagedFlagsString(blockVo.getManagedFlagsString());
		}
		block.setPlannedLecturesNumber(blockVo.getPlannedLectures());
		LectureBlock savedLectureBlock = lectureService.save(block, null);
		if(currentPlannedLectures > 0 && currentPlannedLectures != savedLectureBlock.getPlannedLecturesNumber()) {
			lectureService.adaptRollCalls(savedLectureBlock);
		}
		if(syncParticipants) {
			lectureService.syncParticipantSummaries(savedLectureBlock);
		}
		return savedLectureBlock;
	}
	
	/**
	 * Return the configuration of the specified course or repository entry.
	 * @response.representation.200.qname {http://www.example.com}repositoryEntryLectureConfigurationVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The configuration of the lecture's feature
	 * @response.representation.200.example {@link org.olat.modules.lecture.restapi.Examples#SAMPLE_REPOSITORYENTRYLECTURECONFIGURATIONVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param httpRequest The HTTP request
	 * @return The configuration
	 */
	@GET
	@Path("configuration")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response getConfiguration(@Context HttpServletRequest httpRequest) {
		Roles roles = getRoles(httpRequest);
		if(!roles.isOLATAdmin()) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		RepositoryEntryLectureConfiguration config = lectureService.getRepositoryEntryLectureConfiguration(entry);
		RepositoryEntryLectureConfigurationVO configVo;
		if(config == null ) {
			configVo = new RepositoryEntryLectureConfigurationVO();
		} else {
			configVo = new RepositoryEntryLectureConfigurationVO(config);
		}
		return Response.ok(configVo).build();
	}
	
	/**
	 * Update the configuration of the lecture's feature of a specified
	 * course or repository entry.
	 * @response.representation.200.qname {http://www.example.com}repositoryEntryLectureConfigurationVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The updated configuration
	 * @response.representation.200.example {@link org.olat.modules.lecture.restapi.Examples#SAMPLE_REPOSITORYENTRYLECTURECONFIGURATIONVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param configuration The configuration
	 * @param request The HTTP request
	 * @return It returns the updated configuration.
	 */
	@POST
	@Path("configuration")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response updateConfiguration(RepositoryEntryLectureConfigurationVO configuration, @Context HttpServletRequest httpRequest) {
		Roles roles = getRoles(httpRequest);
		if(!roles.isOLATAdmin()) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		RepositoryEntryLectureConfiguration config = lectureService.getRepositoryEntryLectureConfiguration(entry);
		if(configuration.getLectureEnabled() != null) {
			config.setLectureEnabled(configuration.getLectureEnabled());
		}
		if(configuration.getCalculateAttendanceRate() != null) {
			config.setCalculateAttendanceRate(configuration.getCalculateAttendanceRate());
		}
		if(configuration.getRequiredAttendanceRate() != null) {
			config.setRequiredAttendanceRate(configuration.getRequiredAttendanceRate());
		}
		if(configuration.getOverrideModuleDefault() != null) {
			config.setOverrideModuleDefault(configuration.getOverrideModuleDefault());
		}
		if(configuration.getCourseCalendarSyncEnabled() != null) {
			config.setCourseCalendarSyncEnabled(configuration.getCourseCalendarSyncEnabled());
		}
		if(configuration.getRollCallEnabled() != null) {
			config.setRollCallEnabled(configuration.getRollCallEnabled());
		}
		if(configuration.getTeacherCalendarSyncEnabled() != null) {
			config.setTeacherCalendarSyncEnabled(configuration.getTeacherCalendarSyncEnabled());
		}
		RepositoryEntryLectureConfiguration updatedConfig = lectureService.updateRepositoryEntryLectureConfiguration(config);
		return Response.ok(new RepositoryEntryLectureConfigurationVO(updatedConfig)).build();
	}

	/**
	 * To get the web service for a specific lecture block.
	 * @param lectureBlockKey The primary key of the lecture block
	 * @param httpRequest The HTTP request
	 * @return The web service for a single lecture block.
	 */
	@Path("{lectureBlockKey}")
	public LectureBlockWebService getLectureBlockWebService(@PathParam("lectureBlockKey") Long lectureBlockKey, @Context HttpServletRequest httpRequest) {
		Roles roles = getRoles(httpRequest);
		if(!roles.isOLATAdmin()) {
			return null;
		}
		LectureBlock lectureBlock = lectureService.getLectureBlock(new LectureBlockRefImpl(lectureBlockKey));
		if(lectureBlock == null || !lectureBlock.getEntry().equals(entry)) {
			return null;
		}
		return new LectureBlockWebService(lectureBlock, entry, lectureService);
	}
	
	/**
	 * Synchronize the calendars based on the lecture blocks.
	 * @response.representation.200.doc The calendar is successfully synchronized
	 * @return 200 if the calendar is successfully synchronized
	 */
	@POST
	@Path("sync/calendar")
	public Response syncCalendar() {
		lectureService.syncCalendars(entry);
		return Response.ok().build();
	}
	
	/**
	 * Adapt all roll call to the effective number of lectures. Use with caution!
	 * @response.representation.200.doc The adaptation is successful
	 * @param httpRequest The HTTP request
	 * @return 200 if the adaptation is successful
	 */
	@GET
	@Path("adaptation")
	public Response adapatation(@Context HttpServletRequest httpRequest) {
		Roles roles = getRoles(httpRequest);
		if(!roles.isOLATAdmin()) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		lectureService.adaptAll(getIdentity(httpRequest));
		return Response.ok().build();
	}
}