package mapserver;

import java.io.File;

public class Device {

	private String type;
	private double lat;
	private double lng;
	private int id;
	private File logfile;

	public Device(int id, String type, double lat, double lng) {
		this.type = type;
		this.lat = lat;
		this.lng = lng;
		this.id = id;
	}
	
	public Device(int id){
		this.id = id;
		this.type = "";
		this.lat = -1.0;
		this.lng = -1.0;
	}

	public Device getVehicle() {
		return this;
	}

	public void setVehicle(Device v) {
		type = v.type;
		lat = v.lat;
		lng = v.lng;
	}

	public double getLat() {
		return this.lat;
	}

	public double getLng() {
		return this.lng;
	}

	public void setCoordinate(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public File getLogFile() {
		return this.logfile;
	}

	public void setLogFile(File logfile) {
		this.logfile = logfile;
	}
}
