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
package eu.europa.ec.leos.support.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class SpringTransactionIndicator extends TurboFilter {

    public static final String MDC_XA_ACTIVE_KEY = "xaActive";
    public static final String MDC_XA_READ_ONLY_KEY = "xaReadOnly";
    public static final String MDC_XA_SYNCH_KEY = "xaSynch";
    public static final String MDC_XA_NAME_KEY = "xaName";

    private Level level = Level.OFF;

    @Override
    public void start() {
        if ((level != null) && (level.levelInt < Level.OFF_INT)) {
            super.start();
        }

        if (isStarted()) {
            addInfo("Filter is started...");
        } else {
            addWarn("Filter is NOT started...");
        }
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (isStarted() && (this.level != null) && level.isGreaterOrEqual(this.level)) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                MDC.put(MDC_XA_ACTIVE_KEY, "+");
                boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
                MDC.put(MDC_XA_READ_ONLY_KEY, (readOnly ? "R" : "W"));
                boolean synch = TransactionSynchronizationManager.isSynchronizationActive();
                MDC.put(MDC_XA_SYNCH_KEY, (synch ? "Y" : "X"));
                String name = TransactionSynchronizationManager.getCurrentTransactionName();
                MDC.put(MDC_XA_NAME_KEY, ((name != null) ? name : "N/A"));
            } else {
                MDC.put(MDC_XA_ACTIVE_KEY, "-");
                MDC.put(MDC_XA_READ_ONLY_KEY, "-");
                MDC.put(MDC_XA_SYNCH_KEY, "-");
                MDC.put(MDC_XA_NAME_KEY, "-");
            }
        } else {
            MDC.remove(MDC_XA_ACTIVE_KEY);
            MDC.remove(MDC_XA_READ_ONLY_KEY);
            MDC.remove(MDC_XA_SYNCH_KEY);
            MDC.remove(MDC_XA_NAME_KEY);
        }
        return FilterReply.NEUTRAL;
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName() + "[" + getName() + "]";
    }

    public void setLevel(String level) {
        this.level = Level.toLevel(level);
        addInfo("Setting level: " + this.level);
    }
}
