import com.sap.gateway.ip.core.customdev.util.Message;

def Message processData(Message message) {
                
                // get a map of iflow properties
                def map = message.getProperties()
                def refernceID = map.get("SAPReferenceId")
                def logException = map.get("ExceptionLogging")
                def attachID = ""
                def errordetails = ""
                
                // get an exception java class instance
                def ex = map.get("CamelExceptionCaught")
                if (ex!=null) 
                {
                 // save the error response as a message attachment 
                def messageLog = messageLogFactory.getMessageLog(message);
				if (refernceID == null || refernceID == "" )
				{
					errordetails = "The Employee replication failed because of the following error:  " + ex.toString()
					attachID  = "Error Details for Employee"
				}
				else 
				{
					errordetails = "The Employee replication of  '" + refernceID + "' failed because of the following error:  " + ex.toString()
					attachID  = "Error Details for Employee '" + refernceID + "'"	
				}
                
                if (logException != null && logException.equalsIgnoreCase("TRUE")) 
                {
                    messageLog.addAttachmentAsString(attachID, errordetails, "text/plain");
                }
                }

                return message;
}