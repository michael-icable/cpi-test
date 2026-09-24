import com.sap.it.api.mapping.*

def void identityList(String[] nationalID, String[] passport, Output output, MappingContext context) {
    //def searchFor = attributeToLocate[0]
    if(nationalID[] != null)
    {
    for(int i=0;i<nationalID.length;i++)
	{
    output.addValue(nationalID[i])
    }
	
    }
 
	if(passport[] != null)
    {
    for(int k=0;k<passport.length;k++)
	{
    output.addValue(passport[k])
    }
	}

}	
   