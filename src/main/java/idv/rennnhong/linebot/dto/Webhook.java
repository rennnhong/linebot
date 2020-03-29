package idv.rennnhong.linebot.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import idv.rennnhong.linebot.util.jackson.WebhookDeserializer;

import java.util.List;
@JsonDeserialize(using = WebhookDeserializer.class)
public class Webhook {
    private String destination;
    private List<Event> events;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    @Override
    public String toString() {
        return "Webhook{" +
                "destination='" + destination + '\'' +
                ", events=" + events +
                '}';
    }
}
