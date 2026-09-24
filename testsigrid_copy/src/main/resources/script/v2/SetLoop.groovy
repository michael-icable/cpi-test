// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

import com.sap.it.script.v2.api.Message;
def Message processData(Message message) {
    //Body 
       def map = message.getProperties();
       int pgnr = map.get("PageNumber");
       int totalpages = map.get("TotalPages");
     
       
      if (pgnr>totalpages)
      {
          String loop = "STOP";
          message.setProperty("Loop",loop);
      }
       return message;
}
