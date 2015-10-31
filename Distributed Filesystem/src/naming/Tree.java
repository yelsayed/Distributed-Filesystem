package naming;

import java.util.ArrayList;

import common.Path;

public abstract class Tree {
	
	private String name;
	private Path pathToHere;
	public int numReaders;
	public int numReads;
	public ArrayList<Request> lockQueue;
	
	public Tree(String name, Path p) {
		this.name = name;
		this.pathToHere = p;
		this.numReaders = 0;
		this.numReads = 0;
		lockQueue = new ArrayList<Request>();
	}
	
	public String getName() {
		return this.name;
	}

	public Path getPath() {
		return this.pathToHere;
	}
	
	/**
	 * @return ArrayList of Requests waiting on this node
	 */
	public ArrayList<Request> getLockQueue() {
		return this.lockQueue;
	}
	
	public int getNumReaders() {
		return this.numReaders;
	}
	
	public void addToNumReaders(int i) {
		this.numReaders += i;
	}
	
	/**
	 * Checks if there is any exclusive lock waiting to get access at this node
	 * @return true if there are any exclusive locks waiting on this node
	 * 			false otherwise
	 */
	public boolean writePending() {
		for(Request L : lockQueue) {
			if (L.isExcLock()) {
				return true;
			}
		}
		return false;
	}
	
	public void handleRequests() {
		while (lockQueue.size() > 0) {
			
			Request head = lockQueue.get(0);
						
			// If the first lock in the queue wants exclusive access
			// Then it should be the case that there are no readers
			// on this node/leaf... otherwise we move on
			if (this.numReaders == 0 && head.isExcLock() == true) {
				// Give him exclusive access and lock this tree
				this.numReaders = -1;
				System.out.println("1");
				head.giveAccess();
				lockQueue.remove(0);
				head.getCurrThread().interrupt();
			}
			
			// This node is locked and the request is exclusive
			else if (this.numReaders != -1 && head.isExcLock() == false) {
				this.numReaders++;
				System.out.println("2");
				head.giveAccess();
				lockQueue.remove(0);
				head.getCurrThread().interrupt();
			}
			// None of the above, just return
			else return;
		}
	}
	
	public abstract boolean isDirectory();

}
