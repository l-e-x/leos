package eu.europa.ec.leos.web.event.component;

public class ShowVersionRequestEvent {
    
    private String versionId;
    
    public String getVersionId() {
        return versionId;
    }
    
    public ShowVersionRequestEvent(String versionId) {
        this.versionId = versionId;
    }
    
}
