import org.apache.chemistry.opencmis.commons.*
import org.apache.chemistry.opencmis.commons.data.*
import org.apache.chemistry.opencmis.commons.enums.*
import org.apache.chemistry.opencmis.client.api.*

Folder folder = (Folder) session.getObjectByPath("/local/khandsh/leos")

delete(folder)

def delete(Folder folder) {
    deleteChildren(folder)
    folder.delete(true)
    println "deleting folder:" + folder.name
}

def deleteChildren(Folder fold) {
    fold.getChildren().each { child ->
        if (child instanceof Folder) {
                    deleteChildren(child)
                    println "deleting folder:" + child.name
                    child.delete(true)
        } else if (child instanceof Document) {
                    println "deleting doc:" + child.name
                    child.delete(true)
        } else if (child instanceof Item) {
                    child.delete(true)
        } else if (child instanceof Policy) {
                    child.delete(true)
        }
    }
}
