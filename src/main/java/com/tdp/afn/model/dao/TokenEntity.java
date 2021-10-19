package com.tdp.afn.model.dao;

import com.microsoft.azure.storage.table.TableServiceEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class: TokenEntity. <br/>
 * <b>Copyright</b>: &copy; 2019 Telef&oacute;nica del Per&uacute;<br/>
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
@Getter
@Setter
@NoArgsConstructor
public class TokenEntity extends TableServiceEntity {
    private String accessToken;
    private String refreshToken;

    public TokenEntity(String partitionKey, String rowKey) {
        this.partitionKey = partitionKey;
        this.rowKey = rowKey;
    }

    public TokenEntity mutate() {
        TokenEntity newTokenEntity = new TokenEntity();
        newTokenEntity.setAccessToken(this.getAccessToken());
        newTokenEntity.setEtag(this.getEtag());
        newTokenEntity.setPartitionKey(this.getPartitionKey());
        newTokenEntity.setRefreshToken(this.getRefreshToken());
        newTokenEntity.setRowKey(this.getRowKey());
        newTokenEntity.setTimestamp(this.getTimestamp());
        return newTokenEntity;
    }
}