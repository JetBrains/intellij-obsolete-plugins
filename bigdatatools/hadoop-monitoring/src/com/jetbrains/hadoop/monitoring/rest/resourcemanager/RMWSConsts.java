/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.hadoop.monitoring.rest.resourcemanager;

/**
 * Constants for {@code RMWebServiceProtocol}.
 */
public final class RMWSConsts {

  public static final String EMPTY = "";
  public static final String ANY = "*";

  public static final String FORWARDED_FOR = "X-Forwarded-For";

  // ----------------Paths for RMWebServiceProtocol----------------

  /**
   * Path for {@code RMWebServiceProtocol}.
   */
  public static final String RM_WEB_SERVICE_PATH = "/ws/v1/cluster";

  /**
   * Path for {@code RMWebServiceProtocol#getClusterInfo}.
   */
  public static final String INFO = RM_WEB_SERVICE_PATH + "/info";

  /**
   * Path for {@code RMWebServiceProtocol#getClusterUserInfo}.
   */
  public static final String CLUSTER_USER_INFO = RM_WEB_SERVICE_PATH + "/userinfo";

  /**
   * Path for {@code RMWebServiceProtocol#getClusterMetricsInfo}.
   */
  public static final String METRICS = RM_WEB_SERVICE_PATH + "/metrics";

  /**
   * Path for {@code RMWebServiceProtocol#getSchedulerInfo}.
   */
  public static final String SCHEDULER = RM_WEB_SERVICE_PATH + "/scheduler";

  /**
   * Path for {@code RMWebServices#updateSchedulerConfiguration}.
   */
  public static final String SCHEDULER_CONF = RM_WEB_SERVICE_PATH + "/scheduler-conf";

  /**
   * Path for {@code RMWebServiceProtocol#dumpSchedulerLogs}.
   */
  public static final String SCHEDULER_LOGS = RM_WEB_SERVICE_PATH + "/scheduler/logs";

  /**
   * Path for {@code RMWebServiceProtocol#getNodes}.
   */
  public static final String NODES = RM_WEB_SERVICE_PATH + "/nodes";

  /**
   * Path for {@code RMWebServiceProtocol#getNode}.
   */
  public static final String NODES_NODEID = RM_WEB_SERVICE_PATH + "/nodes/%s";

  /**
   * Path for {@code RMWebServiceProtocol#getApps} and
   * {@code RMWebServiceProtocol#getApp}.
   */
  public static final String APPS = RM_WEB_SERVICE_PATH + "/apps";

  /**
   * Path for {@code RMWebServiceProtocol#getActivities}.
   */
  public static final String SCHEDULER_ACTIVITIES = RM_WEB_SERVICE_PATH + "/scheduler/activities";

  /**
   * Path for {@code RMWebServiceProtocol#getAppActivities}.
   */
  public static final String SCHEDULER_APP_ACTIVITIES = RM_WEB_SERVICE_PATH + "/scheduler/app-activities";

  /**
   * Path for {@code RMWebServiceProtocol#getAppStatistics}.
   */
  public static final String APP_STATISTICS = RM_WEB_SERVICE_PATH + "/appstatistics";

  /**
   * Path for {@code RMWebServiceProtocol#getApp}.
   */
  public static final String APPS_APPID = RM_WEB_SERVICE_PATH + "/apps/%s";

  /**
   * Path for {@code RMWebServiceProtocol#getAppAttempts}.
   */
  public static final String APPS_APPID_APPATTEMPTS = RM_WEB_SERVICE_PATH + "/apps/%s/appattempts";

  /**
   * Path for {@code WebServices#getAppAttempt}.
   */
  public static final String APPS_APPID_APPATTEMPTS_APPATTEMPTID = RM_WEB_SERVICE_PATH + "/apps/{appid}/appattempts/{appattemptid}";

  /**
   * Path for {@code WebServices#getContainers}.
   */
  public static final String APPS_APPID_APPATTEMPTS_APPATTEMPTID_CONTAINERS = RM_WEB_SERVICE_PATH + "/apps/%s/appattempts/%s/containers";

  /**
   * Path for {@code RMWebServiceProtocol#getNodeToLabels}.
   */
  public static final String GET_NODE_TO_LABELS = RM_WEB_SERVICE_PATH + "/get-node-to-labels";

  /**
   * Path for {@code RMWebServiceProtocol#getLabelsToNodes}.
   */
  public static final String LABEL_MAPPINGS = RM_WEB_SERVICE_PATH + "/label-mappings";

  /**
   * Path for {@code RMWebServiceProtocol#replaceLabelsOnNodes}.
   */
  public static final String REPLACE_NODE_TO_LABELS = RM_WEB_SERVICE_PATH + "/replace-node-to-labels";

  /**
   * Path for {@code RMWebServiceProtocol#replaceLabelsOnNode}.
   */
  public static final String NODES_NODEID_REPLACE_LABELS =
    RM_WEB_SERVICE_PATH + "/nodes/{nodeId}/replace-labels";

  /**
   * Path for {@code RMWebServiceProtocol#getClusterNodeLabels}.
   */
  public static final String GET_NODE_LABELS = RM_WEB_SERVICE_PATH + "/get-node-labels";

  /**
   * Path for {@code RMWebServiceProtocol#addToClusterNodeLabels}.
   */
  public static final String ADD_NODE_LABELS = RM_WEB_SERVICE_PATH + "/add-node-labels";

  /**
   * Path for {@code RMWebServiceProtocol#removeFromCluserNodeLabels}.
   */
  public static final String REMOVE_NODE_LABELS = RM_WEB_SERVICE_PATH + "/remove-node-labels";

