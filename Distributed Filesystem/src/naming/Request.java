package naming;

public class Request {
	
	private boolean exclusive;
	private boolean hasAccess;
	private Thread currThread;
	
	public Request(boolean ex) {
		this.exclusive = ex;
		// At first it does not have access unless the Naming server gives it access
		this.hasAccess = false;
		this.currThread = null;
	}
	
	public Request(boolean ex, Thread currentThread) {
		this.exclusive = ex;
		// At first it does not have access unless the Naming server gives it access
		this.hasAccess = false;
		this.currThread = currentThread;
	}

	public void giveAccess() {
		this.hasAccess = true;
	}
	
	public void removeAccess() {
		this.hasAccess = false;
	}
	
	public boolean hasAccess() {
		return this.hasAccess;
	}
	
	public boolean isExcLock() {
		return this.exclusive;
	}
	
	public Thread getCurrThread() {
		return this.currThread;
	}
	
}
