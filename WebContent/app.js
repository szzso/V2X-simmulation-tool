var vehicle = new Array();
var map;
var message = "";

var wsUrl = null;
var websocket = null;
var firstdevice;
var defaultlocation = new google.maps.LatLng(47.473605, 19.052594);
var carindex=0;
Number.prototype.toRad = function () { return this * Math.PI / 180; }

function getRootUri() {
	return "ws://"
			+ (document.location.hostname == "" ? "localhost"
					: document.location.hostname) + ":"
			+ (document.location.port == "" ? "8080" : document.location.port);
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

function toRadian(degree){
	return degree* Math.PI / 180;
}

function onMessage(evt) {
	data = JSON.parse(evt.data);
	var readvehicle = data.vehicles;
	var messagetype = data.type;

	
	
	if (messagetype === "delete") {
		var id = data.id;
		var index = -1;
		for (var j = 0; j < vehicle.length; j++) {
			if (id == vehicle[j].id) {
				index = j;
				// Remove the marker from Map
				vehicle[j].map.setMap(null);
			}
		}
		if (index != -1){
			vehicle.splice(index, 1);
			if(vehicle.length == 0){
				firstdevice =true;
			}
		}
	} else {
		if(firstdevice && readvehicle.length >0){
			
			firstdevice = false;
			var center = map.getCenter();
		    var R = 6371e3; // metres
		    //console.log("center : "+ center);
		    var lat1 = toRadian(readvehicle[0].lat);
		    var lat2 = toRadian(center.lat());
		    var deltalat = toRadian(center.lat()-readvehicle[0].lat);
		    var deltalon = toRadian(center.lng()-readvehicle[0].lng);
		    var a = Math.sin(deltalat/2) * Math.sin(deltalat/2) +
		            Math.cos(lat1) * Math.cos(lat2) *
		            Math.sin(deltalon/2) * Math.sin(deltalon/2);
		    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		    var d = R * c;
		    if(d>1500){
		    	map.setCenter(new google.maps.LatLng(readvehicle[0].lat, readvehicle[0].lng));
		    }
		    
		    
		}
		for ( var i in readvehicle) {
			var found = false;
			var id = readvehicle[i].id
			var type = readvehicle[i].type
			var lat = readvehicle[i].lat
			var lng = readvehicle[i].lng
			var myLatLng = {
				lat : lat,
				lng : lng
			};

			for (var j = 0; j < vehicle.length; j++) {
				if (id == vehicle[j].id) {
					vehicle[j].map.setPosition(myLatLng);
					found = true;
				}
			}
			if (!found) {
				var icon;
				switch(type) {
			    case "car":
			    	icon= "pictures/car"+ carindex +".png"
			        carindex= (carindex+1)%10;
			        break;
			    case "rsu":
			        icon = "pictures/rsu.png";
			        break;
			    case "trafficlight":
			    	icon = "pictures/trafficlight.png";
			    	break;
			    default:
			    	icon = "pictures/default.png";
				} 
				var marker = new google.maps.Marker({
					position : myLatLng,
					map : map,
					icon : icon,
					title : "Hello there Kisumu!"
				})
				// marker.showInfoWindow();

				var v = {
					id : id,
					type : type,
					map : marker
				};
				vehicle.push(v);
			}
		}
	}
}

function doSend() {
	if (websocket === null)
		initWebSocket();
	else if (message !== "")
		websocket.send(message);
}

function initialize() {
	// Egy új térkép létrehozása
	wsUrl = getRootUri() + "/V2X-Map-WebSocket/map";
	firstdevice = true;
	initWebSocket();
	var initialLocation;
	if (navigator.geolocation) {
	     navigator.geolocation.getCurrentPosition(function (position) {
	         initialLocation = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
	         //map.setCenter(initialLocation);
	     });
	     console.log("position: "+ initialLocation);
	     if (initialLocation === undefined || initialLocation === null) {
	    	 initialLocation= defaultlocation;
	    	 console.log("Budapesti lesz mert: "+ initialLocation);
	    }
	 }
	else{
		initialLocation= defaultlocation;//47.7104101, 17.6744103)
	}
	var mapProp = {
		center : initialLocation,// 47.475230,
		// 19.056072),
		zoom : 15,
		mapTypeControl : false,
		mapTypeId : google.maps.MapTypeId.ROADMAP
	};
	 
	// Térkép elhelyezése a HTML kód googleMap id- vel rendelkező elemébe
	map = new google.maps.Map(document.getElementById("googleMap"), mapProp);

	if (typeof(Number.prototype.toRad) === "undefined") {
		  Number.prototype.toRad = function() {
		    return this * Math.PI / 180;
		  }
		}
	
	var init = {
		"type" : "initBrowser"
	};
	
	message = JSON.stringify(init);
	doSend();
}
// window.addEventListener("load", initialize, false);
// Oldal betöltésekor meghívja az initialize függvényt
// google.maps.event.addDomListener(window, 'load', initialize);
