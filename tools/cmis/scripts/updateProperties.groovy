import org.apache.chemistry.opencmis.commons.*
import org.apache.chemistry.opencmis.commons.data.*
import org.apache.chemistry.opencmis.commons.enums.*
import org.apache.chemistry.opencmis.client.api.*
import javax.xml.xpath.*

String cql = "SELECT cmis:objectId  FROM leos:document"

ItemIterable<QueryResult> results = session.query(cql, false)

//ItemIterable<QueryResult> results = session.query(cql, false).getPage(10)
//ItemIterable<QueryResult> results = session.query(cql, false).skipTo(10).getPage(10)

results.each { hit ->
    hit.properties.each { println "${it.queryName}: ${it.firstValue}" 
    println "--------------------------------------"

    Document doc = session.getObject("${it.firstValue}");
    Map<String, Object> properties = new HashMap<>();
    properties.put("leos:language", "en");
    doc.updateProperties(properties, true)
    }
}

