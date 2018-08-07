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

import com.apollographql.apollo.api.Operation;

public class MutationInterceptorMessage {
    final Operation originalMutation;
    final Operation currentMutation;
    String requestIdentifier;
    String requestClassName;
    String serverState;
    String clientState;

    MutationInterceptorMessage(Operation originalMutation, Operation currentMutation) {
        this.originalMutation = originalMutation;
        this.currentMutation = currentMutation;
    }
    MutationInterceptorMessage() {
        this.originalMutation = null;
        this.currentMutation = null;
    }
}

