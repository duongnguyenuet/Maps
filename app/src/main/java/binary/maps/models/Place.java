package binary.maps.models;

/**
 * Created by duong on 11/7/2017.
 */

public class Place {
    private int id;
    private double lat;
    private double lng;

    public Place(int id, double lat, double lng) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
    }

    public Place(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public Place() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
