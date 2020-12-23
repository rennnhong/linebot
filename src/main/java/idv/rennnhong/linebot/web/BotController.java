package idv.rennnhong.linebot.web;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.linecorp.bot.model.action.DatetimePickerAction;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.*;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import idv.rennnhong.linebot.map.model.NearByParam;
import idv.rennnhong.linebot.map.model.Place;
import idv.rennnhong.linebot.message.MessageHandler;
import idv.rennnhong.linebot.service.PlaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@LineMessageHandler
public class BotController {
    @Autowired
    private MessageHandler messageHandler;

    @Autowired
    private PlaceService placeService;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        TextMessageContent message = event.getMessage();
        handleTextContent(event.getReplyToken(), event, message);
    }

    @EventMapping
    public void handleStickerMessageEvent(MessageEvent<StickerMessageContent> event) {
        log.info("此方法尚未實作");
    }

    private String chopStr(String str) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(str));
        return str.length() > 40 ? str.substring(0,40) + "..." : str;
    }

    @EventMapping
    public void handleLocationMessageEvent(MessageEvent<LocationMessageContent> event) {
        String replyToken = event.getReplyToken();
//        String userId = event.getSource().getUserId();

        double latitude = event.getMessage().getLatitude();
        double longitude = event.getMessage().getLongitude();
        List<Place> nearByPlaces = placeService.getNearByPlaces(new NearByParam(latitude, longitude));


        if (nearByPlaces.size() == 0) {
            messageHandler.replyText(replyToken, "查無結果");
        } else {
//            replyMessages.add(new TextMessage(MessageFormat.format("找到以下{0}筆結果", nearByPlaces.size())));
//            List<TextMessage> tmpPlaceMessages = nearByPlaces.stream()
//                    .map(place -> new TextMessage(
//                            MessageFormat.format("{0}({1})\n{2}",
//                                    place.getName(),
//                                    place.getAddress(),
//                                    place.getUrl())
//                    )).collect(Collectors.toList());
//
//            for (TextMessage tmpPlaceMessage : tmpPlaceMessages) {
//                replyMessages.add(tmpPlaceMessage);
//            }

            List<CarouselColumn> carouselColumns = nearByPlaces.stream().map(
                    place -> new CarouselColumn(
                            U,
                            place.getName(),
                            chopStr(place.getAddress()),
                            Lists.newArrayList(new URIAction("來去看看",
                                    URI.create(place.getUrl()),
                                    null)))
            ).collect(Collectors.toList());

            CarouselTemplate carouselTemplate = new CarouselTemplate(carouselColumns);
            messageHandler.replyCarousel(replyToken, "Carousel alt text", carouselTemplate);
        }


//        messageHandler.replyCarousel(replyToken, "Carousel alt text", carouselTemplate);


//        messageHandler.push(userId, replyMessages);
    }

    @EventMapping
    public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event) throws IOException {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleAudioMessageEvent(MessageEvent<AudioMessageContent> event) throws IOException {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleVideoMessageEvent(MessageEvent<VideoMessageContent> event) throws IOException {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleVideoPlayCompleteEvent(VideoPlayCompleteEvent event) throws IOException {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleFileMessageEvent(MessageEvent<FileMessageContent> event) {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleUnfollowEvent(UnfollowEvent event) {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleUnknownEvent(UnknownEvent event) {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleFollowEvent(FollowEvent event) {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleJoinEvent(JoinEvent event) {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) {
        String replyToken = event.getReplyToken();
        String data = event.getPostbackContent().getData();
        Map<String, String> params = event.getPostbackContent().getParams();
        data = Strings.nullToEmpty(data);
        params = Objects.isNull(params) ? ImmutableMap.of() : params;
        messageHandler.replyText(replyToken,
                "Got postback data " + data + ", param " + params.toString());
    }

    @EventMapping
    public void handleBeaconEvent(BeaconEvent event) {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleMemberJoined(MemberJoinedEvent event) {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleMemberLeft(MemberLeftEvent event) {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleMemberLeft(UnsendEvent event) {
        log.info("此方法尚未實作");
        new UnsupportedOperationException();
    }

    @EventMapping
    public void handleOtherEvent(Event event) {
        log.info("Received message(Ignored): {}", event);
        new UnsupportedOperationException();
    }


//    private void handleHeavyContent(String replyToken, String messageId,
//                                    Consumer<MessageContentResponse> messageConsumer) {
//        final MessageContentResponse response;
//        try {
//            response = lineBlobClient.getMessageContent(messageId)
//                    .get();
//        } catch (InterruptedException | ExecutionException e) {
//            reply(replyToken, new TextMessage("Cannot get image: " + e.getMessage()));
//            throw new RuntimeException(e);
//        }
//        messageConsumer.accept(response);
//    }

//    private void handleSticker(String replyToken, StickerMessageContent content) {
//        reply(replyToken, new StickerMessage(
//                content.getPackageId(), content.getStickerId())
//        );
//    }

    private void handleTextContent(String replyToken, Event event, TextMessageContent content)
            throws Exception {
        final String text = content.getText();

        log.info("Got text message from replyToken:{}: text:{} emojis:{}", replyToken, text,
                content.getEmojis());
        switch (text) {
            case "profile":
            case "bye":
            case "group_summary":
            case "group_member_count":
            case "room_member_count":
            case "confirm":
            case "buttons":
            case "carousel": {
//                URI imageUrl = createUri("/static/buttons/1040.jpg");
                URI imageUrl = URI.create("https://b73d7ffb51f8.ngrok.io//rennnhong/static/buttons/1040.jpg");
                CarouselTemplate carouselTemplate = new CarouselTemplate(
                        Arrays.asList(
                                new CarouselColumn(imageUrl, "hoge", "fuga", Arrays.asList(
                                        new URIAction("Go to line.me",
                                                URI.create("https://line.me"), null),
                                        new URIAction("Go to line.me",
                                                URI.create("https://line.me"), null),
                                        new PostbackAction("Say hello1",
                                                "a=1&b=2")
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
                messageHandler.replyCarousel(replyToken, "Carousel alt text", carouselTemplate);
                break;
//                CarouselTemplate carouselTemplate = new CarouselTemplate(
//                        Arrays.asList(
//                                new CarouselColumn(null, "test", "測試測試", Arrays.asList(
//                                        new PostbackAction("HELLO",
//                                                "hello world_1",
//                                                "hello world_1"),
//                                        new PostbackAction("HELLO_1",
//                                                "hello world_1",
//                                                "hello world_1")
//                                )),
//                                new CarouselColumn(null, "test2", "測試測試", Arrays.asList(
//                                        new PostbackAction("HELLO2",
//                                                "hello world2",
//                                                "hello world2")
//                                ))
//                        ));
//
//                messageHandler.replyCarousel(replyToken, "Carousel Test", carouselTemplate);
//                break;
            }
            case "image_carousel":
            case "imagemap":
            case "imagemap_video":
            case "flex":
            case "quickreply":
            case "no_notify":
            case "icon":
            default:
                log.info("Returns echo message {}: {}", replyToken, text);
                messageHandler.replyText(
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

//    private static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
//        log.info("Got content-type: {}", responseBody);
//
//        DownloadedContent tempFile = createTempFile(ext);
//        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
//            ByteStreams.copy(responseBody.getStream(), outputStream);
//            log.info("Saved {}: {}", ext, tempFile);
//            return tempFile;
//        } catch (IOException e) {
//            throw new UncheckedIOException(e);
//        }
//    }

//    private static DownloadedContent createTempFile(String ext) {
//        String fileName = LocalDateTime.now().toString() + '-' + UUID.randomUUID() + '.' + ext;
//        Path tempFile = LinebotApplication.downloadedContentDir.resolve(fileName);
//        tempFile.toFile().deleteOnExit();
//        return new DownloadedContent(
//                tempFile,
//                createUri("/downloaded/" + tempFile.getFileName()));
//    }

//    @Value
//    private static class DownloadedContent {
//        Path path;
//        URI uri;
//    }
}
