package org.kgrid.shelf.repository;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public interface FileListener {

  void onEvent(Path path, WatchEvent.Kind eventKind);
}