  /**
   * Path for {@code RMWebServiceProtocol#getLabelsOnNode}.
   */
  public static final String NODES_NODEID_GETLABELS = RM_WEB_SERVICE_PATH + "/nodes/%s/get-labels";

  /**
   * Path for {@code RMWebServiceProtocol#getAppPriority} and
   * {@code RMWebServiceProtocol#updateApplicationPriority}.
   */
  public static final String APPS_APPID_PRIORITY = RM_WEB_SERVICE_PATH + "/apps/%s/priority";

  /**
   * Path for {@code RMWebServiceProtocol#getAppQueue} and
   * {@code RMWebServiceProtocol#updateAppQueue}.
   */
  public static final String APPS_APPID_QUEUE = RM_WEB_SERVICE_PATH + "/apps/%s/queue";

  /**
   * Path for {@code RMWebServiceProtocol#createNewApplication}.
   */
  public static final String APPS_NEW_APPLICATION = RM_WEB_SERVICE_PATH + "/apps/new-application";

  /**
   * Path for {@code RMWebServiceProtocol#getAppState} and
   * {@code RMWebServiceProtocol#updateAppState}.
   */
  public static final String APPS_APPID_STATE = RM_WEB_SERVICE_PATH + "/apps/%s/state";

  /**
   * Path for {@code RMWebServiceProtocol#postDelegationToken} and
   * {@code RMWebServiceProtocol#cancelDelegationToken}.
   */
  public static final String DELEGATION_TOKEN = RM_WEB_SERVICE_PATH + "/delegation-token";

  /**
   * Path for {@code RMWebServiceProtocol#postDelegationTokenExpiration}.
   */
  public static final String DELEGATION_TOKEN_EXPIRATION = RM_WEB_SERVICE_PATH + "/delegation-token/expiration";

  /**
   * Path for {@code RMWebServiceProtocol#createNewReservation}.
   */
  public static final String RESERVATION_NEW = RM_WEB_SERVICE_PATH + "/reservation/new-reservation";

  /**
   * Path for {@code RMWebServiceProtocol#submitReservation}.
   */
  public static final String RESERVATION_SUBMIT = RM_WEB_SERVICE_PATH + "/reservation/submit";

  /**
   * Path for {@code RMWebServiceProtocol#updateReservation}.
   */
  public static final String RESERVATION_UPDATE = RM_WEB_SERVICE_PATH + "/reservation/update";

  /**
   * Path for {@code RMWebServiceProtocol#deleteReservation}.
   */
  public static final String RESERVATION_DELETE = RM_WEB_SERVICE_PATH + "/reservation/delete";

  /**
   * Path for {@code RMWebServiceProtocol#listReservation}.
   */
  public static final String RESERVATION_LIST = RM_WEB_SERVICE_PATH + "/reservation/list";

  /**
   * Path for {@code RMWebServiceProtocol#getAppTimeout}.
   */
  public static final String APPS_TIMEOUTS_TYPE = RM_WEB_SERVICE_PATH + "/apps/%s/timeouts/%s";

  /**
   * Path for {@code RMWebServiceProtocol#getAppTimeouts}.
   */
  public static final String APPS_TIMEOUTS = RM_WEB_SERVICE_PATH + "/apps/%s/timeouts";

  /**
   * Path for {@code RMWebServiceProtocol#updateApplicationTimeout}.
   */
  public static final String APPS_TIMEOUT = RM_WEB_SERVICE_PATH + "/apps/%s/timeout";

  /**
   * Path for {@code RouterWebServices#getContainer}.
   */
  public static final String GET_CONTAINER = RM_WEB_SERVICE_PATH + "/apps/%s/appattempts/%s/containers/%s";

  /**
   * Path for {code checkUserAccessToQueue#}
   */
  public static final String CHECK_USER_ACCESS_TO_QUEUE = RM_WEB_SERVICE_PATH + "/queues/%s/access";

  // ----------------QueryParams for RMWebServiceProtocol----------------

  public static final String TIME = "time";
  public static final String STATES = "states";
  public static final String NODEID = "nodeId";
  public static final String STATE = "state";
  public static final String FINAL_STATUS = "finalStatus";
  public static final String USER = "user";
  public static final String QUEUE = "queue";
  public static final String QUEUES = "queues";
  public static final String LIMIT = "limit";
  public static final String STARTED_TIME_BEGIN = "startedTimeBegin";
  public static final String STARTED_TIME_END = "startedTimeEnd";
  public static final String FINISHED_TIME_BEGIN = "finishedTimeBegin";
  public static final String FINISHED_TIME_END = "finishedTimeEnd";
  public static final String APPLICATION_TYPES = "applicationTypes";
  public static final String APPLICATION_TAGS = "applicationTags";
  public static final String APP_ID = "appId";
  public static final String MAX_TIME = "maxTime";
  public static final String APPATTEMPTID = "appattemptid";
  public static final String APPID = "appid";
  public static final String LABELS = "labels";
  public static final String RESERVATION_ID = "reservation-id";
  public static final String START_TIME = "start-time";
  public static final String END_TIME = "end-time";
  public static final String INCLUDE_RESOURCE = "include-resource-allocations";
  public static final String TYPE = "type";
  public static final String CONTAINERID = "containerid";
  public static final String APPATTEMPTS = "appattempts";
  public static final String TIMEOUTS = "timeouts";
  public static final String PRIORITY = "priority";
  public static final String TIMEOUT = "timeout";
  public static final String ATTEMPTS = "appattempts";
  public static final String GET_LABELS = "get-labels";
  public static final String DESELECTS = "deSelects";
  public static final String CONTAINERS = "containers";
  public static final String QUEUE_ACL_TYPE = "queue-acl-type";

  private RMWSConsts() {
    // not called
  }

}