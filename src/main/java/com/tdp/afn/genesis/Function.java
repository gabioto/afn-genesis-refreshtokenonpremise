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
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.RestClientException;


/**
 * Azure Functions with Time Trigger.
 */
public class Function {
    private Logger logger;
    private String baseUrl;
    private String storageConnectionString;

    private final String SCOPE = "scope1";
    private final String GRANT_TYPE = "refresh_token";

    public Function() {
        this.logger = null;
        this.baseUrl = null;
        this.storageConnectionString = null;
    }

    @FunctionName("refreshtokenonpremise")
    public String run(@TimerTrigger(name = "keepAliveTrigger", schedule = "0 */2 * * * *") String timerInfo,
            final ExecutionContext context) {
        this.logger = context.getLogger();
        this.logger.info("Java Timer trigger function executed at:" + LocalDateTime.now().toString());

        this.getenv();
        if (storageConnectionString == null || baseUrl == null) {
            this.logger.warning("Setting is not complete");
            return Constants.MESSAGE_ERROR;
        }

        try {
            // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

            // Create the table client.
            CloudTableClient tableClient = storageAccount.createCloudTableClient();

            // Create a cloud table object for the table.
            CloudTable cloudTable = tableClient.getTableReference("token");

            // Specify a partition query.
            TableQuery<TokenEntity> partitionQuery = TableQuery.from(TokenEntity.class);

            // Loop through the results, displaying information about the entity.
            StreamSupport.stream(cloudTable.execute(partitionQuery).spliterator(), true)
                    .map(t -> {
                        this.logger.info("Old Token -> PartitionKey: " + t.getPartitionKey()
                                + " - RowKey: " + t.getRowKey()
                                + " - AccessToken: " + t.getAccessToken()
                                + " - RefreshToken: " + t.getRefreshToken());
                        try {
                            TokenEntity tokenNew = getNewRefreshToken(t);
                            this.logger.info("New Token -> PartitionKey: " + tokenNew.getPartitionKey()
                                    + " - RowKey: " + tokenNew.getRowKey()
                                    + " - AccessToken: " + tokenNew.getAccessToken()
                                    + " - RefreshToken: " + tokenNew.getRefreshToken());
                            return tokenNew;
                        } catch (RestClientException | KeyManagementException | KeyStoreException
                                | NoSuchAlgorithmException e) {
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
        return Constants.MESSAGE_OK;
    }

    private void getenv() {
        this.baseUrl = System.getenv("OnPremiseUrl");
        this.storageConnectionString = System.getenv("StorageConnection");
    }

    private TokenEntity getNewRefreshToken(TokenEntity tokenEntity) throws RestClientException,
            KeyManagementException,KeyStoreException, NoSuchAlgorithmException {
        RestClient client = new RestClientImpl(this.logger);
        TokenRequest request = TokenRequest.builder()
                .clientId(tokenEntity.getPartitionKey())
                .grantType(GRANT_TYPE)
                .refreshToken(tokenEntity.getRefreshToken())
                .scope(SCOPE)
                .build();
        TokenResponse response = client.callRefreshTokenAPI(this.baseUrl, request);
        TokenEntity newTokenEntity = tokenEntity.mutate();
        newTokenEntity.setAccessToken(response.getAccess_token());
        newTokenEntity.setRefreshToken(response.getRefresh_token());
        return newTokenEntity;
    }
}