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
package org.olat.modules.video.ui;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.segmentedview.SegmentViewComponent;
import org.olat.core.gui.components.segmentedview.SegmentViewEvent;
import org.olat.core.gui.components.segmentedview.SegmentViewFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.resource.OresHelper;
import org.olat.repository.RepositoryEntry;

/**
 * 
 * @author dfurrer, dirk.furrer@frentix.com, http://www.frentix.com
 *
 */
public class VideoSettingsController extends BasicController implements Activateable2 {

	private RepositoryEntry entry;

	private VideoMetaDataEditFormController metaDataController;
	private VideoPosterEditController posterEditController;
	private VideoChapterEditController chapterEditController;
	private VideoTrackEditController trackEditController;
	private VideoQualityTableFormController qualityEditController;

	private Link metaDataLink, posterEditLink, chapterEditLink, trackEditLink, qualityConfig;

	private final VelocityContainer mainVC;
	private final SegmentViewComponent segmentView;

	public VideoSettingsController(UserRequest ureq, WindowControl wControl, RepositoryEntry entry ) {
		super(ureq, wControl);

		this.entry = entry;
		mainVC = createVelocityContainer("video_settings");

		segmentView = SegmentViewFactory.createSegmentView("segments", mainVC, this);

		metaDataLink = LinkFactory.createLink("tab.video.metaDataConfig", mainVC, this);
		segmentView.addSegment(metaDataLink, true);
		posterEditLink = LinkFactory.createLink("tab.video.posterConfig", mainVC, this);
		segmentView.addSegment(posterEditLink, false);
		chapterEditLink = LinkFactory.createLink("tab.video.chapterConfig", mainVC, this);
		segmentView.addSegment(chapterEditLink, false);
		trackEditLink = LinkFactory.createLink("tab.video.trackConfig", mainVC, this);
		segmentView.addSegment(trackEditLink, false);
		qualityConfig = LinkFactory.createLink("tab.video.qualityConfig", mainVC, this);
		segmentView.addSegment(qualityConfig, false);

		doOpenMetaDataConfig(ureq);
		putInitialPanel(mainVC);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
		String type = entries.get(0).getOLATResourceable().getResourceableTypeName();
		if("metadata".equalsIgnoreCase(type)) {
			doOpenMetaDataConfig(ureq);
			segmentView.select(metaDataLink);
		} else if("poster".equalsIgnoreCase(type)) {
			doOpenPosterConfig(ureq);
			segmentView.select(posterEditLink);
		} else if("tracks".equalsIgnoreCase(type)) {
			doOpenTrackConfig(ureq);
			segmentView.select(trackEditLink);
		} else if("quality".equalsIgnoreCase(type)) {
			doOpenQualityConfig(ureq);
			segmentView.select(qualityConfig);
		} else if("chapters".equalsIgnoreCase(type)) {
			doOpenChapterConfig(ureq);
			segmentView.select(chapterEditLink);
		}
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(source == segmentView) {
			if(event instanceof SegmentViewEvent) {
				SegmentViewEvent sve = (SegmentViewEvent)event;
				String segmentCName = sve.getComponentName();
				Component clickedLink = mainVC.getComponent(segmentCName);
				if (clickedLink == metaDataLink) {
					doOpenMetaDataConfig(ureq);
				} else if (clickedLink == posterEditLink){
					doOpenPosterConfig(ureq);
				} else if (clickedLink == trackEditLink){
					doOpenTrackConfig(ureq);
				} else if (clickedLink == qualityConfig){
					doOpenQualityConfig(ureq);
				} else if (clickedLink == chapterEditLink){
					doOpenChapterConfig(ureq);
				}
			}
		}
	}

	private void doOpenMetaDataConfig(UserRequest ureq) {
		if(metaDataController == null) {
			OLATResourceable ores = OresHelper.createOLATResourceableType("metadata");
			WindowControl swControl = addToHistory(ureq, ores, null);
			metaDataController = new VideoMetaDataEditFormController(ureq, swControl, entry);
			listenTo(metaDataController);
		} else {
			addToHistory(ureq, metaDataController);
		}
		mainVC.put("segmentCmp", metaDataController.getInitialComponent());
	}

	private void doOpenPosterConfig(UserRequest ureq) {
		if(posterEditController == null) {
			OLATResourceable ores = OresHelper.createOLATResourceableType("poster");
			WindowControl swControl = addToHistory(ureq, ores, null);
			posterEditController = new VideoPosterEditController(ureq, swControl, entry.getOlatResource());
			listenTo(posterEditController);
		} else {
			addToHistory(ureq, posterEditController);
		}
		mainVC.put("segmentCmp", posterEditController.getInitialComponent());
	}

	private void doOpenTrackConfig(UserRequest ureq) {
		if(trackEditController == null) {
			OLATResourceable ores = OresHelper.createOLATResourceableType("tracks");
			WindowControl swControl = addToHistory(ureq, ores, null);
			trackEditController = new VideoTrackEditController(ureq, swControl, entry.getOlatResource());
			listenTo(trackEditController);
		} else {
			addToHistory(ureq, trackEditController);
		}
		mainVC.put("segmentCmp", trackEditController.getInitialComponent());
	}

	private void doOpenQualityConfig(UserRequest ureq) {
		removeAsListenerAndDispose(qualityEditController);
		
		OLATResourceable ores = OresHelper.createOLATResourceableType("quality");
		WindowControl swControl = addToHistory(ureq, ores, null);
		qualityEditController = new VideoQualityTableFormController(ureq, swControl, entry);
		listenTo(qualityEditController);
		mainVC.put("segmentCmp", qualityEditController.getInitialComponent());
	}
	
	private void doOpenChapterConfig(UserRequest ureq){
		if (chapterEditController == null){
			OLATResourceable ores = OresHelper.createOLATResourceableType("chapters");
			WindowControl swControl = addToHistory(ureq, ores, null);
			chapterEditController = new VideoChapterEditController(ureq, swControl, entry);
			listenTo(chapterEditController);
		} else {
			addToHistory(ureq, chapterEditController);
		}
		mainVC.put("segmentCmp", chapterEditController.getInitialComponent());
	} 
}