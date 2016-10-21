var vehicle = new Array();
var map;
var message = "";
var events= new Array();

var uniquID = 1000;
var wsUrl = null;
var websocket = null;
var firstdevice;
var followID;
var defaultlocation = new google.maps.LatLng(47.473605, 19.052594);
var carindex=0;
Number.prototype.toRad = function () { return this * Math.PI / 180; }
window.setInterval(deleteOldEvents, 5000);

function deleteOldEvents(){
	var now = new Date();
	var i = 0;
	while(i<events.length){
		var diff = now-events[i].time;
		if(diff > 30000){
			var object= document.getElementById(events[i].id);
			document.getElementById('noticication').removeChild(object); 
			events.splice(i, 1);
		}
		else
			i++;
	}
	
}

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

function toRadian(degree){
	return degree* Math.PI / 180;
}

function onMessage(evt) {
	data = JSON.parse(evt.data);
	var readvehicle = data.vehicles;
	var messagetype = data.type;
	
	switch(messagetype) {
	    case "delete":
	    	console.log("message : "+ evt.data);
			var id = data.id;
			var index = -1;
			for (var j = 0; j < vehicle.length; j++) {
				if (id == vehicle[j].id) {
					index = j;
					// Remove the marker from Map
					vehicle[j].marker.setMap(null);
				}
			}
			if (index != -1){
				vehicle.splice(index, 1);
			}
			if(vehicle.length == 0)
				firstdevice =true;
	    	break;
	    case "newMessage":
	    	for ( var i in readvehicle) {
				var jsonmessage = readvehicle[i].message;

				var id = readvehicle[i].id;
				var type = readvehicle[i].type;
				if(id != null && id != "" && jsonmessage != null)
					var found = false;
					var icon =""; ;
					var marker;
					for (var j = 0; j < vehicle.length; j++) {
						if (id == vehicle[j].id) {
							icon = vehicle[j].icon;
							found = true;
							marker = vehicle[j].marker;
							
						}
					}
					if(!found){
						console.log("newmes: "+ evt.data);
						console.log("type: "+ type);
						icon = storeNewDevice(type, id);
						console.log("icon: "+ type);
					}
					
					newEventMessage(icon, id, jsonmessage, marker)

					
					
			}
	    	break;
	    case "newCoordinate":
	    	if(firstdevice && readvehicle.length >0){
				
				firstdevice = false;
				var center = map.getCenter();
			    var R = 6371e3; // metres
			    // console.log("center : "+ center);
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
				var id = readvehicle[i].id;
				var type = readvehicle[i].type;
				var lat = readvehicle[i].lat;
				var lng = readvehicle[i].lng;
				var myLatLng = {
					lat : lat,
					lng : lng
				};
				if(followID ==id){
					map.setCenter(myLatLng);
				}

				for (var j = 0; j < vehicle.length; j++) {
					if (id == vehicle[j].id) {
						vehicle[j].marker.setPosition(myLatLng);
						found = true;
					}
				}
				
				if (!found) {
					storeNewDevice(type, id);
				}
			}
	    	break;
	}
}

function storeNewDevice(type, id){
	var icon= getVehicleIcon(type);
	console.log("icon : "+ icon);
	var marker = new google.maps.Marker({
		map:map,
		icon : icon,
		title : "ID: "+ id + " Type: "+ type 
	});
	marker.set("id", id);

	marker.addListener('click', function() {
		map.setZoom(17);
        map.setCenter(marker.getPosition());
        
        followID = marker.get("id");
    });

	var v = {
		id : id,
		type : type,
		marker : marker,
		icon: icon
	};
	vehicle.push(v);
	return icon;
}

function doSend() {
	if (websocket === null)
		initWebSocket();
	else if (message !== "")
		websocket.send(message);
}

function getVehicleIcon(type){
	switch(type) {
    case "car":
    	var result = "pictures/car"+ carindex +".png";
    	carindex= (carindex+1)%10;
    	return result;
        break;
    case "rsu":
    	return "pictures/rsu.png";
        break;
    case "trafficlight":
    	return "pictures/trafficlight.png";
    	break;
    case "ambulance":
    	return "pictures/ambulance.png";
    	break;
    default:
    	return "pictures/default.png";
	} 
}

