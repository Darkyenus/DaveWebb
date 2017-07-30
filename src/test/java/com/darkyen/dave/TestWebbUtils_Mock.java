package com.darkyen.dave;

import junit.framework.TestCase;

import java.net.HttpURLConnection;
import java.util.*;

import static org.mockito.Mockito.*;

public class TestWebbUtils_Mock extends TestCase {

    public void testAddRequestProperties() throws Exception {
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        String dateStr;
        synchronized (WebbUtils.RFC1123_DATE_FORMAT) {
            dateStr = WebbUtils.RFC1123_DATE_FORMAT.format(now);
        }

        HttpURLConnection connection = mock(HttpURLConnection.class);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("date", now);
        map.put("calendar", calendar);
        map.put("int", 4711);
        map.put("bool", true);

        WebbUtils.addRequestProperties(connection, map);

        verify(connection).addRequestProperty("date", dateStr);
        verify(connection).addRequestProperty("calendar", dateStr);
        verify(connection).addRequestProperty("int", "4711");
        verify(connection).addRequestProperty("bool", "true");
    }

    public void testAddRequestProperties_Empty() throws Exception {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        doThrow(new RuntimeException()).when(connection).addRequestProperty(anyString(), anyString());
        WebbUtils.addRequestProperties(connection, null);
    }

    public void testAddRequestProperty() throws Exception {
        HttpURLConnection connection = mock(HttpURLConnection.class);

        WebbUtils.addRequestProperty(connection, "name1", "value1");
        WebbUtils.addRequestProperty(connection, "name2", "value2");
        verify(connection).addRequestProperty("name1", "value1");
        verify(connection).addRequestProperty("name2", "value2");
    }

    public void testEnsureRequestProperty() throws Exception {
        Map<String,List<String>> headers = new HashMap<String, List<String>>();
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getRequestProperties()).thenReturn(headers);

        WebbUtils.ensureRequestProperty(connection, "name", "value");

        verify(connection).addRequestProperty("name", "value");
    }
}
