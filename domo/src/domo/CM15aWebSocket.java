package domo;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import jsmm.cm15a.CM15aData;
import jsmm.cm15a.Utils;

@ServerEndpoint("/domows")
public class CM15aWebSocket {

	Session miSession;
	
	@OnOpen
	public void open(Session session, EndpointConfig config) throws IOException {
		Utils.log("WebSocket: open");
    	miSession=session;
		CM15aSingleton.getInstance().addWS(this);
//		session.getBasicRemote().sendText("Opened");
	}
	@OnClose
    public void onClose(Session session, CloseReason reason) throws IOException {
		Utils.log("WebSocket: close");
    	miSession=null;
    	CM15aSingleton.getInstance().removeWS(this);
    }
	
    @OnMessage
    public void echoTextMessage(Session session, String msg, boolean last) {
        try {
        	System.out.println("==>recibido:"+msg+"."+last);
            if (session.isOpen()) {
                session.getBasicRemote().sendText(msg, last);
            }
        } catch (IOException e) {
            try {
                session.close();
            } catch (IOException e1) {
                // Ignore
            }
        }
    }

    @OnMessage
    public void echoBinaryMessage(Session session, ByteBuffer bb,
            boolean last) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendBinary(bb, last);
            }
        } catch (IOException e) {
            try {
                session.close();
            } catch (IOException e1) {
                // Ignore
            }
        }
    }

    /**
     * Process a received pong. This is a NO-OP.
     *
     * @param pm    Ignored.
     */
    @OnMessage
    public void echoPongMessage(PongMessage pm) {
        // NO-OP
    }

    ///////////////////////////////////////////////////////////
    
    private String toJSon(CM15aData cm15aData) {
    	return ("{"+
    			"\"hc\":"+"\""+cm15aData.getHc()+"\","+
    			"\"dev\":"+"\""+cm15aData.getDevice()+"\","+
    			"\"fun\":"+"\""+cm15aData.getFunction()+"\","+
    			"\"per\":"+"\""+cm15aData.getPercentage()+"\""+
    			"}");
    }
    
	public void receive(CM15aData cm15aData) {
    	System.out.println("CM15aWebSocket ["+this+"]==>recibido:"+cm15aData);
		if (miSession!=null) {
			try {
	            if (miSession.isOpen()) {
	                miSession.getBasicRemote().sendText(toJSon(cm15aData));
	            }
	        } catch (IOException e) {
	            try {
	            	Utils.logErr("Excepcion: "+e);
	                miSession.close();
	            } catch (IOException e1) {
	                // Ignore
	            }
	        }
		}
	}
}