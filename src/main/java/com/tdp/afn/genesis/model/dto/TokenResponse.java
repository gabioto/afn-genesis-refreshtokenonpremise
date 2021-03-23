package com.tdp.afn.genesis.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Class: TokenResponse. <br/>
 * <b>Copyright</b>: &copy; 2020 Telef&oacute;nica del Per&uacute;<br/>
 * <b>Company</b>: Telef&oacute;nica del Per&uacute;<br/>
 *
 * @author Telef&oacute;nica del Per&uacute; (TDP) <br/>
 *         <u>Service Provider</u>: Everis Per&uacute; SAC (EVE) <br/>
 *         <u>Developed by</u>: <br/>
 *         <ul>
 *         <li>Arnoldo Gasperi</li>
 *         </ul>
 *         <u>Changes</u>:<br/>
 *         <ul>
 *         <li>2020-08-10 Creaci&oacute;n del proyecto.</li>
 *         </ul>
 * @version 1.0
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String token_type;
    private String access_token;
    private Long expires_in;
    private Long consented_on;
    private String scope;
    private String refresh_token;
    private Long refresh_token_expires_in;
}
