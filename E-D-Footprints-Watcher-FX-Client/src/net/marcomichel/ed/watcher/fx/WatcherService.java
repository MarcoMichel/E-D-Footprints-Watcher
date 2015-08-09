package net.marcomichel.ed.watcher.fx;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.marcomichel.ed.watcher.IModelObserver;
import net.marcomichel.ed.watcher.Watcher;

public class WatcherService extends Service<Void>implements IModelObserver {

	private static final Logger log = Logger.getLogger(WatcherService.class.getName());

	private WatcherMainApp mainApp;
	private Watcher watcher;

	public WatcherService(WatcherMainApp mainApp) throws IOException {
		super();
		this.mainApp = mainApp;
		this.watcher = new Watcher(mainApp.getUserConfigFile(), this);
	}

	@Override
	protected Task<Void> createTask() {
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				log.info("starting watcher");
				watcher.watch();
				return null;
			}
		};
	}

	@Override
	public void addMessage(String msg) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				mainApp.getMessageData().add(msg);
			}
		});

	}

	@Override
	public void onSystemChange(String currentSystem) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				mainApp.getSettings().setCurrentSystem(currentSystem);
			}
		});
	}

	public Properties getWatcherConfig() {
		return watcher.getConfig();
	}

	public Task<Void> startRegistration() {
		Task<Void> task = new Task<Void>() {
			@Override protected Void call() {
				watcher.startRegistration();
				return null;
			}
		};

		Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        return task;
	}

	public Task<Void> setVerboseLogging() {
		Task<Void> task = new Task<Void>() {
			@Override protected Void call() throws IOException {
				watcher.setVerboseLogging();
				return null;
			}
		};

		Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        return task;
	}

	public Task<Void> publishOnline() {
		Task<Void> task = new Task<Void>() {
			@Override protected Void call() throws IOException {
				watcher.publishOnline();
				return null;
			}
		};

		Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        return task;
	}

	public void publishOffline() {
		watcher.publishOffline();
	}
}
