package naming;

public class DynamicBoolean {
	public boolean mainBool;
	
	public DynamicBoolean(boolean b) {
		this.mainBool = b;
	}
	
	//setter for the boolean attribute
	public void setBool(boolean b) {
		this.mainBool = b;
	}
	
	public boolean equals(Object o) {
		return mainBool == ((DynamicBoolean)o).mainBool;
	}
	
	public String toString() {
		if (this.mainBool) {
			return "true";
		} else return "false";
	}
}
