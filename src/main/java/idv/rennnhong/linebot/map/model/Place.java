package idv.rennnhong.linebot.map.model;

public class Place {
    private String name;
    private String address;
    private String url;
    private String reference;

    public Place(String name, String address, String url, String reference) {
        this.name = name;
        this.address = address;
        this.url = url;
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
