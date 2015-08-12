package net.marcomichel.ed.watcher;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstrakte Klasse um Verzeichnisse auf Veränderugnen zu überwachen.<p>
 *
 * Bei Create und Modify Events wird die Methode onModFile aufgerufen, die von abgeleiteten Klassen
 * implementiert werden muss.
 *
 * @author Marco Michel
 *
 */
public abstract class DirectoryWatcher {

	private static final Logger log = Logger.getLogger(DirectoryWatcher.class
			.getName());

	private Path newestFile = null;
	private Thread touchThread;

	protected void stopWatchDirectory() {
		if (touchThread != null) {
			touchThread.interrupt();
		}
	}

	/**
	 * Startet die Überwachung eines angegebenen Verzeichnisses.
	 *
	 * @param dir der Pfad des Verzeichnisses, das überwacht werden soll
	 */
	protected void startWatchDirectory(String dir, String fileBaseName, int scanIntervall) {
		Path path = Paths.get(dir);
		// Sanity check - Check if path is a folder
		try {
			Boolean isFolder = (Boolean) Files.getAttribute(path,
					"basic:isDirectory", NOFOLLOW_LINKS);
			if (!isFolder) {
				throw new IllegalArgumentException("Path: " + path
						+ " is not a folder");
			}
		} catch (IOException ioe) {
			// Folder does not exists
			log.log(Level.SEVERE, "Folder does not exists.", ioe);
		}

		log.info("Watching path: " + path);

		// this is a hack!
		// WatchService does not receive any modify events when the game is running
		// and has focus. Must touch the actual log-file frequently to get modify event
		Runnable runnable = () -> {
			try {
				while(true) {
					if (newestFile != null) {
						log.finest("touching file " + newestFile);
						long timestamp = System.currentTimeMillis();
						FileTime ft = FileTime.fromMillis(timestamp);
			            Files.setLastModifiedTime(newestFile, ft);
					}
		            TimeUnit.SECONDS.sleep(scanIntervall);
				}
			} catch (Exception e) {
				log.warning(e.toString());
			}
		};
		touchThread = new Thread(runnable);
		touchThread.start();

		// We obtain the file system of the Path
		FileSystem fs = path.getFileSystem();

		// We create the new WatchService using the new try() block
		try (WatchService service = fs.newWatchService()) {

			// We register the path to the service
			// We watch for creation events
			path.register(service, ENTRY_CREATE, ENTRY_MODIFY);

			// Start the infinite polling loop
			WatchKey key = null;
			while (true) {
				key = service.take();

				// Dequeueing events
				Kind<?> kind = null;
				for (WatchEvent<?> watchEvent : key.pollEvents()) {
					// Get the type of the event
					kind = watchEvent.kind();
					if (OVERFLOW == kind) {
						continue; // loop
					} else if (ENTRY_CREATE == kind) {
						// when a file is created, look if it matches the pattern for a log-file
						// and store the file for the touchThread to touch it frequently
						final String file = watchEvent.context().toString();
						log.fine(watchEvent.kind() + " event received for file " + file);
						if (file.startsWith(fileBaseName)) {
							final StringBuilder sb = new StringBuilder();
							sb.append(dir).append("/").append(file);
							newestFile = Paths.get(sb.toString());
							log.finer("Storing file as newest: " + newestFile);
						}
						onModFile(file);
					} else if (ENTRY_MODIFY == kind) {
						log.finer(watchEvent.kind() + " event received for file " + watchEvent.context());
						onModFile(watchEvent.context().toString());
					}
				}

				if (!key.reset()) {
					break; // loop
				}
			}

		} catch (IOException ioe) {
			log.log(Level.SEVERE, "Error watching path", ioe);
		} catch (InterruptedException ie) {
			log.log(Level.WARNING, "Directory-Watching has been interrupted");
		}
	}

	/**
	 * Methode wird aufgerufen, wenn sich in einem Verzeichnis etwas verändert hat.
	 *
	 * @param file Das File, das sich geändert hat
	 */
	protected abstract void onModFile(String file);
}
