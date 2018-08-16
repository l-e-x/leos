/*
 * Copyright 2018 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.ui.view;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import eu.europa.ec.leos.domain.document.LeosDocument;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.compare.ContentComparatorService;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.web.event.view.document.DocumentUpdatedEvent;
import eu.europa.ec.leos.web.event.view.document.MergeSuggestionRequest;
import eu.europa.ec.leos.web.event.view.document.MergeSuggestionResponse;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.apache.jena.ext.com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.BiFunction;

@Component
@Scope("prototype")
public class CommonDelegate<T extends LeosDocument.XmlDocument> {

    private static final Logger LOG = LoggerFactory.getLogger(CommonDelegate.class);
    private EventBus eventBus;
    private MessageHelper messageHelper;

    @Autowired
    public CommonDelegate(EventBus eventBus, MessageHelper messageHelper) {
        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
    }

    /*FIXME : this is unfinished refactoring idea to move common code from presenters.
    * It can not be applied to all presenters as updateFn has different interface for annexService and documentService*/
    public void mergeSuggestion(T document, MergeSuggestionRequest event, ElementProcessor<T> elementProcessor, TriFunction<T, byte[], String, T> updateDocumentFn) {
        byte[] resultXmlContent = elementProcessor.replaceTextInElement(document, event.getOrigText(), event.getNewText(), event.getElementId(), event.getStartOffset(), event.getEndOffset());
        if (resultXmlContent == null) {
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.failed"), MergeSuggestionResponse.Result.ERROR));
            return;
        }
        document = updateDocumentFn.apply(document, resultXmlContent, messageHelper.getMessage("operation.merge.suggestion"));
        if (document != null) {
            eventBus.post(new RefreshDocumentEvent());
            eventBus.post(new DocumentUpdatedEvent());
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.success"), MergeSuggestionResponse.Result.SUCCESS));
        } else {
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.failed"), MergeSuggestionResponse.Result.ERROR));
        }
    }
}