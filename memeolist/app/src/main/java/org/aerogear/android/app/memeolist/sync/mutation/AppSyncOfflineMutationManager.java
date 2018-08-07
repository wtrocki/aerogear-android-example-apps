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

package org.aerogear.android.app.memeolist.sync.mutation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.internal.json.InputFieldJsonWriter;
import com.apollographql.apollo.internal.json.JsonWriter;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okio.Buffer;

/**
 * AppSyncOfflineMutationManager.
 */
public class AppSyncOfflineMutationManager {

    /*
     * Constants of message sent to update handler.
     */
    static final int MSG_EXEC = 100;
    static final int MSG_CHECK = 200;
    static final int MSG_DISCONNECT = 300;
    private static final String TAG = AppSyncOfflineMutationManager.class.getSimpleName();
    private NetworkUpdateHandler networkUpdateHandler;
    private HandlerThread handlerThread;
    private boolean shouldProcess;
    InMemoryOfflineMutationManager inMemoryOfflineMutationManager;
    PersistentOfflineMutationManager persistentOfflineMutationManager;
    private ScalarTypeAdapters scalarTypeAdapters;
    private AppSyncMutationSqlCacheOperations mutationSqlCacheOperations;
    private Handler queueHandler;

    void updateQueueHandler(Handler queueHandler) {
        this.queueHandler = queueHandler;
        this.persistentOfflineMutationManager.updateQueueHandler(queueHandler);
    }

    /*
     * Registers a BroadcastReceiver to receive network status change events. It
     * will update transfer records in database directly.
     */
    private NetworkInfoReceiver networkInfoReceiver;

    public boolean shouldProcess() {
        return shouldProcess;
    }

    public void addMutationObjectInQueue(InMemoryOfflineMutationObject mutationObject) throws IOException {

        inMemoryOfflineMutationManager.addMutationObjectInQueue(mutationObject);
        persistentOfflineMutationManager.addPersistentMutationObject(
                new PersistentOfflineMutationObject(
                        mutationObject.recordIdentifier,
                        httpRequestBody(mutationObject.request.operation),
                        mutationObject.request.operation.getClass().getSimpleName(),
                        new JSONObject(mutationObject.request.operation.variables().valueMap()).toString())
        );

        Log.d("AppSync", "Created both in-memory and persistent records. Now checking queue.");
        processNextInQueueMutation();

    }


    public void processNextInQueueMutation() {
        Log.d("AppSync", "Checking if I need to process next originalMutation");
        if (shouldProcess) {
            Log.d("AppSync", "First check: Internet Available");
            if (!persistentOfflineMutationManager.isQueueEmpty()) {
                Log.d("AppSync", "Processing next in queue: PERSISTENT.");
                persistentOfflineMutationManager.processNextMutationObject();
                return;
            } else {
                Log.d("AppSync", "Second check: Persistent mutations queue is EMPTY!");
            }
            if (!inMemoryOfflineMutationManager.isQueueEmpty()) {
                Log.d("AppSync", "Processing next in queue: INMEMORY.");
                InMemoryOfflineMutationObject mutationObject = inMemoryOfflineMutationManager.processNextMutation();
                persistentOfflineMutationManager.removePersistentMutationObject(mutationObject.recordIdentifier);
                return;
            } else {
                Log.d("AppSync", "Third check: Inmemory mutations queue is EMPTY!");
            }
        }
    }


    public void processAllQueuedMutations() {
        Log.d("AppSync", "Starting to process queued mutations.");
        while (!(inMemoryOfflineMutationManager.isQueueEmpty() &&
                persistentOfflineMutationManager.isQueueEmpty())) {

            if (shouldProcess) {
                if (!persistentOfflineMutationManager.isQueueEmpty()) {
                    Log.d("AppSync", "Starting to process PERSISTENT.");
                    persistentOfflineMutationManager.processNextMutationObject();
                }
                else if (!inMemoryOfflineMutationManager.isQueueEmpty()) {
                    Log.d("AppSync", "Starting to process INMEMORY.");
                    InMemoryOfflineMutationObject mutationObject = inMemoryOfflineMutationManager.processNextMutation();
                    persistentOfflineMutationManager.removePersistentMutationObject(mutationObject.recordIdentifier);
                }
            } else {
                break;
            }
        }
    }

    public AppSyncOfflineMutationManager(Context context,
                                         final Map<ScalarType, CustomTypeAdapter> customTypeAdapters,
                                         final AppSyncMutationSqlCacheOperations mutationSqlCacheOperations,
                                         final AppSyncCustomNetworkInvoker persistentMutationsNetworkInvoker) {
        handlerThread = new HandlerThread(TAG + "-AWSAppSyncOfflineMutationsHandlerThread");
        handlerThread.start();
        this.networkUpdateHandler = new NetworkUpdateHandler(handlerThread.getLooper());
        this.networkInfoReceiver = new NetworkInfoReceiver(context, this.networkUpdateHandler);
        this.inMemoryOfflineMutationManager = new InMemoryOfflineMutationManager();
        persistentOfflineMutationManager = new PersistentOfflineMutationManager(mutationSqlCacheOperations,
                persistentMutationsNetworkInvoker);
        this.scalarTypeAdapters = new ScalarTypeAdapters(customTypeAdapters);
        this.mutationSqlCacheOperations = mutationSqlCacheOperations;

        context.getApplicationContext().registerReceiver(networkInfoReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
    }

    class NetworkUpdateHandler extends Handler {
        public NetworkUpdateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CHECK) {
                // remove messages of the same type
                networkUpdateHandler.removeMessages(MSG_CHECK);
                // start executing the originalMutation queue
                Log.d("AppSync", "Internet connected.");
                shouldProcess = true;
                processNextInQueueMutation();
            } else if (msg.what == MSG_DISCONNECT) {
                // disconnect, pause mutations
                Log.d("AppSync", "Internet DISCONNECTED.");
                shouldProcess = false;
            } else {
                // ignore case
            }
        }
    }


    /**
     * A Broadcast receiver to receive network connection change events.
     */
    static class NetworkInfoReceiver extends BroadcastReceiver {
        private final Handler handler;
        private final ConnectivityManager connManager;

        /**
         * Constructs a NetworkInfoReceiver.
         *
         * @param handler a handle to send message to
         */
        public NetworkInfoReceiver(Context context, Handler handler) {
            this.handler = handler;
            connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                final boolean networkConnected = isNetworkConnected();

                handler.sendEmptyMessage(networkConnected ? MSG_CHECK : MSG_DISCONNECT);
            }
        }

        /**
         * Gets the status of network connectivity.
         *
         * @return true if network is connected, false otherwise.
         */
        boolean isNetworkConnected() {
            final NetworkInfo info = connManager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    // To be used for offline mutations which are persisted across app restarts
    private String httpRequestBody(Operation operation) throws IOException {
        Buffer buffer = new Buffer();
        JsonWriter jsonWriter = JsonWriter.of(buffer);
        jsonWriter.beginObject();
        jsonWriter.name("query").value(operation.queryDocument().replaceAll("\\n", ""));
        jsonWriter.name("variables").beginObject();
        operation.variables().marshaller().marshal(new InputFieldJsonWriter(jsonWriter, scalarTypeAdapters));
        jsonWriter.endObject();
        jsonWriter.endObject();
        jsonWriter.close();
        return buffer.readUtf8();
    }
}

