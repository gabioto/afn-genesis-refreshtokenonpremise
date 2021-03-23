package com.tdp.afn.genesis.rest.impl;

import com.tdp.afn.genesis.model.dto.TokenRequest;
import com.tdp.afn.genesis.model.dto.TokenResponse;
import com.tdp.afn.genesis.rest.RestClient;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class RestClientImpl implements RestClient{

    private final Logger logger;

    @Override
    public TokenResponse callRefreshTokenAPI(String baseUrl,
            TokenRequest request) throws RestClientException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", request.getClientId());
        body.add("scope",request.getScope());
        body.add("refresh_token",request.getRefreshToken());
        body.add("grant_type",request.getGrantType());

        HttpEntity<MultiValueMap<String, String>> entityReq = new HttpEntity<>(body, headers);
        TokenResponse tokenResponse =  getRestTemplate().exchange(baseUrl,
                HttpMethod.POST,
                entityReq,
                TokenResponse.class)
                .getBody();
        this.logger.info("TokenResponse -> AccessToken: " + tokenResponse.getAccess_token()
                + " - RefreshToken: " + tokenResponse.getRefresh_token()
                + " - Scope: " + tokenResponse.getScope()
                + " - TokenType: " + tokenResponse.getToken_type()
                + " - ConsentedOn: " + tokenResponse.getConsented_on()
                + " - ExpiresIn: " + tokenResponse.getExpires_in()
                + " - RefreshTokenExpiresin: " + tokenResponse.getRefresh_token_expires_in());
        return tokenResponse;
    }

    private RestTemplate getRestTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {

            @Override
            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                return true;
            }
        };
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        return new RestTemplate(requestFactory);
    }
}
