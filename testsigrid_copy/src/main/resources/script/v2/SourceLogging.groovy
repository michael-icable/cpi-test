// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

import com.sap.it.script.v2.api.Message;
import java.lang.StringBuffer;

def Message processData(Message message) {
    //Body
    def messageLog = messageLogFactory.getMessageLog(message);
    
    def logProperty = message.getProperty("PropertyLogging") as String;
    def logHeader = message.getProperty("HeaderLogging") as String;
    def logBody = message.getProperty("BodyLogging") as String;
    String content = "";
    
    if(messageLog != null) {
        messageLog.setStringProperty("Logging#1", "Payload");
        
        if (logProperty != null && logProperty.equalsIgnoreCase("TRUE")) {
            def propertyMap = message.getProperties();
            StringBuffer buffer = new StringBuffer();
            for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
                buffer.append("<").append(entry.getKey()).append(">");
                buffer.append(entry.getValue());
                buffer.append("</").append(entry.getKey()).append(">\n");
            }
            content = content + "\n" + buffer.toString();
        }
        
        if (logHeader != null && logHeader.equalsIgnoreCase("TRUE")) {
            def header = message.getHeaders() as String;
            content = content + "\n" + header;
        }
        
        if (logBody == null || logBody.equalsIgnoreCase("TRUE")) {
            def body = message.getBody(java.lang.String) as String;
            content = content + "\n" + body;
        }
        
        if (content.length() > 0) {
            messageLog.addAttachmentAsString("Source MessageLog", content, "text/plain");    
        }
    }

    return message;
}
