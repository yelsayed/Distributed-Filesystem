package naming;

public class DynamicBoolean {
	private boolean mainBool;
	
	public DynamicBoolean(boolean b) {
		this.mainBool = b;
	}
	
	public boolean getBool() {
		return mainBool;
	}
	
	//setter for the boolean attribute
	public void setBool(boolean b) {
		this.mainBool = b;
	}
	
	public boolean equals(Object o) {
		System.out.println("this is: " + this.mainBool + " compared with " +  ((DynamicBoolean)o).getBool());
		return mainBool == ((DynamicBoolean)o).getBool();
	}
	
	public String toString() {
		if (this.mainBool) {
			return "true";
		} else return "false";
	}
}
