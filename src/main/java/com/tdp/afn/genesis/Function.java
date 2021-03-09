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
    @FunctionName("refreshtokenonpremise")
    public String run(@TimerTrigger(name = "keepAliveTrigger", schedule = "0 */2 * * * *") String timerInfo,
            final ExecutionContext context) {
        context.getLogger().info("Java Timer trigger function executed at:" + LocalDateTime.now().toString());

        String storageConnectionString = System.getenv("StorageConnection");
        if (storageConnectionString == null) {
            context.getLogger().warning("StorageConnection not defined");
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
                    .forEach(t -> {
                        context.getLogger()
                                .info("Info Token -> PartitionKey: " + t.getPartitionKey()
                                        + " - RowKey: " + t.getAccessToken()
                                        + " - AccessToken: " + t.getAccessToken()
                                        + " - RefreshToken: " + t.getRefreshToken());

                        // Create an operation to replace the entity.
                        TableOperation replaceEntity = TableOperation.replace(t);

                        // Submit the operation to the table service.
                        try {
                            cloudTable.execute(replaceEntity);
                        } catch (StorageException e) {
                            context.getLogger().warning(e.getMessage());
                        }
                    });
        } catch (InvalidKeyException | URISyntaxException | StorageException e) {
            context.getLogger().warning(e.getMessage());
            return Constants.MESSAGE_ERROR;
        }
        return Constants.MESSAGE_OK;
    }
}