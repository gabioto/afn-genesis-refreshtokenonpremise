package com.tdp.afn.genesis;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.TableOperation;
import com.microsoft.azure.storage.table.TableQuery;
import com.tdp.afn.genesis.model.dao.TokenEntity;
import com.tdp.afn.genesis.model.dto.TokenRequest;
import com.tdp.afn.genesis.model.dto.TokenResponse;
import com.tdp.afn.genesis.rest.RestClient;
import com.tdp.afn.genesis.rest.impl.RestClientImpl;
import com.tdp.afn.genesis.util.Constants;
import com.tdp.genesis.core.security.aes.AESUtil;

import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.RestClientException;


/**
 * Azure Functions with Time Trigger.
 */
public class Function {
    protected String salt;
    protected String baseUrl;
    protected String password;
    protected String algorithm;
    protected String tableName;
    protected String storageConnection;

    private Logger logger;

    private final String SCOPE = "scope1";
    private final String GRANT_TYPE = "refresh_token";

    private static IvParameterSpec IVPARAMETERSPEC = new IvParameterSpec(AESUtil.GENESIS_IVPARAMETER);

    public Function() {
        this.salt = null;
        this.logger = null;
        this.baseUrl = null;
        this.password = null;
        this.algorithm = null;
        this.tableName = null;
        this.storageConnection = null;
    }

    @FunctionName("refreshtokenonpremise")
    public String run(@TimerTrigger(name = "keepAliveTrigger", schedule = "%ScheduleTime%") String timerInfo,
            final ExecutionContext context) {
        this.logger = context.getLogger();
        this.logger.info("Java Timer trigger function executed at:" + LocalDateTime.now().toString());

        this.getenv();
        if (this.isNotValidSetting()) {
            this.logger.warning("Setting is not complete");
            return Constants.MESSAGE_ERROR;
        }

        SecretKey key;
        try {
            key = AESUtil.getKeyFromPassword(this.password, this.salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            String msg = "Error generando el secret key de encriptación: " + e.getMessage();
            this.logger.warning(msg);
            return Constants.MESSAGE_ERROR;
        }

        try {
            // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnection);

            // Create the table client.
            CloudTableClient tableClient = storageAccount.createCloudTableClient();

            // Create a cloud table object for the table.
            CloudTable cloudTable = tableClient.getTableReference("token");

            // Specify a partition query.
            TableQuery<TokenEntity> partitionQuery = TableQuery.from(TokenEntity.class);

            // Loop through the results, displaying information about the entity.
            StreamSupport.stream(cloudTable.execute(partitionQuery).spliterator(), true)
                    .map(t -> {
                        TokenEntity tokenNew = t.mutate();
                        this.logger.info("Processing clientid: " + t.getPartitionKey());
                        try {
                            TokenResponse response = getNewRefreshToken(t.getPartitionKey(),
                                    AESUtil.decrypt(this.algorithm, t.getRefreshToken(), key, IVPARAMETERSPEC));
                            tokenNew.setAccessToken(AESUtil.encrypt(this.algorithm, response.getAccess_token(), key, IVPARAMETERSPEC));
                            tokenNew.setRefreshToken(AESUtil.encrypt(this.algorithm, response.getRefresh_token(), key, IVPARAMETERSPEC));
                            return tokenNew;
                        } catch (RestClientException | KeyManagementException | KeyStoreException
                                | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                            this.logger.warning("Error getting new refresh token: " + e.getMessage());
                            return t;
                        }
                    })
                    .forEach(t -> {
                        if(StringUtils.isBlank(t.getAccessToken()) || StringUtils.isBlank(t.getRefreshToken())) {
                            this.logger.warning("Ni el access token ni el refresh token pueden ser nulos, vacios o estar en blanco");
                            return;  //No se procesa el nuevo TokenEntity
                        }

                        // Create an operation to replace the entity.
                        TableOperation replaceEntity = TableOperation.replace(t);

                        try {
                            // Submit the operation to the table service.
                            cloudTable.execute(replaceEntity);
                        } catch (StorageException e) {
                            this.logger.warning("Error replacing table entity: " + e.getMessage());
                        }
                    });
        } catch (InvalidKeyException | URISyntaxException | StorageException e) {
            this.logger.warning("Error processing tokens: " + e.getMessage());
            return Constants.MESSAGE_ERROR;
        }
        this.logger.info("Java Timer trigger function finished.");
        return Constants.MESSAGE_OK;
    }

    private void getenv() {
        this.salt = System.getenv("Salt");
        this.baseUrl = System.getenv("BaseUrl");
        this.password = System.getenv("Password");
        this.algorithm = System.getenv("Algorithm");
        this.tableName = System.getenv("TableName");
        this.storageConnection = System.getenv("StorageConnection");
    }

    private TokenResponse getNewRefreshToken(String clientId, String refreshToken) throws RestClientException,
            KeyManagementException,KeyStoreException, NoSuchAlgorithmException {
        RestClient client = new RestClientImpl(this.logger);
        TokenRequest request = TokenRequest.builder()
                .clientId(clientId)
                .grantType(GRANT_TYPE)
                .refreshToken(refreshToken)
                .scope(SCOPE)
                .build();
        return client.callRefreshTokenAPI(this.baseUrl, request);
    }

    private boolean isNotValidSetting() {
        return StringUtils.isBlank(this.salt) ||
                StringUtils.isBlank(this.baseUrl) ||
                StringUtils.isBlank(this.password) ||
                StringUtils.isBlank(this.algorithm) ||
                StringUtils.isBlank(this.tableName) ||
                StringUtils.isBlank(this.storageConnection);
    }
}