package edu.jhuapl.trinity.utils;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class HttpsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpsUtils.class);

    public static Image getImage(String urlPath) {
        try {
            // Create a URL object for the image
            //Deprecated as of JDK 20
            //URL url = new URL(urlPath);
            URI uri = new URI(urlPath);
            URL url = uri.toURL();

            // Open a connection to the URL
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Get the input stream from the connection
            InputStream in = conn.getInputStream();

            // Create an image object from the input stream
            return new Image(in);

        } catch (Exception ex) {
            LOG.error("Exception", ex);
        }
        return new Image("");
    }

    public static Image getImageNoSSL(String urlPath) {
        try {
            //Deprecated as of JDK 20
            //URL url = new URL(urlPath);
            URI uri = new URI(urlPath);
            URL url = uri.toURL();
            // Open a connection to the URL
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            // Create an SSL context that uses the trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());


            // Set the SSL socket factory on the HTTPS connection
            conn.setSSLSocketFactory(sslContext.getSocketFactory());

            conn.setRequestMethod("GET");

            // Get the input stream from the connection
            InputStream in = conn.getInputStream();

            // Create an image object from the input stream
            Image image = new Image(in);
            return image;
        } catch (Exception ex) {
            LOG.error("Exception", ex);
        }
        return new Image("");
    }
}
