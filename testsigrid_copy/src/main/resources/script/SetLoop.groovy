import com.sap.gateway.ip.core.customdev.util.Message;
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
