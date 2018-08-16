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
package eu.europa.ec.leos.annotate.repository;

import java.util.List;

import eu.europa.ec.leos.annotate.model.entity.Annotation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * the repository for all {@link Annotation} objects
 * 
 * note: we use a {@link PagingAndSortingRepository} here, which is an extension of {@link CrudRepository}
 *       in particular, it allows to apply sorting and paging easily by passing a {@link Pageable}
 */
@Repository
public interface AnnotationRepository extends PagingAndSortingRepository<Annotation, String>, AnnotationRepositoryCustom, JpaSpecificationExecutor<Annotation> {

    /**
     * find an annotation based on its ID
     * 
     * @param id the annotation's ID
     * 
     * @return found Annotation, or {@literal null}
     */
    Annotation findById(String id);

    /**
     * check if there is an annotation referring to a given document ID
     * 
     * @param documentId the ID of the document
     * 
     * @return {@literal true} if at least one annotation to the document is present
     * 
     * NOTE: there was a bug (see https://jira.spring.io/browse/DATAJPA-851) in the existsBy feature 
     *       before spring-data-jpa 1.11, breaking this generic search functionality
     *       with our current spring-data-jpa version 1.11.9 (part of spring-boot-starter-data-jpa 1.5.9), it works...
     */
    boolean existsByDocumentId(long documentId);

    /**
     * delete all annotations from the database
     * 
     * NOTE: the simple statement
     *           annotRepos.deleteAll();
     *       has a problem with the foreign key constraint of Annotation.Root to Annotation.Annotation_ID (at least when using H2 database)
     *       when this foreign key constraint is removed, everything works; the exception thrown is an ObjectOptimisticLockingFailureException/StaleStateException,
     *       which occurs since a non-existing object is tried to be deleted - it seems that the deleteAll does not use the appropriate order
     *       when deleting the objects, or does not update its entities correctly while deleting...
     *       workaround: we delete the annotations individually - this is ok as this method is only used for tests, not for productive code!
     */
    @Modifying
    @Transactional
    @Query("delete from Annotation a")
    void customDeleteAll();

    /**
     * count all items having a root annotation id set (i.e. replies)
     * 
     * @return number of found annotations
     */
    long countByRootAnnotationIdNotNull();

    /**
     * keep the following signatures commented out here to remind what is easily possible using Spring Data framework
    List<Annotation> findByDocumentIdAndGroupIdAndRootAnnotationIdIsNull(long documentId, long groupId, Pageable page);
    List<Annotation> findByDocumentIdAndGroupIdAndUserIdAndRootAnnotationIdIsNull(long documentId, long groupId, long userId, Pageable page);
    */

    /**
     *  search for annotation replies to a given set of annotations
     *  
     * @param annotationIds the ID of the annotations whose replies are wanted
     * @param page          {@link Pageable} implementation that allows specifying sorting, ordering, and amount of results wanted
     *                      (required here as we have to provide the replies with the same sorting as their parent annotations)
     * 
     * @return              list of annotation objects meeting criteria
     */
    List<Annotation> findByRootAnnotationIdIsIn(List<String> annotationIds, Pageable page);
}
