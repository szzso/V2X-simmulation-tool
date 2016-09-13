var vehicle = new Array();
var map;
var message = "";

var wsUrl = null;
var websocket = null;
function getRootUri() {
	return "ws://" +
	(document.location.hostname == "" ? "localhost" : document.location.hostname)
	+ ":" +
	(document.location.port == "" ? "8080" : document.location.port);
}

function initWebSocket() {
	websocket = new WebSocket(wsUrl);
	websocket.onopen = function(evt) {
		doSend();
		message = "";
	};
	websocket.onmessage = function(evt) {
		onMessage(evt);
	};
	websocket.onerror = function(evt) {
		message = "";
		websocket = null;
	};
	websocket.onclose = function(evt) {
		message = "";
		websocket = null;
	};
	
}
function send_message() {
	if (websocket == null) {
		initWebSocket();
	} else {
		doSend();
	}
}

function onMessage(evt) {
	data = JSON.parse(evt.data);
	var readvehicle = data.vehicles;

	for (var i in readvehicle) {
		var found = false;
		var id = readvehicle[i].id
		var type = readvehicle[i].type
		var lat = readvehicle[i].lat
		var lng = readvehicle[i].lng
		var myLatLng = {lat: lat, lng: lng};

		for (var j = 0; j < vehicle.length; j++) {
			if (id == vehicle[j].id) {
				vehicle[j].map.setPosition(myLatLng);		
				found = true;
			}
		}
		if (!found) {
			var marker = new google.maps.Marker({
				position : myLatLng,
				map : map,
				icon : 'sportscar.png'
			})
			var v = {
				id : id,
				type : type,
				map : marker
			};
			vehicle.push(v);
		}
	}
}


function doSend() {
	if (websocket === null) initWebSocket();
	else if (message !== "") websocket.send(message);
}

function initialize() {
	// Egy új térkép létrehozása
	wsUrl = getRootUri() + "/V2X-Map-WebSocket/map";
	initWebSocket();
	
	var mapProp = {
		center : new google.maps.LatLng(47.7104101, 17.6744103),// 47.475230,
		// 19.056072),
		zoom : 14,
		mapTypeControl : false,
		mapTypeId : google.maps.MapTypeId.ROADMAP
	};
	// Térkép elhelyezése a HTML kód googleMap id- vel rendelkező elemébe
	map = new google.maps.Map(document.getElementById("googleMap"), mapProp);
	
	var init = {"type":"initBrowser"};
	message = JSON.stringify(init);
	doSend();
	
	var getCoordinate = {"type":"getCoordinate"};
	message = JSON.stringify(getCoordinate);
	doSend();
}
window.addEventListener("load", initialize, false);
// Oldal betöltésekor meghívja az initialize függvényt
//google.maps.event.addDomListener(window, 'load', initialize);
