package naming;

import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import rmi.*;
import common.*;
import storage.*;

/**
 * Naming server.
 * 
 * <p>
 * Each instance of the filesystem is centered on a single naming server. The
 * naming server maintains the filesystem directory tree. It does not store any
 * file data - this is done by separate storage servers. The primary purpose of
 * the naming server is to map each file name (path) to the storage server which
 * hosts the file's contents.
 * 
 * <p>
 * The naming server provides two interfaces, <code>Service</code> and
 * <code>Registration</code>, which are accessible through RMI. Storage servers
 * use the <code>Registration</code> interface to inform the naming server of
 * their existence. Clients use the <code>Service</code> interface to perform
 * most filesystem operations. The documentation accompanying these interfaces
 * provides details on the methods supported.
 * 
 * <p>
 * Stubs for accessing the naming server must typically be created by directly
 * specifying the remote network address. To make this possible, the client and
 * registration interfaces are available at well-known ports defined in
 * <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration {
	Skeleton<Service> servSkeleton;
	Skeleton<Registration> regSkeleton;
	Node dirTree;
	public ArrayList<Command> commandStubList;
	public HashMap<Command, Storage> commandStorageMap;
	public HashMap<Storage, Command> storageCommandMap;
	public HashMap<Path, Set<Storage>> pathStorageSetMap;
	public RequestQueue q;

	/**
	 * Creates the naming server object.
	 * 
	 * <p>
	 * The naming server is not started.
	 */
	public NamingServer() {
		InetSocketAddress serviceAdd = new InetSocketAddress(
				NamingStubs.SERVICE_PORT);
		InetSocketAddress regisAdd = new InetSocketAddress(
				NamingStubs.REGISTRATION_PORT);

		this.dirTree = new Node("/", new Path());
		this.commandStubList = new ArrayList<Command>();
		this.commandStorageMap = new HashMap<Command, Storage>();
		this.pathStorageSetMap = new HashMap<Path, Set<Storage>>();
		this.storageCommandMap = new HashMap<Storage, Command>();
		this.q = new RequestQueue();

		servSkeleton = new Skeleton<Service>(Service.class, this, serviceAdd);
		regSkeleton = new Skeleton<Registration>(Registration.class, this,
				regisAdd);

	}

	/**
	 * Starts the naming server.
	 * 
	 * <p>
	 * After this method is called, it is possible to access the client and
	 * registration interfaces of the naming server remotely.
	 * 
	 * @throws RMIException
	 *             If either of the two skeletons, for the client or
	 *             registration server interfaces, could not be started. The
	 *             user should not attempt to start the server again if an
	 *             exception occurs.
	 */
	public synchronized void start() throws RMIException {
		try {
			this.servSkeleton.start();
			this.regSkeleton.start();
		} catch (Exception e) {
		}
	}

	/**
	 * Stops the naming server.
	 * 
	 * <p>
	 * This method waits for both the client and registration interface
	 * skeletons to stop. It attempts to interrupt as many of the threads that
	 * are executing naming server code as possible. After this method is
	 * called, the naming server is no longer accessible remotely. The naming
	 * server should not be restarted.
	 */
	public void stop() {
		this.servSkeleton.stop();
		this.regSkeleton.stop();
		stopped(null);
	}

	/**
	 * Indicates that the server has completely shut down.
	 * 
	 * <p>
	 * This method should be overridden for error reporting and application exit
	 * purposes. The default implementation does nothing.
	 * 
	 * @param cause
	 *            The cause for the shutdown, or <code>null</code> if the
	 *            shutdown was by explicit user request.
	 */
	protected void stopped(Throwable cause) {
	}

	// The following methods are documented in Service.java.
	@Override
	public boolean isDirectory(Path path) throws FileNotFoundException {
		if (path.isRoot()) {
			return true;
		}

		return dirTree.extract(path).isDirectory();
	}

	@Override
	public String[] list(Path directory) throws FileNotFoundException {
		if (!isDirectory(directory)) {
			throw new FileNotFoundException(
					"Given Path does not refer to a directory!");
		}

		ArrayList<Tree> f;

		if (directory.isRoot()) {
			f = this.dirTree.files;
		} else {
			f = ((Node) dirTree.extract(directory)).files;
		}

		String[] dummyArray = new String[0];
		ArrayList<String> retArray = new ArrayList<String>();

		for (Tree t : f) {
			retArray.add(t.getName());
		}

		return retArray.toArray(dummyArray);
	}

	@Override
	public boolean createFile(Path file) throws RMIException,
			FileNotFoundException {
		if (file.isRoot()) {
			return false;
		}
		Path actualPath = file.parent();
		if (!isDirectory(file.parent())) {
			throw new FileNotFoundException(
					"Parent Directory does not exist or is not a directory");
		}

		ArrayList<Tree> f;

		if (actualPath.isRoot()) {
			f = this.dirTree.files;
		} else {
			f = ((Node) dirTree.extract(actualPath)).files;
		}

		for (Tree t : f) {
			if (t.getName().equals(file.last())) {
				return false;
			}
		}

		if (actualPath.isRoot()) {
			Path newPath = new Path(this.dirTree.getPath(), file.last());
			// Give it to a storage using a randomly selected command stub
			Random randomGenerator = new Random();
			int index = randomGenerator.nextInt(commandStubList.size());
			Command randomCommand = commandStubList.get(index);
			Storage randomStorage = this.commandStorageMap.get(randomCommand);

			// Add it to the list of Files in the tree
			this.dirTree.files.add(new Leaf(file.last(), newPath,
					randomStorage, randomCommand));

			// Now create it in the storage server
			randomCommand.create(file);
		} else {
			Node n = (Node) dirTree.extract(actualPath);
			Path newPath = new Path(n.getPath(), file.last());
			// Give it to a storage using a randomly selected command stub
			Random randomGenerator = new Random();
			int index = randomGenerator.nextInt(commandStubList.size());

			Command randomCommand = commandStubList.get(index);
			Storage randomStorage = this.commandStorageMap.get(randomCommand);

			// Add it to the list of Files in the tree
			n.files.add(new Leaf(file.last(), newPath, randomStorage,
					randomCommand));

			// Now create it in the storage server
			randomCommand.create(file);
		}

		return true;
	}

	@Override
	public boolean createDirectory(Path directory)
			throws FileNotFoundException, RMIException {
		if (directory.isRoot()) {
			return false;
		}
		Path actualPath = directory.parent();
		if (!isDirectory(directory.parent())) {
			throw new FileNotFoundException(
					"Parent Directory does not exist or is not a directory");
		}

		ArrayList<Tree> f;

		if (actualPath.isRoot()) {
			f = this.dirTree.files;
		} else {
			f = ((Node) dirTree.extract(actualPath)).files;
		}

		for (Tree t : f) {
			if (t.getName().equals(directory.last())) {
				return false;
			}
		}

		if (actualPath.isRoot()) {
			Path newPath = new Path(this.dirTree.getPath(), directory.last());
			// Give it to a storage using a randomly selected command stub

			// Add it to the list of Files in the tree
			this.dirTree.files.add(new Node(directory.last(), newPath));
		} else {
			Node n = (Node) dirTree.extract(actualPath);
			Path newPath = new Path(n.getPath(), directory.last());

			// Add it to the list of Files in the tree
			n.files.add(new Node(directory.last(), newPath));
		}

		return true;
	}

	@Override
	public boolean delete(Path path) throws FileNotFoundException, RMIException {
		if (path == null) {
			throw new NullPointerException();
		}
		if (path.isRoot()) {
			return false;
		}
		
		// Error checking; will throw an error if it doesn't find it
		Tree parent = this.dirTree.extract(path.parent());
		Tree toDelete = this.dirTree.extract(path);
		
		Set<Storage> deleteSet = pathStorageSetMap.remove(path);
		
		// If it's a file
		if (!toDelete.isDirectory()) {
			for(Storage s : deleteSet) {
				Command cmd = storageCommandMap.get(s);
				cmd.delete(path);
			}
			return ((Node) parent).removeLeaf(path);	
		} else {
			
			Set<Storage> ss;
			
			try {
			// Get the path storage map from the extra stuff that you'll delete
			ss = ((Node) toDelete).removeDir();
			} catch(InterruptedException e) {
				// Something went wrong
				return false;
			}
			for(Storage s : ss) {
				Command cmd = storageCommandMap.get(s);
				cmd.delete(path);
			}
			((Node) parent).files.remove(toDelete);
		}
		
		return true;
			
	}

	@Override
	public Storage getStorage(Path file) throws FileNotFoundException {
		if (this.dirTree.extract(file).isDirectory()) {
			throw new FileNotFoundException("Path referred to a directory!");
		}
		return ((Leaf) this.dirTree.extract(file)).getStorageStub();
	}

	// The method register is documented in Registration.java.
	@Override
	public Path[] register(Storage client_stub, Command command_stub,
			Path[] files) {
		// Error checking
		if (client_stub == null || command_stub == null || files == null) {
			throw new NullPointerException("Null Argument given!");
		}

		// Check if stub is registered or not
		if (this.commandStubList.contains(command_stub)) {
			throw new IllegalStateException("Command Stub already registered!");
		}

		this.commandStubList.add(command_stub);
		this.commandStorageMap.put(command_stub, client_stub);
		this.storageCommandMap.put(client_stub, command_stub);

		ArrayList<Path> duplicatePaths = new ArrayList<Path>();
		Path[] dummyArray = new Path[0];

		for (Path p : files) {
			boolean created = dirTree.addRegistration(p, command_stub,
					client_stub);
			if (created == false) {
				duplicatePaths.add(p);
			} else {
				if (pathStorageSetMap.containsKey(p)) {
					pathStorageSetMap.get(p).add(client_stub);
				} else {
					Set<Storage> ss = new HashSet<Storage>();
					ss.add(client_stub);
					pathStorageSetMap.put(p, ss);
				}
			}

		}

		return duplicatePaths.toArray(dummyArray);
	}

	@Override
	public void lock(Path path, boolean exclusive) throws RMIException,
			FileNotFoundException {
		// Error checking
		if (path == null) {
			throw new NullPointerException("Null Argument given!");
		}

		// This'll throw the FileNotFoundException if it doesn't find the file
		this.dirTree.extract(path);

		// Create a global list for the locks
		ArrayList<DynamicBoolean> lockList = new ArrayList<DynamicBoolean>();

		Tree currNode = this.dirTree;
		// Based on the assumption that the last
		// component is a leaf and the rest are nodes
		String pathAcc = "";
		int readers;
		for (String pComp : path.pComps) {
			// If no one is waiting with an exclusive lock to this Node,
			// and this node is readable then add a lock to the global list

			if (currNode.getPath().isRoot()) {
				readers = this.q.numReaders;
			} else {
				readers = currNode.numReaders;
			}

			if (!this.q.writePending() && readers != -1) {
				DynamicBoolean shallowLock = new DynamicBoolean(true);
				lockList.add(shallowLock);
				// Also add 1 to the number of readers :P
				if (currNode.getPath().isRoot()) {
					this.q.numReaders++;
				} else {
					currNode.numReaders++;
				}
			}
			// Else no one can't read from the currNode, so we make a
			// new lock and add it to the queue of locks
			else {
				Request r = new Request(exclusive, Thread.currentThread());
				this.q.lockQueue.add(r);
				lockList.add(r.hasAccess());
			}

			// Get next node
			pathAcc += "/" + pComp;
			Path currPath = new Path(pathAcc);
			currNode = this.dirTree.extract(currPath);
		}

		if (currNode.getPath().isRoot()) {
			readers = this.q.numReaders;
		} else {
			readers = currNode.numReaders;
		}

		// Now we need to handle the last component of the path.
		// We need to make sure that the reads and the writes are done in order

		// If NO write requests waiting AND lock IS exclusive AND numreaders is
		// 0
		if (readers == 0 && exclusive && !this.q.writePending()) {
			if (currNode.getPath().isRoot()) {
				this.q.numReaders--;
			} else {
				currNode.numReaders--;
			}
			DynamicBoolean shallowLock = new DynamicBoolean(true);
			lockList.add(shallowLock);
		}
		// Else if NO write requests waiting AND lock is NOT exclusive AND
		// numreaders is not -1
		else if (readers != -1 && !exclusive && !this.q.writePending()) {
			if (currNode.getPath().isRoot()) {
				this.q.numReaders++;
			} else {
				currNode.numReaders++;
			}
			DynamicBoolean shallowLock = new DynamicBoolean(true);
			lockList.add(shallowLock);
		} else {
			Request r = new Request(exclusive, Thread.currentThread());
			this.q.lockQueue.add(r);
			lockList.add(r.hasAccess());
		}

		DynamicBoolean fReq = new DynamicBoolean(false);

		// Here we check if we got any non-exclusive locks in our list
		while (lockList.contains(fReq)) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public void unlock(Path path, boolean exclusive) throws RMIException {
		// Error checking
		if (path == null) {
			throw new NullPointerException("Null Argument given!");
		}

		// This'll throw the FileNotFoundException if it doesn't find the file
		try {
			this.dirTree.extract(path);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File not Found!!");
		}

		Set<Storage> storageSet = pathStorageSetMap.get(path);
		Path fileCopy = null;
		Storage chosenStorage = null;

		Tree currNode = this.dirTree;
		String pathAcc = "";
		int readers;

		for (String pComp : path.pComps) {

			// Assuming all the locks on the nodes are non exclusive
			// then treat them like non exclusive locks

			// Subtract one reader from the current node
			if (currNode.getPath().isRoot()) {
				this.q.numReaders--;
			} else {
				currNode.numReaders--;
			}

			// Add One to the readers of this file; this is for replication
			this.q.numReads++;

			if (currNode.getPath().isRoot()) {
				readers = this.q.numReaders;
			} else {
				readers = currNode.numReaders;
			}

			// If we reach a point when no one is reading from this node,
			// then we should handle more requests
			if (readers == 0) {
				this.q.handleRequests();
			}

			// Get next Node
			pathAcc += "/" + pComp;
			Path currPath = new Path(pathAcc);

			try {
				currNode = this.dirTree.extract(currPath);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		if (currNode.getPath().isRoot()) {
			readers = this.q.numReaders;
		} else {
			readers = currNode.numReaders;
		}

		// Unlocking an exclusive lock, reset the number of readers
		// and handle more requests
		if (exclusive) {
			if (currNode.getPath().isRoot()) {
				this.q.numReaders = 0;
			} else {
				currNode.numReaders = 0;
			}
			this.q.handleRequests();
		}
		// Unlocking a non exclusive lock, then do the same thing we did
		// for unlocking the nodes above...
		else if (!exclusive) {
			if (currNode.getPath().isRoot()) {
				this.q.numReaders--;
			} else {
				currNode.numReaders--;
			}
			readers--;
			this.q.numReads++;
			if (readers == 0) {
				this.q.handleRequests();
			}
		}

		if (currNode.getPath() != null && this.q.numReads >= 20) {
			this.q.numReads = 0;
			fileCopy = currNode.getPath();
		}

		// Copying
		if (exclusive && storageSet != null) {

			// Kind of a hack, but it works!
			chosenStorage = storageSet.iterator().next();
			// We remove it because we want to make sure that we don't check it
			// in the next step when.
			storageSet.remove(chosenStorage);

			for (Storage s : storageSet) {
				Command cmd = this.storageCommandMap.get(s);
				cmd.delete(path);
			}

			if (chosenStorage != null) {
				Set<Storage> updatedStorage = new HashSet<Storage>();
				updatedStorage.add(chosenStorage);
				pathStorageSetMap.put(path, updatedStorage);
			} else {
				System.out.println("Chosen Storage is null!!");
			}

		} else if (!exclusive && fileCopy != null) {
			// Kind of a hack, but it works!
			chosenStorage = storageSet.iterator().next();
			for (Storage s : storageCommandMap.keySet()) {
				if (!storageSet.contains(s)) {
					Command cmd = storageCommandMap.get(s);
					try {
						cmd.copy(fileCopy, chosenStorage);
					} catch (IOException e) {
						System.out.println("Error copying files");
					}
					pathStorageSetMap.get(fileCopy).add(s);
					break;
				}
			}
		}
	}
}
