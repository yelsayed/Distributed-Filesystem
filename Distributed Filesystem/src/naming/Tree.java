package naming;

import common.Path;

public abstract class Tree {
	
	private String name;
	private Path pathToHere;
	
	public Tree(String name, Path p) {
		this.name = name;
		this.pathToHere = p;
	}
	
	public String getName() {
		return this.name;
	}

	public Path getPath() {
		return this.pathToHere;
	}
	
	public abstract boolean isDirectory();

}
