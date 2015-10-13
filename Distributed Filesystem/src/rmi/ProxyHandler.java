package rmi;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ProxyHandler implements InvocationHandler, Serializable {
	public InetSocketAddress address;
	public Class<?> c;
	
	public ProxyHandler(InetSocketAddress address,Class<?> c) {
		this.address = address;
		this.c = c;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		String methodName = method.getName();
		Class[] argTypes = method.getParameterTypes();
		Object result = null;
		boolean success = false;
		boolean methodException;
		
		//Implementing toString
		if (methodName.equals("equals")) {
			java.lang.reflect.Proxy s = (java.lang.reflect.Proxy) args[0];
			if (s == null) {
				return false;
			}
			// Check if c is equal to the proxy's c and the addresses are also equal
			if (this.c.equals(((ProxyHandler) Proxy.getInvocationHandler(s)).c) && 
					this.address.equals(((ProxyHandler) Proxy.getInvocationHandler(s)).address)){
				return true;
			} else {
				return false;
			}
		} 
		else if (methodName.equals("toString")) {
			return "Interface " + c.getClass().toString() + " @ " + address.toString();
		} else if (methodName.equals("hashCode")) {
			return this.c.hashCode() + this.address.hashCode();
		} else {
			Socket clientSocket = new Socket();
			try {
				ObjectOutputStream out = null;
				ObjectInputStream in   = null;
				
				clientSocket.connect(this.address);

				out = new ObjectOutputStream(clientSocket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(clientSocket.getInputStream());
				
				out.writeObject(methodName);

				out.writeObject(argTypes);

				out.writeObject(args);
				
				success = (boolean) in.readObject();
				result = in.readObject();
				
				clientSocket.close();	
			}
			catch (Exception e) {
				clientSocket.close();
				throw new RMIException("Hey man, you fail!!");
			}
			if (success == false) {
				throw ((Throwable) result);
			};
			
		}
	
		return result;
	}

}
