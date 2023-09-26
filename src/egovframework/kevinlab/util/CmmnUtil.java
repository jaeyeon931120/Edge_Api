package egovframework.kevinlab.util;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

public class CmmnUtil {

	public static String getString(String str) {
		return StringUtils.defaultString(str);
	}

	public static String getString(Date date) {
		if (date == null) {
			return "";
		}
		return DateFormatUtils.ISO_DATETIME_FORMAT.format(date);
	}

	public static String getString(long l) {
		return String.valueOf(l);
	}

	public static String getString(float f) {
		return String.valueOf(f);
	}

	public static String getString(boolean b) {
		return String.valueOf(b);
	}

	public static String getString(boolean b, String trueString, String falseString) {
		return BooleanUtils.toString(b, trueString, falseString);
	}

	public static String getString(Object obj, String defStr){
        if(obj == null || obj.toString().equals("")) {
            return defStr;
        } else {
            if ("".equals(obj.toString())){
                return defStr;
            } else {
                return obj.toString();
            }
        }
    }

	public static long getLong(String str) {
		return NumberUtils.toLong(str, NumberUtils.LONG_MINUS_ONE);
	}

	public static float getFloat(String str) {
		return NumberUtils.toFloat(str, NumberUtils.FLOAT_MINUS_ONE);
	}

	public static boolean getBoolean(String str) {
		return BooleanUtils.toBoolean(str);
	}

	public static boolean getBoolean(String str, String trueString, String falseString) {
		return BooleanUtils.toBoolean(str, trueString, falseString);
	}

	public static Date getDate(String str) {
		Date date = new Date();

		if (StringUtils.isNotBlank(str)) {
			try {
				date =  DateFormatUtils.ISO_DATE_FORMAT.parse(str);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return date;
	}

	public static Map<String, Object> beanToMap(Object bean) {

		Map<String, Object> map = null;

		try {
			map = PropertyUtils.describe(bean);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} finally {
			if (map == null) {
				map = new HashMap<String, Object>();
			}
		}

		map.remove("class");

		return map;
	}

	public static void mapToBean(Map<String, Object> map, Object bean) {

		try {
			BeanUtils.populate(bean, map);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}
}