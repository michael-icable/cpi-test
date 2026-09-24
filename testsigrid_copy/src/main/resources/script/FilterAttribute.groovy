import com.sap.it.api.mapping.*

def void filterAttributes(String[] attributes, String[] values, String[] attributeToLocate, Output output, MappingContext context) {
    def searchFor = attributeToLocate[0]
    if(attributes[] != null)
    {
    def foundIndex = Arrays.asList(attributes).indexOf(searchFor)
    def value = foundIndex == -1 ? "" : values[foundIndex]
    output.addValue(value)
    }
    else{
        output.addValue("");
    }
}