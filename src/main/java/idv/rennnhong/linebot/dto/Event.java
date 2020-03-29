package idv.rennnhong.linebot.dto;

public class Event<T,K>{
    private String replyToken;
    private String type;
    private String mode;
    private String timestamp;
    private K source;
    private T message;



    public String getReplyToken() {
        return replyToken;
    }

    public void setReplyToken(String replyToken) {
        this.replyToken = replyToken;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public K getSource() {
        return source;
    }

    public void setSource(K source) {
        this.source = source;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Event{" +
                "replyToken='" + replyToken + '\'' +
                ", type='" + type + '\'' +
                ", mode='" + mode + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", source=" + source +
                ", message=" + message +
                '}';
    }
}
