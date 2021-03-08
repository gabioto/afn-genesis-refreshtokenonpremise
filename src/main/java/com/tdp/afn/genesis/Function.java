package com.tdp.afn.genesis;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TableInput;
import com.microsoft.azure.functions.annotation.TableOutput;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.tdp.afn.genesis.model.dao.Token;
import com.tdp.afn.genesis.util.Constants;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Azure Functions with Time Trigger.
 */
public class Function {
    @FunctionName("refreshtokenonpremise")
    public String run(
            @TimerTrigger(name = "keepAliveTrigger", schedule = "0 */2 * * * *") String timerInfo,
            @TableInput(name = "tokens", tableName = "token", connection = "StorageConnection") Optional<List<Token>> tokens,
            final ExecutionContext context) {
        context.getLogger().info("Java Timer trigger function executed at:" + LocalDateTime.now().toString());
        if (tokens.isPresent()){
            List<Token> tokenList = tokens.get();
            tokenList.parallelStream()
                    .forEach(t -> {
                        context.getLogger().info("Info Token -> PartitionKey: " + t.getPartitionKey()
                                + " - RowKey: " + t.getAccessToken() 
                                + " - AccessToken: " + t.getAccessToken() 
                                + " - RefreshToken: " + t.getRefreshToken());
                    });
        } else {
            context.getLogger().warning("Token table is empty");
        }

        return Constants.MESSAGE_OK;
    }
}