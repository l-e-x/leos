/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.support.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.StaxWriter;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import eu.europa.ec.leos.vo.MetaDataVO;
import org.springframework.stereotype.Component;
import javax.xml.stream.XMLStreamException;

import java.io.StringWriter;

@Component
public class XmlMetaDataProcessorImpl implements XmlMetaDataProcessor {

    private XStream xstream;
    private StaxDriver staxDriver;

    public XmlMetaDataProcessorImpl() {
        QNameMap qmap = new QNameMap();
        qmap.setDefaultNamespace("urn:eu:europa:ec:leos");
        qmap.setDefaultPrefix("leos");
        staxDriver = new StaxDriver(qmap);

        xstream = new XStream(staxDriver) {
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {
                    @Override
                    public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                        if (definedIn == Object.class) {
                            // This is not compatible with implicit collections where item name is not defined
                            return false;
                        } else {
                            return super.shouldSerializeMember(definedIn, fieldName);
                        }
                    }
                };
            }
        };
        xstream.alias("proprietary", MetaDataVO.class);
        xstream.useAttributeFor(MetaDataVO.class, "source");
        xstream.aliasField("source", MetaDataVO.class, "source");
    }

    @Override
    public String createXmlForProprietary(MetaDataVO metaDataVO) throws XMLStreamException {

        StringWriter strWriter = new StringWriter();
        StaxWriter sw = new StaxWriter(staxDriver.getQnameMap(),
                staxDriver.getOutputFactory().createXMLStreamWriter(strWriter),
                false, // don't do startDocument
                true); // do repair namespaces
        xstream.marshal(metaDataVO, sw);
        sw.close();
        String objectXml = strWriter.toString();

        return objectXml.replaceAll("leos:proprietary", "proprietary");
    }

    public MetaDataVO createMetaDataVOFromXml(String xml) {
        if (xml == null) {
            return new MetaDataVO();
        }
        xml=xml.replaceAll("leos:", "");//hack to remove as there is no namespace bound in partial xml
        return (MetaDataVO) xstream.fromXML(xml);
    }

}
