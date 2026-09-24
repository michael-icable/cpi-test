import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.xml.*;
import java.util.regex.*;
import java.io.*;

def Message processData(Message message) {
	
	def body = message.getBody(java.lang.String);
	def messageLog = messageLogFactory.getMessageLog(message);
	map = message.getProperties();
	String BP= new String();
	def hmap1 = map.get("hashmapOutput");  
	def workerID=map.get("SAPReferenceID");
   	
   //def Root = new XmlParser().parseText(body);
//	Root.result.each{r->r.[0].value = 
     BP = String.valueOf(hmap1.get(workerID));
	    
	
       
message.setProperty("BusinessPartner", BP);
   	return message;
}
