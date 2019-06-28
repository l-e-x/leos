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
package eu.europa.ec.leos.repository.document;

import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.repository.LeosRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Bill Repository implementation.
 *
 * @constructor Creates a specific Bill Repository, injected with a generic LEOS Repository.
 */
@Repository
public class BillRepositoryImpl implements BillRepository {
    private static final Logger logger = LoggerFactory.getLogger(BillRepositoryImpl.class);

    private final LeosRepository leosRepository;

    @Autowired
    public BillRepositoryImpl(LeosRepository leosRepository) {
        this.leosRepository = leosRepository;
    }

    @Override
    public Bill createBill(String templateId, String path, String name, BillMetadata metadata) {
        logger.debug("Creating Bill... [template=" + templateId + ", path=" + path + ", name=" + name + "]");
        return leosRepository.createDocument(templateId, path, name, metadata, Bill.class);
    }

    @Override
    public Bill createBillFromContent(String path, String name, BillMetadata metadata, byte[] content) {
        logger.debug("Creating Bill From Content... [path=" + path + ", name=" + name + "]");
        return leosRepository.createDocumentFromContent(path, name, metadata, Bill.class, LeosCategory.BILL.name(), content);
    }

    @Override
    public Bill updateBill(String id, BillMetadata metadata, byte[] content, boolean major, String comment) {
        logger.debug("Updating Bill metadata and content... [id=" + id + "]");
        return leosRepository.updateDocument(id, metadata, content, major, comment, Bill.class);
    }

    @Override
    public Bill updateBill(String id, BillMetadata metadata) {
        logger.debug("Updating Bill metadata... [id=" + id + "]");
        return leosRepository.updateDocument(id, metadata, Bill.class);
    }

    @Override
    public Bill updateMilestoneComments(String id, List<String> milestoneComments, byte[] content, boolean major, String comment) {
        logger.debug("Updating Bill milestoneComments... [id=" + id + "]");
        return leosRepository.updateMilestoneComments(id, content, milestoneComments, major, comment, Bill.class);
    }

    @Override
    public Bill updateMilestoneComments(String id, List<String> milestoneComments) {
        logger.debug("Updating Bill milestoneComments... [id=" + id + "]");
        return leosRepository.updateMilestoneComments(id, milestoneComments, Bill.class);
    }

    @Override
    public Bill findBillById(String id, boolean latest) {
        logger.debug("Finding Bill by ID... [id=" + id + ", latest=" + latest + "]");
        return leosRepository.findDocumentById(id, Bill.class, latest);
    }

    @Override
    public List<Bill> findBillVersions(String id, boolean fetchContent) {
        logger.debug("Finding Bill versions... [id=" + id + "]");
        return leosRepository.findDocumentVersionsById(id, Bill.class, fetchContent);
    }
}
