import com.sap.it.api.mapping.*;

def void splitWorkers(String[] input, Output output, MappingContext context) { 
    for(item in input[0].split(';')){
     output.addValue(item);
    }
}