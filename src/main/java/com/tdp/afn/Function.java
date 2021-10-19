package com.tdp.afn;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.TableOperation;
import com.microsoft.azure.storage.table.TableQuery;
import com.tdp.afn.exception.NotValidHttpStatusException;
import com.tdp.afn.model.dao.TokenEntity;
import com.tdp.afn.model.dto.TokenRequest;
import com.tdp.afn.model.dto.TokenResponse;
import com.tdp.afn.rest.RestClient;
import com.tdp.afn.rest.impl.RestClientImpl;
import com.tdp.genesis.core.security.aes.AesUtil;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.RestClientException;

/**
 * Azure Functions with HTTP Trigger. <b>Copyright</b>: &copy; 2021
 * Telef&oacute;nica del Per&uacute;<br/>
 * <b>Company</b>: Telef&oacute;nica del Per&uacute;<br/>
 *
 * @author Telef&oacute;nica del Per&uacute; (TDP) <br/>
 *         <u>Service Provider</u>: Everis Per&uacute; SAC (EVE) <br/>
 *         <u>Developed by</u>: <br/>
 *         <ul>
 *         <li>Developer name</li>
 *         </ul>
 *         <u>Changes</u>:<br/>
 *         <ul>
 *         <li>YYYY-MM-DD Creaci&oacute;n del proyecto.</li>
 *         </ul>
 * @version 1.0
 */
public class Function {

    protected String salt;
    protected String baseUrl;
    protected String password;
    protected String tableName;
    protected String storageConnection;

    private Logger logger;
    private boolean isError;

    private static final String SCOPE = "scope1";
    private static final String GRANT_TYPE = "refresh_token";

    public Function() {
        this.salt = null;
        this.logger = null;
        this.baseUrl = null;
        this.password = null;
        this.tableName = null;
        this.storageConnection = null;
    }

    @FunctionName("refreshtokenonpremise")
    public String run(
        @TimerTrigger(
            name = "keepAliveTrigger", 
            schedule = "%ScheduleTime%") String timerInfo,
            final ExecutionContext context) {

        this.logger = context.getLogger();
        this.logger.info("Java Timer trigger function executed at: " + LocalDateTime.now().toString());

        this.getSetting();

        this.isNotValidSetting();

        if (!this.isError) {
            try {
                SecretKey key = AesUtil.getSecretKeyFromPassword(this.password, this.salt);
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnection);
                CloudTableClient tableClient = storageAccount.createCloudTableClient();
                CloudTable cloudTable = tableClient.getTableReference(this.tableName);
                TableQuery<TokenEntity> partitionQuery = TableQuery.from(TokenEntity.class);
                this.updateToken(cloudTable, partitionQuery, key);
            } catch (InvalidKeyException | URISyntaxException | StorageException e) {
                this.logger.warning("Error al procesar tokens: " + e.getMessage());
                this.isError = true;
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                String msg = "Error generando el secret key de encriptación: " + e.getMessage();
                this.logger.warning(msg);
                this.isError = true;
            }
        }

        this.logger.info("Java Timer trigger function finished.");

        return Boolean.toString(this.isError);
    }

    protected void getSetting() {
        this.salt = System.getenv("Salt");
        this.baseUrl = System.getenv("BaseUrl");
        this.password = System.getenv("Password");
        this.tableName = System.getenv("TableName");
        this.storageConnection = System.getenv("StorageConnection");
    }

    protected TokenResponse getNewRefreshToken(String clientId, String refreshToken) throws RestClientException,
            KeyManagementException, KeyStoreException, NoSuchAlgorithmException, NotValidHttpStatusException {
        RestClient client = new RestClientImpl(this.logger);
        TokenRequest request = TokenRequest.builder().clientId(clientId).grantType(GRANT_TYPE)
                .refreshToken(refreshToken).scope(SCOPE).build();
        return client.callRefreshTokenApi(this.baseUrl, request);
    }

    protected void isNotValidSetting() {
        boolean isEmptySettings = StringUtils.isNoneBlank(this.salt, this.baseUrl, this.password, this.tableName,
                this.storageConnection);
        if (!isEmptySettings) {
            this.isError = true;
            this.logger.warning("Setting is not complete");
        }
    }

    private void updateToken(CloudTable cloudTable, TableQuery<TokenEntity> partitionQuery, SecretKey key) {
        StreamSupport.stream(cloudTable.execute(partitionQuery).spliterator(), true).map(t -> {
            TokenEntity tokenNew = t.mutate();
            this.logger.info("Processing clientid: " + t.getPartitionKey());
            TokenResponse response;
            try {
                response = getNewRefreshToken(t.getPartitionKey(), AesUtil.decryptString(t.getRefreshToken(), key));
                tokenNew.setAccessToken(AesUtil.encryptString(response.getAccessToken(), key));
                tokenNew.setRefreshToken(AesUtil.encryptString(response.getRefreshToken(), key));
                return tokenNew;
            } catch (RestClientException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException
                    | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException
                    | BadPaddingException | IllegalBlockSizeException | NotValidHttpStatusException
                    | IllegalArgumentException e) {
                this.logger.warning("Error al obtener un nuevo refresh token: " + e.getMessage());
                this.isError = true;
                return t;
            }
        }).forEach(t -> {
            if (!StringUtils.isNoneBlank(t.getAccessToken(), t.getRefreshToken())) {
                this.logger
                        .warning("Ni el access token ni el refresh token pueden ser nulos, vacios o estar en blanco");
                this.isError = true;
            } else {
                TableOperation replaceEntity = TableOperation.replace(t);
                try {
                    cloudTable.execute(replaceEntity);
                } catch (StorageException e) {
                    this.logger.warning("Error al remplazar la tabla entidada: " + e.getMessage());
                    this.isError = true;
                }
            }
        });
    }

}
