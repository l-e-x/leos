package eu.europa.ec.leos.services.store;

import eu.europa.ec.leos.domain.cmis.document.XmlDocument;

public interface XmlDocumentService {
    
    boolean updateInternalReferences(XmlDocument xmlDocument) throws Exception;
    
}
