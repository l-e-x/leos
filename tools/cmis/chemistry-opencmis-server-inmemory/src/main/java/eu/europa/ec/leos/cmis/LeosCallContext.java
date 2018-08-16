package eu.europa.ec.leos.cmis;

import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.inmemory.ConfigConstants;
import org.apache.chemistry.opencmis.inmemory.DummyCallContext;

import java.util.HashMap;
import java.util.Map;

public class LeosCallContext extends DummyCallContext {
    // internal configuration keys, not declared in CallContext interface
    // - none required at this time...
    
    // external configuration keys, not declared in ConfigConstants class
    private static final String REPOSITORY_USER_KEY = "InMemoryServer.User";
    private static final String REPOSITORY_PASS_KEY = "InMemoryServer.Password";

    // mapping of internal to external configuration keys
    private static final Map<String, String> CONFIG_MAP = new HashMap<>();
    static {
        CONFIG_MAP.put(REPOSITORY_ID, ConfigConstants.REPOSITORY_ID);
        CONFIG_MAP.put(USERNAME, REPOSITORY_USER_KEY);
        CONFIG_MAP.put(PASSWORD, REPOSITORY_PASS_KEY);
    }

    public LeosCallContext(Map<String, String> params) {
        // static configuration
        put(LOCALE, "en");

        // dynamic configuration
        for (String key : CONFIG_MAP.keySet()) {
            put(key, params.get(CONFIG_MAP.get(key)));
        }
    }

    @Override
    public String getBinding() {
        // use local binding for better performance
        return BINDING_LOCAL;
    }

    @Override
    public CmisVersion getCmisVersion() {
        // ensure CMIS version 1.1, as required by LEOS
        return CmisVersion.CMIS_1_1;
    }
}
