package com.petbuddy.feedDistributionService.util;

import lombok.extern.slf4j.Slf4j;
import ch.hsr.geohash.GeoHash;

@Slf4j
public class GeoUtil {
    public static String encode(double latitude, double longitude) {
        return GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, 7);
    }
    public static String getGeoPrefix(double latitude, double longitude, int precision) {
        return GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, precision);
    }
    public static boolean isClose(String geohash1, String geohash2) {
        return geohash1.substring(0, 5).equals(geohash2.substring(0, 5));
    }
}
