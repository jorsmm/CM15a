<%@page import="domo.*"%><%
if ("true".equalsIgnoreCase(request.getParameter("sensor"))) {
	HueSensor.start();
}
else if ("false".equalsIgnoreCase(request.getParameter("sensor"))) {
	HueSensor.stop();
}
%>{"running":<%=(Boolean.toString(HueSensor.isRunning()))%>, "presence":<%=(Boolean.toString(HueSensor.isPresence()))%>, "lastupdated":"<%=HueSensor.getLastupdated()%>"}