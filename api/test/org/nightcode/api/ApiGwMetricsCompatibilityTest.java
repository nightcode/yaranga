/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nightcode.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.unix.DomainSocketAddress;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import org.nightcode.net.BootstrapServerFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ApiGwMetricsCompatibilityTest {

  private final List<ApiGw> gateways = new ArrayList<>();

  @AfterEach void tearDown() {
    for (ApiGw apiGw : gateways) {
      ApiGwMetrics.removeApiGw(apiGw);
      apiGw.acceptorGroup().shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
      apiGw.workerGroup().shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
    }
    gateways.clear();
  }

  @Test public void testCollectWithTcpIpServerFactory() {
    ApiGw apiGw = newApiGw("test-metrics-tcp"
        , BootstrapServerFactory.tcpIpServerFactory(freeLocalAddress()));

    MetricSnapshots snapshots = assertDoesNotThrow(() -> ApiGwMetrics.INSTANCE.collect()
        , "collect() must be compatible with TcpIpServerFactory group types");

    assertGatewayMetrics(snapshots, apiGw.name());
  }

  @Test public void testCollectWithUnixSocketServerFactory() {
    assumeTrue(Epoll.isAvailable() || KQueue.isAvailable()
        , "netty native transport (Epoll/KQueue) is required for Unix Domain Socket");

    ApiGw apiGw = newApiGw("test-metrics-unix"
        , BootstrapServerFactory.unixSocketServerFactory(new DomainSocketAddress(tempSocketPath())));

    MetricSnapshots snapshots = assertDoesNotThrow(() -> ApiGwMetrics.INSTANCE.collect()
        , "collect() must be compatible with UnixSocketServerFactory group types");

    assertGatewayMetrics(snapshots, apiGw.name());
  }

  @Test public void testCollectWithStartedTcpIpGateway() throws Exception {
    try (ApiGw apiGw = ApiGwBuilder.builder()
        .name("test-metrics-tcp-started")
        .bootstrapFactory(BootstrapServerFactory.tcpIpServerFactory(freeLocalAddress()))
        .buildTcpIpApiGw()) {
      ApiGwMetrics.addApiGw(apiGw);
      try {
        apiGw.startAsync().get();

        MetricSnapshots snapshots = assertDoesNotThrow(() -> ApiGwMetrics.INSTANCE.collect()
            , "collect() must be compatible with a running TcpIpServerFactory-based gateway");

        assertGatewayMetrics(snapshots, apiGw.name());
      } finally {
        ApiGwMetrics.removeApiGw(apiGw);
      }
    }
  }

  private ApiGw newApiGw(String name, BootstrapServerFactory<?> serverFactory) {
    ApiGw apiGw = ApiGwBuilder.builder()
        .name(name)
        .bootstrapFactory(serverFactory)
        .buildTcpIpApiGw();
    gateways.add(apiGw);
    return ApiGwMetrics.addApiGw(apiGw);
  }

  private void assertGatewayMetrics(MetricSnapshots snapshots, String apiGwName) {
    int nThreads = Runtime.getRuntime().availableProcessors();

    assertEquals(0.0, dataPointValue(snapshots, "nc_api_gw_requests", apiGwName));

    // both factories create a single-threaded acceptor and an nThreads worker group
    assertEquals(1.0, dataPointValue(snapshots, "nc_api_gw_acceptor_executor_count", apiGwName));
    assertEquals((double) nThreads, dataPointValue(snapshots, "nc_api_gw_worker_executor_count", apiGwName));

    // executors of both group types are SingleThreadEventLoop, so pending tasks must be reported
    assertTrue(dataPointValue(snapshots, "nc_api_gw_acceptor_pending_tasks", apiGwName) >= 0.0);
    assertTrue(dataPointValue(snapshots, "nc_api_gw_worker_pending_tasks", apiGwName) >= 0.0);
  }

  private static InetSocketAddress freeLocalAddress() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return new InetSocketAddress("127.0.0.1", socket.getLocalPort());
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static String tempSocketPath() {
    Path path = Path.of(System.getProperty("java.io.tmpdir")
        , "nc-gw-" + UUID.randomUUID().toString().substring(0, 8) + ".sock");
    try {
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    path.toFile().deleteOnExit();
    return path.toString();
  }

  private static double dataPointValue(MetricSnapshots snapshots, String metricName, String apiGwLabel) {
    DataPointSnapshot dataPoint = findDataPoint(snapshots, metricName, apiGwLabel);
    assertNotNull(dataPoint, "missing data point " + metricName + "{api_gw=\"" + apiGwLabel + "\"}");
    if (dataPoint instanceof CounterDataPointSnapshot counter) {
      return counter.getValue();
    }
    if (dataPoint instanceof GaugeDataPointSnapshot gauge) {
      return gauge.getValue();
    }
    throw new AssertionError("unexpected data point type: " + dataPoint.getClass());
  }

  private static DataPointSnapshot findDataPoint(MetricSnapshots snapshots, String metricName, String apiGwLabel) {
    for (MetricSnapshot snapshot : snapshots) {
      if (!metricName.equals(snapshot.getMetadata().getName())) {
        continue;
      }
      List<? extends DataPointSnapshot> dataPoints;
      if (snapshot instanceof CounterSnapshot counter) {
        dataPoints = counter.getDataPoints();
      } else if (snapshot instanceof GaugeSnapshot gauge) {
        dataPoints = gauge.getDataPoints();
      } else {
        continue;
      }
      for (DataPointSnapshot dataPoint : dataPoints) {
        if (apiGwLabel.equals(dataPoint.getLabels().get("api_gw"))) {
          return dataPoint;
        }
      }
    }
    return null;
  }
}
