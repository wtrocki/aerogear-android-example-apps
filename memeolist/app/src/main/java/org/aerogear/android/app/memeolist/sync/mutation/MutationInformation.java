package org.aerogear.android.app.memeolist.sync.mutation;

import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.interceptor.ApolloInterceptor;

/**
 * MutationInformation.
 */
public class MutationInformation {
    InMemoryOfflineMutationObject originalInMemoryMutation;
    PersistentOfflineMutationObject originalPersistMutation;
    String clientState;
    ApolloInterceptor.CallBack customerCallBack;
    Mutation retryMutation;
    MuationType muationType;
    String uniqueIdentifier;

    public MutationInformation(String uniqueIdentifier,
                               InMemoryOfflineMutationObject originalInMemoryMutation,
                               ApolloInterceptor.CallBack customerCallBack,
                               String clientState) {
        this.originalInMemoryMutation = originalInMemoryMutation;
        this.customerCallBack = customerCallBack;
        this.clientState = clientState;
        this.muationType = MuationType.InMemory;
        this.uniqueIdentifier = uniqueIdentifier;
    }

    public MutationInformation(String uniqueIdentifier,
                               PersistentOfflineMutationObject persistentOfflineMutationObject,
                               String clientState) {

        this.uniqueIdentifier = uniqueIdentifier;
        this.originalPersistMutation = persistentOfflineMutationObject;
        this.clientState = clientState;
        this.muationType = MuationType.Persistent;
    }

    void updateRetryMutation(Mutation retryMutation) {
        this.retryMutation = retryMutation;
    }

    void updateCustomerCallBack(ApolloInterceptor.CallBack customerCallBack) {
        this.customerCallBack = customerCallBack;
    }


}