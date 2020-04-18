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
package eu.europa.ec.leos.web.event;

import com.vaadin.ui.UI;

import java.util.Arrays;

public class NotificationEvent {

    public enum Type {
        INFO, WARNING, ERROR, TRAY
    }

    private final String messageKey;
    private final Type type;
    private final Object[] args;
    private String captionKey;
    private UI targetedUI;

    public NotificationEvent(Type type, String messageKey, Object... args) {
        this.messageKey = messageKey;
        this.type = type;
        this.args = args;
        this.targetedUI = UI.getCurrent();
    }

    public NotificationEvent(String captionKey, String messageKey, Type type, Object... args) {
        this.messageKey = messageKey;
        this.captionKey = captionKey;
        this.type = type;
        this.args = args;
        this.targetedUI = UI.getCurrent();
    }

    public NotificationEvent(UI targetedUI, Type type, String messageKey, Object... args) {
        this(type, messageKey, args);
        this.targetedUI = targetedUI;
    }

    public NotificationEvent(UI targetedUI, String captionKey, String messageKey, Type type, Object... args) {
        this(captionKey, messageKey, type, args);
        this.targetedUI = targetedUI;
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

    public UI getTargetedUI() {
        return targetedUI;
    }

    public void setTargetedUI(UI targetedUI) {
        this.targetedUI = targetedUI;
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationEvent that = (NotificationEvent) o;
        if (!Arrays.equals(args, that.args)) return false;
        if (messageKey != null ? !messageKey.equals(that.messageKey) : that.messageKey != null) return false;
        return (type == that.type);
    }

    @Override
    public int hashCode() {
        int result = messageKey != null ? messageKey.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (args != null ? Arrays.hashCode(args) : 0);
        return result;
    }
}
