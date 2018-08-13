package org.kgrid.shelf.repository;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemCDOWatcher implements Runnable, AutoCloseable {

  private final WatchService watchService;
  private final Map<WatchKey, Path> watchKeys = new HashMap<>();
  private final List<FileListener> fileListeners = new CopyOnWriteArrayList<>();
  private final Logger log = LoggerFactory.getLogger(FedoraCDOStore.class);
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final Lock readLock = lock.readLock();
  private final Lock writeLock = lock.writeLock();
  private final AtomicBoolean running = new AtomicBoolean(false);

  public FilesystemCDOWatcher() throws IOException {
    watchService = FileSystems.getDefault().newWatchService();
  }

  @Override
  public void run() {
    if (running.compareAndSet(false, true)) {
      long now = System.currentTimeMillis();
      while(running.get()) {
        WatchKey key;
        try {
          key = watchService.take();
        } catch (InterruptedException e) {
          break;
        }
        if(key.isValid()) {
          readLock.lock();
          try {
            for(WatchEvent event : key.pollEvents()) {
              WatchEvent.Kind kind = event.kind();
              if(kind == OVERFLOW) {
                continue;
              }
              Path name = (Path)event.context();
              Path path = watchKeys.get(key).resolve(name);
              if((System.currentTimeMillis() - now ) > 100 && !name.toString().startsWith(".")) {
                now = System.currentTimeMillis();
                fireOnEvent(path, event.kind());
              }
              if (event.kind() == ENTRY_CREATE) {
                try {
                  if (Files.isDirectory(path, NOFOLLOW_LINKS)) {
                    readLock.unlock();
                    registerAll(path);
                  }
                } catch (IOException x) {
                  log.warn(x.getMessage());
                }
              }
            }
          } finally {
            if(readLock.tryLock())
              readLock.unlock();
          }
          if(!key.reset()) {
            break;
          }
        }
      }
      running.set(false);
    }
  }

  private void registerAll(Path path) throws IOException {
    registerAll(path, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
  }

  public void registerAll(Path path, WatchEvent.Kind... eventKinds)
      throws IOException {
    Files.walkFileTree(path, EnumSet.of(FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if(registerPath(dir, eventKinds))
          return FileVisitResult.CONTINUE;
        else
          return FileVisitResult.TERMINATE;
      }
    });
  }

  public boolean registerPath(Path path, WatchEvent.Kind... eventKinds) {
    try {
      if(path != null) {
        watchKeys.put(path.register(watchService, eventKinds), path);
        return true;
      }
    } catch (IOException ex ) {
      log.error(ex.getMessage());
    }
    return false;
  }

  public void addFileListener(FileListener fileListener) {
    fileListeners.add(fileListener);
  }

  public void removeFileListener(FileListener fileListener) {
    fileListeners.remove(fileListener);
  }

  private void fireOnEvent(Path path, WatchEvent.Kind eventKind) {
    for(FileListener fileListener : fileListeners) {
      fileListener.onEvent(path, eventKind);
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  @Override
  public void close() throws IOException {
    running.set(false);
    writeLock.lock();
    try {
      watchService.close();
    } finally {
      writeLock.unlock();
    }
  }

}
