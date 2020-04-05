package idv.rennnhong.linebot.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.io.ByteStreams;
import com.linecorp.bot.client.LineBlobClient;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.DatetimePickerAction;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.*;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.*;
import com.linecorp.bot.model.message.imagemap.*;
import com.linecorp.bot.model.message.sender.Sender;
import com.linecorp.bot.model.message.template.*;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import idv.rennnhong.linebot.LinebotApplication;
import idv.rennnhong.linebot.dto.LBReplyMessage;
import idv.rennnhong.linebot.dto.Webhook;
import idv.rennnhong.linebot.util.jackson.WebhookDeserializer;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Slf4j
@LineMessageHandler
public class BotController{
    @Autowired
    private LineMessagingClient lineMessagingClient;

    @Autowired
    private LineBlobClient lineBlobClient;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        TextMessageContent message = event.getMessage();
        handleTextContent(event.getReplyToken(), event, message);
    }

    @EventMapping
    public void handleStickerMessageEvent(MessageEvent<StickerMessageContent> event) {
        handleSticker(event.getReplyToken(), event.getMessage());
    }

    @EventMapping
    public void handleLocationMessageEvent(MessageEvent<LocationMessageContent> event) {
        LocationMessageContent locationMessage = event.getMessage();
        reply(event.getReplyToken(), new LocationMessage(
                locationMessage.getTitle(),
                locationMessage.getAddress(),
                locationMessage.getLatitude(),
                locationMessage.getLongitude()
        ));
    }

    @EventMapping
    public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event) throws IOException {
        // You need to install ImageMagick
        handleHeavyContent(
                event.getReplyToken(),
                event.getMessage().getId(),
                responseBody -> {
                    final ContentProvider provider = event.getMessage().getContentProvider();
                    final DownloadedContent jpg;
                    final DownloadedContent previewImg;
                    if (provider.isExternal()) {
                        jpg = new DownloadedContent(null, provider.getOriginalContentUrl());
                        previewImg = new DownloadedContent(null, provider.getPreviewImageUrl());
                    } else {
                        jpg = saveContent("jpg", responseBody);
                        previewImg = createTempFile("jpg");
                        system(
                                "convert",
                                "-resize", "240x",
                                jpg.path.toString(),
                                previewImg.path.toString());
                    }
                    reply(event.getReplyToken(),
                            new ImageMessage(jpg.getUri(), previewImg.getUri()));
                });
    }

    @EventMapping
    public void handleAudioMessageEvent(MessageEvent<AudioMessageContent> event) throws IOException {
        handleHeavyContent(
                event.getReplyToken(),
                event.getMessage().getId(),
                responseBody -> {
                    final ContentProvider provider = event.getMessage().getContentProvider();
                    final DownloadedContent mp4;
                    if (provider.isExternal()) {
                        mp4 = new DownloadedContent(null, provider.getOriginalContentUrl());
                    } else {
                        mp4 = saveContent("mp4", responseBody);
                    }
                    reply(event.getReplyToken(), new AudioMessage(mp4.getUri(), 100));
                });
    }

    @EventMapping
    public void handleVideoMessageEvent(MessageEvent<VideoMessageContent> event) throws IOException {
        // You need to install ffmpeg and ImageMagick.
        handleHeavyContent(
                event.getReplyToken(),
                event.getMessage().getId(),
                responseBody -> {
                    final ContentProvider provider = event.getMessage().getContentProvider();
                    final DownloadedContent mp4;
                    final DownloadedContent previewImg;
                    if (provider.isExternal()) {
                        mp4 = new DownloadedContent(null, provider.getOriginalContentUrl());
                        previewImg = new DownloadedContent(null, provider.getPreviewImageUrl());
                    } else {
                        mp4 = saveContent("mp4", responseBody);
                        previewImg = createTempFile("jpg");
                        system("convert",
                                mp4.path + "[0]",
                                previewImg.path.toString());
                    }
                    reply(event.getReplyToken(),
                            new VideoMessage(mp4.getUri(), previewImg.uri));
                });
    }

    @EventMapping
    public void handleFileMessageEvent(MessageEvent<FileMessageContent> event) {
        this.reply(event.getReplyToken(),
                new TextMessage(String.format("Received '%s'(%d bytes)",
                        event.getMessage().getFileName(),
                        event.getMessage().getFileSize())));
    }

    @EventMapping
    public void handleUnfollowEvent(UnfollowEvent event) {
        log.info("unfollowed this bot: {}", event);
    }

    @EventMapping
    public void handleFollowEvent(FollowEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Got followed event");
    }

    @EventMapping
    public void handleJoinEvent(JoinEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Joined " + event.getSource());
    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken,
                "Got postback data " + event.getPostbackContent().getData() + ", param " + event
                        .getPostbackContent().getParams().toString());
    }

    @EventMapping
    public void handleBeaconEvent(BeaconEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Got beacon message " + event.getBeacon().getHwid());
    }

    @EventMapping
    public void handleMemberJoined(MemberJoinedEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Got memberJoined message " + event.getJoined().getMembers()
                .stream().map(com.linecorp.bot.model.event.source.Source::getUserId)
                .collect(Collectors.joining(",")));
    }

    @EventMapping
    public void handleMemberLeft(MemberLeftEvent event) {
        log.info("Got memberLeft message: {}", event.getLeft().getMembers()
                .stream().map(com.linecorp.bot.model.event.source.Source::getUserId)
                .collect(Collectors.joining(",")));
    }

    @EventMapping
    public void handleOtherEvent(com.linecorp.bot.model.event.Event event) {
        log.info("Received message(Ignored): {}", event);
    }

    private void reply(@NonNull String replyToken, @NonNull Message message) {
        reply(replyToken, singletonList(message));
    }

    private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
        reply(replyToken, messages, false);
    }

    private void reply(@NonNull String replyToken,
                       @NonNull List<Message> messages,
                       boolean notificationDisabled) {
        try {
            BotApiResponse apiResponse = lineMessagingClient
                    .replyMessage(new ReplyMessage(replyToken, messages, notificationDisabled))
                    .get();
            log.info("Sent messages: {}", apiResponse);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void replyText(@NonNull String replyToken, @NonNull String message) {
        if (replyToken.isEmpty()) {
            throw new IllegalArgumentException("replyToken must not be empty");
        }
        if (message.length() > 1000) {
            message = message.substring(0, 1000 - 2) + "……";
        }
        this.reply(replyToken, new TextMessage(message));
    }

    private void handleHeavyContent(String replyToken, String messageId,
                                    Consumer<MessageContentResponse> messageConsumer) {
        final MessageContentResponse response;
        try {
            response = lineBlobClient.getMessageContent(messageId)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            reply(replyToken, new TextMessage("Cannot get image: " + e.getMessage()));
            throw new RuntimeException(e);
        }
        messageConsumer.accept(response);
    }

    private void handleSticker(String replyToken, StickerMessageContent content) {
        reply(replyToken, new StickerMessage(
                content.getPackageId(), content.getStickerId())
        );
    }

    private void handleTextContent(String replyToken, Event event, TextMessageContent content)
            throws Exception {
        final String text = content.getText();

        log.info("Got text message from replyToken:{}: text:{}", replyToken, text);
        switch (text) {
            case "profile": {
                log.info("Invoking 'profile' command: source:{}",
                        event.getSource());
                final String userId = event.getSource().getUserId();
                if (userId != null) {
                    if (event.getSource() instanceof GroupSource) {
                        lineMessagingClient
                                .getGroupMemberProfile(((GroupSource) event.getSource()).getGroupId(), userId)
                                .whenComplete((profile, throwable) -> {
                                    if (throwable != null) {
                                        this.replyText(replyToken, throwable.getMessage());
                                        return;
                                    }

                                    this.reply(
                                            replyToken,
                                            Arrays.asList(new TextMessage("(from group)"),
                                                    new TextMessage(
                                                            "Display name: " + profile.getDisplayName()),
                                                    new ImageMessage(profile.getPictureUrl(),
                                                            profile.getPictureUrl()))
                                    );
                                });
                    } else {
                        lineMessagingClient
                                .getProfile(userId)
                                .whenComplete((profile, throwable) -> {
                                    if (throwable != null) {
                                        this.replyText(replyToken, throwable.getMessage());
                                        return;
                                    }

                                    this.reply(
                                            replyToken,
                                            Arrays.asList(new TextMessage(
                                                            "Display name: " + profile.getDisplayName()),
                                                    new TextMessage("Status message: "
                                                            + profile.getStatusMessage()))
                                    );

                                });
                    }
                } else {
                    this.replyText(replyToken, "Bot can't use profile API without user ID");
                }
                break;
            }
            case "bye": {
                Source source = event.getSource();
                if (source instanceof GroupSource) {
                    this.replyText(replyToken, "Leaving group");
                    lineMessagingClient.leaveGroup(((GroupSource) source).getGroupId()).get();
                } else if (source instanceof RoomSource) {
                    this.replyText(replyToken, "Leaving room");
                    lineMessagingClient.leaveRoom(((RoomSource) source).getRoomId()).get();
                } else {
                    this.replyText(replyToken, "Bot can't leave from 1:1 chat");
                }
                break;
            }
            case "confirm": {
                ConfirmTemplate confirmTemplate = new ConfirmTemplate(
                        "Do it?",
                        new MessageAction("Yes", "Yes!"),
                        new MessageAction("No", "No!")
                );
                TemplateMessage templateMessage = new TemplateMessage("Confirm alt text", confirmTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }
            case "buttons": {
                URI imageUrl = createUri("/static/buttons/1040.jpg");
                ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                        imageUrl,
                        "My button sample",
                        "Hello, my button",
                        Arrays.asList(
                                new URIAction("Go to line.me",
                                        URI.create("https://line.me"), null),
                                new PostbackAction("Say hello1",
                                        "hello こんにちは"),
                                new PostbackAction("言 hello2",
                                        "hello こんにちは",
                                        "hello こんにちは"),
                                new MessageAction("Say message",
                                        "Rice=米")
                        ));
                TemplateMessage templateMessage = new TemplateMessage("Button alt text", buttonsTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }
            case "carousel": {
                URI imageUrl = createUri("/static/buttons/1040.jpg");
                CarouselTemplate carouselTemplate = new CarouselTemplate(
                        Arrays.asList(
                                new CarouselColumn(imageUrl, "hoge", "fuga", Arrays.asList(
                                        new URIAction("Go to line.me",
                                                URI.create("https://line.me"), null),
                                        new URIAction("Go to line.me",
                                                URI.create("https://line.me"), null),
                                        new PostbackAction("Say hello1",
                                                "hello こんにちは")
                                )),
                                new CarouselColumn(imageUrl, "hoge", "fuga", Arrays.asList(
                                        new PostbackAction("言 hello2",
                                                "hello こんにちは",
                                                "hello こんにちは"),
                                        new PostbackAction("言 hello2",
                                                "hello こんにちは",
                                                "hello こんにちは"),
                                        new MessageAction("Say message",
                                                "Rice=米")
                                )),
                                new CarouselColumn(imageUrl, "Datetime Picker",
                                        "Please select a date, time or datetime", Arrays.asList(
                                        DatetimePickerAction.OfLocalDatetime
                                                .builder()
                                                .label("Datetime")
                                                .data("action=sel")
                                                .initial(LocalDateTime.parse("2017-06-18T06:15"))
                                                .min(LocalDateTime.parse("1900-01-01T00:00"))
                                                .max(LocalDateTime.parse("2100-12-31T23:59"))
                                                .build(),
                                        DatetimePickerAction.OfLocalDate
                                                .builder()
                                                .label("Date")
                                                .data("action=sel&only=date")
                                                .initial(LocalDate.parse("2017-06-18"))
                                                .min(LocalDate.parse("1900-01-01"))
                                                .max(LocalDate.parse("2100-12-31"))
                                                .build(),
                                        DatetimePickerAction.OfLocalTime
                                                .builder()
                                                .label("Time")
                                                .data("action=sel&only=time")
                                                .initial(LocalTime.parse("06:15"))
                                                .min(LocalTime.parse("00:00"))
                                                .max(LocalTime.parse("23:59"))
                                                .build()
                                ))
                        ));
                TemplateMessage templateMessage = new TemplateMessage("Carousel alt text", carouselTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }
            case "image_carousel": {
                URI imageUrl = createUri("/static/buttons/1040.jpg");
                ImageCarouselTemplate imageCarouselTemplate = new ImageCarouselTemplate(
                        Arrays.asList(
                                new ImageCarouselColumn(imageUrl,
                                        new URIAction("Goto line.me",
                                                URI.create("https://line.me"), null)
                                ),
                                new ImageCarouselColumn(imageUrl,
                                        new MessageAction("Say message",
                                                "Rice=米")
                                ),
                                new ImageCarouselColumn(imageUrl,
                                        new PostbackAction("言 hello2",
                                                "hello こんにちは",
                                                "hello こんにちは")
                                )
                        ));
                TemplateMessage templateMessage = new TemplateMessage("ImageCarousel alt text",
                        imageCarouselTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }
            case "imagemap":
                //            final String baseUrl,
                //            final String altText,
                //            final ImagemapBaseSize imagemapBaseSize,
                //            final List<ImagemapAction> actions) {
                this.reply(replyToken, ImagemapMessage
                        .builder()
                        .baseUrl(createUri("/static/rich"))
                        .altText("This is alt text")
                        .baseSize(new ImagemapBaseSize(1040, 1040))
                        .actions(Arrays.asList(
                                URIImagemapAction.builder()
                                        .linkUri("https://store.line.me/family/manga/en")
                                        .area(new ImagemapArea(0, 0, 520, 520))
                                        .build(),
                                URIImagemapAction.builder()
                                        .linkUri("https://store.line.me/family/music/en")
                                        .area(new ImagemapArea(520, 0, 520, 520))
                                        .build(),
                                URIImagemapAction.builder()
                                        .linkUri("https://store.line.me/family/play/en")
                                        .area(new ImagemapArea(0, 520, 520, 520))
                                        .build(),
                                MessageImagemapAction.builder()
                                        .text("URANAI!")
                                        .area(new ImagemapArea(520, 520, 520, 520))
                                        .build()
                        ))
                        .build());
                break;
            case "imagemap_video":
                this.reply(replyToken, ImagemapMessage
                        .builder()
                        .baseUrl(createUri("/static/imagemap_video"))
                        .altText("This is an imagemap with video")
                        .baseSize(new ImagemapBaseSize(722, 1040))
                        .video(
                                ImagemapVideo.builder()
                                        .originalContentUrl(
                                                createUri("/static/imagemap_video/originalContent.mp4"))
                                        .previewImageUrl(
                                                createUri("/static/imagemap_video/previewImage.jpg"))
                                        .area(new ImagemapArea(40, 46, 952, 536))
                                        .externalLink(
                                                new ImagemapExternalLink(
                                                        URI.create("https://example.com/see_more.html"),
                                                        "See More")
                                        )
                                        .build()
                        )
                        .actions(singletonList(
                                MessageImagemapAction.builder()
                                        .text("NIXIE CLOCK")
                                        .area(new ImagemapArea(260, 600, 450, 86))
                                        .build()
                        ))
                        .build());
                break;
            case "flex":
                this.reply(replyToken, new ExampleFlexMessageSupplier().get());
                break;
            case "quickreply":
                this.reply(replyToken, new MessageWithQuickReplySupplier().get());
                break;
            case "no_notify":
                this.reply(replyToken,
                        singletonList(new TextMessage("This message is send without a push notification")),
                        true);
                break;
            case "icon":
                this.reply(replyToken,
                        TextMessage.builder()
                                .text("Hello, I'm cat! Meow~")
                                .sender(Sender.builder()
                                        .name("Cat")
                                        .iconUrl(createUri("/static/icon/cat.png"))
                                        .build())
                                .build());
                break;
            default:
                log.info("Returns echo message {}: {}", replyToken, text);
                this.replyText(
                        replyToken,
                        text
                );
                break;
        }
    }

    private static URI createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .scheme("https")
                .path(path).build()
                .toUri();
    }

    private void system(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        try {
            Process start = processBuilder.start();
            int i = start.waitFor();
            log.info("result: {} =>  {}", Arrays.toString(args), i);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
        log.info("Got content-type: {}", responseBody);

        DownloadedContent tempFile = createTempFile(ext);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            ByteStreams.copy(responseBody.getStream(), outputStream);
            log.info("Saved {}: {}", ext, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DownloadedContent createTempFile(String ext) {
        String fileName = LocalDateTime.now().toString() + '-' + UUID.randomUUID() + '.' + ext;
        Path tempFile = LinebotApplication.downloadedContentDir.resolve(fileName);
        tempFile.toFile().deleteOnExit();
        return new DownloadedContent(
                tempFile,
                createUri("/downloaded/" + tempFile.getFileName()));
    }

    @Value
    private static class DownloadedContent {
        Path path;
        URI uri;
    }
}


//@RestController
//@RequestMapping("bot")
//public class BotController {
//    private final static String token = "JNxj5B/4iGryud4KWzx5Hc2Im286qgJ1kaFtP2JhI/KA0o5w/DtyzSKuJzPgIEnQDRlw+txN/ydoFH0unAkbQJDSbd+MxpjiqWFLi8tcTMxNwmRCExns27ylqBVjo85QqfkYEyIvSSW5n0KigNt9tQdB04t89/1O/w1cDnyilFU=";
//
//
//    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
//    public String processEvents(@RequestHeader("X-Line-Signature") String signature, @RequestBody Object body) throws JsonProcessingException {
//        ObjectMapper mapper = new ObjectMapper();
//        String json = mapper.writeValueAsString(body);
//        System.out.println("request body = " + json);
//        Webhook webhook = mapper.readValue(json, Webhook.class);
//        System.out.println(webhook.toString());
////        for (Event event : webhook.getEvents()) {
////            if (event.getType().equals("message")) {
////                RestTemplate rt = new RestTemplate();
////
////                HttpHeaders requestHeaders = new HttpHeaders();
////                requestHeaders.setBearerAuth(token);
////                HttpEntity<String> requestEntity = new HttpEntity<String>(null, requestHeaders);
////                checkRequestBody(requestEntity);
////                ResponseEntity<String> res = rt.exchange("https://api.line.me/v2/bot/profile/{userId}", HttpMethod.GET, requestEntity, String.class, ((Source) event.getSource()).getUserId());
////                String str = res.getBody();
////                JsonNode node = mapper.readTree(str);
////                String displayName = node.get("displayName") != null ? node.get("displayName").asText() : "";
////                String userId = node.get("userId") != null ? node.get("userId").asText() : "";
////                String pictureUrl = node.get("pictureUrl") != null ? node.get("pictureUrl").asText() : "";
////                String statusMessage = node.get("statusMessage") != null ? node.get("statusMessage").asText() : "";
////
////
////                List<LBReplyMessage.Message> messages = new ArrayList<>();
////                messages.add(new LBReplyMessage.Message("text",
////                        String.format("您的資訊如下:\n%s\n%s\n%s\n%s\n",
////                                displayName, userId, pictureUrl, statusMessage)));
////
////                LBReplyMessage lbReplyMessage = new LBReplyMessage();
////                lbReplyMessage.setReplyToken(event.getReplyToken());
////                lbReplyMessage.setMessages(messages);
////
////                HttpHeaders headers = new HttpHeaders();
////                headers.setContentType(MediaType.APPLICATION_JSON);
////                headers.setBearerAuth(token);
////
////                HttpEntity<LBReplyMessage> httpEntity = new HttpEntity(lbReplyMessage, headers);
////
////
////                checkRequestBody(httpEntity);
////
////
////                ResponseEntity<String> response = rt.postForEntity(
////                        "https://api.line.me/v2/bot/message/reply",
////                        httpEntity,
////                        String.class
////                );
////            }
////        }
//
//
//        return "hahahaha";
//    }
//
//
////    @PostMapping
////    @JsonDeserialize(using = WebhookDeserializer.class)
////    public String processEvents(@RequestHeader("X-Line-Signature") String signature, @RequestBody Object body) throws JsonProcessingException {
//////        System.out.println("signature = " + signature);
//////        System.out.println("request body = " + body);
////        ObjectMapper mapper = new ObjectMapper();
////        String json = mapper.writeValueAsString(body);
////        System.out.println("request body = " + json);
////        Webhook webhook = mapper.readValue(json, Webhook.class);
////
////        for (Webhook.Event event : webhook.getEvents()) {
////            if (event.getType().equals("message")) {
////                RestTemplate rt = new RestTemplate();
////
////                HttpHeaders requestHeaders = new HttpHeaders();
////                requestHeaders.setBearerAuth(token);
////                HttpEntity<String> requestEntity = new HttpEntity<String>(null, requestHeaders);
////                checkRequestBody(requestEntity);
////                ResponseEntity<String> res = rt.exchange("https://api.line.me/v2/bot/profile/{userId}", HttpMethod.GET, requestEntity, String.class, event.getSource().getUserId());
////                String str = res.getBody();
////                JsonNode node = mapper.readTree(str);
////                String displayName =  node.get("displayName") != null?node.get("displayName").asText():"";
////                String userId =  node.get("userId") != null?node.get("userId").asText():"";
////                String pictureUrl =  node.get("pictureUrl") != null?node.get("pictureUrl").asText():"";
////                String statusMessage = node.get("statusMessage") != null?node.get("statusMessage").asText():"";
////
////
////                List<LBReplyMessage.Message> messages = new ArrayList<>();
////                messages.add(new LBReplyMessage.Message("text",
////                        String.format("您的資訊如下:\n%s\n%s\n%s\n%s\n",
////                                displayName, userId, pictureUrl, statusMessage)));
////
////                LBReplyMessage lbReplyMessage = new LBReplyMessage();
////                lbReplyMessage.setReplyToken(event.getReplyToken());
////                lbReplyMessage.setMessages(messages);
////
////                HttpHeaders headers = new HttpHeaders();
////                headers.setContentType(MediaType.APPLICATION_JSON);
////                headers.setBearerAuth(token);
////
////                HttpEntity<LBReplyMessage> httpEntity = new HttpEntity(lbReplyMessage, headers);
////
////
////                checkRequestBody(httpEntity);
////
////
////                ResponseEntity<String> response = rt.postForEntity(
////                        "https://api.line.me/v2/bot/message/reply",
////                        httpEntity,
////                        String.class
////                );
////            }
////        }
////        return "hahahaha";
////    }
//
//    private void checkRequestBody(HttpEntity httpEntity) throws JsonProcessingException {
//        for (Map.Entry<String, List<String>> stringListEntry : httpEntity.getHeaders().entrySet()) {
//            System.out.println(stringListEntry.getKey());
//            for (String s : stringListEntry.getValue()) {
//                System.out.println("-" + s);
//            }
//        }
//
//        ObjectMapper mapper = new ObjectMapper();
//        String json = mapper.writeValueAsString(httpEntity.getBody());
//        System.out.println(json);
//
//
//    }
//
//}

