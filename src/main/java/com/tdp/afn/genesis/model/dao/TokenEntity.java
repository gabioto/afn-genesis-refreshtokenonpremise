package com.tdp.afn.genesis.model.dao;

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
 *         <li>Arnoldo Gasperi (AEGA) From (EVE)</li>
 *         </ul>
 *         <u>Changes</u>:<br/>
 *         <ul>
 *         <li>2021-03-0 Creaci&oacute;n del proyecto.</li>
 *         </ul>
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class TokenEntity extends TableServiceEntity{
    private String AccessToken;
    private String RefreshToken;

    public TokenEntity(String partitionKey, String rowKey) {
      this.partitionKey = partitionKey;
      this.rowKey = rowKey;
    }
}
