/*******************************************************************************
 * Copyright (c) 2015-2016, WSO2.Telco Inc. (http://www.wso2telco.com) 
 * 
 * All Rights Reserved. WSO2.Telco Inc. licences this file to youunder the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.identity.application.authentication.endpoint.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Map;

/**
 * The class MutualSSLClient.
 */
public class MutualSSLClient {

    private static final Log log = LogFactory.getLog(MutualSSLClient.class);
    private static KeyStore keyStore;
    private static KeyStore trustStore;
    private static String keyStorePassword;
    private static String KEY_STORE_TYPE = "JKS";
    private static String TRUST_STORE_TYPE = "JKS";
    private static String KEY_MANAGER_TYPE = "SunX509";
    private static String TRUST_MANAGER_TYPE = "SunX509";
    private static String PROTOCOL = "SSLv3";
    private static HttpsURLConnection httpsURLConnection;
    private static SSLSocketFactory sslSocketFactory;

    /**
     * load key store with given keystore.jks
     *
     * @param keyStorePath
     * @param keyStorePassoword
     * @throws java.security.KeyStoreException
     * @throws java.io.IOException
     * @throws java.security.cert.CertificateException
     * @throws java.security.NoSuchAlgorithmException
     */
    public static void loadKeyStore(String keyStorePath, String keyStorePassoword)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        keyStorePassword = keyStorePassoword;
        keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        keyStore.load(new FileInputStream(keyStorePath),
                keyStorePassoword.toCharArray());
    }

    /**
     * load trust store with given .jks file
     *
     * @param trustStorePath
     * @param trustStorePassoword
     * @throws java.security.KeyStoreException
     * @throws java.io.IOException
     * @throws java.security.cert.CertificateException
     * @throws java.security.NoSuchAlgorithmException
     */
    public static void loadTrustStore(String trustStorePath, String trustStorePassoword)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        trustStore = KeyStore.getInstance(TRUST_STORE_TYPE);
        trustStore.load(new FileInputStream(trustStorePath),
                trustStorePassoword.toCharArray());
    }

    /**
     * create basic SSL connection factory
     *
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyStoreException
     * @throws java.security.KeyManagementException
     * @throws java.io.IOException
     * @throws java.security.UnrecoverableKeyException
     */
    public static void initMutualSSLConnection()
            throws NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException, IOException, UnrecoverableKeyException {

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KEY_MANAGER_TYPE);
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TRUST_MANAGER_TYPE);
        trustManagerFactory.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        sslSocketFactory = sslContext.getSocketFactory();
    }

    /**
     * send mutual ssl https post request and return data
     *
     * @param backendURL
     * @param message
     * @param requestProps
     * @return
     * @throws java.io.IOException
     */
    public static String sendPostRequest(String backendURL, String message, Map<String, String> requestProps)
            throws IOException {
        URL url = new URL(backendURL);
        httpsURLConnection = (HttpsURLConnection) url.openConnection();
        httpsURLConnection.setSSLSocketFactory(sslSocketFactory);
        httpsURLConnection.setDoOutput(true);
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setRequestMethod("POST");
        if (requestProps != null && requestProps.size() > 0) {
            for (Map.Entry<String, String> entry : requestProps.entrySet()) {
                httpsURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        OutputStream outputStream = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        StringBuilder response = null;
        try {
            outputStream = httpsURLConnection.getOutputStream();
            outputStream.write(message.getBytes());
            inputStream = httpsURLConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            log.error("Calling url : " + url + "failed. ", e);
        } finally {
            reader.close();
            inputStream.close();
            outputStream.close();
        }
        return response.toString();
    }

    /**
     * send mutual ssl https get request and return data
     * @param backendURL
     * @param message
     * @param requestProps
     * @return
     * @throws java.io.IOException
     */
    public static String sendGetRequest(String backendURL, String message, Map<String, String> requestProps)
            throws IOException {
        URL url = new URL(backendURL);
        httpsURLConnection = (HttpsURLConnection) url.openConnection();
        httpsURLConnection.setSSLSocketFactory(sslSocketFactory);
        httpsURLConnection.setDoOutput(true);
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setRequestMethod("GET");
        if (requestProps != null && requestProps.size() > 0) {
            for (Map.Entry<String, String> entry : requestProps.entrySet()) {
                httpsURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        OutputStream outputStream = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        StringBuilder response = null;
        try {
            outputStream = httpsURLConnection.getOutputStream();
            //outputStream.write(message.getBytes());
            inputStream = httpsURLConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            log.error("Calling url : " + url + "failed. ", e);
        } finally {
            reader.close();
            inputStream.close();
            outputStream.close();
        }
        return response.toString();
    }

    public static String getKeyStoreType() {
        return KEY_STORE_TYPE;
    }

    public static void setKeyStoreType(String KEY_STORE_TYPE) {
        MutualSSLClient.KEY_STORE_TYPE = KEY_STORE_TYPE;
    }

    public static String getTrustStoreType() {
        return TRUST_STORE_TYPE;
    }

    public static void setTrustStoreType(String TRUST_STORE_TYPE) {
        MutualSSLClient.TRUST_STORE_TYPE = TRUST_STORE_TYPE;
    }

    public static String getKeyManagerType() {
        return KEY_MANAGER_TYPE;
    }

    public static void settKeyManagerType(String KEY_MANAGER_TYPE) {
        MutualSSLClient.KEY_MANAGER_TYPE = KEY_MANAGER_TYPE;
    }

    public static String getTrustManagerType() {
        return TRUST_MANAGER_TYPE;
    }

    public static void getTrustManagerType(String TRUST_MANAGER_TYPE) {
        MutualSSLClient.TRUST_MANAGER_TYPE = TRUST_MANAGER_TYPE;
    }

    public static HttpsURLConnection getHttpsURLConnection() {
        return httpsURLConnection;
    }

    public static void setProtocol(String PROTOCOL) {
        MutualSSLClient.PROTOCOL = PROTOCOL;
    }
}
