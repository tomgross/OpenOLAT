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
package org.olat.modules.video;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 * 
 * Initial date: 5 avr. 2019<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@RunWith(Parameterized.class)
public class VideoFormatTest {
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "video.mov", "mov", VideoFormat.mp4 },
                { "video.zip", "zip", VideoFormat.mp4 },
                { "video.foo", "somethingelse", VideoFormat.mp4 },
                { "https://www.youtube.com/watch?v=Sy5cXJL7K90", "youtube", VideoFormat.youtube },
                { "https://vimeo.com/36085398", "vimeo", VideoFormat.vimeo },
                { "https://demo.hosted.panopto.com/Panopto/Pages/Viewer.aspx?id=9b4d2c73-acd9-46ec-a5ae-ab4a010c47d0", "panopto", VideoFormat.panopto },
                { "video.mp4", "mp4", VideoFormat.mp4 },
                { "https://tube.switch.ch/videos/78ece87d", "switchtube", VideoFormat.switchtube },
                { null, null, null }
        });
    }
    
    private String url;
    private String format;
    private VideoFormat expectedFormat;
    
    public VideoFormatTest(String url, String format, VideoFormat expectedFormat) {
        this.url = url;
    	this.format = format;
    	this.expectedFormat = expectedFormat;
    }
    
    @Test
    public void conve() {
    	VideoFormat formatEnum = VideoFormat.secureValueOf(format);
    	Assert.assertEquals(expectedFormat, formatEnum);
    }

    @Test
    public void valueOfUrl() {
        // only test URL specific providers
        if (expectedFormat == VideoFormat.mp4) {
            return;
        }
        VideoFormat formatUrl = VideoFormat.valueOfUrl(url);
        Assert.assertEquals(expectedFormat, formatUrl);
    }
}
