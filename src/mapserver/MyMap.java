package mapserver;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
	private static Map<Session, Device> devices = new HashMap<Session, Device>();

	private static String logpath = "";

	private static Double speed = 0.0;

	private void setLogPath() {
		logpath = System.getProperty("user.dir").concat("/log/");
		DateFormat df = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss");
		Date dateobj = new Date();
		String path = df.format(dateobj).toString();
		createDirectory(path, "Creating log directory: ");
		logpath = logpath.concat(path.concat("/"));
	}

	private void createDirectory(String path, String message) {

		File dir = new File(logpath.concat(path));

		// if the directory does not exist, create it
		if (!dir.exists()) {
			System.out.println(message + logpath.concat(path));
			boolean result = false;

			try {
				dir.mkdirs();
				result = true;
			} catch (SecurityException se) {
				System.out.println("No permission to create file!");
			}
			if (result) {
				System.out.println("DIR created");
			}
		}
	}

	@OnOpen
	public void open(Session session) {
	}

	@OnClose
	public void close(Session session) {
		// Close the websocket session
		System.out.println("WebSocket closed: " + session.getId());
		if (browser.contains(session)) {
			browser.remove(session);
		} else if (devices.containsKey(session)) {
			Device actdevice = devices.get(session);

			Map<String, ?> config = null;
			JsonBuilderFactory factory = Json.createBuilderFactory(config);
			JsonObject value = factory.createObjectBuilder().add("type", "delete").add("id", actdevice.getId()).build();

			SendtoBrowser(session, value.toString());
			devices.remove(session);
		}
	}

	@OnError
	public void error(Throwable t) {
		System.out.println("WebSocket error: " + t.getMessage());
	}

	@OnMessage
	public String open(Session session, String message) {
		// Message in websocket
		JsonReader jsonReader = Json.createReader(new StringReader(message));
		JsonObject input = jsonReader.readObject();

		jsonReader.close();
		Map<String, ?> config = null;
		JsonBuilderFactory factory = Json.createBuilderFactory(config);
		JsonObject value;
		String mtype = input.getString("type");

		switch (mtype) {
		case "initBrowser":
			// New browser
			browser.add(session);
			System.out.println("New browser: " + browser.size());
			JsonArrayBuilder builder = Json.createArrayBuilder();

			// Send joined the vehicles in JSON
			for (Map.Entry<Session, Device> entry : devices.entrySet()) {
				Device actdevice = entry.getValue();

				JsonObject js = factory.createObjectBuilder().add("id", actdevice.getId())
						.add("type", actdevice.getType()).add("lat", actdevice.getLat()).add("lng", actdevice.getLng())
						.build();
				builder.add(js);
			}
			JsonArray vehiclearray = builder.build();

			value = factory.createObjectBuilder().add("type", "newCoordinate").add("vehicles", vehiclearray).build();
			System.out.println(value.toString());
			return value.toString();

		case "initDevice":
			// new OBU/RSU join
			try {
				int id = input.getInt("id");
				// init log path
				if (logpath == "")
					setLogPath();

				devices.put(session, new Device(id));
				System.out.println("New Device: " + devices.size());
			} catch (NullPointerException e) {
				System.out.println("Missing id from device initialization");
			}
			break;
		case "newCoordinate":
			// new coordinate arrived
			JsonArray jsonarray = input.getJsonArray("vehicles");

			try {
				for (JsonValue jsonValue : jsonarray) {
					storeVehicle(session, jsonValue);
				}
				SendtoBrowser(session, input.toString());
				// System.out.println(input.toString());
			} catch (NullPointerException e) {
				System.out.println("Missing required json key");

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "newMessage":
			// new event message from OBU/RSU
			SendtoBrowser(session, input.toString());
			Device actdevice = devices.get(session);
			List<String> msg = new ArrayList<>();
			msg.add(input.toString());

			// Save it to their own file, the file name is the station is
			Path path = Paths.get(logpath.concat(Integer.toString(actdevice.getId()) + ".txt"));
			if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
				try {
					Files.write(path, msg, StandardOpenOption.APPEND);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					Files.write(path, msg, StandardOpenOption.CREATE);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			break;
		default:
			System.out.println("Invalid message type!");
			break;
		}

		return "";
	}

	/**
	 * Save the vehile's data
	 * 
	 * @param session
	 *            The incomming session
	 * @param jsonValue
	 *            The vehicle JSON array
	 */
	private void storeVehicle(Session session, JsonValue jsonValue) {
		String type = "";
		Double lat = -1.0;
		Double lng = -1.0;
		int id = -1;
		try {
			JsonObject jsonvehicle = (JsonObject) jsonValue;

			id = jsonvehicle.getInt("id");
			lat = jsonvehicle.getJsonNumber("lat").doubleValue();
			lng = jsonvehicle.getJsonNumber("lng").doubleValue();

			if (jsonvehicle.containsKey("type")) {
				type = jsonvehicle.getString("type");
			}

			// if session exist in devices map
			if (devices.containsKey(session)) {
				if (lat >= 0 && lng >= 0)
					devices.get(session).setCoordinate(lat, lng);
				if (type != "")
					devices.get(session).setType(type);
			} else {
				// save the new vehicle
				if (id != -1) {
					if (type != "" && lat >= 0 && lng >= 0) {
						devices.put(session, new Device(id, type, lat, lng));
					} else {
						devices.put(session, new Device(id));
					}
				}
			}
		} catch (NullPointerException e) {
			System.out.println("Missing required json key");
		}
	}

	/**
	 * Send message to browsers
	 * 
	 * @param session
	 *            The incomming session
	 * @param message
	 *            The message, which we want to send
	 */
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
