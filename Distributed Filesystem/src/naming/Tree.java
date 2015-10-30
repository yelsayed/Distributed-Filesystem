package naming;

import java.util.ArrayList;

import common.Path;

public abstract class Tree {
	
	private String name;
	private Path pathToHere;
	private int numReaders;
	private int numReads;
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
		return this.getNumReaders();
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
	
	public abstract boolean isDirectory();

}
