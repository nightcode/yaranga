package org.nightcode.common.io;

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.util.logging.Log;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * todo.
 */
@Beta
public class DirectoryWatchService implements AutoCloseable {

  /**
   * Type of event.
   */
  public enum Kind {
    BOOTSTRAP,
    CREATE,
    MODIFY,
    REMOVE
  }

  /**
   *  A directory event listener.
   */
  public interface Listener {
    void onEvent(String base, String relative, Kind kind);
  }

  private static final class WatchPath {
    private final String base;
    private final String relative;

    private WatchPath(String base) {
      this.base = base;
      this.relative = "";
    }

    private WatchPath(String base, String resolved) {
      this.base = base;
      this.relative = (base.equals(resolved)) ? "" : resolved.substring(base.length() + 1);
    }
  }

  private static final int DEFAULT_MAX_DEPTH = 1;

  private static final WatchEvent.Kind<?>[] EVENT_KINDS
      = new WatchEvent.Kind[] {ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE};

  private final WatchService ws;
  private final int maxDepth;

  private final Thread thread = new Thread(this::run, "DirectoryWatchService.thread");
  private volatile boolean running = true;

  private final Map<WatchKey, WatchPath> watchKeys = new ConcurrentHashMap<>();

  private final BlockingQueue<WatchPath> pathsToRegister = new LinkedBlockingQueue<>();
  private final BlockingQueue<Listener> listenersToRegister = new LinkedBlockingQueue<>();

  private final Set<WatchKey> watchKeysToDeregister = new CopyOnWriteArraySet<>();

  private final List<Listener> listeners = new ArrayList<>();

  public DirectoryWatchService() throws IOException {
    this(DEFAULT_MAX_DEPTH);
  }

  public DirectoryWatchService(int maxDepth) throws IOException {
    this.maxDepth = maxDepth;
    ws = FileSystems.getDefault().newWatchService();
  }

  @Override public void close() throws Exception {
    running = false;
    thread.interrupt();
    try {
      thread.join(500);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    ws.close();
  }

  public void registerListener(Listener listener) {
    listenersToRegister.add(listener);
    thread.interrupt();
  }

  public void registerPath(String path) {
    pathsToRegister.add(new WatchPath(path));
    thread.interrupt();
  }

  public void deregisterPath(String path) {
    watchKeys.keySet().stream()
        .filter(wk -> {
          WatchPath watchPath = watchKeys.get(wk);
          String fullName = watchPath.base + (watchPath.relative.isEmpty() ? "" : "/" + watchPath.relative);
          return fullName.equals(path) || fullName.startsWith(path + "/");
        }).forEach(wk -> {
          watchKeysToDeregister.add(wk);
          wk.cancel();
        });
  }

  public void start() {
    thread.start();
  }

  private void addPath(String base, String relative) {
    Path resolved = Paths.get(base).resolve(relative);
    if (Files.isDirectory(resolved)) {
      try (Stream<Path> stream = Files.walk(resolved, maxDepth, FileVisitOption.FOLLOW_LINKS)) {
        stream.forEach(candidate -> addPath0(base, candidate));
      } catch (IOException ex) {
        Log.warn().log(getClass(), ex, "unable to walk directory '{}'", resolved);
      }
      bootstrapPath(listeners, base, resolved);
    }
  }

  private void addPath0(String base, Path resolved) {
    if (Files.isDirectory(resolved)) {
      try {
        WatchKey watchKey = resolved.register(ws, EVENT_KINDS);
        watchKeys.put(watchKey, new WatchPath(base, resolved.toString()));
      } catch (IOException ex) {
        Log.warn().log(getClass(), ex, "unable to add directory '{}'", resolved);
      }
    }
  }

  private void bootstrap(Collection<Listener> listeners) {
    for (WatchPath pathInfo : watchKeys.values()) {
      Path resolved = Paths.get(pathInfo.base).resolve(pathInfo.relative);
      bootstrapPath(listeners, pathInfo.base, resolved);
    }
  }

  private void bootstrapPath(Collection<Listener> listeners, String base, Path resolved) {
    try (Stream<Path> stream = Files.walk(resolved, maxDepth, FileVisitOption.FOLLOW_LINKS)) {
      stream.forEach(candidate -> {
        if (resolved.equals(candidate)) {
          return;
        }
        for (Listener listener : listeners) {
          String filename = candidate.toString().substring(base.length() + 1);
          listener.onEvent(base, filename, Kind.BOOTSTRAP);
        }
      });
    } catch (IOException ex) {
      Log.warn().log(getClass(), ex, "unable to bootstrap listener with directory '{}'", resolved);
    }
  }

  private void notifyListeners(String base, String relative, Kind kind) {
    for (Listener listener : listeners) {
      listener.onEvent(base, relative, kind);
    }
  }

  private void run() {
    WatchKey watchKey;
    while (running) {
      try {
        Set<Listener> listenerSet = new HashSet<>();
        listenersToRegister.drainTo(listenerSet);
        if (!listenerSet.isEmpty()) {
          bootstrap(listenerSet);
          listeners.addAll(listenerSet);
        }
        Set<WatchPath> watchPathSet = new HashSet<>();
        pathsToRegister.drainTo(watchPathSet);
        if (!watchPathSet.isEmpty()) {
          watchPathSet.forEach(p -> addPath(p.base, p.relative));
        }

        watchKey = ws.take();

        if (watchKey == null) {
          break;
        }

        WatchPath pathInfo = watchKeys.get(watchKey);
        for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
          if (watchEvent.kind() == OVERFLOW) {
            Log.warn().log(getClass(), "OVERFLOW event for {}"
                , pathInfo.base + (pathInfo.relative.isEmpty() ? "" : "/" + pathInfo.relative));
            bootstrap(listeners);
            continue;
          }

          @SuppressWarnings("unchecked")
          WatchEvent<Path> pathEvent = (WatchEvent<Path>) watchEvent;
          String relative = (pathInfo.relative.isEmpty() ? "" : pathInfo.relative + "/")
              + pathEvent.context().toString();
          if (pathEvent.kind() == ENTRY_CREATE) {
            notifyListeners(pathInfo.base, relative, Kind.CREATE);
            addPath(pathInfo.base, relative);
          } else if (pathEvent.kind() == ENTRY_MODIFY) {
            notifyListeners(pathInfo.base, relative, Kind.MODIFY);
          } else if (pathEvent.kind() == ENTRY_DELETE) {
            notifyListeners(pathInfo.base, relative, Kind.REMOVE);
            deregisterPath(pathInfo.base + "/" + relative);
          }
        }
        watchKey.reset();

        if (watchKeysToDeregister.contains(watchKey)) {
          watchKeys.remove(watchKey);
          watchKeysToDeregister.remove(watchKey);
        }
      } catch (InterruptedException ignore) {
        // do nothing
      }
    }
  }
}
