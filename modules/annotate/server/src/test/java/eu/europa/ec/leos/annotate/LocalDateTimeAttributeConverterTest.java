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
@SpringBootTest(properties = "spring.config.name=anot")
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

        final LocalDateTimeAttributeConverter conv = new LocalDateTimeAttributeConverter();
        Assert.assertNull(conv.convertToDatabaseColumn(null));
    }

    @Test
    public void testValueToDatabase() {

        final LocalDateTimeAttributeConverter conv = new LocalDateTimeAttributeConverter();
        final LocalDateTime domain = LocalDateTime.of(2012, 5, 3, 12, 48, 25);

        final Timestamp expected = Timestamp.valueOf(domain);

        final Timestamp dbValue = conv.convertToDatabaseColumn(domain);
        Assert.assertEquals(expected, dbValue);
    }

    @Test
    public void testNullToDomain() {

        final LocalDateTimeAttributeConverter conv = new LocalDateTimeAttributeConverter();
        Assert.assertNull(conv.convertToEntityAttribute(null));
    }

    @Test
    public void testValueToDomain() {

        final LocalDateTime expected = LocalDateTime.of(2012, 5, 3, 12, 48, 25);

        final LocalDateTimeAttributeConverter conv = new LocalDateTimeAttributeConverter();
        final Timestamp dbValue = Timestamp.valueOf(expected);

        final LocalDateTime actual = conv.convertToEntityAttribute(dbValue);
        Assert.assertEquals(expected, actual);
    }
}
