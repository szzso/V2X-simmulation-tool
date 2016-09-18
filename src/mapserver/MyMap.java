package mapserver;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/map")
public class MyMap {

	private static List<Session> browser = new ArrayList<Session>();
	private static Map<Session, Integer> device = new HashMap<Session, Integer>();

	private static List<Vehicle> vehicle = new ArrayList<Vehicle>();

	@OnOpen
	public void open(Session session) {
	}

	@OnClose
	public void close(Session session) {
		System.out.println("WebSocket closed: " + session.getId());
		if (browser.contains(session)) {
			browser.remove(session);
		} else if (device.containsKey(session)) {
			int id = device.get(session);
			for (int i = 0; i < vehicle.size(); i++) {
				if (vehicle.get(i).getId() == id) {
					vehicle.remove(i);
					Map<String, ?> config = null;
					JsonBuilderFactory factory = Json.createBuilderFactory(config);
					JsonObject value = factory.createObjectBuilder().add("type", "delete").add("id", id).build();

					SendtoBrowser(session, value.toString());

					break;
				}
			}
			device.remove(session);
		}

	}

	@OnError
	public void error(Throwable t) {
		System.out.println("WebSocket error: " + t.getMessage());
	}

	@OnMessage
	public String open(Session session, String message) {
		JsonReader jsonReader = Json.createReader(new StringReader(message));
		JsonObject input = jsonReader.readObject();

		jsonReader.close();
		Map<String, ?> config = null;
		JsonBuilderFactory factory = Json.createBuilderFactory(config);
		JsonObject value;
		String mtype = input.getString("type");

		

		switch (mtype) {
		case "initBrowser":

			browser.add(session);
			System.out.println("initBrowser:" + browser.size());

			JsonArrayBuilder builder = Json.createArrayBuilder();
			for (int i = 0; i < vehicle.size(); i++) {
				JsonObject js = factory.createObjectBuilder().add("id", vehicle.get(i).getId())
						.add("type", vehicle.get(i).getType()).add("notification", vehicle.get(i).getNotification())
						.add("lat", vehicle.get(i).getLat()).add("lng", vehicle.get(i).getLng()).build();
				builder.add(js);
			}
			JsonArray vehiclearray = builder.build();

			value = factory.createObjectBuilder().add("type", "sendData").add("vehicles", vehiclearray).build();
			return value.toString();

		case "initDevice":
			try {
				System.out.println("New Device:");
				int id = input.getInt("id");
				System.out.println(id);
				device.put(session, id);
				System.out.println("initDevice:" + device.size());
				Iterator it = device.entrySet().iterator();
				System.out.println("Values:");
				while(it.hasNext()) {
					Map.Entry pair = (Map.Entry)it.next();
					System.out.println(pair.getValue());
				}
			} catch (NullPointerException e) {
				System.out.println("Missing id from device initialization");
			}
			return "";
		case "newCoordinate":
				if (device.containsKey(session)) {
					try {
					JsonArray jsonarray = input.getJsonArray("vehicles");
					for (JsonValue jsonValue : jsonarray) {
						
						storeVehicle(jsonValue);

					}

					value = factory.createObjectBuilder().add("type", "sendData").add("vehicles", jsonarray).build();

					SendtoBrowser(session, value.toString());
					} catch (NullPointerException e) {
						System.out.println("Missing required json key");

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
				else{
					try {
						JsonArray jsonarray = input.getJsonArray("vehicles");
						for (JsonValue jsonValue : jsonarray) {
							JsonObject jsonvehicle = (JsonObject) jsonValue;

							int id = jsonvehicle.getInt("id");
							if(!device.containsValue(id)){
								device.put(session, id);
								System.out.println("initDevice:" + device.size());
							}
							else{
								storeVehicle(jsonValue);
							}
						}

						return "";
					} catch (NullPointerException e) {
						System.out.println("Missing id from device initialization");
					}
					break;
				}
				
			
		default:
			break;
		}

		return "";
	}
	
	private void storeVehicle(JsonValue jsonValue){
		String type = "";
		Double lat = -1.0;
		Double lng = -1.0;
		String notification = "";
		int id = -1;
		
		JsonObject jsonvehicle = (JsonObject) jsonValue;

		id = jsonvehicle.getInt("id");
		lat = jsonvehicle.getJsonNumber("lat").doubleValue();
		lng = jsonvehicle.getJsonNumber("lng").doubleValue();

		if (jsonvehicle.containsKey("type"))
			type = jsonvehicle.getString("type");
		if (jsonvehicle.containsKey("notification"))
			notification = jsonvehicle.getString("notification");
		

		boolean found = false;

		for (int j = 0; j < vehicle.size(); j++) {
			if (vehicle.get(j).getId() == id) {
				vehicle.get(j).setCoordinate(lat, lng);
				vehicle.get(j).setNotification(notification);
				found = true;
			}
		}

		if (!found) {
			Vehicle v = new Vehicle(id, type, lat, lng, notification);
			vehicle.add(v);
		}
	}

	private void SendtoBrowser(Session session, String message) {
		for (Session s : browser) {
			if (s != session && s.isOpen()) {
				try {
					s.getBasicRemote().sendText(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
