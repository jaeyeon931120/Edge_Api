package egovframework.kevinlab.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class JSONUtil {
	
	public static List parse(String jsonStr) throws Exception{
		
		List<Map> ret = null;
		
		try{
			
			ObjectMapper mapper = new ObjectMapper();
			ret = mapper.readValue(jsonStr.replaceAll("&quot;", "\""), new TypeReference<List<Map>>() {});			
			
		}catch(Exception e){
			
			ret = new ArrayList();
		}
		
		return ret;
	}
	
	public static List parse(String jsonStr,TypeReference valueTypeRef) throws Exception{
		
		List<Map> ret = null;
		
		try{
			
			ObjectMapper mapper = new ObjectMapper();
			ret = mapper.readValue(jsonStr.replaceAll("&quot;", "\""), valueTypeRef);
			
		}catch(Exception e){
			
			ret = new ArrayList();
		}
		
		return ret;
	}
	
	//json 문자열이 json Array 구조 인지 여부
	public static boolean isJSONArray(String json){
		
		if(json == null || json.length()==0) return false;		
		
		if( json.indexOf("[") == 0 && json.indexOf("]") == (json.length()-1) ){
			return true;
		}
		
		return false;
	}
	
}
