import org.apache.chemistry.opencmis.commons.*
import org.apache.chemistry.opencmis.commons.data.*
import org.apache.chemistry.opencmis.commons.enums.*
import org.apache.chemistry.opencmis.client.api.*

cmis = new scripts.CMIS(session)

// destination folder
Folder destFolder = cmis.getFolder("/")

// source folder
String localPath = "D:\\LEOS\\Backup"
//String localPath = "D://LEOS//CMIS_REPO//acc//leos"

// upload folder tree
upload(destFolder, localPath)


//--------------------------------------------------
def upload(destination, String localPath,
        String folderType = "leos:folder",
        String documentType = "leos:document",
        VersioningState versioningState = VersioningState.MAJOR) {

    println "Uploading...\n"
    doUpload(destination, new File(localPath), folderType, documentType, versioningState)
    println "\n...done."
}

def doUpload(Folder parent, File folder, String folderType, String documentType, VersioningState versioningState) {
    folder.eachFile {
        if (it.name.startsWith(".")) {
            println "Skipping ${it.name}"
            return
        }

        println it.name

        if (it.isFile()) {
              Document doc=cmis.createDocumentFromFile(parent, it, documentType, versioningState)
              Map<String, Object> properties = new HashMap<>();
              properties.put("leos:language", "en");
              if (folder.name.equals("samples")){
                      def akomaNtoso = new XmlSlurper().parse(it)
                      def strName = akomaNtoso.bill.preface.longTitle.p.docTitle.children().join(" ")
                      if(strName==null ||strName.equals("")){
                          strName=it.name
                      }
                      properties.put("cmis:name",strName)
                      properties.put("leos:title",strName)
                      properties.put("leos:stage", "DRAFT");
                      properties.put("leos:system", "LEOS");                      
                      properties.put("leos:template", "SJ-016");                                            
                      println "updated name "+strName
               }
              doc.updateProperties(properties, true)
        }
        else if(it.isDirectory()) {
            Folder newFolder = cmis.createFolder(parent, it.name, folderType)
            doUpload(newFolder, it, folderType, documentType, versioningState)
        }
    }
}