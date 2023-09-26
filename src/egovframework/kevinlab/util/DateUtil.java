package egovframework.kevinlab.util;

import java.util.Calendar;

public class DateUtil {

	/**
	 * 
	 * @return year : 2014
	 */
	public static String getYear() {
		Calendar cal = Calendar.getInstance();
		String year = String.format("%04d", cal.get(Calendar.YEAR) );
		return year;
	}

	/**
	 * 
	 * @return year : 2014
	 */
	public static String getMonth() {
		Calendar cal = Calendar.getInstance();
		String month = String.format("%02d",  cal.get(Calendar.MONTH) +1 );
		return month;
	}
	
	/**
	 * 
	 * @return timestamp : 2014-09-11 20:17:15
	 */
	public static String getTime() {
		Calendar cal = Calendar.getInstance();
		String time = String.format("%04d-%02d-%02d %02d:%02d:%02d	", 	
										cal.get(Calendar.YEAR), 
										cal.get(Calendar.MONTH) + 1, 
										cal.get(Calendar.DAY_OF_MONTH),
										cal.get(Calendar.HOUR_OF_DAY),
										cal.get(Calendar.MINUTE),
										cal.get(Calendar.SECOND)
										);
		return time;
	}
	
	/**
	 * 
	 * @return timestamp : 20140911201715
	 */
	public static String getTimeF() {
		Calendar cal = Calendar.getInstance();
		String time = String.format("%04d%02d%02d%02d%02d%02d", 	
										cal.get(Calendar.YEAR), 
										cal.get(Calendar.MONTH) + 1, 
										cal.get(Calendar.DAY_OF_MONTH),
										cal.get(Calendar.HOUR_OF_DAY),
										cal.get(Calendar.MINUTE),
										cal.get(Calendar.SECOND)
										);
		return time;
	}
	
	/**
	 * 
	 * @return date : 20140911
	 */
	public static String getDate() {
		Calendar cal = Calendar.getInstance();
		String date = String.format("%04d%02d%02d", 	
										cal.get(Calendar.YEAR), 
										cal.get(Calendar.MONTH) + 1, 
										cal.get(Calendar.DAY_OF_MONTH)
										);
		return date;
	}
	
	/**
	 * 
	 * @return date : 2014-09-11
	 */
	public static String getDateDash() {
		Calendar cal = Calendar.getInstance();
		String date = String.format("%04d-%02d-%02d", 	
										cal.get(Calendar.YEAR), 
										cal.get(Calendar.MONTH) + 1, 
										cal.get(Calendar.DAY_OF_MONTH)
										);
		return date;
	}
	
	/**
	 * 
	 * @return date : 2014.09.11
	 */
	public static String getDateDot() {
		Calendar cal = Calendar.getInstance();
		String date = String.format("%04d.%02d.%02d", 	
										cal.get(Calendar.YEAR), 
										cal.get(Calendar.MONTH) + 1, 
										cal.get(Calendar.DAY_OF_MONTH)
										);
		return date;
	}
	
	/**
	 * 
	 * @param days : - or + integer
	 * @return date : 2014-09-11
	 */
	public static String getDateCalc(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, days);
		String date = String.format("%04d-%02d-%02d", 	
										cal.get(Calendar.YEAR), 
										cal.get(Calendar.MONTH) + 1, 
										cal.get(Calendar.DAY_OF_MONTH)
										);
		return date;
	}
	
	/**
	 * 
	 * @param days : - or + integer
	 * @return date : 20140911
	 */
	public static String getDateCalcNoDash(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, days);
		String date = String.format("%04d%02d%02d", 	
										cal.get(Calendar.YEAR), 
										cal.get(Calendar.MONTH) + 1, 
										cal.get(Calendar.DAY_OF_MONTH)
										);
		return date;
	}
	
	/**
	 * First DAY of Month
	 * @return date : 2014-09-01
	 */
	public static String getDateFirstDayOfMonth() {
		Calendar cal = Calendar.getInstance();

		String date = String.format("%04d-%02d-%02d", 	
										cal.get(Calendar.YEAR), 
										cal.get(Calendar.MONTH) + 1, 
										1
										);
		return date;
	}
	
	/**
	 * Last DAY of month and year
	 * @param year
	 * @param month
	 * @return date : from 1 to 31 number
	 */
	public static String getDateLastDayOfMonth(String year, String month){
		//String date = "";
		Calendar cal = Calendar.getInstance();
		cal.set(Integer.parseInt(year), Integer.parseInt(month) -1, 1);
		
		cal.add(Calendar.MONTH, 1);
		cal.add(Calendar.DATE, -1);
		int date = cal.get(Calendar.DATE);

		return String.valueOf(date);
	}
}