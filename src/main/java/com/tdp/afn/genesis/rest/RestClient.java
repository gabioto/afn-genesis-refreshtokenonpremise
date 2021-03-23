package com.tdp.afn.genesis.rest;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import com.tdp.afn.genesis.model.dto.TokenRequest;
import com.tdp.afn.genesis.model.dto.TokenResponse;

import org.springframework.web.client.RestClientException;

public interface RestClient {
    TokenResponse callRefreshTokenAPI(String baseUrl,
            TokenRequest request) throws RestClientException, KeyManagementException,
                    KeyStoreException, NoSuchAlgorithmException;
}
