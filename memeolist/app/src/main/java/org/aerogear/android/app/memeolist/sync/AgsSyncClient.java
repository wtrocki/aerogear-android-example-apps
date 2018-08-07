package org.aerogear.android.app.memeolist.sync;

import static org.aerogear.mobile.core.utils.SanityCheck.nonNull;

import javax.annotation.Nonnull;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport.Factory;

import org.aerogear.mobile.core.MobileCore;
import org.aerogear.mobile.core.configuration.ServiceConfiguration;

import okhttp3.OkHttpClient;

/**
 *
 */
public final class AgsSyncClient {

    private final ApolloClient apolloClient;

    public AgsSyncClient(@Nonnull OkHttpClient okHttpClient, @Nonnull String serverUrl,
                         @Nonnull String webSocketUrl) {
        apolloClient = ApolloClient.builder().serverUrl(nonNull(serverUrl, "serverUrl"))
                        .okHttpClient(nonNull(okHttpClient, "okHttpClient"))
                        .subscriptionTransportFactory(new Factory(webSocketUrl, okHttpClient))
                        .build();
    }

    public ApolloClient getApolloClient() {
        return apolloClient;
    }

//
//  public static class Builder extends ApolloClient.Builder{
//
//    private final String serverUrl;
//    private final String webSocketUrl;
//
//    public Builder(){
//    }
//
//    @Override
//    public ApolloClient build() {
//      ApolloClient build = super.build();
//      return build;
//    }
//
//    public Builder(@Nonnull String serverUrl,
//                   @Nonnull String webSocketUrl) {
//      this.serverUrl = serverUrl;
//      this.webSocketUrl = webSocketUrl;
//
////      apolloClient = ApolloClient.builder().serverUrl(nonNull(serverUrl, "serverUrl"))
////              .okHttpClient(nonNull(okHttpClient, "okHttpClient"))
////              .subscriptionTransportFactory(new Factory(webSocketUrl, okHttpClient))
////              .build();
//    }
//
//  }


}