package com.tdp.afn.genesis;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.tdp.afn.genesis.util.Constants;

import java.time.LocalDateTime;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("refreshtokenonpremise")
    public String run(
            @TimerTrigger(name = "keepAliveTrigger", schedule = "0 */2 * * * *") String timerInfo,
            final ExecutionContext context) {
        context.getLogger().info("Java Timer trigger function executed at:" + LocalDateTime.now().toString());
        return Constants.MESSAGE_OK;
    }
}