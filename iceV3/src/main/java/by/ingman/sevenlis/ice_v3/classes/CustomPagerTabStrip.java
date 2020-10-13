package by.ingman.sevenlis.ice_v3.classes;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerTabStrip;

import java.util.Calendar;

import by.ingman.sevenlis.ice_v3.R;

public class CustomPagerTabStrip extends PagerTabStrip {
    private Context ctx;
    
    public CustomPagerTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setDrawFullUnderline(true);
        this.ctx = context;
        this.setTextColor(getDateColor(Calendar.getInstance()));
        this.setTabIndicatorColor(getDateColor(Calendar.getInstance()));
    }
    
    public int getDateColor(Calendar dateCalendar) {
        int dayOfWeek = dateCalendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == 1 || dayOfWeek == 7) {
            return ContextCompat.getColor(ctx, R.color.color_red);
        } else {
            return ContextCompat.getColor(ctx, R.color.dark_grey);
        }
    }
}
