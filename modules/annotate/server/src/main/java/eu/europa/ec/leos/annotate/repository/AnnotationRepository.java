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

import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * the repository for all {@link Annotation} objects
 * 
 * note: we use a {@link PagingAndSortingRepository} here, which is an extension of {@link CrudRepository}
 *       in particular, it allows to apply sorting and paging easily by passing a {@link Pageable}
 */
@Repository("annotationRepos")
public interface AnnotationRepository extends PagingAndSortingRepository<Annotation, String>, AnnotationRepositoryCustom, JpaSpecificationExecutor<Annotation> {

    /**
     * find an annotation with a given ID and having a specific status
     * 
     * @param annotId 
     *        the annotation's ID
     * @param status 
     *        the desired {@link AnnotationStatus} of the annotation
     * 
     * @return found Annotation, or {@literal null}
     */
    Annotation findByIdAndStatus(String annotId, AnnotationStatus status);

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
     * count all items having a given metadata Id
     * 
     * @param metadataId the ID of the metadata set assigned to annotations
     * @return number of annotations assigned to given metadata set
     */
    long countByMetadataId(long metadataId);

    /**
     * find all annotations having one of given metadata Ids and a certain status
     * 
     * @param metadataIds 
     *        the IDs of the associated metadata
     * @param status 
     *        the desired {@link AnnotationStatus} of the annotation
     * 
     * @return found annotations
     */
    List<Annotation> findByMetadataIdIsInAndStatus(List<Long> metadataIds, AnnotationStatus status);

    /**
     * find all annotations having one of given metadata Ids
     * 
     * @param metadataIds 
     *        the IDs of the associated metadata
     * 
     * @return found {@link Annotation}s
     */
    List<Annotation> findByMetadataIdIsIn(List<Long> metadataIds);
    
    /**
     * keep the following signatures commented out here to remind what is easily possible using Spring Data framework
    List<Annotation> findByDocumentIdAndGroupIdAndRootAnnotationIdIsNull(long documentId, long groupId, Pageable page);
    List<Annotation> findByDocumentIdAndGroupIdAndUserIdAndRootAnnotationIdIsNull(long documentId, long groupId, long userId, Pageable page);
    */

    /**
     *  search for annotation replies to a given set of annotations
     *  
     * @param annotationIds 
     *        the ID of the annotations whose replies are wanted
     * @param status        
     *        the status that the annotations should have
     * @param page          
     *        {@link Pageable} implementation that allows specifying sorting, ordering, and amount of results wanted
     *        (required here as we have to provide the replies with the same sorting as their parent annotations)
     * 
     * @return list of annotation objects meeting criteria
     */
    List<Annotation> findByRootAnnotationIdIsInAndStatus(List<String> annotationIds, AnnotationStatus status, Pageable page);
    
    /**
     * search for annotations having sentDeleted=true and certain metadata IDs and certain status IDs
     * 
     * @param metadataIds
     *        the IDs of the desired metadata sets
     * @param status
     *        the status that the annotations should have
     * @return
     */
    List<Annotation> findByMetadataIdIsInAndStatusIsInAndSentDeletedIsTrue(List<Long> metadataIds, List<AnnotationStatus> status);
}
