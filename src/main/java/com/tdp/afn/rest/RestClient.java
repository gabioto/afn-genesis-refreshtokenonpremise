package com.tdp.afn.rest;

import com.tdp.afn.exception.NotValidHttpStatusException;
import com.tdp.afn.model.dto.TokenRequest;
import com.tdp.afn.model.dto.TokenResponse;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.springframework.web.client.RestClientException;

public interface RestClient {
    TokenResponse callRefreshTokenApi(String baseUrl, TokenRequest request) throws RestClientException,
            KeyManagementException, KeyStoreException, NoSuchAlgorithmException, NotValidHttpStatusException;
}