import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.Date;
import java.util.TimeZone;

def Message processData(Message message) 
{   
    def prop_timezone = message.getProperty("Timezone");
    def timezone = TimeZone.getTimeZone(prop_timezone)
    def dateTime = new Date() 
    def currDateTime = (dateTime.format("yyyy-MM-dd'T'HH:mm:ss",timezone))

    message.setProperty("datetimestamp",currDateTime); 
    
    return message
}
