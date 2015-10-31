package storage;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{
	public File root;
	Skeleton<Storage> storageSkeleton;
	Skeleton<Command> commandSkeleton;
	
    /** Creates a storage server, given a directory on the local filesystem.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root)
    {
    	if (root == null) {
    		throw new NullPointerException("Null argument passed in!");
    	}
    	
    	this.root = root;
    	storageSkeleton = new Skeleton<Storage>(Storage.class, this);
    	commandSkeleton = new Skeleton<Command>(Command.class,this);
    	
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
    	// Error checking
    	if(!root.exists() || root.isFile()) {
    		throw new FileNotFoundException("Directory with which the server was"
                    + "created does not exist or is in fact a file");
    	}
    	
    	storageSkeleton.start();
    	commandSkeleton.start();
    	
    	Storage storageStub = (Storage) Stub.create(Storage.class, storageSkeleton, hostname);
    	Command commandStub = (Command) Stub.create(Command.class, commandSkeleton, hostname);
    	
    	// Get the duplicate files from the naming server
    	Path[] dupFiles = naming_server.register(storageStub, commandStub, Path.list(root));
    	
    	// Delete those duplicate files, assuming everything exists on the storage :D
    	for (Path p : dupFiles) {
    		File currentFile = p.toFile(root);
    		File parentFile = new File(currentFile.getParent());
    		currentFile.delete();
    		
    		// Prune the parent file if it's empty
    		while(!parentFile.equals(root)) {
    			if (parentFile.list().length == 0) {
        			parentFile.delete();
        			parentFile =  new File(parentFile.getParent());
        		} else {
        			break;
        		}
    		}
    	}
    	
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        storageSkeleton.stop();
        commandSkeleton.stop();
        this.stopped(null);
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File f = file.toFile(root);
        if (f.isDirectory() || !f.exists()) {
        	throw new FileNotFoundException("File cannot be found or is a directory!");
        }
        return f.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
    	File f = file.toFile(root);
    	
    	// Error Checking
        if (f.isDirectory() || !f.exists()) {
        	throw new FileNotFoundException("File cannot be found or is a directory!");
        }
        
        if (offset < 0 || length < 0 || length + offset > f.length()) {
        	throw new IndexOutOfBoundsException("Offset and length are out of bounds "
        			+ "given the length of the file or they are negative!");
        }
        
        // Learnt a lot from the following link :D
        //http://www.java-examples.com/read-file-byte-array-using-fileinputstream
        InputStream fin = new FileInputStream(f);
    	byte[] fileContent = new byte[length];
    	fin.read(fileContent, ((int) offset), length);
    	
    	if (fin != null) {    		
    		fin.close();
    	}
    	
    	return fileContent;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
    	File f = file.toFile(root);

    	// Error Checking
    	if (f.isDirectory() || !f.exists()) {
        	throw new FileNotFoundException("File cannot be found or is a directory!");
        }
    	
    	if (offset < 0) {
        	throw new IndexOutOfBoundsException("Offset is negative!");
        }
    	
    	// Once again...
    	// http://stackoverflow.com/questions/4350084/byte-to-file-in-java
    	// http://www.tutorialspoint.com/java/io/fileoutputstream_write_byte_len.htm
    	// So you don't think I'm cheating 
    	
    	FileOutputStream fos = new FileOutputStream(f);
    	FileChannel ch = fos.getChannel();
    	ch.position(offset);
    	ch.write(ByteBuffer.wrap(data));
    	
    	if(fos != null) {    		
    		fos.close();
    	}
    	
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
    	File f = file.toFile(root);
    	// Cannot create the root, so return false
        if (file.isRoot()) {
        	return false;
        }
        
        File parentFile = file.parent().toFile(root);

        // Check if the parent is a directory or not, if it's not then make a directory
        if (!parentFile.isDirectory()) { 
        	parentFile.mkdirs();
        }
        
        // Create the file
        try {
			return f.createNewFile();
		} catch (IOException e) {
			return false;
		}
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        File f = path.toFile(root);
        
        // Cannot delete root so return false
        if (path.isRoot()) {
        	return false;
        }
        
        if (f.isFile()) {
        	return f.delete();
        } else {
        	File[] fileList = f.listFiles();
        	
        	if (fileList != null) {
        		for (File f1 : fileList) {
        			if(deleteDir(f1) == false){
        				return false;
        			}
            	}
        	}	
    		return f.delete();
        }
    }
    
    private boolean deleteDir(File f) {
    	File[] fileList = f.listFiles();
    	
    	if (fileList != null) {
    		for (File f1 : fileList) {
    			if(deleteDir(f1) == false){
    				return false;
    			}
        	}
    	}	
		return f.delete();
    }

	@Override
	public boolean copy(Path file, Storage server) throws RMIException,
			FileNotFoundException, IOException {
		File f = file.toFile(root);
		long fileSize = server.size(file);
		byte[] bytesToCopy;
		int readby = Integer.MAX_VALUE;
		
		if (f.exists()) {
			f.delete();
		}
		create(file);
		
		for(long offset = 0; offset < fileSize; offset+=readby) {
			readby = (int) Math.min(readby, fileSize-offset);
			bytesToCopy = server.read(file, offset, readby);
			write(file, offset, bytesToCopy);
		}
		
		return true;
	}
}
