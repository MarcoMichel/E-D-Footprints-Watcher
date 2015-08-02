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
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
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

	/**
	 * Startet die Überwachung eines angegebenen Verzeichnisses.
	 *
	 * @param dir der Pfad des Verzeichnisses, das überwacht werden soll
	 */
	protected void watchDirectoryPath(String dir) {
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
			log.log(Level.SEVERE, "Foldes does not exists.", ioe);
		}

		log.info("Watching path: " + path);

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
						log.fine(watchEvent.kind() + " event received for file " + watchEvent.context());
						onModFile(watchEvent.context().toString());
					} else if (ENTRY_MODIFY == kind) {
						log.fine(watchEvent.kind() + " event received for file " + watchEvent.context());
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
