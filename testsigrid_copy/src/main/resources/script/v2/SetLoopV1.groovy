// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

import com.sap.it.script.v2.api.Message;
import java.util.HashMap;

def Message processData(Message message) {
    // 1. Fetch all exchange properties
    def map = message.getProperties();
    
    // 2. Safely retrieve and convert properties to Integers
    // If the property is null or empty, it assigns a safe default value
    int pgnr = map.get("PageNumber") ? map.get("PageNumber").toString().toInteger() : 0;
    int totalpages = map.get("TotalPages") ? map.get("TotalPages").toString().toInteger() : 1;
     
    // 3. Compare values and set the Loop property to STOP if the limit is exceeded
    if (pgnr > totalpages) {
        String loop = "STOP";
        message.setProperty("Loop", loop);
    }
    
    return message;
}