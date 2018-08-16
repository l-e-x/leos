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
package eu.europa.ec.leos.annotate;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class LocalDateTimeAttributeConverterTest {

    /**
     * Rudimentary tests on the {@link LocalDateTimeAttributeConverter}
     */

    // -------------------------------------
    // Tests
    // -------------------------------------

    @Test
    public void testNullToDatabase() {

        LocalDateTimeAttributeConverter conv = new LocalDateTimeAttributeConverter();
        Assert.assertNull(conv.convertToDatabaseColumn(null));
    }

    @Test
    public void testValueToDatabase() {

        LocalDateTimeAttributeConverter conv = new LocalDateTimeAttributeConverter();
        LocalDateTime domain = LocalDateTime.of(2012, 5, 3, 12, 48, 25);

        final Timestamp expected = Timestamp.valueOf(domain);

        Timestamp db = conv.convertToDatabaseColumn(domain);
        Assert.assertEquals(expected, db);
    }

    @Test
    public void testNullToDomain() {

        LocalDateTimeAttributeConverter conv = new LocalDateTimeAttributeConverter();
        Assert.assertNull(conv.convertToEntityAttribute(null));
    }

    @Test
    public void testValueToDomain() {

        final LocalDateTime expected = LocalDateTime.of(2012, 5, 3, 12, 48, 25);

        LocalDateTimeAttributeConverter conv = new LocalDateTimeAttributeConverter();
        final Timestamp db = Timestamp.valueOf(expected);

        LocalDateTime actual = conv.convertToEntityAttribute(db);
        Assert.assertEquals(expected, actual);
    }
}
