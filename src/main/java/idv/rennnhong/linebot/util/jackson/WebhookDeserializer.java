package idv.rennnhong.linebot.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import idv.rennnhong.linebot.dto.Event;
import idv.rennnhong.linebot.dto.Source;
import idv.rennnhong.linebot.dto.TextMessage;
import idv.rennnhong.linebot.dto.Webhook;

import java.io.IOException;
import java.util.ArrayList;

public class WebhookDeserializer extends JsonDeserializer<Webhook> {

    @Override
    public Webhook deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        JsonNode rootNode = jsonParser.getCodec().readTree(jsonParser);
        JsonNode events = rootNode.get("events");

        Webhook webhook = new Webhook();
        webhook.setDestination(rootNode.get("destination").asText());
        webhook.setEvents(new ArrayList<>());
        if(events.isArray()){
            for (JsonNode event : events) {
                String eventType = event.get("type").asText();
                if(eventType.equals("message")){
                    JsonNode message = event.get("message");
                    String msgType = message.get("type").asText();
                    if(msgType.equals("text")){
                        webhook.getEvents().add(parseMessageEvent(event));
                    }
                }
            }
        }


        return webhook;
    }

    private Event<TextMessage, Source> parseMessageEvent(JsonNode jn){
        ObjectMapper mapper = new ObjectMapper();

        JsonNode messageNode = jn.get("message");
        JsonNode sourceNode = jn.get("source");
        TextMessage message =  mapper.convertValue(messageNode,TextMessage.class);
        Source source = mapper.convertValue(sourceNode,Source.class);

        Event<TextMessage, Source> event = new Event();
        event.setReplyToken(jn.get("replyToken").asText());
        event.setType(jn.get("type").asText());
        event.setMode(jn.get("mode").asText());
        event.setTimestamp(jn.get("timestamp").asText());
        event.setMessage(message);
        event.setSource(source);

        return event;
    }
}
