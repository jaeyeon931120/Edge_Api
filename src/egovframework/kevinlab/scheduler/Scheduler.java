package egovframework.kevinlab.scheduler;

import javax.annotation.Resource;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import egovframework.kevinlab.service.api.AlarmService;
import egovframework.kevinlab.service.api.EdgeDBService;

@Component
public class Scheduler extends Thread{
	
	@Resource(name = "EdgeDBService")
	private EdgeDBService edgeDBService;
	
	@Resource(name = "AlarmService")
	private AlarmService alarmService;
		
//	 -초 0-59 , - * / 
//	 -분 0-59 , - * / 
//	 -시 0-23 , - * / 
//	 -일 1-31 , - * ? / L W
//	 -월 1-12 or JAN-DEC , - * / 
//	 -요일 1-7 or SUN-SAT , - * ? / L # 
//	 -년(옵션) 1970-2099 , - * /
		
//   1 2 3 4 5 6 7
//	 * * * * * * *
//	 초  분  시 일 월 요일 년도(생략가능)
	
	/**
	 * 테스트용 스케줄러
	 */		
//	@Scheduled(cron = "0 0/1 * * * ?") // 매 1분마다 한번씩 실행
	public void gettestData() throws Exception {
		try {
//			edgeDBService.Edge_Hour_Insert(); //1분 데이터 가져오기
//			edgeDBService.Edge_Day_Update();
//			edgeDBService.Edge_BackUp();
//			edgeDBService.Edge_meter_insert();
//			edgeDBService.Edge_electric_insert();
//			edgeDBService.Edge_elechot_insert();
//			edgeDBService.equipmentSync(null);
//			alarmService.Edge_alarm_insert();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ntek에서 각 meter테이블별로 insert
	 */		
//	@Scheduled(cron = "0 0/5 * * * ?") // 매 5분마다 한번씩 실행
	public void getLowData() throws Exception {
		try {
			edgeDBService.Edge_meter_insert();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 계산된 각 층별 전체전력 및 전열 로그 작성
	 */		
//	@Scheduled(cron = "0 2/5 * * * ?") // 매 5분마다 2분뒤에 5분단위로 한번씩 실행
	public void getelectricData() throws Exception {
		try {
			edgeDBService.Edge_electric_insert();
			edgeDBService.Edge_elechot_insert();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 각 meter 테이블에서 오류체크해서 알람테이블에 insert
	 */	
	@Scheduled(cron = "0 0/10 * * * ?") // 매 10분마다 한번씩 실행
	public void getalarmData() throws Exception {
		try {
			alarmService.Edge_alarm_insert();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 *  1시간 데이터 insert
	 */
//	@Scheduled(cron="0 10 0/1 * * ?")  // 1시간 10분마다 한번씩 실행
	public void getHourData() throws Exception {		

		try {
			edgeDBService.Edge_Hour_Insert();
		} catch (Exception e) {
			e.printStackTrace();	
		}
	}
	
	/**
	 *  하루의 마지막 업데이트, 백업 기간에 따른 하루마다 삭제, 장비 동기화
	 */
//	@Scheduled(cron="0 30 0 * * ?")  // 매일 자정 30분에 실행
	public void getDayData() throws Exception {		

		try {
			edgeDBService.Edge_Day_Update();
			edgeDBService.Edge_BackUp();
			edgeDBService.equipmentSync(null);
		} catch (Exception e) {
			e.printStackTrace();	
		}
	}
}