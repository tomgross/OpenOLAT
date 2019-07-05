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
package org.olat.core.commons.services.doceditor;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Initial date: 31 Mar 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class DocEditorConfigs {
	
	public static interface Config {
		public String getType();
	}
	
	private static final DocEditorConfigs NONE = DocEditorConfigs.builder().build();
	
	private Map<String, Config> configs;

	private DocEditorConfigs(Builder builder) {
		this.configs = new HashMap<>(builder.configs);
	}
	
	public Config getConfig(String type) {
		return this.configs.get(type);
	}
	
	public static DocEditorConfigs none() {
		return NONE;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private Map<String, Config> configs = new HashMap<>();

		private Builder() {
		}

		public Builder addConfig(Config config) {
			this.configs.put(config.getType(), config);
			return this;
		}

		public DocEditorConfigs build() {
			return new DocEditorConfigs(this);
		}
	}
	
}
