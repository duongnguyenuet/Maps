package binary.maps.utils;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.asin;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;


/**
 * Created by truongdq on 11/13/2017.
 */

public class CalculateDistance {
    //Góc 15 độ
    public static final int ANGLE = 15;
    // Vạn tốc trung bình
    public static final int AVERAGE_SPEED = 100;
    public static final double EARTH_RADIUS = 6371009;
    public static final int DEFAULT_SPEED = 50;

    //Truyền vào Mảng LatLgn và thời gian để tính
    public static List<LatLng> caculaterReviewTrip2(List<LatLng> latLngs, int time) {
        List<LatLng> latLng2 = new ArrayList<>();
        int latLngSize = latLngs.size();
        for (int i = 0; i < latLngSize; i++) {
            LatLng temp = latLngs.get(i);
            if (i > 0 && i < latLngSize - 1) {
                // Kiểm tra góc của 3 điểm nếu mà lớn hơn 15 độ thì bị nhảy GPS
                LatLng preLatLng = latLng2.get(latLng2.size() - 1);
                LatLng nextLatLng = latLngs.get(i + 1);
                double distance1 = computeDistanceBetween(temp, preLatLng);
                double distance2 = computeDistanceBetween(temp, nextLatLng);
                double distance3 = computeDistanceBetween(preLatLng, nextLatLng);
                //Hàm tính Góc bn độ computeAngle
                if (computeAngle(distance1, distance2, distance3) > ANGLE) {
                    latLng2.add(temp);
                }
            } else {
                latLng2.add(temp);
            }
        }
        // Tính tổng distance của mảng latlgn sau khi lọc điểm nhảy GPS
        double result = calculatedDistance(latLng2);
        double convertTime = convertTimeToDouble(time);
        //Nếu vận tốc chia cho thời gian > vận tóc Trung bình sẽ tính lại Distance còn k return ra
        if (result / convertTime < AVERAGE_SPEED) {
            return latLng2;
        } else {
            return calculateAgainReviewTrip(latLng2, time);
        }
    }

    public static List<LatLng> calculateAgainReviewTrip(List<LatLng> latLngs, int time) {
        List<LatLng> latLngs2 = new ArrayList<LatLng>();
        int indexSize = 0;
        int latLngSize = latLngs.size();
        // Lọc để lấy 20 điểm trong mảng
        if (latLngSize > 40) {
            indexSize = (int) ceil(latLngSize / 20);
            for (int i = 0; i < 20; i++) {
                if ((indexSize * i) >= (latLngSize - 1)) {
                    break;
                } else {
                    latLngs2.add(new LatLng(latLngs.get(indexSize * i).latitude, latLngs.get(indexSize * i).longitude));
                }
            }
            latLngs2.add(new LatLng(latLngs.get(latLngSize - 1).latitude, latLngs.get(latLngSize - 1).longitude));
        } else {
            if (latLngSize <= 2) {
                latLngs2.addAll(latLngs);
            } else {
                indexSize = 2;
                for (int i = 0; i < 20; i++) {
                    if ((indexSize * i) >= (latLngSize - 1)) {
                        break;
                    } else {
                        latLngs2.add(new LatLng(latLngs.get(indexSize * i).latitude, latLngs.get(indexSize * i).longitude));
                    }
                }
                latLngs2.add(new LatLng(latLngs.get(latLngSize - 1).latitude, latLngs.get(latLngSize - 1).longitude));
            }
        }
        double result = calculatedDistance(latLngs2);
        double convertTime = convertTimeToDouble(time);
        //Nếu vận tốc chia cho thời gian > vận tóc Trung bình sẽ tính lại Distance theo thời gian * vận tốc mặc định
        if (result / convertTime < AVERAGE_SPEED) {
            return latLngs2;
        } else {
            return calculateDefaultSpeed(latLngs, convertTime);
        }
    }

    public static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    public static double calculateDistance(LatLng latLng1, LatLng latLng2) {
        try {
            if (latLng1.latitude == latLng2.latitude
                    && latLng1.longitude == latLng2.longitude) {
                return 0;
            } else {
                double dist = Math
                        .acos(sin(deg2rad(latLng1.latitude))
                                * sin(deg2rad(latLng2.latitude))
                                + cos(deg2rad(latLng1.latitude))
                                * cos(deg2rad(latLng2.latitude))
                                * cos(deg2rad(latLng1.longitude
                                - latLng2.longitude))) * 6371;
                return dist;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public static double calculatedDistance(List<LatLng> latLng2) {
        double result = 0;
        if (latLng2.size() > 2) {
            for (int i = 0; i < latLng2.size() - 1; i++) {
                LatLng latLng = latLng2.get(i);
                LatLng latLng1 = latLng2.get(i + 1);
                result += calculateDistance(latLng, latLng1);
            }
        } else if (latLng2.size() == 2) {
            result = calculateDistance(latLng2.get(0), latLng2.get(1));
        }
        return formatNumberDouble(result);
    }

    public static double formatNumberDouble(double number) {
        if (number > 0) {
            double finalValue = 0;
            finalValue = Math.round(number * 10.0) / 10.0;
            return finalValue;
        } else {
            return 0;
        }
    }

    public static double convertTimeToDouble(int time) {
        return (double) time / 60;
    }

    public static List<LatLng> calculateDefaultSpeed(List<LatLng> latLngs, double time) {
        double distance = time * DEFAULT_SPEED;
        return latLngs;
    }

    public static double computeDistanceBetween(LatLng from, LatLng to) {
        return computeAngleBetween(from, to) * EARTH_RADIUS;
    }

    public static double computeAngleBetween(LatLng from, LatLng to) {
        return distanceRadians(toRadians(from.latitude), toRadians(from.longitude),
                toRadians(to.latitude), toRadians(to.longitude));
    }

    static double havDistance(double lat1, double lat2, double dLng) {
        return hav(lat1 - lat2) + hav(dLng) * cos(lat1) * cos(lat2);
    }

    static double hav(double x) {
        double sinHalf = sin(x * 0.5);
        return sinHalf * sinHalf;
    }

    private static double distanceRadians(double lat1, double lng1, double lat2, double lng2) {
        return arcHav(havDistance(lat1, lat2, lng1 - lng2));
    }

    static double arcHav(double x) {
        return 2 * asin(sqrt(x));
    }

    public static double computeAngle(double a, double b, double c) {
        double angle = Math.acos((a * a + b * b - c * c) / (2 * a * b));
        return Math.toDegrees(angle);
    }
}
