# DaveWebb
[![](https://jitpack.io/v/Darkyenus/DaveWebb.svg)](https://jitpack.io/#Darkyenus/DaveWebb)


Lightweight Java HTTP-Client for calling REST-Services

## Problem ##

If you have to call a RESTful Webservice from Java, especially if you are on Android, you have some options:

 * Use `DefaultHttpClient` or `AndroidHttpClient`. It is already deployed on Android and it's easy to use.
   But wait a moment -
   [Google doesn't recommend using it](http://android-developers.blogspot.de/2011/09/androids-http-clients.html),
   only on very old Android versions.
 * Use `HttpURLConnection`. This is what Google recommends for newer Android versions (>= Gingerbread).
   It is part of JDK, but it's cumbersome to use.
 * Add `Unirest`, `Restlet` or some other "all-you-can-eat", universal, multi-part, File-upload and all-cases
   supporting library which adds some hundred KB of jars to your APK.

## Solved ##

**DaveWebb** is a paper-thin wrapper around
[HttpURLConnection](http://docs.oracle.com/javase/7/docs/api/java/net/HttpURLConnection.html).
It supports most HTTP communication cases when you talk to REST services. It is very
lightweight (~18 KB jar) and super-easy to use.

## Features ##

  * Supports GET, POST, PUT, DELETE
  * add HTTP headers (per request, per client or globally)
  * convert params to `x-www-form-urlencoded` body **or** URI search params
  * fluent API
  * wraps all Exceptions in a WebbException (a RuntimeException)
  * automatically sets many boiler-plate HTTP headers (like 'Accept', 'Content-Type', 'Content-Length')
  * GZip-compression for uploads (POST/PUT)
  * Un-compress gzip/deflate downloads
  * supports HTTPS and enables relaxing SSL-handshake (self-signed certificates, hostname verification)
  * option to retry the request in case of special errors (503, 504, 'connection reset by peer')
  * multi-valued parameters
  * use streams as input and output
  
Philosophy: *Do essentials right and nothing else*

# Usage Examples

```java
// create the client settings bundle (one-time, can be used from different threads)
Webb webb = new Webb(SyncPreferences.REST_ENDPOINT);
webb.setDefaultHeader(Webb.HDR_USER_AGENT, Const.UA);

// later we authenticate
Response<JSONObject> response = webb
        .post("/session") // We POST to relative URI /session
        .param("authentication", createAuthentication(syncPreferences)) // With urlencoded params authentication...
        .param("deviceId", syncPreferences.getDeviceId()) // ... and deviceId
        .ensureSuccess() // We'd like to get exception instead of non 2XX response
        .execute(JSON_TRANSLATOR); // And finally, do the REST call. Translate what is returned to Json.
// Note: JSON_TRANSLATOR is not included by default, because we don't have any json dependency.
// Our tests however do, so you can see how to write such translator yourself.

JSONObject apiResult = response.getBody();

AccessToken accessToken = new AccessToken();
accessToken.token = apiResult.getString("token");
accessToken.validUntil = apiResult.getLong("validUntil");

webb.setDefaultHeader(HDR_ACCESS_TOKEN, accessToken.token);

JSONObject sync = webb.post("/startSync")
        .param("lastSync", syncPreferences.getLastSync())
        .ensureSuccess()
        .execute(JSON_TRANSLATOR)
        .getBody();

// ... etc. etc.

// releaseAccessToken
webb.delete("/session").execute();
accessToken = null;
```

**Using Google Directions API:**

```java
Webb webb = new Webb();
JSONObject result = webb
        .get("http://maps.googleapis.com/maps/api/directions/json")
        .param("origin", new GeoPoint(47.8227, 12.096933))
        .param("destination", new GeoPoint(47.8633, 12.215533))
        .param("mode", "walking")
        .param("sensor", "true")
        .ensureSuccess()
        .execute(JSON_TRANSLATOR)
        .getBody();

JSONArray routes = result.getJSONArray("routes");
```

**Deal with "connection reset by peer" or other recoverable errors**

Android (at least >= GINGERBREAD) automatically sets the "Connection" header to "keep-alive".
This sometimes causes errors, because the server might already have closed the connection without
the mobile device knowing it. It would be cumbersome to cope with this and other situation where
a retry solved all problems. Since version 1.2.0 it is more comfortable for you:

```java
Webb webb = Webb.create();
JSONObject result = webb
        .get("https://example.com/api/request")
        .retry(1, false) // at most one retry, don't do exponential backoff
        .executeString()
        .getBody();
```

In many cases you will need to change the behaviour of how and when to retry a request.
For this, you can register your own `RetryManager`, see `webb.setRetryManager()`.

**More Samples**

If you want to see more examples, just have a look at the JUnit TestCase (src/test/java/...).

## Special Case Android < Froyo

You should add this if you build for legacy Android devices:

```java
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
    System.setProperty("http.keepAlive", "false");
}
```

## In Case Of Problems

I've encountered some problems which could be solved by disabling **keep-alive** of HTTP connections.
The solution above did not always work on recent versions of Android. To be sure to do without
keep-alive, you can set an HTTP header like this:

```
// disable 'keep-alive' for all requests created by this instance of Webb
webb.setDefaultHeader("Connection", "close");

// disable only for this request
Request<String> request = webb.post("/some-resource").header("Connection", "close");
```

# Background

This library started as a fork of well written [DavidWebb](https://github.com/hgoebl/DavidWebb) - check it out!
Some features were then removed (JSON) and some added (Async).
Name has been changed slightly to prevent confusion with original and incompatible library.

# License

MIT License, see LICENSE file

# Testing

The Unit-Tests do not mock any network-libraries, but depend on a small Express-application running.

Run following commands before you start the JUnit-tests or skip the tests in Maven build with command line
option `-DskipTests`

```
cd src/test/api-test-server
npm install
node .
```

## Android Tests

Before running the Android tests, build with maven, deploy the Android app and be sure to
set the timezone of your emulator to the same as your PC and synchronize date/time,
otherwise some tests will fail:

There is a script (`src/test/android/run-tests-emulator.sh`) which also builds the test-app with gradle.
