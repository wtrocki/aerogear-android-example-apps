/**
 * Copyright 2018-2018 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 *
 *     http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
 */

package org.aerogear.android.app.memeolist.sync;

import android.content.Context;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.fetcher.CacheFirstFetcher;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import org.aerogear.android.app.memeolist.sync.cache.AppSyncSqlHelper;
import org.aerogear.android.app.memeolist.sync.conflicts.ConflictResolverInterface;
import org.aerogear.android.app.memeolist.sync.mutation.AppSyncCustomNetworkInvoker;
import org.aerogear.android.app.memeolist.sync.mutation.AppSyncMutationSqlCacheOperations;
import org.aerogear.android.app.memeolist.sync.mutation.AppSyncMutationsSqlHelper;
import org.aerogear.android.app.memeolist.sync.mutation.AppSyncOfflineMutationInterceptor;
import org.aerogear.android.app.memeolist.sync.mutation.AppSyncOfflineMutationManager;
import org.aerogear.android.app.memeolist.sync.mutation.MutationInformation;
import org.aerogear.android.app.memeolist.sync.mutation.user.PersistentMutationsCallback;
import org.aerogear.android.app.memeolist.sync.objects.AppSyncMutationCall;
import org.aerogear.android.app.memeolist.sync.objects.AppSyncQueryCall;
import org.aerogear.android.app.memeolist.sync.objects.AppSyncSubscriptionCall;
import org.aerogear.android.app.memeolist.sync.store.AppSyncStore;
import org.aerogear.android.app.memeolist.sync.updates.AppSyncOptimisticUpdateInterceptor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class AWSAppSyncClient {
    private static final String defaultSqlStoreName = "appsyncstore";
    private static final String defaultMutationSqlStoreName = "appsyncstore_mutation";
    ApolloClient mApolloClient;
    AppSyncStore mSyncStore;
    private Context applicationContext;
    Map<Mutation, MutationInformation> mutationMap;

    private AWSAppSyncClient(AWSAppSyncClient.Builder builder) {
        applicationContext = builder.mContext.getApplicationContext();


        OkHttpClient.Builder okHttpClientBuilder;
        if (builder.mOkHttpClient == null) {
            okHttpClientBuilder = new OkHttpClient.Builder();
        } else {
            okHttpClientBuilder = builder.mOkHttpClient.newBuilder();
        }

        OkHttpClient okHttpClient = okHttpClientBuilder
                .build();

        AppSyncMutationsSqlHelper mutationsSqlHelper = new AppSyncMutationsSqlHelper(builder.mContext, defaultMutationSqlStoreName);
        AppSyncMutationSqlCacheOperations sqlCacheOperations = new AppSyncMutationSqlCacheOperations(mutationsSqlHelper);
        mutationMap = new HashMap<>();

        AppSyncOptimisticUpdateInterceptor optimisticUpdateInterceptor = new AppSyncOptimisticUpdateInterceptor();

        AppSyncCustomNetworkInvoker networkInvoker =
                new AppSyncCustomNetworkInvoker(HttpUrl.parse(builder.mServerUrl),
                        okHttpClient,
                        new ScalarTypeAdapters(builder.customTypeAdapters),
                        builder.mPersistentMutationsCallback);

        ApolloClient.Builder clientBuilder = ApolloClient.builder()
                .serverUrl(builder.mServerUrl)
                .normalizedCache(builder.mNormalizedCacheFactory, builder.mResolver)
                .addApplicationInterceptor(optimisticUpdateInterceptor)
                .addApplicationInterceptor(new AppSyncOfflineMutationInterceptor(
                        new AppSyncOfflineMutationManager(builder.mContext,
                                builder.customTypeAdapters,
                                sqlCacheOperations,
                                networkInvoker),
                        false,
                        builder.mContext,
                        mutationMap,
                        this,
                        builder.mConflictResolver))
                .okHttpClient(okHttpClient);

        for (ScalarType scalarType : builder.customTypeAdapters.keySet()) {
            clientBuilder.addCustomTypeAdapter(scalarType, builder.customTypeAdapters.get(scalarType));
        }

        if (builder.mDispatcher != null) {
            clientBuilder.dispatcher(builder.mDispatcher);
        }

        if (builder.mCacheHeaders != null) {
            clientBuilder.defaultCacheHeaders(builder.mCacheHeaders);
        }

        if (builder.mDefaultResponseFetcher != null) {
            clientBuilder.defaultResponseFetcher(builder.mDefaultResponseFetcher);
        }

        mApolloClient = clientBuilder.build();

        mSyncStore = new AppSyncStore(mApolloClient.apolloStore());

        optimisticUpdateInterceptor.setStore(mApolloClient.apolloStore());
    }

    public static class Builder {
        NormalizedCacheFactory mNormalizedCacheFactory;
        CacheKeyResolver mResolver;
        ConflictResolverInterface mConflictResolver;

        // Apollo
        String mServerUrl;
        final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
        Executor mDispatcher;
        OkHttpClient mOkHttpClient;
        ResponseFetcher mDefaultResponseFetcher = new CacheFirstFetcher();
        CacheHeaders mCacheHeaders;
        PersistentMutationsCallback mPersistentMutationsCallback;

        // Android
        Context mContext;

        private Builder() { }


        public Builder serverUrl(String serverUrl) {
            mServerUrl = serverUrl;
            return this;
        }

        public Builder context(Context context) {
            mContext = context;
            return this;
        }

        public Builder normalizedCache(NormalizedCacheFactory normalizedCacheFactory) {
            mNormalizedCacheFactory = normalizedCacheFactory;
            return this;
        }

        public Builder resolver(CacheKeyResolver resolver) {
            mResolver = resolver;
            return this;
        }

        public Builder conflictResolver(ConflictResolverInterface conflictResolver) {
            mConflictResolver = conflictResolver;
            return this;
        }

        public <T> Builder addCustomTypeAdapter(ScalarType scalarType,
                                            final CustomTypeAdapter<T> customTypeAdapter) {
            customTypeAdapters.put(scalarType, customTypeAdapter);
            return this;
        }

        public Builder dispatcher(Executor dispatcher) {
            mDispatcher = dispatcher;
            return this;
        }

        public Builder defaultCacheHeaders(CacheHeaders cacheHeaders) {
            mCacheHeaders = cacheHeaders;
            return this;
        }

        public Builder okHttpClient(OkHttpClient okHttpClient) {
            mOkHttpClient = okHttpClient;
            return this;
        }

        public Builder defaultResponseFetcher(ResponseFetcher defaultResponseFetcher) {
            mDefaultResponseFetcher = defaultResponseFetcher;
            return this;
        }

        public Builder persistentMutationsCallback(PersistentMutationsCallback persistentMutationsCallback) {
            mPersistentMutationsCallback = persistentMutationsCallback;
            return this;
        }

        public AWSAppSyncClient build() {
            if (mNormalizedCacheFactory == null) {
                AppSyncSqlHelper appSyncSqlHelper = AppSyncSqlHelper.create(mContext, defaultSqlStoreName);

                //Create NormalizedCacheFactory
                mNormalizedCacheFactory = new SqlNormalizedCacheFactory(appSyncSqlHelper);
            }

            if (mResolver == null) {
                mResolver = new CacheKeyResolver() {
                    @Nonnull
                    @Override
                    public CacheKey fromFieldRecordSet(@Nonnull ResponseField field, @Nonnull Map<String, Object> recordSet) {
                        return formatCacheKey((String) recordSet.get("id"));
                    }

                    @Nonnull
                    @Override
                    public CacheKey fromFieldArguments(@Nonnull ResponseField field, @Nonnull Operation.Variables variables) {

                        return formatCacheKey((String) field.resolveArgument("id", variables));
                    }

                    private CacheKey formatCacheKey(String id) {
                        if (id == null || id.isEmpty()) {
                            return CacheKey.NO_KEY;
                        } else {
                            return CacheKey.from(id);
                        }
                    }
                };
            }

            return new AWSAppSyncClient(this);
        }
    }

    /**
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    public <D extends Query.Data, T, V extends Query.Variables> AppSyncQueryCall<T> query(@Nonnull Query<D, T, V> query) {
        return mApolloClient.query(query);
    }

    public <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(@Nonnull Mutation<D, T, V> mutation) {
        return mutate(mutation, false);
    }

    protected <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(@Nonnull Mutation<D, T, V> mutation, boolean isRetry) {
        if (isRetry) {
            mutationMap.put(mutation, null);
        }
        return mApolloClient.mutate(mutation);
    }

    protected <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(@Nonnull Mutation<D, T, V> mutation, @Nonnull D withOptimisticUpdates, boolean isRetry) {
        if (isRetry) {
            mutationMap.put(mutation, null);
        }
        return mApolloClient.mutate(mutation, withOptimisticUpdates);
    }


    public <D extends Subscription.Data, T, V extends Subscription.Variables> AppSyncSubscriptionCall<T> subscribe(@Nonnull Subscription<D, T, V> subscription) {
        return mApolloClient.subscribe(subscription);
    }

    public <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(@Nonnull Mutation<D, T, V> mutation, @Nonnull D withOptimisticUpdates) {
        return mutate(mutation, withOptimisticUpdates, false);

    }

    public AppSyncStore getStore() {
        return mSyncStore;
    }

}
