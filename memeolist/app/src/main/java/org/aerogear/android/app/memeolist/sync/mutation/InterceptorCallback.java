package org.aerogear.android.app.memeolist.sync.mutation;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;

import org.aerogear.android.app.memeolist.sync.util.MessageNumberUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * InterceptorCallback.
 */
public class InterceptorCallback implements ApolloInterceptor.CallBack {

    ApolloInterceptor.CallBack customerCallBack;
    final Handler handler;
    boolean shouldRetry = true;
    Operation originalMutation;
    Operation currentMutation;
    String clientState;
    String recordIdentifier;

    public InterceptorCallback(ApolloInterceptor.CallBack customerCallBack, Handler handler,
                               final Operation originalMutation,
                               final  Operation currentMutation,
                               final String clientState,
                               final String recordIdentifier) {
        this.customerCallBack = customerCallBack;
        this.handler = handler;
        this.originalMutation = originalMutation;
        this.currentMutation = currentMutation;
        this.clientState = clientState;
        this.recordIdentifier = recordIdentifier;

    }

    @Override
    public void onResponse(@Nonnull ApolloInterceptor.InterceptorResponse response) {
        Log.d("AppSync", "onResponse()");
        //The conditional request failed
        if ((response.parsedResponse.get() != null) && (response.parsedResponse.get().errors().size() >= 1)) {
            Log.d("AppSync", "onResponse -- found error");
            if ( response.parsedResponse.get().errors().get(0).toString().contains("The conditional request failed")) {
                Log.d("AppSync", "onResponse -- string match");
                // if !shouldRetry AND conflict detected
                if (shouldRetry) {
                  String conflictString = null;
                  // TODO adjust conflict
                  conflictString = response.parsedResponse.toString();
                  Log.d("AppSync", "Conflict String: " + conflictString);
                    Log.d("AppSync", "Client String: " + clientState);
                    Message message = new Message();
                    MutationInterceptorMessage msg = new MutationInterceptorMessage(originalMutation, currentMutation);
                    msg.serverState = conflictString;
                    msg.clientState = clientState;
                    msg.requestIdentifier = recordIdentifier;
                    msg.requestClassName = currentMutation.getClass().getSimpleName();
                    //msg.requestIdentifier = originalMutation.un
                    message.obj = msg;
                    message.what = MessageNumberUtil.RETRY_EXEC;
                    handler.sendMessage(message);
                    shouldRetry = false;
                    return;
                }
            }
        }

        customerCallBack.onResponse(response);
        Message message = new Message();
        message.obj = new MutationInterceptorMessage(originalMutation, currentMutation);
        message.what = MessageNumberUtil.SUCCESSFUL_EXEC;
        handler.sendMessage(message);
    }

    @Override
    public void onFetch(ApolloInterceptor.FetchSourceType sourceType) {
        Log.d("AppSync", "onFetch()");
        customerCallBack.onFetch(sourceType);
    }

    @Override
    public void onFailure(@Nonnull ApolloException e) {
        Log.d("AppSync", "onFailure()" + e.getLocalizedMessage());
        shouldRetry = false;
        Message message = new Message();
        message.obj = new MutationInterceptorMessage(originalMutation, currentMutation);
        message.what = MessageNumberUtil.FAIL_EXEC;
        handler.sendMessage(message);
        customerCallBack.onFailure(e);
        return;
    }

    @Override
    public void onCompleted() {
        Log.d("AppSync", "onCompleted()");

    }
}
