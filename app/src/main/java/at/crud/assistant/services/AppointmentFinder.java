package at.crud.assistant.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import at.crud.assistant.models.CalendarDay;
import at.crud.assistant.models.Event;
import at.crud.assistant.models.RecurringAction;
import at.crud.assistant.utils.EventFactory;
import at.crud.assistant.utils.EventRepository;


public class AppointmentFinder {


    protected EventRepository eventRepository;
    protected FreetimeCalculator freetimeCalculator;
    protected String[] calendarIds;

    public AppointmentFinder(String[] calendarIds, EventRepository eventRepository, FreetimeCalculator freetimeCalculator) {
        this.eventRepository = eventRepository;
        this.freetimeCalculator = freetimeCalculator;
        this.calendarIds = calendarIds;
    }

    public void createAppointmentsForTimeSpan(RecurringAction recurringAction, Date startDate, Date endDate) {
        List<Event> eventList = findPossibleAppointments(recurringAction, startDate, endDate);
        for (Event event: eventList) {
            // TODO use prefered calendar
            int calendarId = 1;
            eventRepository.insert(EventFactory.createContentValueFromEvent(recurringAction.getId(), event, calendarId));
        }
    }

    public List<Event> findPossibleAppointments(RecurringAction recurringAction, Date startDate, Date endDate) {
        List<CalendarDay> availableDays = getAvailableDays(recurringAction, startDate, endDate);
        return makeAppointments(availableDays, recurringAction);
    }

    protected List<CalendarDay> getAvailableDays(RecurringAction recurringAction, Date startDate, Date endDate) {
        if (recurringAction.getFirstDay() != null && recurringAction.getFirstDay().after(startDate)) {
            startDate = recurringAction.getFirstDay();
        }
        if (recurringAction.getLastDay() != null && recurringAction.getLastDay().before(endDate)) {
            endDate = recurringAction.getLastDay();
        }
        Calendar cIterator = Calendar.getInstance();
        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.setTime(endDate);
        List<CalendarDay> dayList = new ArrayList<>();
        long availableMinutesOverall = 0;
        for (cIterator.setTime(startDate); cIterator.before(calendarEnd); cIterator.add(Calendar.DATE, recurringAction.getSettings().getDayIntervall())) {

            CalendarDay cDay = new CalendarDay();
            cDay.setDate(new Date(cIterator.getTimeInMillis()));
            List<Event> eventList = eventRepository.findEventsForDay(calendarIds, cIterator);
            cDay.setEventList(eventList);
            int minutes = freetimeCalculator.getFreeMinutes(eventList);
            if (minutes > 0) {
                availableMinutesOverall += minutes;
                cDay.setMinutesAvailable(minutes);
                dayList.add(cDay);
            }
        }

        for (CalendarDay calDay : dayList) {
            float percentage = ((float)calDay.getMinutesAvailable()) / (float)availableMinutesOverall;
            calDay.setPercentageAvailable(percentage);
        }

        Collections.sort(dayList);
        return dayList;
    }

    protected List<Event> makeAppointments(List<CalendarDay> availableDays, RecurringAction recurringAction) {
        int overallPensumInMinutes = Math.round(recurringAction.getHoursPerWeek() * 60);
        List<Event> eventList = new ArrayList<>();
        for (CalendarDay day: availableDays) {
            int pensumForDay = Math.round((overallPensumInMinutes * day.getPercentageAvailable()) / 10) * 10;
            if (pensumForDay > recurringAction.getSettings().getMinimalDurationMinutes()) {
                Calendar calendarSapce = freetimeCalculator.searchForSpace(recurringAction.getSettings(), day, pensumForDay);
                if (calendarSapce != null) {
                    Event event = EventFactory.createEvent(calendarSapce, pensumForDay, recurringAction.getTitle());
                    eventList.add(event);
                }
            }

        }

        return eventList;
    }

}