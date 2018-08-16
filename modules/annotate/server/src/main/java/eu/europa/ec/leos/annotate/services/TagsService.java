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
package eu.europa.ec.leos.annotate.services;

import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Tag;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TagsService {

    // conversion of string list of tags to Tag objects associated to an annotation
    List<Tag> getTagList(List<String> tags, Annotation annotation);

    // removal of Tag objects
    @Transactional
    void removeTags(List<Tag> tagsToRemove);

    // check if a tag identifying an annotation as a suggestion is present in a list of tags
    boolean hasSuggestionTag(List<Tag> itemTags);
}
