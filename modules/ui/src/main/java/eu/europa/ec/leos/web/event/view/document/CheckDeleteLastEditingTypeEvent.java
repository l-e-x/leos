package eu.europa.ec.leos.web.event.view.document;

public class CheckDeleteLastEditingTypeEvent {

    private final String elementId;
    private final Object actionEvent;

    public CheckDeleteLastEditingTypeEvent(String elementId, Object actionEvent) {
        this.elementId = elementId;
        this.actionEvent = actionEvent;
    }

    public String getElementId() {
        return elementId;
    }

    public Object getActionEvent() {
        return actionEvent;
    }

}
