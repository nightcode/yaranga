package org.nightcode.common.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class DirectoryWatchServiceTest {

  public static final String TARGET = System.getProperty("project.build.directory", "target");

  private final String base = TARGET + "/DirectoryWatchService";
  private final String base2 = TARGET + "/DirectoryWatchService2";

  @Before public void setup() throws IOException {
    tearDown();
    new File(base).mkdirs();
    new File(base2).mkdirs();
  }

  @After public void tearDown() throws IOException {
    deleteDirs(base);
    deleteDirs(base2);
  }

  @Test public void testWatch() throws Exception {
    AtomicInteger counter = new AtomicInteger(0);
    DirectoryWatchService.Listener listener = (base, relative, kind) -> {
      System.out.printf("base: %s, relative: %s, kind: %s\n", base, relative, kind);
      counter.incrementAndGet();
    };

    try (DirectoryWatchService service = new DirectoryWatchService(3)) {
      service.start();
      service.registerListener(listener);
      service.registerPath(base);

      new File(base + "/dir01").mkdir();
      new File(base + "/dir02").mkdir();
      new File(base + "/dir01/file01.txt").createNewFile();
      new File(base + "/dir01/file02.txt").createNewFile();
      new File(base + "/dir02/file01.txt").createNewFile();

      new File(base2 + "/dir03").mkdir();
      new File(base2 + "/dir04").mkdir();
      new File(base2 + "/dir04/file01.txt").createNewFile();

      new File(base2).renameTo(new File(base + "/dir05"));
    }
    Assert.assertEquals(9, counter.get());
  }

  private void deleteDirs(String dirName) throws IOException {
    File file = new File(dirName);
    if (file.exists()) {
      Files.walk(file.toPath())
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }
}
