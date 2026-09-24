import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
def Message processData(Message message) {
    //Body 
       def map = message.getProperties();
       String addID = map.get("WorkdayAddressID")
       String address = addID.replace('ADDRESS_REFERENCE-','')
       message.setProperty("WorkdayAddressID", address);
       return message;
}