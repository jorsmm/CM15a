<%@page import="domo.*"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="initial-scale=1, maximum-scale=1">
<title>Sensor</title>
<style>
.verde {color: green;}
.rojo {color: red;}
.negro {color: black;}
.azul {color: blue;}
</style>
<script>
var jsonresul;
var estadoactual;

var myinterval=null;
function interval() {
	if (myinterval==null) {
		myinterval=setInterval(consulta, 400);
	}
	else {
		clearInterval(myinterval);
		myinterval=null;
	}
    document.getElementById("textinterval").innerHTML = (myinterval!=null?'Apagar':'Encender');
    document.getElementById("textinterval").className = (myinterval!=null?'verde':'rojo');
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
		    document.getElementById("textsensor").className = (jsonresul.running?'verde':'rojo');
		    document.getElementById("textsensor2").innerHTML = (jsonresul.running?'Apagar':'Encender');
		    document.getElementById("textsensor2").className = (jsonresul.running?'verde':'rojo');
		    document.getElementById("textpresence").innerHTML = (jsonresul.presence?'Hay presencia':'No hay nadie');
		    document.getElementById("textpresence").className = (jsonresul.presence?'azul':'negro');
		    document.getElementById("textlasupdated").innerHTML = (jsonresul.lastupdated);
	    }
	};
	xhttp.open("GET", "info.jsp"+param, true);
	xhttp.send();
}
function navidad(param) {
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
	    if (this.readyState == 4 && this.status == 200) {
	    	console.log("####"+this.responseText+"####");
	    }
	};
	xhttp.open("GET", "http://192.168.1.2:18080/domo/iPHC-process.php?q=ssm+nav"+param, true);
	xhttp.send();
}
</script>
</head>
<body onload="interval()">
<h1>El Sensor está <span id="textsensor"></span></h1>
<h2><span id="textpresence"></span></h2>
<h3><span id="textlasupdated"></span></h3>
<hr/>
<a onclick="cambia()">Pulsa para <span id="textsensor2"></span> el Sensor</a>
<hr/>
<br/>
<br/>
<a class="verde" onclick="navidad('ON')">Pulsa para encender NAVIDAD</a>
<br/>
<a class="rojo" onclick="navidad('OFF')">Pulsa para apagar NAVIDAD</a>
<hr/>
<br/>
<br/>
<a onclick="interval()">Pulsa para <span id="textinterval"></span> el refresco</a>
</body>
</html>