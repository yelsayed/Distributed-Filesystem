package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Serializable
{
	
	/**
	 * Automatically generated Serial ID.
	 */
	private static final long serialVersionUID = -3348007561138286549L;
	public ArrayList<String> pComps;
	
    /** Creates a new path which represents the root directory. */
    public Path()
    {
        pComps = new ArrayList<String>();
    }

    /** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
    	// Checking for illegal arguments
        if (component.isEmpty() || component.contains(":") || component.contains("/")) {
        	throw new IllegalArgumentException("Illegal arguments in the string path recieved!");
        }

        // Get pComps of path
        ArrayList<String> oldPathComps = path.getPathComponents();
        pComps = new ArrayList<String>();
        
        // Reconstruct old path to current one
        for (String s : oldPathComps){
        	pComps.add(s);
        }
        
        // And now we add the new component
        this.pComps.add(component);        
    }
    
    public ArrayList<String> getPathComponents() {
    	return this.pComps;
    }
    
    public Path(ArrayList<String> components){
    	this.pComps = (ArrayList<String>) components.clone();
    }
    /** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
    	// Checking for illegal Arguments
    	if (path.isEmpty() || path.charAt(0) != '/' || path.contains(":")){
    		throw new IllegalArgumentException("Illegal arguments in the string path recieved!");
    	}
    	
    	// Tokenize the path
    	String[] tokens = path.split("/");
    	pComps = new ArrayList<String>();
    	
    	// Add the tokens to the path
    	for(int i = 0; i < tokens.length;i++){
    		if (!tokens[i].isEmpty()) {    			
    			pComps.add(tokens[i]);
    		}
    	}
    }

    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
        return new ArrayListIterator();
    }
    
    /** Creates an iterator with the same functionalities as Java's iterator, 
     *  with the remove function ... err, well... removed :D */
    class ArrayListIterator implements Iterator<String> {

    	Iterator<String> iter;
    	
    	public ArrayListIterator() {
			iter = pComps.iterator();
		}
    	
		@Override
		public boolean hasNext() {
			return this.iter.hasNext();
		}

		@Override
		public String next() {
			return this.iter.next();
		}
    	
		@Override
		public void remove() {
			throw new UnsupportedOperationException("Remove is removed from this functionality!");
		}
		
    }

    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
    	// Error checking, check if File is a directory or not. And whether it exists or not.
    	if (!directory.exists()) {
    		throw new FileNotFoundException("Cannot find directory!");
    	} else if (!directory.isDirectory()) {
    		throw new IllegalArgumentException("File passed in is not a directory!");
    	}
    	
    	// Create a new list of files for recursion
    	ArrayList<Path> filePaths = new ArrayList<Path>();
    	ArrayList<Path> newPaths = listRec(new Path(), directory, filePaths);
    	
    	// Static array of all files in the system.
    	Path[] files = new Path[0];
    	
    	return newPaths.toArray(files);
    }
    
    public static ArrayList<Path> listRec(Path p, File directory, 
    		ArrayList<Path> paths) throws FileNotFoundException {
    	
    	// Error checking, check if File is a directory or not. And whether it exists or not.
    	if (!directory.exists()) {
    		throw new FileNotFoundException("Cannot find directory!");
    	} else if (!directory.isDirectory()) {
    		throw new IllegalArgumentException("File passed in is not a directory!");
    	}
    	
    	// Static array of all files in the system.
    	File[] files = directory.listFiles();
    	
    	// Go through all the files
    	for (File f : files) {
    		// Base Case: If it's a file then add it to the path
    		if (f.isFile()) {
    			paths.add(new Path(p, f.getName()));
    		} 
    		// Recursive Case: If it's not a file then call the function again and add the files to the path.
    		else if (f.isDirectory()) {
    			listRec(new Path (p, f.getName()), f, paths);
    		}
    	}
    	
    	return paths;
    }

    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
    	// If there are no components then you know this is the root
    	boolean root = this.pComps.isEmpty();
        return root;
    }
    
    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
    	// Error Checking
        if (this.isRoot()){
        	throw new IllegalArgumentException("Illegal Argument, root has no parent!");
        }
        
        // Get a copy of the path components
        ArrayList<String> c = (ArrayList<String>) this.pComps.clone();
        
        // Remove the last component of the copy
        c.remove(this.pComps.size()-1);
        return new Path(c);
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
    	// Error Checking
    	if(this.isRoot()) {
    		throw new IllegalArgumentException("Illegal Argument, root has no last!");
    	} 
    	// Get the last component of this path
    	return this.pComps.get(this.pComps.size()-1);
    }

    /** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
        Iterator<String> otherIterator = other.iterator();
        Iterator<String> thisIterator = this.iterator();
        int equalComps = 0;
        
        if(other.getPathComponents().size() > this.pComps.size()) {
        	return false;
        }
        
        while(otherIterator.hasNext() && otherIterator.next().equals(thisIterator.next())) {
        	equalComps++;
        }
        
        return equalComps == other.getPathComponents().size();
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
    	return new File(root.getPath().concat(this.toString()));
    }

    /** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        ArrayList<String> otherComps = ((Path) other).getPathComponents();
        if (otherComps.size() != this.pComps.size()) {
        	return false;
        } else {
        	for (int i = 0; i < otherComps.size(); i++){
        		if (!otherComps.get(i).equals(this.pComps.get(i))){
        			return false;
        		}
        	}
        }
        return true;
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        return this.pComps.hashCode();
    }

    /** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
     */
    @Override
    public String toString()
    {
    	String finalString = "";
        
        if (this.isRoot()) {
        	finalString += "/";
        	return finalString;
        }
        
        for (String s : pComps){
        	finalString += "/" + s; 
        }
        return finalString;
    }
}
