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
import com.tdp.afn.genesis.util.Constants;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.LocalDateTime;
import java.util.stream.StreamSupport;

/**
 * Azure Functions with Time Trigger.
 */
public class Function {
    private String baseUrl;
    private ExecutionContext context;
    private String storageConnectionString;

    public Function() {
        this.baseUrl = null;
        this.context = null;
        this.storageConnectionString = null;
    }

    @FunctionName("refreshtokenonpremise")
    public String run(@TimerTrigger(name = "keepAliveTrigger", schedule = "0 */2 * * * *") String timerInfo,
            final ExecutionContext context) {
        this.context = context;
        context.getLogger().info("Java Timer trigger function executed at:" + LocalDateTime.now().toString());

        this.getenv();
        if (storageConnectionString == null || baseUrl == null) {
            context.getLogger().warning("Setting is not complete");
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
                        context.getLogger()
                                .info("Old Token -> PartitionKey: " + t.getPartitionKey()
                                        + " - RowKey: " + t.getAccessToken()
                                        + " - AccessToken: " + t.getAccessToken()
                                        + " - RefreshToken: " + t.getRefreshToken());
                        return getNewRefreshToken(t);
                    })
                    .forEach(t -> {
                        context.getLogger()
                                .info("New Token -> PartitionKey: " + t.getPartitionKey()
                                        + " - RowKey: " + t.getAccessToken()
                                        + " - AccessToken: " + t.getAccessToken()
                                        + " - RefreshToken: " + t.getRefreshToken());

                        // Create an operation to replace the entity.
                        TableOperation replaceEntity = TableOperation.replace(t);

                        try {
                            // Submit the operation to the table service.
                            cloudTable.execute(replaceEntity);
                        } catch (StorageException e) {
                            context.getLogger().warning("Error replacing table entity: " + e.getMessage());
                        }
                    });
        } catch (InvalidKeyException | URISyntaxException | StorageException e) {
            context.getLogger().warning("Error processing tokens: " + e.getMessage());
            return Constants.MESSAGE_ERROR;
        }
        return Constants.MESSAGE_OK;
    }

    private void getenv() {
        this.baseUrl = System.getenv("OnPremiseUrl");
        this.storageConnectionString = System.getenv("StorageConnection");
    }

    private TokenEntity getNewRefreshToken(TokenEntity tokenEntity) {
        this.context.getLogger().info("Request al API del refresh token");
        return tokenEntity;
    }
}