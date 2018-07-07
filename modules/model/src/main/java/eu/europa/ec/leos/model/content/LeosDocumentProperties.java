/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.model.content;

public interface LeosDocumentProperties extends LeosFileProperties{

    public static final String TITLE    = "leos:title";
    public static final String TEMPLATE = "leos:template";
    public static final String LANGUAGE = "leos:language";
    public static final String STAGE    = "leos:stage";
    public static final String SYSTEM   = "leos:system";

    /** Returns the title of the document stored in repository 
     * @return title as string
     */
    String getTitle();

    /** returns the Language code stored in repository object (default=en)
     * @return langugae code
     */
    String getLanguage();

    /** returns template for leos document
     * @return template
     */
    String getTemplate();
    /** returns Stage for leos document
     * @return Stage
     */
    Stage getStage();
    /** returns OwnerSystem for leos document
     * @return System
     */
    OwnerSystem getOwnerSystem();


    //Enum definition for System
    public enum OwnerSystem{
        LEOS("LEOS"),
        CISNET("CISNET");
        
        private String value;
        
        private OwnerSystem(String value){
            this.value=value;
        }
        
        public String getValue(){
            return value;
        }
        
        public static OwnerSystem getOwnerSystem(String value){
            for(OwnerSystem s:OwnerSystem.values()){
                if(s.value.equals(value)){
                    return s;
                }
            }
            throw new IllegalArgumentException("Invalid Owner System!!");
        }
    } 

    //Enum definition for Stage
    public enum Stage{
        DRAFT("DRAFT"),
        EDIT("EDIT"), //Always Synchronise with WSDL
        FEEDBACK("REVIEW"),
        ARCHIVED("ARCHIVED");//Archived functionality needs to be revisited

        private String value;

        Stage(String value){
            this.value=value;
        }
        
        public String getValue(){
            return this.value;
        }
        public static Stage getStage(String value){
            for(Stage s:Stage.values()){
                if(s.value.equals(value)){
                    return s;
                }
            }
            throw new IllegalArgumentException("Invalid Stage!!");
        }
    }
}
