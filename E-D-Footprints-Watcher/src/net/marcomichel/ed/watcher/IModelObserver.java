package net.marcomichel.ed.watcher;

public interface IModelObserver {

	public void addMessage(String msg);
	public void onSystemChange(String currentSystem);
}
