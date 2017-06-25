package com.goebl.david;

import com.esotericsoftware.jsonbeans.JsonValue;
import junit.framework.TestCase;

/**
 * Real world testing a Google API.
 *
 * @author goebl
 * @since 15.12.13
 */
public class TestWithGoogleApi extends TestCase {

    // this is a replacement for com.google.android.maps.GeoPoint
    private static class GeoPoint {
        final int latitudeE6;
        final int longitudeE6;

        private GeoPoint(int latitudeE6, int longitudeE6) {
            this.latitudeE6 = latitudeE6;
            this.longitudeE6 = longitudeE6;
        }

        private GeoPoint(double latitude, double longitude) {
            this.latitudeE6 = (int) (latitude * 1e6);
            this.longitudeE6 = (int) (longitude * 1e6);
        }

        public int getLatitudeE6() {
            return latitudeE6;
        }

        public int getLongitudeE6() {
            return longitudeE6;
        }

        public String toString() {
            return (latitudeE6 / 1e6) + "," + (longitudeE6 / 1e6);
        }


    }

    public void testGetDistance() throws Exception {

        GeoPoint src = new GeoPoint(47.8227, 12.096933);
        GeoPoint dest = new GeoPoint(47.8633, 12.215533);

        Webb webb = new Webb(null);
        JsonValue result = webb
                .get("http://maps.googleapis.com/maps/api/directions/json")
                .param("origin", src)
                .param("destination", dest)
                .param("mode", "walking")
                .param("sensor", "true")
                .ensureSuccess()
                .execute(AbstractTestWebb.JSON_TRANSLATOR)
                .getBody();

        assertNotNull(result);
        // System.out.println(result.toString(2));

        JsonValue array = result.get("routes");
        JsonValue routes = array.get(0);
        JsonValue legs = routes.get("legs");
        JsonValue steps = legs.get(0);
        JsonValue distance = steps.get("distance");

        int iDistance = distance.getInt("value");
        // System.out.println(distance.toString());

        assertEquals("distance ~11.6km", iDistance / 1000f, 11.6f, 1f);
    }

}