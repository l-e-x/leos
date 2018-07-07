/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.support.xml
import com.google.common.base.Stopwatch
import eu.europa.ec.leos.vo.CommentVO
import eu.europa.ec.leos.vo.CommentVO.RefersTo
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

@Component
class XmlCommentProcessorImpl  implements XmlCommentProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(XmlCommentProcessorImpl.class);
    private static final SimpleDateFormat dateFormatISO_GMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

    @Override
    def List<CommentVO> fromXML(String xmlFragment) {
        Stopwatch watch = Stopwatch.createStarted();

        XmlSlurper xmlSlurper = new XmlSlurper(false, false)
        GPathResult rootNode = xmlSlurper.parseText(xmlFragment)
        List<CommentVO> commentList = new ArrayList<CommentVO>();

        dateFormatISO_GMT.setTimeZone(TimeZone.getTimeZone("UTC"));

        rootNode
        .'**'
        .findAll{it.name()=="popup"}
                .each { commentNode ->

            String commentId = commentNode.@'id'.text() ?: null
            String userLogin = commentNode.@'leos:userid'.text() ?: null
            String userName = commentNode.@'leos:username'.text() ?: null
            String date = commentNode.@'leos:datetime'.text() ?: null
            String dg = commentNode.@'leos:dg'.text() ?: null
            Date timestamp = (date != null) ? dateFormatISO_GMT.parse(date) : new Date()
            GPathResult commentText = commentNode.'*'.find{ it.name() == "p" }//TODO handle comments inside suggestions.
            String content = nodeToXML(commentText);

            String refersTo = commentNode.@'refersTo'.text() ?: null
            String elementId = rootNode.@'id'.text() ?: null

            commentList.add(new CommentVO(commentId, elementId, content, userName, userLogin, dg, timestamp, RefersTo.fromString(refersTo)));
        }
        LOG.trace("Created commentVO in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));

        return commentList;
    }

    private String nodeToXML(groovy.util.slurpersupport.NodeChild node) {
        def builder = new StreamingMarkupBuilder()
        builder.setUseDoubleQuotes(true)
        builder.setProperty("encoding", "UTF-8");
        def w = builder.bind { mkp.yield node };
        return w.toString();
    }
}



