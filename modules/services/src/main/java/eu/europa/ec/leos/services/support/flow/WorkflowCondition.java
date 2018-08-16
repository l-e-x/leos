/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.services.support.flow;

import eu.europa.ec.leos.domain.common.InstanceContext;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

import java.util.Properties;

public class WorkflowCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        String instanceWorkflow = ((Properties)conditionContext.getBeanFactory().getBean("applicationProperties")).getProperty("leos.workflow");
        MultiValueMap<String, Object> attrs = annotatedTypeMetadata.getAllAnnotationAttributes(Workflow.class.getName());
        InstanceContext.Type workflowValue = null;
        if (attrs != null) {
            workflowValue = (InstanceContext.Type) attrs.getFirst("value");
        }

        return workflowValue!= null && workflowValue.getValue().equals(instanceWorkflow);
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.REGISTER_BEAN;
    }
}
