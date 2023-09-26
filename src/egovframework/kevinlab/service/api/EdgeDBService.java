package egovframework.kevinlab.service.api;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract interface EdgeDBService {
	
	// Edge DB hourdata insert
	public void Edge_Hour_Insert() throws Exception;
	
	// Edge DB daydata insert
	public void Edge_Day_Update() throws Exception;
	
	// Edge BackUp설정(delete)
	public void Edge_BackUp() throws Exception;
	
	// Edge meter테이블 insert
	public void Edge_meter_insert() throws Exception;
	
	// Edge meter(층별 및 총)테이블 insert
	public void Edge_electric_insert() throws Exception;
	
	// Edge meter(전열)테이블 insert
	public void Edge_elechot_insert() throws Exception;
	
	// Edge BackUp설정(date)
	public Map<String, Object> Edge_Set_BackUpDate(HashMap<String, Object> param) throws Exception;
	
	// Edge 데이터 재전송 전체 데이터 response
	public Map<String, Object> Edge_All_Data(HashMap<String, Object> param) throws Exception;
	
	// Edge System 로그인
	public Map<String, Object> Edge_Login(HashMap<String, Object> param) throws Exception;
	
	// Edge 장비 동기화
	public Map<String, Object> equipmentSync(HashMap<String, Object> param) throws Exception;
}
