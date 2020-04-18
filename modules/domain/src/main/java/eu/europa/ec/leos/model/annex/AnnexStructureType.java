package eu.europa.ec.leos.model.annex;

public enum AnnexStructureType {

    ARTICLE("article"),
    LEVEL("level");
    
    private String type;

    AnnexStructureType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
}
