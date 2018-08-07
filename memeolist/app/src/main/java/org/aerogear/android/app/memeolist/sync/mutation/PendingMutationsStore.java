package org.aerogear.android.app.memeolist.sync.mutation;

import com.apollographql.apollo.api.Mutation;

import java.util.HashMap;
import java.util.Map;

/**
 * Store for PendingMutations
 */

public class PendingMutationsStore {
  Map<Mutation, MutationInformation> mutationMap = new HashMap<>();

}
