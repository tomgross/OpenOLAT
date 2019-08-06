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
package org.olat.modules.forms.manager;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.TypedQuery;

import org.olat.basesecurity.IdentityRef;
import org.olat.core.commons.persistence.DB;
import org.olat.modules.forms.EvaluationFormResponse;
import org.olat.modules.forms.EvaluationFormSession;
import org.olat.modules.forms.EvaluationFormSessionStatus;
import org.olat.modules.forms.model.jpa.EvaluationFormResponseImpl;
import org.olat.modules.portfolio.PageBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 12 déc. 2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class EvaluationFormResponseDAO {
	
	@Autowired
	private DB dbInstance;
	
	public EvaluationFormResponse createResponse(String responseIdentifier, BigDecimal numericalValue, String stringuifiedResponse,
			Path fileResponse, EvaluationFormSession session) {
		EvaluationFormResponseImpl response = new EvaluationFormResponseImpl();
		response.setCreationDate(new Date());
		response.setLastModified(response.getCreationDate());
		response.setSession(session);
		response.setResponseIdentifier(responseIdentifier);
		response.setFileResponse(fileResponse);
		response.setNumericalResponse(numericalValue);
		response.setStringuifiedResponse(stringuifiedResponse);
		dbInstance.getCurrentEntityManager().persist(response);
		return response;
	}
	
	public List<EvaluationFormResponse> getResponsesFromPortfolioEvaluation(IdentityRef identity, PageBody anchor) {
		StringBuilder sb = new StringBuilder();
		sb.append("select response from evaluationformresponse as response")
		  .append(" inner join response.session as session")
		  .append(" where session.identity.key=:identityKey and session.pageBody.key=:bodyKey");
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), EvaluationFormResponse.class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("bodyKey", anchor.getKey())
				.getResultList();
	}
	
	public List<EvaluationFormResponse> getResponsesFromPortfolioEvaluation(List<? extends IdentityRef> identities, PageBody anchor, EvaluationFormSessionStatus status) {
		if(identities == null || identities.isEmpty()) return Collections.emptyList();
		
		List<Long> identitiyKeys = identities.stream().map(i -> i.getKey()).collect(Collectors.toList());
		StringBuilder sb = new StringBuilder();
		sb.append("select response from evaluationformresponse as response")
		  .append(" inner join response.session as session")
		  .append(" where session.identity.key in (:identityKeys) and session.pageBody.key=:bodyKey");
		if(status != null) {
			sb.append(" and session.status=:status");
		}
		TypedQuery<EvaluationFormResponse> rQuery = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), EvaluationFormResponse.class)
				.setParameter("identityKeys", identitiyKeys)
				.setParameter("bodyKey", anchor.getKey());
		if(status != null) {
			rQuery.setParameter("status", status.name());
		}
		return rQuery.getResultList();
	}
	
	public EvaluationFormResponse updateResponse(BigDecimal numericalValue, String stringuifiedResponse,
			Path fileResponse, EvaluationFormResponse response) {
		EvaluationFormResponseImpl evalResponse = (EvaluationFormResponseImpl)response;
		evalResponse.setLastModified(new Date());
		evalResponse.setNumericalResponse(numericalValue);
		evalResponse.setStringuifiedResponse(stringuifiedResponse);
		evalResponse.setFileResponse(fileResponse);
		return dbInstance.getCurrentEntityManager().merge(response);
	}
}
