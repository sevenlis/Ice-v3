package by.ingman.sevenlis.ice_v3.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.classes.Order;

public class FormatsUtils {
    public static String getDateFormatted(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("E. dd.MM.yyyy г.", Locale.getDefault());
        return sdf.format(date);
        /*return  String.format(Locale.ROOT, "%02d", date.getDate()) + "." +
                String.format(Locale.ROOT, "%02d", date.getMonth() + 1)    + "." +
                String.format(Locale.ROOT, "%04d", date.getYear() + 1900)  + " г.";*/
    }
    
    public static String getDateFormatted(Date date, String formatString) {
        SimpleDateFormat sdf = new SimpleDateFormat(formatString, Locale.getDefault());
        return sdf.format(date);
    }
    
    public static String getDateFormattedWithSeconds(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("E. dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(date);
        /*return  String.format(Locale.ROOT, "%02d", date.getDate()) + "." +
                String.format(Locale.ROOT, "%02d", date.getMonth() + 1)    + "." +
                String.format(Locale.ROOT, "%04d", date.getYear() + 1900)  + " г. " +
                String.format(Locale.ROOT, "%02d", date.getHours()) + ":" +
                String.format(Locale.ROOT, "%02d", date.getMinutes()) + ":" +
                String.format(Locale.ROOT, "%02d", date.getSeconds());*/
    }
    
    public static String getNumberFormatted(double number, int num) {
        return String.format("%8." + num + "f", number).replace(",",".");
    }

    public static String getNumberFormatted(double number, int len, int num) {
        return String.format("%" + len + "." + num + "f", number).replace(",",".");
    }

    public static Calendar roundDayToStart(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }
    
    public static Calendar roundDayToEnd(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c;
    }
    
    public static double roundDigit(double d, int z) {
        if (z == 0) return Math.round(d);
        double zz = Math.pow(10, z);
        return Math.round(d * zz) / zz;
    }
    
    public static double roundDigit(float d, int z) {
        if (z == 0) return Math.round(d);
        double zz = Math.pow(10, z);
        return Math.round(d * zz) / zz;
    }

    public static int getPixelsForDp(Context mContext, int dpMeasure) {
        Resources resources = mContext.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpMeasure,
                resources.getDisplayMetrics()
        );
    }

    public static String getOrdersUidsForInClause(List<Order> orderList) {
        StringBuilder builder = new StringBuilder();
        for (Order order : orderList) {
            builder.append("'").append(order.getOrderUid()).append("',");
        }
        builder.deleteCharAt(builder.length()-1);
        return builder.toString();
    }

    public static String getUidsForInClause(List<String> uidsList) {
        StringBuilder builder = new StringBuilder();
        for (String uid : uidsList) {
            builder.append("'").append(uid).append("',");
        }
        builder.deleteCharAt(builder.length()-1);
        return builder.toString();
    }

}
