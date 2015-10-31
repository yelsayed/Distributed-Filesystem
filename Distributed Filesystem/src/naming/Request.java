package naming;

public class Request {
	
	private boolean exclusive;
	private DynamicBoolean hasAccess;
	private Thread currThread;
	
	public Request(boolean ex) {
		this.exclusive = ex;
		// At first it does not have access unless the Naming server gives it access
		this.hasAccess = new DynamicBoolean(false);
		this.currThread = null;
	}
	
	public Request(boolean ex, Thread currentThread) {
		this.exclusive = ex;
		// At first it does not have access unless the Naming server gives it access
		this.hasAccess = new DynamicBoolean(false);
		this.currThread = currentThread;
	}

	public void giveAccess() {
		this.hasAccess.setBool(true);
	}
	
	public void removeAccess() {
		this.hasAccess.setBool(false);
	}
	
	public DynamicBoolean hasAccess() {
		return this.hasAccess;
	}
	
	public boolean isExcLock() {
		return this.exclusive;
	}
	
	public Thread getCurrThread() {
		return this.currThread;
	}
	
	@Override
	public boolean equals(Object obj) {
		Request r = (Request) obj;
		System.out.println("this is: " + exclusive + " compared with " +  r.isExcLock());
		return r.isExcLock() == exclusive;
	}
}
