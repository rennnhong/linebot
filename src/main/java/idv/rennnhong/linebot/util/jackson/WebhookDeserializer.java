package idv.rennnhong.linebot.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import idv.rennnhong.linebot.dto.*;

import java.io.IOException;
import java.util.ArrayList;

public class WebhookDeserializer extends JsonDeserializer<Webhook> {

    @Override
    public Webhook deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode rootNode = jsonParser.getCodec().readTree(jsonParser);
        JsonNode events = rootNode.get("events");

        Webhook webhook = new Webhook();
        webhook.setDestination(rootNode.get("destination").asText());
        webhook.setEvents(new ArrayList<>());
        if (events.isArray()) {
            for (JsonNode event : events) {
                String eventType = event.get("type").asText();
                if (eventType.equals("message")) {
                    webhook.getEvents().add(parseEvent(event));
                }
            }
        }
        return webhook;
    }

    private Event parseEvent(JsonNode jn) {
        Event event = new Event();
        event.setReplyToken(jn.get("replyToken").asText());
        event.setType(jn.get("type").asText());
        event.setMode(jn.get("mode").asText());
        event.setTimestamp(jn.get("timestamp").asText());
        JsonNode message = jn.get("message");
        String msgType = message.get("type").asText();
        if (msgType.equals("text")) {
            parseMessageEvent(jn, event);
        } else if (msgType.equals("location")) {
            parseLocationEvent(jn, event);
        }
        return event;
    }

    private Event parseMessageEvent(JsonNode jn, Event event) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode messageNode = jn.get("message");
        JsonNode sourceNode = jn.get("source");
        TextMessage message = mapper.convertValue(messageNode, TextMessage.class);
        Source source = mapper.convertValue(sourceNode, Source.class);
        event.setMessage(message);
        event.setSource(source);
        return event;
    }

    private Event parseLocationEvent(JsonNode jn, Event event) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode messageNode = jn.get("message");
        JsonNode sourceNode = jn.get("source");
        LocationMessage message = mapper.convertValue(messageNode, LocationMessage.class);
        Source source = mapper.convertValue(sourceNode, Source.class);
        event.setMessage(message);
        event.setSource(source);
        return event;
    }
}
