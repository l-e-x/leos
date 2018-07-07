/**
 * Copyright 2016 European Commission
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
package eu.europa.ec.leos.test.support.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class ReaderDbHelper {

    private static final String TABLE_NAME_PLACE_HOLDER = "%TABLE_NAME%";
    private static final String CONDITION_PLACE_HOLDER = "%CONDITION%";
    private static final String COUNT_FOR_RECORDS = "SELECT COUNT(*) FROM " + TABLE_NAME_PLACE_HOLDER + " " + CONDITION_PLACE_HOLDER;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int countRows(String tableName, ColumnAndValueCondition... columnAndValues) {

        String sql = COUNT_FOR_RECORDS;
        sql = sql.replaceAll(TABLE_NAME_PLACE_HOLDER, tableName);

        StringBuilder conditionBuilder = new StringBuilder();
        if (columnAndValues != null && columnAndValues.length > 0) {
            conditionBuilder.append(" WHERE ");
            for (int i = 0, columnAndValueLength = columnAndValues.length; i < columnAndValueLength; i++) {
                ColumnAndValueCondition cav = columnAndValues[i];
                conditionBuilder.append(cav.getColumn());
                conditionBuilder.append(' ');
                conditionBuilder.append(cav.getOperator());
                conditionBuilder.append(' ');
                if (cav.getValue() != null) {
                    conditionBuilder.append('\'').append(cav.getValue()).append('\'');
                } else {
                    conditionBuilder.append("NULL");
                }

                if (i < (columnAndValueLength - 1)) {
                    conditionBuilder.append(" AND ");
                }
            }
        }
        sql = sql.replaceAll(CONDITION_PLACE_HOLDER, conditionBuilder.toString());

        Number number = jdbcTemplate.queryForObject(sql, Number.class);
        return number.intValue();
    }
}
