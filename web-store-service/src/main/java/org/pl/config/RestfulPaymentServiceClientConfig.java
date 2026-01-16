package org.pl.config;

import org.pl.webstore.client.payment.api.DefaultApi;
import org.pl.webstore.client.payment.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestfulPaymentServiceClientConfig {

    @Value("${restful.payment.service.url}")
    private String paymentServiceUrl;

    @Bean
    public ApiClient paymentApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(paymentServiceUrl);
        return apiClient;
    }

    @Bean
    public DefaultApi balanceApi(ApiClient paymentApiClient) {
        return new DefaultApi(paymentApiClient);
    }
}