package com.twd.talktime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import com.twd.talktime.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	Boolean m_showDetails;
	int m_billDay;
	int m_telcoPlanMin;
	Calendar m_fromDate;
	
	TableLayout m_tableLayout;
	OnSwipeTouchListener onSwipeTouchListener;
	int page_num = 0; // 0 is the current billing cycle
	
	private static final int RESULT_SETTINGS = 1;
//	TalkTimeUsageGraphic ttuGraphic;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUserPreferences();
        m_tableLayout = (TableLayout)findViewById(R.id.tableLayout1);
        m_tableLayout.setStretchAllColumns(true);
        m_tableLayout.setShrinkAllColumns(true);
        
//        Log.w("", "OK onCreate init");

        Calendar calendar = getBillingCycleDate(m_billDay);
        m_fromDate = (Calendar)calendar.clone();
//        Log.w("", "OK calendar:" + calendar);
        createTableForDateRange(calendar,null);
        
        
        onSwipeTouchListener = new OnSwipeTouchListener() {
            public void onSwipeTop() {
                
            }
            public void onSwipeRight() {
                if (page_num==0) { // if going to prev cycle
//                	Log.w("", "going to previous cycle relative to " + m_fromDate);
                	page_num=1;
                	Calendar toDate = (Calendar) m_fromDate.clone();
                	m_fromDate.add(Calendar.MONTH, -1);
//                 	reset m_tableLayout
                    m_tableLayout.removeAllViews();
                	// recreate table from new from_date
                	createTableForDateRange(m_fromDate, toDate);                	
                } else {
                	Toast.makeText(MainActivity.this, "this is it!", Toast.LENGTH_SHORT).show();
                }
            }
            public void onSwipeLeft() {
                if (page_num==1) {
                	page_num=0;
                	m_fromDate.add(Calendar.MONTH, 1);
//                 	reset m_tableLayout
                    m_tableLayout.removeAllViews();
                	// recreate table from new from_date
                	createTableForDateRange(m_fromDate, null);
                } else {
                	Toast.makeText(MainActivity.this, "don't have a time machine yet!", Toast.LENGTH_SHORT).show();
                }
            }
            public void onSwipeBottom() {
//                Toast.makeText(MainActivity.this, "bottom", Toast.LENGTH_SHORT).show();
            }
        };
        
        // code for left and right fling action
        ScrollView sv = (ScrollView) findViewById(R.id.scrollView1);
        sv.setOnTouchListener(onSwipeTouchListener);
        
        // imageview can also be added to tablelayout
        // TODO*** lets draw the rectangle-like graphic visually representing usage
//        ttuGraphic = new TalkTimeUsageGraphic(this);
//        ttuGraphic.setBackgroundColor(Color.GREEN);
//        table.addView(ttuGraphic); 

    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        onSwipeTouchListener.getGestureDetector().onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);  
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    
		switch (item.getItemId()) {
			
			case R.id.action_settings:
				Intent i1 = new Intent(this, SettingsActivity.class);
				startActivityForResult(i1, RESULT_SETTINGS);
				break;
				
			case R.id.action_about:
				Intent i2 = new Intent(this, AboutActivity.class);
				startActivityForResult(i2, RESULT_SETTINGS);
				break;

		}

		return true;
	}
    
	/**
	 * Initialize User Preferences 
	 */
	private void initUserPreferences() {
		// init user preference 
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		m_billDay = Integer.parseInt(sharedPrefs.getString("pref_billDay", "18"));
        m_showDetails = sharedPrefs.getBoolean("pref_showDetails", true);
        m_telcoPlanMin = Integer.parseInt(sharedPrefs.getString("pref_planMinutes", "400"));
	}


	
	/**
	 * Generate table for records between fromDate and toDate (inclusive of fromDate)
	 * @param fromDate
	 * @param toDate
	 */
	@SuppressLint("SimpleDateFormat")
	private void createTableForDateRange(Calendar fromDate, Calendar toDate) {
//		Log.w("", "OK inside createTableForDateRange: " + fromDate);
		// query should include record from the beginning of from Date		
		String searchQuery = "TYPE=" + CallLog.Calls.OUTGOING_TYPE + " AND DURATION>0" + " AND DATE> " + fromDate.getTime().getTime();
		// select outgoing calls
		if (toDate != null) {
			searchQuery += " AND DATE< " + toDate.getTime().getTime();
		}
//		Log.w("", "OK searchQuery: " + searchQuery);
        Cursor managedCursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, searchQuery, null, null);
//        Log.w("", "OK query executed ");
        int c_number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int c_date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int c_duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
        
        SimpleDateFormat df = new SimpleDateFormat("MMM-dd hh:mm a");
        SimpleDateFormat df2 = new SimpleDateFormat("dd-MMM");
        int totalDuration=0, count=0;
        
        TableRow tr[] = new TableRow[managedCursor.getCount()];
