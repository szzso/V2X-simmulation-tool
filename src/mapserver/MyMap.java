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

	// private static List<Device> vehicle = new ArrayList<Device>();
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
		// System.out.println("WebSocket error: " + t.getMessage());
		t.printStackTrace();
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
			System.out.println("New browser: " + browser.size());
			JsonArrayBuilder builder = Json.createArrayBuilder();

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
			try {
				int id = input.getInt("id");
				if (logpath == "")
					setLogPath();

				devices.put(session, new Device(id));
				System.out.println("New Device: " + devices.size());
			} catch (NullPointerException e) {
				System.out.println("Missing id from device initialization");
			}
			break;
		case "newCoordinate":

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
			SendtoBrowser(session, input.toString());
			Device actdevice = devices.get(session);
			List<String> msg = new ArrayList<>();
			msg.add(input.toString());

			/*JsonArray myjsonarray = input.getJsonArray("vehicles");
			for (JsonValue jsonValue : myjsonarray) {
				JsonObject jsonvehicle = (JsonObject) jsonValue;
				JsonArray mymsg = jsonvehicle.getJsonArray("message");

				for (JsonValue onemsg : mymsg) {

					JsonObject readmesg = (JsonObject) onemsg;

					if (readmesg.getString("type").equals("CAM")) {

						if (readmesg.containsKey("speed"))
							if (readmesg.getJsonNumber("speed").doubleValue() > 0)
								speed = readmesg.getJsonNumber("speed").doubleValue();
							else {

								Path path = Paths.get(logpath.concat(Integer.toString(actdevice.getId()) + "_CAM.csv"));

								String output = readmesg.getInt("dst") + ";" + readmesg.getString("time") + ";"
										+ speed.toString();
								System.out.println(output);
								List<String> msg2 = new ArrayList<>();
								msg2.add(output);
								if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
									try {
										Files.write(path, msg2, StandardOpenOption.APPEND);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								} else {
									try {
										Files.write(path, msg2, StandardOpenOption.CREATE);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
					}
				}
			}*/

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

			if (devices.containsKey(session)) {
				if (lat >= 0 && lng >= 0)
					devices.get(session).setCoordinate(lat, lng);
				if (type != "")
					devices.get(session).setType(type);
			} else {
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
