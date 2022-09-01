/*
 * Copyright 2020 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.connect.kafka.config.sink;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.connect.kafka.handler.sink.N1qlSinkHandler;
import com.couchbase.connect.kafka.handler.sink.SinkHandler;
import com.couchbase.connect.kafka.handler.sink.SubDocumentSinkHandler;
import com.couchbase.connect.kafka.util.ScopeAndCollection;
import com.couchbase.connect.kafka.util.TopicMap;
import com.couchbase.connect.kafka.util.config.annotation.Default;
import org.apache.kafka.common.config.ConfigDef;

import java.time.Duration;
import java.util.List;

import static com.couchbase.connect.kafka.util.config.ConfigHelper.validate;

public interface SinkBehaviorConfig {

  /**
   * Qualified name (scope.collection) of the destination collection for messages
   * from topics that don't have an entry in the `couchbase.topic.to.collection` map.
   */
  @Default("_default._default")
  String defaultCollection();

  @SuppressWarnings("unused")
  static ConfigDef.Validator defaultCollectionValidator() {
    return validate(ScopeAndCollection::parse, "A collection name qualified by a scope name (scope.collection)");
  }

  /**
   * A map from Kafka topic to Couchbase collection.
   * <p>
   * Topic and collection are joined by an equals sign.
   * Map entries are delimited by commas.
   * <p>
   * For example, if you want to write messages from topic "topic1"
   * to collection "scope-a.invoices", and messages from topic "topic2"
   * to collection "scope-a.widgets", you would write:
   * "topic1=scope-a.invoices,topic2=scope-a.widgets".
   * <p>
   * Defaults to an empty map, with all documents going to the collection
   * specified by `couchbase.default.collection`.
   */
  @Default
  List<String> topicToCollection();

  @SuppressWarnings("unused")
  static ConfigDef.Validator topicToCollectionValidator() {
    return validate(TopicMap::parseTopicToCollection, "topic=scope.collection,...");
  }

  /**
   * The fully-qualified class name of the sink handler to use.
   * The sink handler determines how the Kafka record is translated into actions on Couchbase documents.
   * <p>
   * The built-in handlers are:
   * `com.couchbase.connect.kafka.handler.sink.UpsertSinkHandler`,
   * `com.couchbase.connect.kafka.handler.sink.N1qlSinkHandler`, and
   * `com.couchbase.connect.kafka.handler.sink.SubDocumentSinkHandler`.
   * <p>
   * You can customize the sink connector's behavior by implementing your own SinkHandler.
   */
  @Default("com.couchbase.connect.kafka.handler.sink.UpsertSinkHandler")
  Class<? extends SinkHandler> sinkHandler();

  /**
   * Overrides the `couchbase.sink.handler` property.
   * <p>
   * A value of `N1QL` forces the handler to `com.couchbase.connect.kafka.handler.sink.N1qlSinkHandler`.
   * A value of `SUBDOCUMENT` forces the handler to `com.couchbase.connect.kafka.handler.sink.SubDocumentSinkHandler`.
   *
   * @deprecated Please set the `couchbase.sink.handler` property instead.
   */
  @Default("DOCUMENT")
  @Deprecated
  DocumentMode documentMode();

  /**
   * Format string to use for the Couchbase document ID (overriding the message key).
   * May refer to document fields via placeholders like ${/path/to/field}
   */
  @Default
  String documentId();

  /**
   * Whether to remove the ID identified by 'couchbase.documentId' from the document before storing in Couchbase.
   */
  @Default("false")
  boolean removeDocumentId();

  /**
   * Document expiration time specified as an integer followed by a time unit (s = seconds, m = minutes, h = hours, d = days).
   * For example, to have documents expire after 30 minutes, set this value to "30m".
   * <p>
   * A value of "0" (the default) means documents never expire.
   */
  @Default("0")
  Duration documentExpiration();

  /**
   * Retry failed writes to Couchbase until this deadline is reached.
   * If time runs out, the connector terminates.
   * <p>
   * A value of `0` (the default) means the connector will terminate
   * immediately when a write fails.
   * <p>
   * NOTE: This retry timeout is distinct from the KV timeout (which you can set
   * via `couchbase.env.*`). The KV timeout affects an individual write attempt,
   * while the retry timeout spans multiple attempts and makes the connector
   * resilient to more kinds of transient failures.
   * <p>
   * TIP: Try not to confuse this with the Kafka Connect framework's built-in
   * `errors.retry.timeout` config property, which applies only to failures occurring
   * _before_ the framework delivers the record to the Couchbase connector.
   *
   * @since 4.1.4
   */
  @Default("0")
  @Stability.Uncommitted
  Duration retryTimeout();

  /**
   * @deprecated in favor of using the `couchbase.sink.handler` config property
   * to specify the sink handler.
   */
  @Deprecated
  enum DocumentMode {
    /**
     * Honors the sink handler specified by the `couchbase.sink.handler`
     * config property.
     */
    DOCUMENT,

    /**
     * Forces the sink handler to be {@link SubDocumentSinkHandler}.
     */
    SUBDOCUMENT,

    /**
     * Forces the sink handler to be {@link N1qlSinkHandler}.
     */
    N1QL;
  }
}
