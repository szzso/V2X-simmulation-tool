package mapserver;

public class Vehicle {

	private String type;
	private double lat;
	private double lng;
	private int id;
	private String notification;

	public Vehicle(int id, String type, double lat, double lng, String notification) {
		this.type = type;
		this.lat = lat;
		this.lng = lng;
		this.id = id;
		this.notification = notification;
	}

	public Vehicle getVehicle() {
		return this;
	}

	public void setVehicle(Vehicle v) {
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
	
	public String getNotification(){
		return this.notification;
	}
	
	public void setNotification(String notification){
		this.notification = notification;
	}
}
