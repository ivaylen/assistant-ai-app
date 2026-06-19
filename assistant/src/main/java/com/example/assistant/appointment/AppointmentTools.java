package com.example.assistant.appointment;

import java.util.ArrayList;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AppointmentTools {

	private final AppointmentService appointmentService;

	public AppointmentTools(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@Tool(name = "schedule_appointment", description = """
			Creates a local appointment request when the user wants to schedule a doctor visit. Required details are:
			patient name, reason for visit, preferred appointment day/time, and contact phone or email.
			Do not require a specialist type. If any required details are missing, returns which details to ask for.
			""")
	public String scheduleAppointment(
			@ToolParam(required = false, description = "Patient name. Example: Ivan.") String patientName,
			@ToolParam(required = false, description = "Reason for the appointment. Example: headache.") String reason,
			@ToolParam(required = false, description = "Preferred appointment day and time. Example: tomorrow afternoon.") String preferredTime,
			@ToolParam(required = false, description = "Contact details, such as phone or email. Example: 0888123456.") String contact,
			ToolContext toolContext) {
		var missing = new ArrayList<String>();
		addMissing(missing, "patient name", patientName);
		addMissing(missing, "reason for the visit", reason);
		addMissing(missing, "preferred day and time", preferredTime);
		addMissing(missing, "contact phone or email", contact);

		if (!missing.isEmpty()) {
			return "Missing appointment details: " + String.join(", ", missing)
					+ ". Ask only for these missing details before creating the appointment request.";
		}

		var username = toolContext.getContext().getOrDefault("username", "anonymous").toString();
		var result = appointmentService.createAppointment(username,
				new AppointmentDetails(patientName, reason, preferredTime, contact));

		return "Appointment request created with id " + result.id() + " for " + result.requestedFor()
				+ ". Tell the user this is a request, not a confirmed appointment, and the clinic should confirm it.";
	}

	private void addMissing(ArrayList<String> missing, String label, String value) {
		if (!StringUtils.hasText(value)) {
			missing.add(label);
		}
	}

}
