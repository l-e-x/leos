package eu.europa.ec.leos.services.messaging.conf;

import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;
import java.util.Base64;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.UTF_8;

public final class Base64Serializer {

    private Base64Serializer() {
    }

    public static String serialize(Serializable serializable) {
        byte[] encoded = SerializationUtils.serialize(serializable);
        return new String(Base64.getEncoder().encode(encoded), UTF_8);
    }

    public static Serializable deserialize(String serialized) {
        byte[] decoded = Base64.getDecoder().decode(serialized);
        return (Serializable) SerializationUtils.deserialize(decoded);
    }

}