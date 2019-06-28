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
package eu.europa.ec.leos.cmis.extensions;

import eu.europa.ec.leos.cmis.domain.ContentImpl;
import eu.europa.ec.leos.cmis.domain.SourceImpl;
import eu.europa.ec.leos.cmis.mapping.CmisProperties;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.document.*;
import io.atlassian.fugue.Option;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

import static eu.europa.ec.leos.cmis.extensions.CmisMetadataExtensions.*;

public class CmisDocumentExtensions {

    private static final Logger logger = LoggerFactory.getLogger(CmisDocumentExtensions.class);

    @SuppressWarnings("unchecked")
    public static <T extends LeosDocument> T toLeosDocument(Document document, Class<? extends T> type, boolean fetchContent) {

        T leosDocument;
        LeosCategory category = getCategory(document);
        switch (category) {
            case PROPOSAL:
                if (type.isAssignableFrom(Proposal.class)) {
                    leosDocument = (T) toLeosProposal(document, fetchContent);
                } else {
                    throw new IllegalStateException("Incompatible types! [category=" + category + ", mappedType=" + Proposal.class.getSimpleName() + ", wantedType=" + type.getSimpleName() + ']');
                }
                break;
            case MEMORANDUM:
                if (type.isAssignableFrom(Memorandum.class)) {
                    leosDocument = (T) toLeosMemorandum(document, fetchContent);
                } else {
                    throw new IllegalStateException("Incompatible types! [category=" + category + ", mappedType=" + Memorandum.class.getSimpleName() + ", wantedType=" + type.getSimpleName() + ']');
                }
                break;
            case BILL:
                if (type.isAssignableFrom(Bill.class)) {
                    leosDocument = (T) toLeosBill(document, fetchContent);
                } else {
                    throw new IllegalStateException("Incompatible types! [category=" + category + ", mappedType=" + Bill.class.getSimpleName() + ", wantedType=" + type.getSimpleName() + ']');
                }
                break;
            case ANNEX:
                if (type.isAssignableFrom(Annex.class)) {
                    leosDocument = (T) toLeosAnnex(document, fetchContent);
                } else {
                    throw new IllegalStateException("Incompatible types! [category=" + category + ", mappedType=" + Annex.class.getSimpleName() + ", wantedType=" + type.getSimpleName() + ']');
                }
                break;
            case MEDIA:
                if (type.isAssignableFrom(MediaDocument.class)) {
                    leosDocument = (T) toLeosMediaDocument(document, fetchContent);
                } else {
                    throw new IllegalStateException("Incompatible types! [category=" + category + ", mappedType=" + MediaDocument.class.getSimpleName() + ", wantedType=" + type.getSimpleName() + ']');
                }
                break;
            case CONFIG:
                if (type.isAssignableFrom(ConfigDocument.class)) {
                    leosDocument = (T) toLeosConfigDocument(document, fetchContent);
                } else {
                    throw new IllegalStateException("Incompatible types! [category=" + category + ", mappedType=" + ConfigDocument.class.getSimpleName() + ", wantedType=" + type.getSimpleName() + ']');
                }
                break;
            case LEG:
                if (type.isAssignableFrom(LegDocument.class)) {
                    leosDocument = (T) toLeosLegDocument(document, fetchContent);
                } else {
                    throw new IllegalStateException("Incompatible types! [category=" + category + ", mappedType=" + LegDocument.class.getSimpleName() + ", wantedType=" + type.getSimpleName() + ']');
                }
                break;
            default:
                throw new IllegalStateException("Unknown category:" + category);
        }

        return leosDocument;
    }

    private static Proposal toLeosProposal(Document d, boolean fetchContent) {
        return new Proposal(d.getId(), d.getName(), d.getCreatedBy(),
                getCreationInstant(d),
                d.getLastModifiedBy(),
                getLastModificationInstant(d),
                d.getVersionSeriesId(), d.getVersionLabel(), d.getCheckinComment(), d.isMajorVersion(), d.isLatestVersion(),
                getTitle(d),
                getCollaborators(d),
                getMilestoneComments(d),
                getInitialCreatedBy(d),
                getInitialCreationInstant(d),
                contentOption(d, fetchContent),
                getProposalMetadataOption(d));
    }

    private static Memorandum toLeosMemorandum(Document d, boolean fetchContent) {
        return new Memorandum(d.getId(), d.getName(), d.getCreatedBy(),
                getCreationInstant(d),
                d.getLastModifiedBy(),
                getLastModificationInstant(d),
                d.getVersionSeriesId(), d.getVersionLabel(), d.getCheckinComment(), d.isMajorVersion(), d.isLatestVersion(),
                getTitle(d),
                getCollaborators(d),
                getMilestoneComments(d),
                contentOption(d, fetchContent),
                getMemorandumMetadataOption(d));
    }

    private static Bill toLeosBill(Document d, boolean fetchContent) {
        return new Bill(d.getId(), d.getName(), d.getCreatedBy(),
                getCreationInstant(d),
                d.getLastModifiedBy(),
                getLastModificationInstant(d),
                d.getVersionSeriesId(), d.getVersionLabel(), d.getCheckinComment(), d.isMajorVersion(), d.isLatestVersion(),
                getTitle(d),
                getCollaborators(d),
                getMilestoneComments(d),
                contentOption(d, fetchContent),
                getBillMetadataOption(d));
    }

