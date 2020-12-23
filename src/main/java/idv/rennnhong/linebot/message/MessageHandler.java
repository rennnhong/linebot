package idv.rennnhong.linebot.message;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.linecorp.bot.client.LineBlobClient;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonList;

@Slf4j
@Component
public class MessageHandler {

    @Autowired
    public LineMessagingClient lineMessagingClient;

    @Autowired
    public LineBlobClient lineBlobClient;


    public void replyText(@NonNull String replyToken, @NonNull String message) {
        if (replyToken.isEmpty()) {
            throw new IllegalArgumentException("replyToken must not be empty");
        }
        if (message.length() > 1000) {
            message = message.substring(0, 1000 - 2) + "……";
        }
        this.reply(replyToken, new TextMessage(message));
    }

    public void replyCarousel(@NonNull String replyToken, @NonNull String altText, @NonNull CarouselTemplate carouselTemplate) {
        if (replyToken.isEmpty()) {
            throw new IllegalArgumentException("replyToken must not be empty");
        }
        TemplateMessage templateMessage = new TemplateMessage("Carousel alt text", carouselTemplate);
        this.reply(replyToken, templateMessage);
    }

    public void reply(@NonNull String replyToken, @NonNull Message message) {
        reply(replyToken, singletonList(message));
    }

    public void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
        reply(replyToken, messages, false);
    }

    public void reply(@NonNull String replyToken,
                      @NonNull List<Message> messages,
                      boolean notificationDisabled) {
        Preconditions.checkArgument(messages.size() >= 1 || messages.size() <= 5, "messages size must between 1 to 5");
        try {
            BotApiResponse apiResponse = lineMessagingClient
                    .replyMessage(new ReplyMessage(replyToken, messages, notificationDisabled))
                    .get();
            log.info("Sent messages: {}", apiResponse);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void push(@NonNull String to, @NonNull Message message) {
        push(to, singletonList(message));
    }

    public void push(@NonNull String to, @NonNull List<Message> messages) {
        push(to, messages, false);
    }

    public void push(@NonNull String to,
                     @NonNull List<Message> messages,
                     boolean notificationDisabled) {
        Preconditions.checkArgument(messages.size() >= 1 || messages.size() <= 5, "messages size must between 1 to 5");
        try {
            List<List<Message>> messagePartitions = Lists.partition(messages, 5);
            for (List<Message> messagePartition : messagePartitions) {
                BotApiResponse apiResponse = lineMessagingClient.pushMessage(new PushMessage(to, messagePartition, notificationDisabled))
                        .get();
                log.info("Push messages: {}", apiResponse);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
//
//    public URI getPlacePhotoUri(String reference, Integer maxWidth, Integer maxHeight) {
//        Preconditions.checkArgument(Objects.nonNull(maxWidth) || Objects.nonNull(maxHeight), "maxWidth or maxHeight must has value each one");
//
//        Map<String, Object> params = Maps.newHashMap();
//        params.put("photoreference ", reference);
//        if (Objects.nonNull(maxWidth)) params.put("maxwidth  ", maxWidth);
//        if (Objects.nonNull(maxHeight)) params.put("maxheight  ", maxHeight);
////        UriBuilder build = new UriComponentsBuilder();
////                .queryParam(params).build();
////
////        "https://maps.googleapis.com/maps/api/place/photo"
//    }

}
