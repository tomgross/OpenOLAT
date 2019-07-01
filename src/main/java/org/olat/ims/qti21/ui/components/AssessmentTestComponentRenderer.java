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
package org.olat.ims.qti21.ui.components;

import static org.olat.ims.qti21.ui.components.AssessmentRenderFunctions.contentAsString;
import static org.olat.ims.qti21.ui.components.AssessmentRenderFunctions.testFeedbackVisible;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormJSHelper;
import org.olat.core.gui.components.form.flexible.impl.NameValuePair;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.ims.qti21.AssessmentTestSession;
import org.olat.ims.qti21.model.audit.CandidateEvent;
import org.olat.ims.qti21.model.audit.CandidateTestEventType;
import org.olat.ims.qti21.ui.CandidateSessionContext;
import org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent.Event;
import org.w3c.dom.Element;

import uk.ac.ed.ph.jqtiplus.node.ForeignElement;
import uk.ac.ed.ph.jqtiplus.node.QtiNode;
import uk.ac.ed.ph.jqtiplus.node.content.basic.TextRun;
import uk.ac.ed.ph.jqtiplus.node.content.variable.PrintedVariable;
import uk.ac.ed.ph.jqtiplus.node.content.variable.RubricBlock;
import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.item.template.declaration.TemplateDeclaration;
import uk.ac.ed.ph.jqtiplus.node.outcome.declaration.OutcomeDeclaration;
import uk.ac.ed.ph.jqtiplus.node.test.AssessmentSection;
import uk.ac.ed.ph.jqtiplus.node.test.NavigationMode;
import uk.ac.ed.ph.jqtiplus.node.test.TestFeedback;
import uk.ac.ed.ph.jqtiplus.node.test.TestFeedbackAccess;
import uk.ac.ed.ph.jqtiplus.node.test.TestPart;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentItem;
import uk.ac.ed.ph.jqtiplus.running.TestSessionController;
import uk.ac.ed.ph.jqtiplus.state.AssessmentSectionSessionState;
import uk.ac.ed.ph.jqtiplus.state.EffectiveItemSessionControl;
import uk.ac.ed.ph.jqtiplus.state.ItemSessionState;
import uk.ac.ed.ph.jqtiplus.state.TestPartSessionState;
import uk.ac.ed.ph.jqtiplus.state.TestPlanNode;
import uk.ac.ed.ph.jqtiplus.state.TestPlanNode.TestNodeType;
import uk.ac.ed.ph.jqtiplus.state.TestPlanNodeKey;
import uk.ac.ed.ph.jqtiplus.state.TestSessionState;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.value.Value;