    private static Annex toLeosAnnex(Document d, boolean fetchContent) {
        return new Annex(d.getId(), d.getName(), d.getCreatedBy(),
                getCreationInstant(d),
                d.getLastModifiedBy(),
                getLastModificationInstant(d),
                d.getVersionSeriesId(), d.getVersionLabel(), d.getCheckinComment(), d.isMajorVersion(), d.isLatestVersion(),
                getTitle(d),
                getCollaborators(d),
                getMilestoneComments(d),
                contentOption(d, fetchContent),
                getAnnexMetadataOption(d));
    }

    private static MediaDocument toLeosMediaDocument(Document d, boolean fetchContent) {
        return new MediaDocument(d.getId(), d.getName(), d.getCreatedBy(),
                getCreationInstant(d),
                d.getLastModifiedBy(),
                getLastModificationInstant(d),
                d.getVersionSeriesId(), d.getVersionLabel(), d.getCheckinComment(), d.isMajorVersion(), d.isLatestVersion(),
                contentOption(d, fetchContent));
    }

    private static ConfigDocument toLeosConfigDocument(Document d, boolean fetchContent) {
        return new ConfigDocument(d.getId(), d.getName(), d.getCreatedBy(),
                getCreationInstant(d),
                d.getLastModifiedBy(),
                getLastModificationInstant(d),
                d.getVersionSeriesId(), d.getVersionLabel(), d.getCheckinComment(), d.isMajorVersion(), d.isLatestVersion(),
                contentOption(d, fetchContent));
    }

    private static LegDocument toLeosLegDocument(Document d, boolean fetchContent) {
        return new LegDocument(d.getId(), d.getName(), d.getCreatedBy(),
                getCreationInstant(d),
                d.getLastModifiedBy(),
                getLastModificationInstant(d),
                d.getVersionSeriesId(), d.getVersionLabel(), d.getCheckinComment(), d.isMajorVersion(), d.isLatestVersion(),
                getMilestoneComments(d),
                contentOption(d, fetchContent),
                getJobId(d),
                getJobDate(d),
                getStatus(d));
    }

    private static LeosCategory getCategory(Document document) {
        // FIXME add check for leos:document primary type???
        String cmisCategory = document.getPropertyValue(CmisProperties.DOCUMENT_CATEGORY.getId());
        return LeosCategory.valueOf(cmisCategory);
    }

    static Instant getCreationInstant(Document document) {
        GregorianCalendar creationDate = document.getCreationDate();
        return creationDate != null ? creationDate.toInstant() : Instant.MIN;
    }

    static Instant getLastModificationInstant(Document document) {
        GregorianCalendar lastModificationDate = document.getLastModificationDate();
        return lastModificationDate != null ? lastModificationDate.toInstant() : Instant.MIN;
    }

    private static Option<Content> contentOption(Document document, boolean fetchContent) {
        Content content = null;
        if (fetchContent) {
            ContentStream contentStream = document.getContentStream();
            if (contentStream != null) {
                content = new ContentImpl(contentStream.getFileName(), contentStream.getMimeType(),
                        contentStream.getLength(), new SourceImpl(contentStream.getStream()));
            }
        }

        return Option.option(content);
    }


    static Map<String, String> getCollaborators(Document document) {

        Property<String> collaboratorsProperty = document.getProperty(CmisProperties.COLLABORATORS.getId());
        List<String> collaboratorsPropertyValues = collaboratorsProperty.getValues();

        Map<String, String> users = new HashMap<>();
        collaboratorsPropertyValues.forEach(value -> {
            try {
                String[] values = value.split("::");
                if (values.length != 2) {
                    throw new UnknownFormatConversionException("User record is in incorrect format, required format[login::Authority ], present value=" + value);
                }

                users.put(values[0], values[1]);
            } catch (Exception e) {
                logger.error("Failure in processing user record [value=" + value + "], continuing...", e);
            }
        });

        return users;
    }

    // FIXME maybe move title property to metadata or remove it entirely
    private static String getTitle(Document document) { // FIXME add check for leos:xml primary type
        return document.getPropertyValue(CmisProperties.DOCUMENT_TITLE.getId());
    }


    private static List<String> getMilestoneComments(Document document) {
        Property<String> milestoneComments = document.getProperty(CmisProperties.MILESTONE_COMMENTS.getId());
        return milestoneComments.getValues();
    }


    private static String getJobId(Document document) {
        return document.getPropertyValue(CmisProperties.JOB_ID.getId());
    }


    private static Instant getJobDate(Document document) {
        GregorianCalendar jobDate = document.getPropertyValue(CmisProperties.JOB_DATE.getId());
        return jobDate != null ? jobDate.toInstant() : Instant.MIN;
    }


    private static LeosLegStatus getStatus(Document document) {
        return LeosLegStatus.valueOf(document.getPropertyValue(CmisProperties.STATUS.getId()));
    }

    static String getInitialCreatedBy(Document document) {
        String initialCreatedBy = document.getPropertyValue(CmisProperties.INITIAL_CREATED_BY.getId());
        return initialCreatedBy != null ? initialCreatedBy : document.getCreatedBy();
    }

    static Instant getInitialCreationInstant(Document document) {
        GregorianCalendar initialCreationDate = document.getPropertyValue(CmisProperties.INITIAL_CREATION_DATE.getId());
        return initialCreationDate != null ? initialCreationDate.toInstant() : getCreationInstant(document);
    }
}
