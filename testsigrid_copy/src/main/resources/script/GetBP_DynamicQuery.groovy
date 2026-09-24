import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.xml.*;
import java.util.regex.*;
import java.io.*;

def Message processData(Message message) {
	
	def body = message.getBody(java.io.Reader);
	//HashMap<String, String> hmap1 = new HashMap<String,String>();
	String customQuery = new String();
    customQuery = "\$filter=";
	map = message.getProperties();
	def response = new XmlSlurper().parse(body);

  response.Response_Data.Worker.each{
      if (it != response.Response_Data.Worker.last()){
        try{
        customQuery = customQuery + "BusinessPartnerIDByExtSystem eq'"+ it.Worker_Data.Worker_ID.text() + "' or ";
        }
       catch(Exception ex){
            //do nothing, skip the record
        }
      }
      else{
         customQuery = customQuery + "BusinessPartnerIDByExtSystem eq'"+ it.Worker_Data.Worker_ID.text() + "'"; 
      }
       
    }
   // message.setProperty("ParsedXML", response);
	message.setProperty("customQuery", customQuery);
	return message;
}