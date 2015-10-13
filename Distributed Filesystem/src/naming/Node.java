package naming;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import storage.Command;
import storage.Storage;
import common.Path;

public class Node extends Tree {

	public ArrayList<Tree> files;
	
	public Node(String name, Path p) {
		super(name,p);
		this.files = new ArrayList<Tree>();
	}
	
	public Tree extract(Path p) throws FileNotFoundException  {
		Path dummyPath = new Path(p.pComps);
		return extractRec(dummyPath);
	}
	
	public Tree extractRec(Path p) throws FileNotFoundException {
		// Should not get here, if we're here then something went wrong
		if (p.pComps.size() == 0) {
			return this;
		}
		
		String firstComp = p.pComps.get(0);
		
		if (p.pComps.size() == 1) {
			for(Tree t : files) {
				if (t.getName().equals(firstComp)) {
					return t;
				}
			}
		}
		else {
			for (Tree t : this.files) {
				if (t.getName().equals(firstComp) && t.isDirectory()) {
					p.pComps.remove(0);
					return ((Node) t).extractRec(p);
				} else if (t.getName().equals(firstComp) && !t.isDirectory()) {
					throw new FileNotFoundException("Path is incorrect!");
				}
			}
		}
		// Did not find file/directory
		throw new FileNotFoundException("Path does not refer to a file!");
	}
	
	public boolean addRegistration(Path p, Command commandStub, Storage storageStub) {
		if (p.isRoot()) {
			return true;
		}
		Path dummyPath = new Path(p.pComps);
		return addRec(dummyPath, commandStub, storageStub, new Path());
	}
	
	public boolean addRec(Path p, Command commandStub, Storage storageStub, Path pathAcc) {
		
		// If there is no more components in the path then we have added it in the previous stage
		// and thus we return true
		if (p.pComps.size() == 0) {
			return true;
		}
		
		// Else we get the first component and add it to the accumulated path
		String firstComp = p.pComps.get(0);
		pathAcc.pComps.add(firstComp);
		
		if (p.pComps.size() == 1) {
			for (Tree t : files) {
				if (t.getName().equals(firstComp)) {
					return false;
				}
			}
			this.files.add(new Leaf(firstComp, pathAcc, storageStub, commandStub));
			return true;
		} 
		
		// Search in the array for a leaf/node with name == to firstComp
		for (Tree t : files) {
			// If it's a node then recurse
			if (t.getName().equals(firstComp) && t.isDirectory()) {
				// Remove the first component in the path components, add it 
				// to the accumulated path and then recurse
				p.pComps.remove(0);
				return ((Node) t).addRec(p, commandStub, storageStub, pathAcc);
			}
			// If it's a file, return false immediately, this is a duplicate file
			else if (t.getName().equals(firstComp) && !(t.isDirectory())) {
				return false;
			}
		}
		

		// If it doesn't exist then you have to add it recursively and return true
		// Case it's the last thing in the path, and is therefore a Leaf
		
		// Else it's not the last thing, and is therefore a Node
		Node n = new Node(firstComp, pathAcc);
		this.files.add(n);
		p.pComps.remove(0);
		
		return n.addRec(p, commandStub, storageStub,pathAcc);
	}
	
	@Override
	public String toString(){
		String ret = "";
		System.out.println("This is: " + this.getName());
		
		for (Tree t : files) {
			System.out.println(t.getName());
		}
		
		for (Tree t : files) {
			if (t.isDirectory() == true) {
				ret += t.getName() + "/" + t.toString();
			} else {
				ret += t.getName() + "\n";
			}
		}
		return ret;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}
}
