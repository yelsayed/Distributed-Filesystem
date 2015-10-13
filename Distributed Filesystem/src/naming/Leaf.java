package naming;

import common.Path;

import storage.Command;
import storage.Storage;

public class Leaf extends Tree {
	
	private Storage storageStub;
	private Command commandStub;
	
	public Leaf(String name, Path p, Storage s, Command c) {
		super(name, p);
		this.storageStub = s;
		this.commandStub = c;
	}
	
	public Storage getStorageStub() {
		return this.storageStub;
	}
	
	public Command getCommandStub() {
		return this.commandStub;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}
	
}
