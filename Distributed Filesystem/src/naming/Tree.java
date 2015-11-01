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
	
	public abstract boolean isDirectory();

}