//        Log.w("", "number of records: " + managedCursor.getCount());
        
        while(managedCursor.moveToNext()) {
            Date callDateTime = new Date(Long.valueOf(managedCursor.getString(c_date)));
            String phNumber = managedCursor.getString(c_number);
            int callDuration = managedCursor.getInt(c_duration);
            tr[count] =  getRow(df.format(callDateTime), phNumber, callDuration + "s",0);            
            totalDuration +=callDuration; //only for current billing cycle
            count++;       	
        }
        managedCursor.close();
        // ---------------------------------------------
        // *** let's create table view on my own! done!
        // ---------------------------------------------
        if (toDate == null) {
        	Calendar calendar = GregorianCalendar.getInstance();
        	calendar.setTime(fromDate.getTime());
        	calendar.add(Calendar.MONTH, 1);
        	long daysremaining = daysBetween(GregorianCalendar.getInstance(), calendar);
//        	Log.w("", "daysremaining: " + daysremaining);
        	TableRow rowTitle1 = getRow("Talktime remaining: "+ (m_telcoPlanMin-(totalDuration/60))+"min"+"/"+ daysremaining+" days");
        	m_tableLayout.addView(rowTitle1);
        } else {
        	TableRow rowTitle2 = getRow("Talktime utilized: "+ (totalDuration/60)+"min off "+ m_telcoPlanMin+"min");
        	m_tableLayout.addView(rowTitle2);
        }
        
        // total row
        TableRow finalRow =  getRow("Grand Total", count + " calls", totalDuration/60 + "min", 1);
        finalRow.setBackgroundColor(Color.LTGRAY);
        finalRow.setBackgroundResource(R.drawable.row_border);
        m_tableLayout.addView(finalRow);

        // optional display: if user wants details and there are details
        if (m_showDetails && tr.length>0) {
        	// title row for optional display
            TableRow rowTitle2 = getRow("Outgoing Calls (" + df2.format(fromDate.getTime()) + (toDate==null ? " onwards" : " to " + df2.format(toDate.getTime()))+")"); 
            m_tableLayout.addView(rowTitle2);
            //
            m_tableLayout.addView(getRow("DateTime", "Number", "Duration",1)); 
        	for(int i=0;i < tr.length;i++) {
        		m_tableLayout.addView(tr[i]);
        	}
        }
                
//        return table;
	}
	

	/**
	 * @param totalDuration
	 * @return
	 */
	private TableRow getRow(String text) {
		TableRow rowTitle = new TableRow(this);
        rowTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        rowTitle.setBackgroundColor(Color.GRAY);
        
        TextView title = new TextView(this);
//        billingCycleDate.
        title.setText(text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.SERIF, Typeface.BOLD);
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.span=3;
        rowTitle.addView(title, params);
		return rowTitle;
	}


	/**
	 * @return
	 */
	private TableRow getRow(String s1, String s2, String s3, int type) {
        TableRow row =  new TableRow(this);
        TextView c1 = new TextView(this);
        c1.setText(s1);
        TextView c2 = new TextView(this);
        c2.setText(s2);
        TextView c3 = new TextView(this);
        c3.setGravity(Gravity.RIGHT);
        c3.setText(s3);
        c1.setBackgroundResource(R.drawable.row_border);
        c2.setBackgroundResource(R.drawable.row_border);
        c3.setBackgroundResource(R.drawable.row_border);
        
        if (type==1) {
        	c1.setTypeface(Typeface.SERIF, Typeface.BOLD);
            c1.setTextColor(Color.RED);
            c2.setTypeface(Typeface.SERIF, Typeface.BOLD);
            c2.setTextColor(Color.RED);
            c3.setTypeface(Typeface.SERIF, Typeface.BOLD);
            c3.setTextColor(Color.RED);
        }
        row.addView(c1);
        row.addView(c2);
        row.addView(c3);
		return row;
	}
   
	/**
	 * @param billingCycleDay: The date (day_of_month) from when telco usually starts billing
	 * @return Date from when current billing cycle begins (and plan minutes utilization can be calculated)
	 */
	public static Calendar getBillingCycleDate(int billingCycleDay) {
		Calendar now = GregorianCalendar.getInstance();
		Calendar lastBillingDate = GregorianCalendar.getInstance();
 		
		int m = now.get(Calendar.MONTH);
		int d = now.get(Calendar.DAY_OF_MONTH);
		int y = now.get(Calendar.YEAR);
		
		if (m==0 && d< billingCycleDay) {
			lastBillingDate.set(y-1, 11, billingCycleDay,0,0,0);
		} else if (m==0 && d>= billingCycleDay) {
			lastBillingDate.set(y, 0, billingCycleDay,0,0,0);
		} else if (m>0 && d< billingCycleDay)  {
			lastBillingDate.set(y, m-1, billingCycleDay,0,0,0);
		} else if (m>0 && d>= billingCycleDay)  {
			lastBillingDate.set(y, m, billingCycleDay,0,0,0);
		}
		
		return lastBillingDate;
	}
	
	/** not as easy as it looks but i will ignore the outlier scenarios  **/
	/** Using Calendar - THE CORRECT WAY**/
    public static long daysBetween(Calendar startDate, Calendar endDate) {
        Calendar date = (Calendar) startDate.clone();
        long daysBetween = 0;
        while (date.before(endDate)) {
            date.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }
        return daysBetween;
    }
	
}
