package org.aerogear.android.app.memeolist.sync.cache;

import android.content.Context;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Helper used to create projection cache
 */

public class ProjectionCacheHelper {

  private final static String DATABASE_NAME = "aerogear_sync";


  /**
   * Create cache factory used to store local projection of the data.
   *
   * @param context
   */
  public static NormalizedCacheFactory createFactory(Context context) {
    //Create the ApolloSqlHelper. Please note that if null is passed in as the name, you will get an in-memory SqlLite database that
    // will not persist across restarts of the app.

    ApolloSqlHelper apolloSqlHelper = ApolloSqlHelper.create(context, DATABASE_NAME);

    //Create NormalizedCacheFactory
    NormalizedCacheFactory cacheFactory = new SqlNormalizedCacheFactory(apolloSqlHelper);
    return cacheFactory;
  }

  /**
   * Create cache key resolver - field that will be used as id for caching purposes
   */
  public static CacheKeyResolver createDefaultResolver() {
    //Create the cache key resolver, this example works well when all types have globally unique ids.
    CacheKeyResolver resolver = new CacheKeyResolver() {
      @NotNull
      @Override
      public CacheKey fromFieldRecordSet(@NotNull ResponseField field, @NotNull Map<String, Object> recordSet) {
        // Change to _id for server
        return formatCacheKey((String) recordSet.get("id"));
      }

      @NotNull
      @Override
      public CacheKey fromFieldArguments(@NotNull ResponseField field, @NotNull Operation.Variables variables) {
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
    return resolver;
  }
}
