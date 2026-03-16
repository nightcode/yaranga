# Yaranga 

[![Build Status](https://github.com/nightcode/yaranga/actions/workflows/maven.yml/badge.svg)](https://github.com/nightcode/yaranga/actions/workflows/maven.yml)
[![GitHub license](https://img.shields.io/github/license/nightcode/yaranga.svg)](https://github.com/nightcode/yaranga/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/org.nightcode.yaranga/yaranga.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3Aorg.nightcode.yaranga%20a%3Ayaranga)

Useful stuff for Java 21+.

#### Available options

| Name                                                       | Possible values                 | Default value                                                                 |
|------------------------------------------------------------|---------------------------------|-------------------------------------------------------------------------------|
| org.nightcode.config.useEnv                                | true, false                     | true                                                                          |
| org.nightcode.terminal.SIGTERM                             | true, false                     | true                                                                          |
| org.nightcode.trace.disable                                | true, false                     | false                                                                         |
| org.nightcode.trace.service.name                           | string                          | pid: {pid}                                                                    |
| org.nightcode.trace.service.version                        | string                          | unknown                                                                       |
| org.nightcode.trace.sampling.ratio                         | [0.0f, 1.0f]                    | 0.01f                                                                         |
| org.nightcode.trace.SpanExporterProvider                   | provider's class name           | org.nightcode.common.trace.opentelemetry.exporter.LoggingSpanExporterProvider |
| org.nightcode.trace.batch.maxQueueSize                     | [0, Integer.MAX_VALUE]          | 2048                                                                          |
| org.nightcode.trace.batch.maxExportBatchSize               | [0, Integer.MAX_VALUE]          | 512                                                                           |
| org.nightcode.trace.batch.scheduleDelayMs                  | [0, Long.MAX_VALUE]             | 5000                                                                          |
| org.nightcode.trace.batch.exporterTimeoutMs                | [0, Long.MAX_VALUE]             | 30000                                                                         |
| org.nightcode.trace.exporter.LoggingSpanExporter.level     | TRACE, DEBUG, INFO, WARN, ERROR | INFO                                                                          |
| org.nightcode.trace.exporter.OtlpSpanExporter.timeoutMs    | [0, Long.MAX_VALUE]             | 10000                                                                         |
| org.nightcode.trace.exporter.OtlpSpanExporter.protocol     | HTTP, GRPC                      | HTTP                                                                          |
| org.nightcode.trace.exporter.OtlpSpanExporter.endpoint     | string                          | http://127.0.0.1:4318/v1/traces                                               |
| org.nightcode.trace.exporter.OtlpSpanExporter.staticApiKey | string                          | null                                                                          |
| org.nightcode.net.tcp.{name}.backlog                       | system-dependent                | 1024                                                                          |
| org.nightcode.net.tcp.{name}.reuseAddress                  | true, false                     | true                                                                          |
| org.nightcode.net.tcp.{name}.keepAlive                     | true, false                     | true                                                                          |
| org.nightcode.net.tcp.{name}.tcpNoDelay                    | true, false                     | true                                                                          |
| org.nightcode.net.tcp.{name}.reuseAddress                  | true, false                     | true                                                                          |
| org.nightcode.net.unix.{name}.backlog                      | system-dependent                | 1024                                                                          |
| org.nightcode.api.maxAttempt                               | [0, Integer.MAX_VALUE]          | 3                                                                             |
| org.nightcode.api.http.userAgent                           | string                          | nightcode-api/0.1                                                             |


Download
--------

Download [the latest jar][1] via Maven:
```xml
<dependency>
  <groupId>org.nightcode.yaranga</groupId>
  <artifactId>yaranga</artifactId>
  <version>0.12.3</version>
</dependency>
```

Feedback is welcome. Please don't hesitate to open up a new [github issue](https://github.com/nightcode/yaranga/issues) or simply drop me a line at <dmitry@nightcode.org>.


   [1]: http://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=org.nightcode.yaranga&a=yaranga&v=LATEST
