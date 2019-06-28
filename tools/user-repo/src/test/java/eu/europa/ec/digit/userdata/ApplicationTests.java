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
package eu.europa.ec.digit.userdata;

import eu.europa.ec.digit.userdata.entities.User;
import eu.europa.ec.digit.userdata.repositories.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class ApplicationTests {

    private static Logger LOG = LoggerFactory.getLogger(ApplicationTests.class);
    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional(readOnly = true)
    public void test_findUsersByKey() {
        Stream<User> users = userRepository.findUsersByKey("doe");
        users.forEach(user -> LOG.debug(user.getLogin()));
    }

    @Test
    @Transactional(readOnly = true)
    public void test_findBylogin() {
        User user = userRepository.findByLogin("jane");
        assertNotNull(user);
        assertEquals(user.getLogin(),"jane");
        assertEquals(user.getRoles().get(0), "SUPPORT");
        assertEquals(user.getPerId(), Long.valueOf(3)); //from data-h2.sql
    }

    @Test
    @Transactional(readOnly = true)
    public void test_findAllDg() {
        Stream<String> dgs = userRepository.findAllEntities();
        assertNotNull(dgs);
        assertEquals(dgs.count(), 2); //unique dgs from data-h2.sql
    }
}
