package ar.com.leo.api.nube.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NubeCredentials {

    @JsonProperty("app_id")
    public String appId;

    @JsonProperty("client_secret")
    public String clientSecret;

    @JsonProperty("stores")
    public Map<String, StoreCredentials> stores;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StoreCredentials {

        @JsonProperty("store_id")
        public String storeId;

        @JsonProperty("access_token")
        public String accessToken;
    }
}