function newEventMessage(icon, id, notification, marker){
	for(i in notification){
		
		var dst = notification[i].dst;
		var src = notification[i].src;
		var messagetype = notification[i].type;
		var description = notification[i].description;
		var time = notification[i].time;
		
			
		var object = document.createElement('div');
		object.setAttribute('class', 'object');
		
		var messageNode = document.createElement('div');
		messageNode.setAttribute('class', 'message');
		
		
		messageNode.appendChild(buildMessage("Type: ", messagetype));
		messageNode.appendChild(buildMessage("Source: ", src));
		messageNode.appendChild(buildMessage("Destination: ", dst));
		messageNode.appendChild(buildMessage("Description: ", ""));
		
		messageNode.appendChild(document.createTextNode(description));
		
		var imageNode = document.createElement("img");
		imageNode.setAttribute('src', icon);
		imageNode.setAttribute('alt', "ID: "+ id);
		imageNode.setAttribute('title', "ID: "+ id);
		imageNode.setAttribute('class', 'icon');
		
		var date = document.createElement('div');
		date.setAttribute('class', 'date');
	
		var now = new Date();
		if(time === undefined || time === null)
			time = now.getFullYear()+"."+(now.getMonth()+1)+"."+now.getDate()+" "+now.getHours()+":"+now.getMinutes()+":"+now.getSeconds();
		date.appendChild(document.createTextNode(time));//now.getFullYear()+"."+(now.getMonth()+1)+"."+now.getDate()+" "+now.getHours()+":"+now.getMinutes()+":"+now.getSeconds()));
		
		object.appendChild(imageNode);
		object.appendChild(messageNode);
		object.appendChild(date);
		object.setAttribute('id',uniquID);
		document.getElementById('noticication').insertBefore(object,document.getElementById('noticication').firstChild);
	
		var e = {
				time : now,
				id: uniquID
			};
		uniquID++;
		events.push(e);
		
		
		var info = document.createElement('div');
		info.appendChild(buildMessage("Type: ", messagetype));
		info.appendChild(document.createTextNode(description));
		//Info window:
		if(marker.infowindow != null)
			marker.infowindow.close();
		marker.infowindow = new google.maps.InfoWindow({
		    content: info
		});
		marker.infowindow.open(map, marker);
	}
}

function buildMessage(name, content){
	var title = document.createElement('div');
	var b = document.createElement('b');
	
	var txt = document.createTextNode(name);
	b.appendChild(txt);
	
	title.appendChild(b);
	title.appendChild(document.createTextNode(content));
	
	return title;
}

function initialize() {
	wsUrl = getRootUri() + "/V2X-Map-WebSocket/map";
	firstdevice = true;
	followID = -1;
	initWebSocket();
	var initialLocation;
	//Get own location
	if (navigator.geolocation) {
	     navigator.geolocation.getCurrentPosition(function (position) {
	         initialLocation = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
	     });
	     console.log("position: "+ initialLocation);
	     //if the location data not valid use default
	     if (initialLocation === undefined || initialLocation === null) {
	    	 initialLocation= defaultlocation;
	    	 console.log("Budapesti lesz mert: "+ initialLocation);
	    }
	 }
	else{
		initialLocation= defaultlocation;
	}
	//new map
	var mapProp = {
		center : initialLocation,
		zoom : 17,
		mapTypeControl : false,
		mapTypeId : google.maps.MapTypeId.ROADMAP
	};
	 
	//Put the map on the web site
	map = new google.maps.Map(document.getElementById("googleMap"), mapProp);
	
	//New Event: center of the map will the click position
	google.maps.event.addListener(map, "click", function (event) {
	    var latitude = event.latLng.lat();
	    var longitude = event.latLng.lng();

	    map.setCenter(new google.maps.LatLng(latitude,longitude));
	    //Do not follow the marker
	    followID = -1;

	});
	
	var init = {
		"type" : "initBrowser"
	};
	
	message = JSON.stringify(init);
	doSend();
}
