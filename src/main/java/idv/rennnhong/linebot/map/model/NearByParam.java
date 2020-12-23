package idv.rennnhong.linebot.map.model;

public class NearByParam {
    private final double latitude;
    private final double longitude;
    private final int radius;

    public NearByParam(double latitude, double longitude, int radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public NearByParam(double latitude, double longitude) {
        this(latitude, longitude, 500);
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public int getRadius() {
        return radius;
    }
}
