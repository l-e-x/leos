/*
 * Copyright 2019 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.instance;

import eu.europa.ec.leos.domain.common.InstanceType;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Properties;

public class InstanceCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        String leosInstance = ((Properties)conditionContext.getBeanFactory().getBean("applicationProperties")).getProperty("leos.instance");
        InstanceType currentInstance = InstanceType.valueOf(leosInstance);
        MultiValueMap<String, Object> attributes = annotatedTypeMetadata.getAllAnnotationAttributes(Instance.class.getName());

        InstanceType value = (InstanceType) attributes.getFirst("value");
        InstanceType[] instances = (InstanceType[]) attributes.getFirst("instances");

        if(InstanceType.ANY == value && instances != null && instances.length == 0){
            return true;
        } else if(value != null && InstanceType.ANY != value){
            return value == currentInstance;
        } else if(instances != null && instances.length != 0){
            return Arrays.stream(instances).anyMatch(instanceType -> instanceType == currentInstance);
        }
        return false;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.REGISTER_BEAN;
    }
}
