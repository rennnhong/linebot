package idv.rennnhong.linebot.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import idv.rennnhong.linebot.dto.LBReplyMessage;
import idv.rennnhong.linebot.dto.Webhook;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("bot")
public class BotController {
    private final static String token = "JNxj5B/4iGryud4KWzx5Hc2Im286qgJ1kaFtP2JhI/KA0o5w/DtyzSKuJzPgIEnQDRlw+txN/ydoFH0unAkbQJDSbd+MxpjiqWFLi8tcTMxNwmRCExns27ylqBVjo85QqfkYEyIvSSW5n0KigNt9tQdB04t89/1O/w1cDnyilFU=";

    @PostMapping
    public String processEvents(@RequestHeader("X-Line-Signature") String signature, @RequestBody Object body) throws JsonProcessingException {
//        System.out.println("signature = " + signature);
//        System.out.println("request body = " + body);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(body);
        Webhook webhook = mapper.readValue(json, Webhook.class);

        for (Webhook.Event event : webhook.getEvents()) {
            if (event.getType().equals("message")) {
                RestTemplate rt = new RestTemplate();

                List<LBReplyMessage.Message> messages = new ArrayList<>();
                messages.add(new LBReplyMessage.Message("text", String.format("你說%s嗎", event.getMessage().getText())));

                LBReplyMessage lbReplyMessage = new LBReplyMessage();
                lbReplyMessage.setReplyToken(event.getReplyToken());
                lbReplyMessage.setMessages(messages);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(token);

                HttpEntity<LBReplyMessage> httpEntity = new HttpEntity(lbReplyMessage, headers);


                checkRequestBody(httpEntity);


                ResponseEntity<String> response = rt.postForEntity(
                        "https://api.line.me/v2/bot/message/reply",
                        httpEntity,
                        String.class
                );

            }
        }
        return "hahahaha";
    }

    private void checkRequestBody(HttpEntity httpEntity) throws JsonProcessingException {
        for (Map.Entry<String, List<String>> stringListEntry : httpEntity.getHeaders().entrySet()) {
            System.out.println(stringListEntry.getKey());
            for (String s : stringListEntry.getValue()) {
                System.out.println("-" + s);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(httpEntity.getBody());
        System.out.println(json);


    }

}

