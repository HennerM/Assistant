package at.crud.assistant.services;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import at.crud.assistant.R;
import at.crud.assistant.SettingsFragment;
import at.crud.assistant.models.Event;
import at.crud.assistant.models.RecurringAction;
import at.crud.assistant.utils.CalendarRepository;
import at.crud.assistant.utils.DatabaseHelper;
import at.crud.assistant.utils.EventRepository;


public class AppointmentWizard {

    // TODO move to Preference
    private static final int OUTLOOK_DAYS = 7;
    private static final int WIZARD_NOTIFICATION = 1;

    private Context context;
    private AppointmentFinder appointmentFinder;
    private DatabaseHelper databaseHelper;

    public AppointmentWizard(Context context) {
        this.context = context;
        ContentResolver contentResolver = context.getContentResolver();
        EventRepository eventRepository = new EventRepository(contentResolver, new CalendarRepository(contentResolver));
        FreetimeCalculator freetimeCalculator = new FreetimeCalculator();
        appointmentFinder = new AppointmentFinder(getCalendarIds(), eventRepository, freetimeCalculator);
        databaseHelper = new DatabaseHelper(context);
    }

    protected String[] getCalendarIds() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> calendarPreference = sharedPref.getStringSet(SettingsFragment.PREFERENCE_KEY_RELEVANT_CALENDARS, new HashSet<String>(0));
        if (calendarPreference == null) {
            return new String[0];
        } else {
            return calendarPreference.toArray(new String[calendarPreference.size()]);
        }
    }

    public void recreateAppointments(List<RecurringAction> actions) {
        // TODO delete old Appointments
        Date startDate = getStartDate();
        Date endDate = getEndDate(startDate, OUTLOOK_DAYS);
        int nrOfAppointments = 0;
        for (RecurringAction recurringAction : actions) {
            nrOfAppointments += appointmentFinder.createAppointmentsForTimeSpan(recurringAction, startDate, endDate);
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Termine erstellt")
                        .setContentText("es wurden " + nrOfAppointments + " neue Termine erstellt!");

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(WIZARD_NOTIFICATION, mBuilder.build());

    }

    public void refreshAllActions() {
        try {
            List<RecurringAction> actions = getActiveActions();
            recreateAppointments(actions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refreshAction(RecurringAction action) {
        List<RecurringAction> actionList = new LinkedList<>();
        actionList.add(action);
        recreateAppointments(actionList);
    }

    public List<Event> getEventsNextWeekForAction(RecurringAction recAction) {
        Calendar fromDate = Calendar.getInstance();
        fromDate.add(Calendar.DATE, 1);
        CalendarUtil.setToMidnight(fromDate);
        Date tomorrow = new Date(fromDate.getTimeInMillis());
        fromDate.add(Calendar.DATE, 6);
        Date nextWeek = new Date(fromDate.getTimeInMillis());
        return appointmentFinder.findPossibleAppointments(recAction, tomorrow, nextWeek);
    }

    private Date getStartDate() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 1);
        CalendarUtil.setToMidnight(c);
        return new Date(c.getTimeInMillis());
    }

    private Date getEndDate(Date startDate, int outlookDays) {
        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        c.add(Calendar.DATE, outlookDays);
        return new Date(c.getTimeInMillis());
    }

    private List<RecurringAction> getActiveActions() throws SQLException {
        Dao<RecurringAction, Integer> recurringActionDao = databaseHelper.getRecurringActionDao();
        QueryBuilder<RecurringAction, Integer> queryBuilder = recurringActionDao.queryBuilder();

        queryBuilder.where().le("firstDay", getStartDate());
        return recurringActionDao.query(queryBuilder.prepare());
    }
}
