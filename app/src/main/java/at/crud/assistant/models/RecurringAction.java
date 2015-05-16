package at.crud.assistant.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@DatabaseTable
public class RecurringAction {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField
    private String title;

    @DatabaseField
    private Date createdDate;

    @DatabaseField
    private Date firstDay;

    @DatabaseField
    private Date lastDay;

    @DatabaseField
    private float hoursPerWeek;        // TODO hoursPerWeek refactoren

    @ForeignCollectionField
    private Collection<Event> events;

    @DatabaseField( foreign = true)
    private RecurringActionSettings settings = new RecurringActionSettings();

    public RecurringAction() {
        super();
        firstDay = new Date();
    }

    public RecurringAction(String title, Date firstDay, float hoursPerWeek) {
        this.title = title;
        this.firstDay = firstDay;
        this.hoursPerWeek = hoursPerWeek;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getFirstDay() {
        return firstDay;
    }

    public void setFirstDay(Date firstDay) {
        this.firstDay = firstDay;
    }

    public Date getLastDay() {
        return lastDay;
    }

    public void setLastDay(Date lastDay) {
        this.lastDay = lastDay;
    }

    public float getHoursPerWeek() {
        return hoursPerWeek;
    }

    public void setHoursPerWeek(float hoursPerWeek) {
        this.hoursPerWeek = hoursPerWeek;
    }

    public int getId() {
        return id;
    }

    public RecurringActionSettings getSettings() {
        return settings;
    }

    public void setSettings(RecurringActionSettings settings) {
        this.settings = settings;
    }


    public Collection<Event> getEvents() {
        return events;
    }

    public void removeEvent(Event event) {
        events.remove(event);
    }

    public void addEvent(Event event) {
        events.add(event);
    }

    public void setEvents(Collection<Event> events) {
        this.events = events;
    }
}
