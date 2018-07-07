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
package eu.europa.ec.leos.web.event;

import java.util.Arrays;

public class NotificationEvent {

    public static enum Type {
        INFO, WARNING, ERROR, TRAY, DISCLAIMER
    }

    private final String messageKey;
    private final Type type;
    private final Object[] args;
    private String captionKey;

    public NotificationEvent(Type type, String messageKey, Object... args) {
        this.messageKey = messageKey;
        this.type = type;
        this.args = args;
    }

    public void setCaptionKey(String captionKey) {
        this.captionKey=captionKey;
    }

    public String getCaptionKey() {
        return captionKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Type getType() {
        return type;
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotificationEvent that = (NotificationEvent) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(args, that.args)) return false;
        if (messageKey != null ? !messageKey.equals(that.messageKey) : that.messageKey != null) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = messageKey != null ? messageKey.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (args != null ? Arrays.hashCode(args) : 0);
        return result;
    }
}
