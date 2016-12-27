<%@page import="domo.*"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="initial-scale=1, maximum-scale=1">
<title>Sensor</title>
<script>
var jsonresul;
var estadoactual;

var myinterval=null;
function onload() {
	myinterval=setInterval(consulta, 400);
}
function cancelinterval() {
	if (myinterval==null) {
		onload();
	}
	else {
		clearInterval(myinterval);
		myinterval=null;
	}
}
function consulta() {
	execrequest("");
}
function cambia() {
	console.log(jsonresul);
	execrequest("?sensor="+!jsonresul.running);
}
function execrequest(param) {
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
	    if (this.readyState == 4 && this.status == 200) {
	    	console.log("####"+this.responseText+"####");
	    	jsonresul=JSON.parse(this.responseText);
		    document.getElementById("textsensor").innerHTML = (jsonresul.running?'Encendido':'Apagado');
		    document.getElementById("textsensor2").innerHTML = (jsonresul.running?'Apagar':'Encender');
		    document.getElementById("textpresence").innerHTML = (jsonresul.presence?'Hay presencia':'No hay nadie');
		    document.getElementById("textlasupdated").innerHTML = (jsonresul.lastupdated);
	    }
	};
	xhttp.open("GET", "info.jsp"+param, true);
	xhttp.send();
}
</script>
</head>
<body onload="onload()">
<h1>El Sensor está <span id="textsensor"></span></h1>
<h2><span id="textpresence"></span></h2>
<h3><span id="textlasupdated"></span></h3>
<br/>
<hr/>
<a onclick="cambia()">Pulsa para <span id="textsensor2"></span></a>
<br/>
<br/>
<hr/>
<br/>
<a onclick="cancelinterval()">Pulsa para cambiar refresco</span></a>
</body>
</html>