package org.aerogear.android.app.memeolist.sync;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport;

import org.aerogear.mobile.core.MobileCore;
import org.aerogear.mobile.core.configuration.ServiceConfiguration;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;

import static org.aerogear.mobile.core.utils.SanityCheck.nonNull;

/**
 * Build singleton that contains all configuration options for sync
 */

//public class AgsSyncInstance {
//
//  public static final String TYPE = "sync";
//
//  private static AgsSyncClient instance;
//
//  private final ApolloClient apolloClient;
//
//  public AGSSyncClient(@Nonnull OkHttpClient okHttpClient, @Nonnull String serverUrl,
//                       @Nonnull String webSocketUrl) {
//    apolloClient = ApolloClient.builder().serverUrl(nonNull(serverUrl, "serverUrl"))
//            .okHttpClient(nonNull(okHttpClient, "okHttpClient"))
//            .subscriptionTransportFactory(new WebSocketSubscriptionTransport.Factory(webSocketUrl, okHttpClient))
//            .build();
//  }
//
//  public static AGSSyncClient getInstance() {
//    if (instance == null) {
//      MobileCore mobileCore = MobileCore.getInstance();
//      ServiceConfiguration configuration = mobileCore.getServiceConfigurationByType(TYPE);
//      String serverUrl = configuration.getUrl();
//      String webSocketUrl = configuration.getProperty("subscription");
//      OkHttpClient okHttpClient = mobileCore.getHttpLayer().getClient();
//
//
//      AGSSyncClient.instance = new AGSSyncClient(okHttpClient, serverUrl, webSocketUrl);
//    }
//    return instance;
//  }
//
//  public ApolloClient getApolloClient() {
//    return apolloClient;
//  }
//}
