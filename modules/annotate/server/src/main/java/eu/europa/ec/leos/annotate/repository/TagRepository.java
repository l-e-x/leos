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
package eu.europa.ec.leos.annotate.repository;

import eu.europa.ec.leos.annotate.model.entity.Tag;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * the repository for all {@link Tag} objects denoting the tags assigned to annotations
 */
public interface TagRepository extends CrudRepository<Tag, Long> {

    /**
     * count the number of tags associated to a given annotation ID
     * 
     * @param annotationId the annotation ID to which the number of tags is wanted
     * @return number of tags associated to an annotation
     */
    long countByAnnotationId(String annotationId);

    /**
     * custom delete function
     * 
     * due to the way we modeled {@link Annotation} and {@link Tag} objects, the Annotation is the master in this 1:n relationship;
     * as a consequence, hibernate does not launch a DELETE statement when asking it to do so!
     * even CrudRepository-generated functions like deleteById don't do it
     * so the only way to achieve it is by using a custom query
     * 
     * @param tagId the ID of the tag to be deleted
     */
    @Modifying
    @Query("delete from Tag t where t.id =:tagId")
    void customDelete(@Param("tagId") long tagId);
}
