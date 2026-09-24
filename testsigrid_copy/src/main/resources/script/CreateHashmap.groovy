import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.xml.*;
import java.util.regex.*;
import java.io.*;

def Message processData(Message message) {
	
	def body = message.getBody(java.io.Reader);
	HashMap<String, String> hmap1 = new HashMap<String,String>();	
	map = message.getProperties();
	def response = new XmlSlurper().parse(body);

    response.Message1.A_BusinessPartner.A_BusinessPartnerType.each{
        try{
        hmap1.put(it.BusinessPartnerIDByExtSystem.text(), it.BusinessPartner.text());
        }
        catch(Exception ex){
            //do nothing, skip the record
        }
    }
    //	message.setProperty("ParsedXML", response);
	message.setProperty("hashmapOutput", hmap1);
	return message;
}