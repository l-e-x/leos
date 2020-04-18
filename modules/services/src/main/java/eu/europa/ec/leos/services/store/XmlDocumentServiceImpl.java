package eu.europa.ec.leos.services.store;

import eu.europa.ec.leos.cmis.repository.CmisRepository;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static eu.europa.ec.leos.cmis.support.RepositoryUtil.updateDocumentProperties;

@Service
public class XmlDocumentServiceImpl implements XmlDocumentService {
    
    private final CmisRepository cmisRepository;
    private final XmlContentProcessor xmlContentProcessor;
    private final MessageHelper messageHelper;
    
    @Autowired
    public XmlDocumentServiceImpl(CmisRepository cmisRepository, XmlContentProcessor xmlContentProcessor, MessageHelper messageHelper) {
        this.cmisRepository = cmisRepository;
        this.xmlContentProcessor = xmlContentProcessor;
        this.messageHelper = messageHelper;
    }
    
    public boolean updateInternalReferences(XmlDocument xmlDocument) throws Exception {
        byte[] content = xmlDocument.getContent().get().getSource().getBytes();
        byte[] newContent = xmlContentProcessor.updateReferences(content);
    
        boolean updated = newContent != content;
        if(updated) {
            String message = messageHelper.getMessage("internal.ref.checkinComment");
            cmisRepository.updateDocument(xmlDocument.getId(), updateDocumentProperties(xmlDocument.getMetadata().get()), newContent, VersionType.MINOR, message);
        }
        return updated;
    }
}