/**
 * 
 * Initial date: 14.09.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentTestComponentRenderer extends AssessmentObjectComponentRenderer {
	
	private static final OLog log = Tracing.createLoggerFor(AssessmentTestComponentRenderer.class);
	
	@Override
	public void render(Renderer renderer, StringOutput sb, Component source, URLBuilder ubu,
			Translator translator, RenderResult renderResult, String[] args) {

		AssessmentTestComponent cmp = (AssessmentTestComponent)source;
		TestSessionController testSessionController = cmp.getTestSessionController();

		if(testSessionController.getTestSessionState().isEnded()) {
			renderTerminated(sb, translator);
		} else {
	        /* Create appropriate options that link back to this controller */
	        TestSessionState testSessionState = testSessionController.getTestSessionState();
			CandidateSessionContext candidateSessionContext = cmp.getCandidateSessionContext();
			final AssessmentTestSession candidateSession = candidateSessionContext.getCandidateSession();
			
	        if (candidateSession.isExploded()) {
	            renderExploded(sb, translator);
	        } else if (candidateSessionContext.isTerminated()) {
	            renderTerminated(sb, translator);
	        } else {
				/* Touch the session's duration state if appropriate */
				if (testSessionState.isEntered() && !testSessionState.isEnded()) {
				    final Date timestamp = candidateSessionContext.getCurrentRequestTimestamp();
				    testSessionController.touchDurations(timestamp);
				}
				
				/* Render event */
				AssessmentRenderer renderHints = new AssessmentRenderer(renderer);
				renderTestEvent(testSessionController, renderHints, sb, cmp, ubu, translator);
			}
		}
	}
    
	private void renderTestEvent(TestSessionController testSessionController, AssessmentRenderer renderer, StringOutput target,
			AssessmentTestComponent component, URLBuilder ubu, Translator translator) {

		CandidateSessionContext candidateSessionContext = component.getCandidateSessionContext();
		CandidateEvent candidateEvent = candidateSessionContext.getLastEvent();
		CandidateTestEventType testEventType = candidateEvent.getTestEventType();

        /* If session has terminated, render appropriate state and exit */
        final TestSessionState testSessionState = testSessionController.getTestSessionState();
        if (candidateSessionContext.isTerminated() || testSessionState.isExited()) {
        	renderTerminated(target, translator);
        } else if (testEventType == CandidateTestEventType.REVIEW_ITEM) {
        	renderer.setReviewMode(true);
        	TestPlanNodeKey itemKey = extractTargetItemKey(candidateEvent);
        	RenderingRequest options = RenderingRequest.getItemReview();
        	renderTestItem(renderer, target, component, itemKey, ubu, translator, options);
        }  else if (testEventType == CandidateTestEventType.SOLUTION_ITEM) {
            renderer.setSolutionMode(true);
        	TestPlanNodeKey itemKey = extractTargetItemKey(candidateEvent);
        	RenderingRequest options = RenderingRequest.getItemSolution();
        	renderTestItem(renderer, target, component, itemKey, ubu, translator, options);
        } else {
            /* Render current state */
            final TestPlanNodeKey currentTestPartKey = testSessionState.getCurrentTestPartKey();
            if (testSessionState.isEnded()) {
                /* At end of test, so show overall test feedback */
                renderTestPartFeedback(renderer, target, component, ubu, translator);
            } else if (currentTestPartKey != null) {
                final TestPartSessionState currentTestPartSessionState = testSessionState.getTestPartSessionStates().get(currentTestPartKey);
                final TestPlanNodeKey currentItemKey = testSessionState.getCurrentItemKey();
                if (currentItemKey != null) {
                    /* An item is selected, so render it in appropriate state */
                	RenderingRequest options = RenderingRequest.getItem(testSessionController);
                	renderTestItem(renderer, target, component, currentItemKey, ubu, translator, options);
                } else {
                    /* No item selected */
                    if (currentTestPartSessionState.isEnded()) {
                        /* testPart has ended, so must be showing testPart feedback */
                        renderTestPartFeedback(renderer, target, component, ubu, translator);
                    } else {
                        /* testPart not ended, so we must be showing the navigation menu in nonlinear mode */
                        renderNavigation(renderer, target, component, ubu, translator);
                    }
                }
            } else {
                /* No current testPart == start of multipart test */
                renderTestEntry(target, component, translator);
            }
        }
    }
	
	private void renderTestEntry(StringOutput sb, AssessmentTestComponent component, Translator translator) {
		int numOfParts = component.getAssessmentTest().getTestParts().size();
		sb.append("<h4>").append(translator.translate("test.entry.page.title")).append("</h4>")
		  .append("<div class='o_info'>")
		  .append(translator.translate("test.entry.page.text", new String[]{ Integer.toString(numOfParts) }))
		  .append("</div><div class='o_button_group'>");
		//precondition -> up to
		String title = translator.translate("assessment.test.enter.test");
		renderControl(sb, component, title, true, "o_sel_enter_test",
				new NameValuePair("cid", Event.advanceTestPart.name())); 
		
		sb.append("</div>");
	}
	
	private void renderControl(StringOutput sb, AssessmentTestComponent component, String title, boolean primary, String cssClass, NameValuePair... pairs) {
		Form form = component.getQtiItem().getRootForm();
		String dispatchId = component.getQtiItem().getFormDispatchId();
		sb.append("<button type='button' onclick=\"");
		sb.append(FormJSHelper.getXHRFnCallFor(form, dispatchId, 1, true, true, pairs))
		  .append(";\" class='btn ").append("btn-primary ", "btn-default ", primary).append(cssClass).append("'").append("><span>").append(title).append("</span></button>");
	}
	
	private void renderTestItem(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component,
			TestPlanNodeKey itemRefKey, URLBuilder ubu, Translator translator, RenderingRequest options) {
		final TestSessionController testSessionController = component.getTestSessionController();
		final TestSessionState testSessionState = testSessionController.getTestSessionState();
        String key = itemRefKey.toString();

        /* We finally do the transform on the _item_ (NB!) */
        sb.append("<div class='qtiworks o_assessmentitem o_assessmenttest'>");

		//test part feedback 'during'
		//test feedback 'during'
		TestPlanNode itemRefNode = testSessionState.getTestPlan().getNode(itemRefKey);
		final EffectiveItemSessionControl effectiveItemSessionControl = itemRefNode.getEffectiveItemSessionControl();
		final boolean allowComments = effectiveItemSessionControl.isAllowComment() && component.isPersonalNotes();
		renderer.setCandidateCommentAllowed(allowComments);

		//write section rubric
		renderSectionRubrics(renderer, sb, component, itemRefNode, ubu, translator);

		// test part -> section -> item
		renderTestItemBody(renderer, sb, component, itemRefNode, ubu, translator, options);
		
		//controls
		sb.append("<div class='o_button_group o_assessmentitem_controls'>");
		
		//submit button
		final ItemSessionState itemSessionState = component.getItemSessionState(itemRefNode.getKey());
		if(component.isItemSessionOpen(itemSessionState, options.isSolutionMode())) {
			Component submit = component.getQtiItem().getSubmitButton().getComponent();
			submit.getHTMLRendererSingleton().render(renderer.getRenderer(), sb, submit, ubu, translator, new RenderResult(), null);
		}
		//advanceTestItemAllowed /* && testSessionState.getCurrentItemKey() != null && testSessionController.mayAdvanceItemLinear() */
		if(options.isAdvanceTestItemAllowed() ) {//TODO need to find if there is a next question
			String title = translator.translate("assessment.test.nextQuestion");
			renderControl(sb, component, title, false, "o_sel_next_question", new NameValuePair("cid", Event.finishItem.name()));
		}
		//nextItem
		if(options.isNextItemAllowed() && testSessionController.hasFollowingNonLinearItem()) {
			String title = translator.translate("assessment.test.nextQuestion");
			renderControl(sb, component, title, false, "o_sel_next_question", new NameValuePair("cid", Event.nextItem.name()));
		}
		//testPartNavigationAllowed"
		if(options.isTestPartNavigationAllowed() && component.isRenderNavigation()) {
			String title = translator.translate("assessment.test.questionMenu");
			renderControl(sb, component, title, false, "o_sel_question_menu", new NameValuePair("cid", Event.testPartNavigation.name()));
		}
		//endTestPartAllowed
		if(options.isEndTestPartAllowed()) {
			String title = component.hasMultipleTestParts()
					? translator.translate("assessment.test.end.testPart") : translator.translate("assessment.test.end.test");
			renderControl(sb, component, title, false, "o_sel_end_testpart", new NameValuePair("cid", Event.endTestPart.name()));
		}
		
		//reviewMode
		if(options.isReviewMode()) {
			String title = translator.translate("assessment.test.backToTestFeedback");
			renderControl(sb, component, title, false, "o_sel_back_test_feedback", new NameValuePair("cid", Event.reviewTestPart.name()));
		}
		
		// <xsl:variable name="provideItemSolutionButton" as="xs:boolean" select="$reviewMode and $showSolution and not($solutionMode)"/>
		if(options.isReviewMode() && effectiveItemSessionControl.isShowSolution() && !options.isSolutionMode()) {
			String title = translator.translate("assessment.solution.show");
			renderControl(sb, component, title, false, "o_sel_show_solution",
					new NameValuePair("cid", Event.itemSolution.name()), new NameValuePair("item", key));
		}
		if(options.isReviewMode() && options.isSolutionMode()) {
			String title = translator.translate("assessment.solution.hide");
			renderControl(sb, component, title, false, "o_sel_solution_hide",
					new NameValuePair("cid", Event.reviewItem.name()), new NameValuePair("item", key));
		}
		sb.append("</div>");//end controls
		sb.append("</div>");// end assessmentItem
	}
	
	private void renderSectionRubrics(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component, TestPlanNode itemRefNode, URLBuilder ubu, Translator translator) {
		boolean writeRubrics = false;
		boolean writeTitles = false;
		List<AssessmentSection> sectionParentLine = new ArrayList<>();
		for(TestPlanNode parentNode=itemRefNode.getParent(); parentNode.getParent() != null; parentNode = parentNode.getParent()) {
			AssessmentSection selectedSection = component.getAssessmentSection(parentNode.getIdentifier());
			if(selectedSection != null && selectedSection.getVisible()) {
				sectionParentLine.add(selectedSection);
				if(selectedSection.getRubricBlocks().size() > 0) {
					for(RubricBlock rubric:selectedSection.getRubricBlocks()) {
						if(rubric.getBlocks().size() > 0) {
							writeRubrics = true;
						}
					}
				}
				if(StringHelper.containsNonWhitespace(selectedSection.getTitle())) {
					writeTitles = true;
				}
			}
		}
		
		if(writeRubrics) {
			sb.append("<div class='o_info o_assessmentsection_rubrics'>");
			//write the titles first
			if(writeTitles) {
				sb.append("<h4>");
				for(int i=0; i<sectionParentLine.size(); i++) {
					if(i == 1) {
						sb.append("<small>");
					} else if(i > 1) {
						sb.append(" / ");
					}
					sb.append(sectionParentLine.get(i).getTitle());
				}
				
				if(sectionParentLine.size() > 1) {
					sb.append("</small>");
				}
				sb.append("</h4>");
			}
			

			for(int i=sectionParentLine.size(); i-->0; ) {
				AssessmentSection selectedSection = sectionParentLine.get(i);
				for(RubricBlock rubricBlock:selectedSection.getRubricBlocks()) {
					sb.append("<div class='rubric'>");//@view (candidate)
					rubricBlock.getBlocks().forEach((block) -> renderBlock(renderer, sb, component, null, null, block, ubu, translator));
					sb.append("</div>");
				}
			}
			sb.append("</div>");
		}
	}
	
	private void renderTestItemBody(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component, TestPlanNode itemNode,
			URLBuilder ubu, Translator translator, RenderingRequest options) {
		final ItemSessionState itemSessionState = component.getItemSessionState(itemNode.getKey());
		
		URI itemSystemId = itemNode.getItemSystemId();
		ResolvedAssessmentItem resolvedAssessmentItem = component.getResolvedAssessmentTest()
				.getResolvedAssessmentItemBySystemIdMap().get(itemSystemId);
		final AssessmentItem assessmentItem = resolvedAssessmentItem.getRootNodeLookup().extractIfSuccessful();

		sb.append("<div class='o_assessmentitem_wrapper'>");
		//title + status
		sb.append("<h4 class='itemTitle'>");
		renderItemStatus(sb, itemSessionState, options, translator);
		sb.append(StringHelper.escapeHtml(itemNode.getSectionPartTitle()), component.isShowTitles())
		  .append("</h4>")
		  .append("<div id='itemBody' class='clearfix'>");

		//render itemBody
		assessmentItem.getItemBody().getBlocks().forEach((block)
				-> renderBlock(renderer, sb, component, resolvedAssessmentItem, itemSessionState, block, ubu, translator));

		//comment
		renderComment(renderer, sb, component, itemSessionState, translator);
		
		//submit button -> moved with the other buttons
		/*if(component.isItemSessionOpen(itemSessionState, options.isSolutionMode())) {
			Component submit = component.getQtiItem().getSubmitButton().getComponent();
			submit.getHTMLRendererSingleton().render(renderer.getRenderer(), sb, submit, ubu, translator, new RenderResult(), null);
		}*/
		//end body
		sb.append("</div>");

		// Display active modal feedback (only after responseProcessing)
		if(component.isItemFeedbackAllowed(itemNode, assessmentItem, options)) {
			renderTestItemModalFeedback(renderer, sb, component, resolvedAssessmentItem, itemSessionState, ubu, translator);
		}
		sb.append("</div>"); // end wrapper
	}
	
	@Override
	protected void renderPrintedVariable(AssessmentRenderer renderer, StringOutput sb, AssessmentObjectComponent component, ResolvedAssessmentItem resolvedAssessmentItem,
			ItemSessionState itemSessionState, PrintedVariable printedVar) {
		
		AssessmentTestComponent testCmp = (AssessmentTestComponent)component;
		Identifier identifier = printedVar.getIdentifier();
		sb.append("<span class='printedVariable'>");
		if(itemSessionState == null) {
			Value outcomeValue = testCmp.getTestSessionController().getTestSessionState().getOutcomeValue(identifier);
			if(outcomeValue != null) {
				OutcomeDeclaration outcomeDeclaration = testCmp.getAssessmentTest().getOutcomeDeclaration(identifier);
				renderPrintedVariable(renderer, sb, printedVar, outcomeDeclaration, outcomeValue);
			}
		} else {
			Value templateValue = itemSessionState.getTemplateValues().get(identifier);
			Value outcomeValue = itemSessionState.getOutcomeValues().get(identifier);
			if(outcomeValue != null) {
				OutcomeDeclaration outcomeDeclaration = resolvedAssessmentItem.getRootNodeLookup()
						.extractIfSuccessful().getOutcomeDeclaration(identifier);
				renderPrintedVariable(renderer, sb, printedVar, outcomeDeclaration, outcomeValue);
			} else if(templateValue != null) {
				TemplateDeclaration templateDeclaration = resolvedAssessmentItem.getRootNodeLookup()
						.extractIfSuccessful().getTemplateDeclaration(identifier);
				renderPrintedVariable(renderer, sb, printedVar, templateDeclaration, templateValue);
			} else {
				sb.append("(variable ").append(identifier.toString()).append(" was not found)");
			}
		}
		sb.append("</span>");
	}
	
	@Override
	protected void renderMath(AssessmentRenderer renderer, StringOutput out, AssessmentObjectComponent component,
			ResolvedAssessmentItem resolvedAssessmentItem, ItemSessionState itemSessionState, QtiNode mathElement) {
		if(resolvedAssessmentItem != null) {
			super.renderMath(renderer, out, component, resolvedAssessmentItem, itemSessionState, mathElement);
		} else if(mathElement instanceof ForeignElement) {
			ForeignElement fElement = (ForeignElement)mathElement;
			boolean mi = fElement.getQtiClassName().equals("mi");
			boolean ci = fElement.getQtiClassName().equals("ci");
			if(mi || ci) {
				String text = contentAsString(fElement);
				Identifier identifier = Identifier.assumedLegal(text);

				AssessmentTestComponent testComponent = (AssessmentTestComponent)component;
				Value outcomeValue = testComponent.getTestSessionController().getTestSessionState().getOutcomeValue(identifier);
				if(outcomeValue != null) {
					if(ci) {
						substituteCi(out, outcomeValue);
					} else if(mi) {
						substituteMi(out, outcomeValue);
					}
				} else {
					renderStartHtmlTag(out, component, resolvedAssessmentItem, fElement, null);
					fElement.getChildren().forEach((child)
							-> renderMath(renderer, out, component, resolvedAssessmentItem, itemSessionState, child));
					renderEndTag(out, fElement);
				}
			} else {
				renderStartHtmlTag(out, component, resolvedAssessmentItem, fElement, null);
				fElement.getChildren().forEach((child)
						-> renderMath(renderer, out, component, resolvedAssessmentItem, itemSessionState, child));
				renderEndTag(out, fElement);
			}
		} else if(mathElement instanceof TextRun) {
			out.append(((TextRun)mathElement).getTextContent());
		}
	}
	
	private void renderTestPartFeedback(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component,
			URLBuilder ubu, Translator translator) {
        sb.append("<div class='qtiworks o_assessmenttest testFeedback'>")
		  .append("<h1>");
		if(component.hasMultipleTestParts()) {
			sb.append(translator.translate("test.part.complete"));
		} else {
			sb.append(translator.translate("test.complete"));
		}
		sb.append("</h1>");

		 // Show 'atEnd' testPart feedback 
		TestPlanNode currentTestPartNode = component.getCurrentTestPartNode();
		TestPart currentTestPart = component.getTestPart(currentTestPartNode.getIdentifier());
		renderTestFeebacks(renderer, sb, currentTestPart.getTestFeedbacks(), component, TestFeedbackAccess.AT_END, ubu, translator);

		//Show 'atEnd' test feedback f there's only 1 testPart
		//if(!component.hasMultipleTestParts()) {
		renderTestFeebacks(renderer, sb, component.getAssessmentTest().getTestFeedbacks(), component, TestFeedbackAccess.AT_END, ubu, translator);
		//}
		
		//test part review
		component.getTestSessionController().getTestSessionState().getTestPlan()
			.getTestPartNodes().forEach((testPartNode)
					-> renderReview(renderer, sb, component, testPartNode, ubu, translator));

		//controls
		/*
		sb.append("<div class='o_button_group'>");
		String title = component.hasMultipleTestParts()
				? translator.translate("assessment.test.end.testPart") : translator.translate("assessment.test.end.test");
		renderControl(sb, component, title, "o_sel_end_testpart",
				new NameValuePair("cid", Event.advanceTestPart.name()));
		sb.append("</div>");
		*/
		
		sb.append("</div>");
	}
	
	private void renderReview(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component, TestPlanNode node,
			URLBuilder ubu, Translator translator) {
		
		switch(node.getTestNodeType()) {
			case TEST_PART: renderReviewTestPart(renderer, sb, component, node, ubu, translator); break;
			case ASSESSMENT_SECTION: renderReviewAssessmentSection(renderer, sb, component, node, ubu, translator); break;
			case ASSESSMENT_ITEM_REF: renderReviewAssessmentItem(sb, component, node, translator); break;
			default: break;
		}
	}
	
	private void renderReviewTestPart(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component, TestPlanNode node,
			URLBuilder ubu, Translator translator) {
		if(component.isRenderNavigation() || true) {
			//".//qw:node[@type='ASSESSMENT_ITEM_REF' and (@allowReview='true' or @showFeedback='true')]" as="element(qw:node)*"/>
			boolean hasReviewableItems = node.searchDescendants(TestNodeType.ASSESSMENT_ITEM_REF)
					.stream().anyMatch(itemNode
							-> itemNode.getEffectiveItemSessionControl().isAllowReview()
							|| itemNode.getEffectiveItemSessionControl().isShowFeedback());
			if(hasReviewableItems) {
				sb.append("<h4>").append(translator.translate("review.responses")).append("</h4>");
				sb.append("<p>").append(translator.translate("review.responses.desc")).append("</p>");
				sb.append("<div class='o_qti_menu_buttonstyle'>");
				sb.append("<ul class='o_testpartnavigation'>");
				
				node.getChildren().forEach((childNode)
					-> renderReview(renderer, sb, component, childNode, ubu, translator));
		
				sb.append("</ul></div>");
			}
		}
	}
	
	private void renderReviewAssessmentSection(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component, TestPlanNode sectionNode, 
			URLBuilder ubu, Translator translator) {
		
		AssessmentSectionSessionState assessmentSessionSessionState = component.getTestSessionController()
				.getTestSessionState().getAssessmentSectionSessionStates().get(sectionNode.getKey());
		TestPart currentTestPart = component.getTestPart(component.getCurrentTestPartNode().getIdentifier());
		//<xsl:if test="$currentTestPart/@navigationMode='nonlinear' or exists($assessmentSessionSessionState/@entryTime)">
		if(currentTestPart.getNavigationMode() == NavigationMode.NONLINEAR || assessmentSessionSessionState.getEntryTime() != null) {
			sb.append("<li class='o_assessmentsection'>")
			  .append("<header><h2>")
			  .append(StringHelper.escapeHtml(sectionNode.getSectionPartTitle())).append("</h2>");
			renderAssessmentSectionRubrickBlock(renderer, sb, component, sectionNode, ubu, translator);
			sb.append("</header>");
			sb.append("<ul class='o_testpartnavigation_inner list-unstyled'>");
			
			sectionNode.getChildren().forEach((childNode)
					-> renderReview(renderer, sb, component, childNode, ubu, translator));
			
			sb.append("</ul>");
		}
	}
	
	private void renderReviewAssessmentItem(StringOutput sb, AssessmentTestComponent component, TestPlanNode itemNode, Translator translator) {
		EffectiveItemSessionControl itemSessionControl = itemNode.getEffectiveItemSessionControl();
		
		//<xsl:variable name="reviewable" select="@allowReview='true' or @showFeedback='true'" as="xs:boolean"/>
		boolean reviewable = itemSessionControl.isAllowReview() || itemSessionControl.isShowFeedback();
		//<xsl:variable name="itemSessionState" select="$testSessionState/qw:item[@key=current()/@key]/qw:itemSessionState" as="element(qw:itemSessionState)"/>
		ItemSessionState itemSessionState = component.getTestSessionController().getTestSessionState().getItemSessionStates().get(itemNode.getKey());
		//<xsl:if test="$currentTestPart/@navigationMode='nonlinear' or exists($itemSessionState/@entryTime)">
		TestPart currentTestPart = component.getTestPart(component.getCurrentTestPartNode().getIdentifier());
		if(currentTestPart.getNavigationMode() == NavigationMode.NONLINEAR || itemSessionState.getEntryTime() != null) {
			
			sb.append("<li class='o_assessmentitem'>");
			sb.append("<button type='button' onclick=\"");
			String key = itemNode.getKey().toString();
			sb.append(FormJSHelper.getXHRFnCallFor(component.getQtiItem(), true, true,
					new NameValuePair("cid", Event.reviewItem.name()), new NameValuePair("item", key)));
			sb.append(";\" class='btn btn-default' ").append(" disabled", !reviewable).append("><span class='questionTitle'>")
			  .append(StringHelper.escapeHtml(itemNode.getSectionPartTitle())).append("</span>");

			if(!reviewable) {
				renderItemStatusMessage("reviewNotAllowed", "assessment.item.status.reviewNot", sb, translator);
			} else if(itemSessionState.getUnboundResponseIdentifiers().size() > 0
					|| itemSessionState.getInvalidResponseIdentifiers().size() > 0) {
				renderItemStatusMessage("reviewInvalid", "assessment.item.status.reviewInvalidAnswer", sb, translator);
			} else if(itemSessionState.isResponded()) {
				renderItemStatusMessage("review", "assessment.item.status.review", sb, translator);
			} else if(itemSessionState.getEntryTime() != null) {
				renderItemStatusMessage("reviewNotAnswered", "assessment.item.status.reviewNotAnswered", sb, translator);
			} else {
				renderItemStatusMessage("reviewNotSeen", "assessment.item.status.reviewNotSeen", sb, translator);
			}
			
			sb.append("</button></li>");
		}
	}
	
	private void renderTestFeebacks(AssessmentRenderer renderer, StringOutput sb, List<TestFeedback> testFeedbacks, AssessmentTestComponent component, TestFeedbackAccess access,
			URLBuilder ubu, Translator translator) {
		if(component.isHideFeedbacks()) return;
		
		for(TestFeedback testFeedback:testFeedbacks) {
			if(testFeedback.getTestFeedbackAccess() == access) {
				renderTestFeeback(renderer, sb, component, testFeedback, ubu, translator);
			}
		}
	}
	
	private void renderTestFeeback(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component, TestFeedback testFeedback,
			URLBuilder ubu, Translator translator) {
		if(component.isHideFeedbacks()) return;
		
		TestSessionState testSessionState = component.getTestSessionController().getTestSessionState();
		if(testFeedbackVisible(testFeedback, testSessionState)) {
			sb.append("<div class='o_info clearfix'>");
			sb.append("<h3>");
			if(StringHelper.containsNonWhitespace(testFeedback.getTitle())) {
				sb.append(StringHelper.escapeHtml(testFeedback.getTitle()));
			} else {
				sb.append(translator.translate("assessment.test.modal.feedback"));
			}
			sb.append("</h3>");

			testFeedback.getChildren().forEach((flow)
				-> renderFlow(renderer, sb, component, null, null, flow, ubu, translator));
			sb.append("</div>");
		}
	}
	
	private void renderNavigation(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component, URLBuilder ubu, Translator translator) {
		if(component.isRenderNavigation()) {
			sb.append("<div id='o_qti_menu' class='qtiworks o_assessmenttest o_testpartnavigation o_qti_menu_buttonstyle'>");
			
			//title
			boolean multiPartTest = component.hasMultipleTestParts();
			String title = multiPartTest ?
					translator.translate("assessment.test.nav.title.multiPartTestMenu") : translator.translate("assessment.test.nav.title.questionMenu");
			sb.append("<h3>").append(title).append("</h3>");
			
			//part, sections and item refs
			sb.append("<ul class='o_testpartnavigation list-unstyled'>");
			component.getCurrentTestPartNode().getChildren().forEach((node)
					-> renderNavigation(renderer, sb, component, node, ubu, translator));
			sb.append("</ul>");
			
			// test controls
			TestSessionController testSessionController = component.getTestSessionController();
			boolean allowedToEndTestPart = testSessionController.getTestSessionState().getCurrentTestPartKey() != null
					&& testSessionController.mayEndCurrentTestPart();
			
			sb.append("<div class='o_button_group'>");
			sb.append("<button type='button' onclick=\"");
			if(allowedToEndTestPart) {
				Form form = component.getQtiItem().getRootForm();
				String dispatchId = component.getQtiItem().getFormDispatchId();
				sb.append(FormJSHelper.getXHRFnCallFor(form, dispatchId, 1, true, true,
						new NameValuePair("cid", Event.endTestPart.name())));
			} else {
				sb.append("javascript:");
			}
			String endTestTitle = multiPartTest ?
					translator.translate("assessment.test.end.testPart") : translator.translate("assessment.test.end.test");
			sb.append(";\" class='btn btn-default o_sel_end_testpart'").append(" disabled", !allowedToEndTestPart).append("><span>")
			  .append(endTestTitle).append("</span>");
	
			sb.append("</button>");
			sb.append("</div></div>");
		}
	}
	
	private void renderNavigation(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component, TestPlanNode node,
			URLBuilder ubu, Translator translator) {
		switch(node.getTestNodeType()) {
			case ASSESSMENT_SECTION: renderNavigationAssessmentSection(renderer, sb, component, node, ubu, translator); break;
			case ASSESSMENT_ITEM_REF: renderNavigationAssessmentItem(sb, component, node, translator); break;
			default: break;
		}
	}
	
	private void renderNavigationAssessmentSection(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component, TestPlanNode sectionNode,
			URLBuilder ubu, Translator translator) {
		sb.append("<li class='o_assessmentsection o_qti_menu_item'>")
		  .append("<header><h4>").append(StringHelper.escapeHtml(sectionNode.getSectionPartTitle())).append("</h4>");
		renderAssessmentSectionRubrickBlock(renderer, sb, component, sectionNode, ubu, translator);

		sb.append("</header><ul class='o_testpartnavigation_inner list-unstyled'>");
		sectionNode.getChildren().forEach((child)
				-> renderNavigation(renderer, sb, component, child, ubu, translator));
		sb.append("</ul></li>");
	}
	
	private void renderAssessmentSectionRubrickBlock(AssessmentRenderer renderer, StringOutput sb, AssessmentTestComponent component, TestPlanNode sectionNode,
			URLBuilder ubu, Translator translator) {
		AssessmentSection selectedSection = component.getAssessmentSection(sectionNode.getIdentifier());
		if(selectedSection != null && selectedSection.getRubricBlocks().size() > 0) {
			for(RubricBlock rubricBlock:selectedSection.getRubricBlocks()) {
				sb.append("<div class='rubric'>");//@view (candidate)
				rubricBlock.getBlocks().forEach((block) -> renderBlock(renderer, sb, component, null, null, block, ubu, translator));
				sb.append("</div>");
			}
		}
	}
	
	private void renderItemStatusMessage(String status, String i18nKey, StringOutput sb, Translator translator) {
		String title = translator.translate(i18nKey);
		sb.append("<span class='o_assessmentitem_status ").append(status).append(" ' title=\"").append(StringEscapeUtils.escapeHtml(title))
		.append("\"><i class='o_icon o_icon-fw o_icon_qti_").append(status).append("'> </i><span>").append(title).append("</span></span>");
	}

	
	private void renderNavigationAssessmentItem(StringOutput sb, AssessmentTestComponent component, TestPlanNode itemNode, Translator translator) {
		String key = itemNode.getKey().toString();
		sb.append("<li class='o_assessmentitem'>");
		sb.append("<button type='button' onclick=\"");
		
		Form form = component.getQtiItem().getRootForm();
		String dispatchId = component.getQtiItem().getFormDispatchId();
		sb.append(FormJSHelper.getXHRFnCallFor(form, dispatchId, 1, true, true,
				new NameValuePair("cid", Event.selectItem.name()), new NameValuePair("item", key)));
		sb.append(";\" class='btn btn-default'><span class='questionTitle'>")
		  .append(StringHelper.escapeHtml(itemNode.getSectionPartTitle())).append("</span>");
		
		ItemSessionState itemSessionState = component.getItemSessionState(itemNode.getKey());
		if(itemSessionState.getEndTime() != null) {
			renderItemStatusMessage("ended", "assessment.item.status.finished", sb, translator);
		} else if(itemSessionState.getUnboundResponseIdentifiers().size() > 0
				|| itemSessionState.getInvalidResponseIdentifiers().size() > 0) {
			renderItemStatusMessage("invalid", "assessment.item.status.needsAttention", sb, translator);
		} else if(itemSessionState.isResponded() || itemSessionState.hasUncommittedResponseValues()) {
			renderItemStatusMessage("answered", "assessment.item.status.answered", sb, translator);
		} else if(itemSessionState.getEntryTime() != null) {
			renderItemStatusMessage("notAnswered", "assessment.item.status.notAnswered", sb, translator);
		} else {
			renderItemStatusMessage("notPresented", "assessment.item.status.notSeen", sb, translator);
		}
		
		sb.append("</button>");
		sb.append("</li>");
	}
	
	public static void printDocument(Element doc, OutputStream out) {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			transformer.transform(new DOMSource(doc), 
			     new StreamResult(new OutputStreamWriter(out, "UTF-8")));
		} catch (IllegalArgumentException | UnsupportedEncodingException | TransformerFactoryConfigurationError | TransformerException e) {
			log.error("", e);
		}
	}
	
    private TestPlanNodeKey extractTargetItemKey(final CandidateEvent candidateEvent) {
        final String keyString = candidateEvent.getTestItemKey();
        try {
            return TestPlanNodeKey.fromString(keyString);
        } catch (final Exception e) {
            throw new OLATRuntimeException("Unexpected Exception parsing TestPlanNodeKey " + keyString, e);
        }
    }
}
