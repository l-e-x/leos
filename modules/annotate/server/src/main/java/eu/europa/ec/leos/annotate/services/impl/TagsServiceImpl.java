/*
 * Copyright 2019 European Commission
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
package eu.europa.ec.leos.annotate.services.impl;

import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Tag;
import eu.europa.ec.leos.annotate.repository.TagRepository;
import eu.europa.ec.leos.annotate.services.TagsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for managing tags associated to annotations 
 */
@Service
public class TagsServiceImpl implements TagsService {

    private static final Logger LOG = LoggerFactory.getLogger(TagsServiceImpl.class);

    @Autowired
    private TagRepository tagRepos;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tag> getTagList(final List<String> tags, final Annotation annotation) {

        if (annotation == null) {
            LOG.error("Cannot save tags as belonging annotation is missing!");
            return null;
        }

        if (tags == null || tags.isEmpty()) {
            LOG.info("No tags found for saving");
            return null;
        }

        final List<Tag> preparedTags = new ArrayList<Tag>();
        tags.forEach(tag -> preparedTags.add(new Tag(tag, annotation)));

        return preparedTags;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void removeTags(final List<Tag> tagsToRemove) {

        if(tagsToRemove != null) {
            for(final Tag t : tagsToRemove) {
                tagRepos.customDelete(t.getId());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasSuggestionTag(final List<Tag> tags) {

        if(CollectionUtils.isEmpty(tags)) {
            return false;
        }
        
        return tags.stream().anyMatch(tag -> tag.getName().equals(Annotation.ANNOTATION_SUGGESTION));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasHighlightTag(final List<Tag> tags) {

        if(CollectionUtils.isEmpty(tags)) {
            return false;
        }
        
        return tags.stream().anyMatch(tag -> tag.getName().equals(Annotation.ANNOTATION_HIGHLIGHT));
    }
}
