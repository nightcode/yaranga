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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.nio.NioIoHandler;
import io.netty.util.concurrent.EventExecutorGroup;
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
import org.junit.jupiter.api.function.ThrowingSupplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link ApiGwMetrics}.
 */
public class ApiGwMetricsTest {

  private static final ThreadFactory THREAD_FACTORY = Thread::new;

  private static final class StubServerFactory implements BootstrapServerFactory<InetSocketAddress> {

    private final EventLoopGroup acceptorGroup;
    private final EventLoopGroup workerGroup;

    StubServerFactory(EventLoopGroup acceptorGroup, EventLoopGroup workerGroup) {
      this.acceptorGroup = acceptorGroup;
      this.workerGroup   = workerGroup;
    }

    @Override public ServerBootstrap create(String name, int nThreads) {
      return new ServerBootstrap().group(acceptorGroup, workerGroup);
    }

    @Override public InetSocketAddress localAddress() {
      return new InetSocketAddress("127.0.0.1", 0);
    }
  }

  private final List<ApiGw>              gateways = new ArrayList<>();
  private final List<EventExecutorGroup> groups   = new ArrayList<>();

  @AfterEach void tearDown() {
    gateways.forEach(ApiGwMetrics::removeApiGw);
    groups.forEach(g -> g.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS));
  }

  @Test public void testCollectEmptyWhenNoGateways() {
    MetricSnapshots snapshots = ApiGwMetrics.INSTANCE.collect();
    assertEquals(0, snapshots.size());
  }

  @Test public void testCollectWithSingleThreadAcceptorGroup() {
    newApiGw("gw-single-thread-acceptor", new SingleThreadIoEventLoop(null, THREAD_FACTORY, NioIoHandler.newFactory())
        , new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory()));

    MetricSnapshots snapshots = assertDoesNotThrow((ThrowingSupplier<MetricSnapshots>) ApiGwMetrics.INSTANCE::collect);

    assertEquals(1.0, dataPointValue(snapshots, "nc_api_gw_acceptor_executor_count", "gw-single-thread-acceptor"));
    assertEquals(2.0, dataPointValue(snapshots, "nc_api_gw_worker_executor_count", "gw-single-thread-acceptor"));
    assertEquals(0.0, dataPointValue(snapshots, "nc_api_gw_requests", "gw-single-thread-acceptor"));
  }

  @Test public void testCollectWithMultithreadGroups() {
    newApiGw("gw-multithread", new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory())
        , new MultiThreadIoEventLoopGroup(3, NioIoHandler.newFactory()));

    MetricSnapshots snapshots = ApiGwMetrics.INSTANCE.collect();

    assertEquals(1.0, dataPointValue(snapshots, "nc_api_gw_acceptor_executor_count", "gw-multithread"));
    assertEquals(3.0, dataPointValue(snapshots, "nc_api_gw_worker_executor_count", "gw-multithread"));
  }

  @Test public void testPendingTasksReportedForEventLoopGroups() throws Exception {
    MultiThreadIoEventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    newApiGw("gw-pending", new SingleThreadIoEventLoop(null, THREAD_FACTORY, NioIoHandler.newFactory()), workerGroup);

    // block the single worker loop, then queue tasks so they stay pending
    CountDownLatch blockLatch   = new CountDownLatch(1);
    CountDownLatch runningLatch = new CountDownLatch(1);
    workerGroup.next().execute(() -> {
      runningLatch.countDown();
      try {
        blockLatch.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    });
    assertTrue(runningLatch.await(10, TimeUnit.SECONDS));

    int queued = 5;
    for (int i = 0; i < queued; i++) {
      workerGroup.next().execute(() -> {
      });
    }

    try {
      MetricSnapshots snapshots = ApiGwMetrics.INSTANCE.collect();
      double          pending   = dataPointValue(snapshots, "nc_api_gw_worker_pending_tasks", "gw-pending");
      assertTrue(pending >= queued, "expected at least " + queued + " pending tasks but was " + pending);
    } finally {
      blockLatch.countDown();
    }
  }

  @Test public void testRemoveApiGwStopsCollecting() {
    ApiGw apiGw = newApiGw("gw-removed", new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory())
        , new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory()));

    assertNotNull(findDataPoint(ApiGwMetrics.INSTANCE.collect(), "nc_api_gw_requests", "gw-removed"));

    ApiGwMetrics.removeApiGw(apiGw);

    MetricSnapshots snapshots = ApiGwMetrics.INSTANCE.collect();
    if (snapshots.size() > 0) {
      assertNull(findDataPoint(snapshots, "nc_api_gw_requests", "gw-removed"));
    }
  }

  @Test public void testGetPrometheusNames() {
    List<String> names = ApiGwMetrics.INSTANCE.getPrometheusNames();
    assertTrue(names.containsAll(List.of("nc_api_gw_requests", "nc_api_gw_acceptor_executor_count", "nc_api_gw_acceptor_pending_tasks"
        , "nc_api_gw_worker_executor_count", "nc_api_gw_worker_pending_tasks")));
  }

  private ApiGw newApiGw(String name, EventLoopGroup acceptorGroup, EventLoopGroup workerGroup) {
    groups.add(acceptorGroup);
    groups.add(workerGroup);
    ApiGw apiGw = ApiGwBuilder.builder().name(name).bootstrapFactory(new StubServerFactory(acceptorGroup, workerGroup)).buildTcpIpApiGw();
    gateways.add(apiGw);
    return ApiGwMetrics.addApiGw(apiGw);
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
