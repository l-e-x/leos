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
package eu.europa.ec.leos.annotate.helper;

import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class TestDbHelper {

    @SuppressWarnings("unused")
    private static Logger LOG = LoggerFactory.getLogger(TestDbHelper.class);

    public static final String DEFAULT_GROUP_INTERNALNAME = "__world__";

    // clean the database by cleaning individual repositories
    public static void cleanupRepositories(Object instance) throws Exception {

        Class<?> clazz = instance.getClass();

        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        for (Field field : fields) {
            // look for field being Autowired
            List<Annotation> annots = Arrays.asList(field.getAnnotations());

            boolean isCandidateField = false;
            for (Annotation annot : annots) {
                Class<? extends Annotation> annotType = annot.annotationType();
                if (org.springframework.beans.factory.annotation.Autowired.class.isAssignableFrom(annotType)) {
                    isCandidateField = true;
                    break;
                }
            }

            if (!isCandidateField) {
                continue;
            }

            // check if the found field is one of our repositories
            Class<?> fieldType = field.getType();
            if (fieldType.getCanonicalName().startsWith("eu.europa.ec.leos.annotate.repository.")) {

                Method m = null;

                String targetMethodName = "deleteAll";
                if (fieldType.getCanonicalName().endsWith("AnnotationRepository")) {
                    targetMethodName = "customDeleteAll";
                }

                if (!field.isAccessible()) {
                    // make private fields become callable
                    field.setAccessible(true);
                }
                Object fieldValue = field.get(instance); // retrieve the current instance of the field variable
                m = fieldValue.getClass().getMethod(targetMethodName); // look for the desired method
                m.invoke(fieldValue); // run
            }
        }
    }

    public static Group insertDefaultGroup(GroupRepository groupRepos) {

        // insert required default group
        Group defaultGroup = new Group(DEFAULT_GROUP_INTERNALNAME, "Public", "Group for everybody", true);
        groupRepos.save(defaultGroup);
        return defaultGroup;
    }

}
