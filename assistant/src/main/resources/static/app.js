(function () {
	const { useEffect, useMemo, useState } = React;
	const h = React.createElement;

	function App() {
		const [user, setUser] = useState("");
		const [admin, setAdmin] = useState(false);
		const [conversationId, setConversationId] = useState("web");
		const [question, setQuestion] = useState("");
		const [messages, setMessages] = useState([{
			role: "assistant",
			content: "Hello, what is your condition? How can I help you?"
		}]);
		const [appointments, setAppointments] = useState([]);
		const [loading, setLoading] = useState(false);
		const canSend = question.trim().length > 0 && !loading;

		useEffect(() => {
			fetch("/api/me").then((res) => res.json()).then((data) => {
				setUser(data.username);
				setAdmin(data.admin);
			});
			loadAppointments();
		}, []);

		function loadAppointments() {
			fetch("/api/appointments")
				.then((res) => res.json())
				.then(setAppointments);
		}

		function sendMessage(event) {
			if (event) {
				event.preventDefault();
			}
			if (!canSend) {
				return;
			}

			const text = question.trim();
			setQuestion("");
			setMessages((current) => [...current, { role: "user", content: text }]);
			setLoading(true);

			fetch("/api/chat", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ question: text, conversationId })
			})
				.then((res) => {
					if (!res.ok) {
						throw new Error("Request failed");
					}
					return res.json();
				})
				.then((data) => {
					setMessages((current) => [...current, { role: "assistant", content: data.message }]);
					loadAppointments();
				})
				.catch(() => {
					setMessages((current) => [...current, {
						role: "assistant",
						content: "The assistant could not answer right now."
					}]);
				})
				.finally(() => setLoading(false));
		}

		function handleComposerKeyDown(event) {
			if (event.key === "Enter" && !event.shiftKey) {
				sendMessage(event);
			}
		}

		function confirmAppointment(id) {
			fetch("/api/appointments/" + id + "/confirm", { method: "POST" })
				.then((res) => {
					if (!res.ok) {
						throw new Error("Confirm failed");
					}
					return res.json();
				})
				.then(loadAppointments)
				.catch(() => {
					setMessages((current) => [...current, {
						role: "assistant",
						content: "The appointment could not be confirmed right now."
					}]);
				});
		}

		const appointmentRows = useMemo(() => appointments.map((appointment) => h("tr", { key: appointment.id },
			admin && h("td", { "data-label": "Owner" }, appointment.username),
			h("td", { "data-label": "Patient" }, appointment.patientName),
			h("td", { "data-label": "Reason" }, appointment.reason),
			h("td", { "data-label": "Preferred" }, appointment.preferredTime),
			h("td", { "data-label": "Status" },
				h("span", {
					className: "status-badge " + appointment.status.toLowerCase()
				}, appointment.status)
			),
			admin && h("td", { "data-label": "Action" }, appointment.status === "REQUESTED"
				? h("button", {
					type: "button",
					className: "confirm-button",
					onClick: () => confirmAppointment(appointment.id)
				}, "Confirm")
				: h("span", { className: "muted" }, "Checked"))
		)), [appointments, admin]);

		return h("div", { className: "shell" },
			h("header", { className: "topbar" },
				h("div", null,
					h("h1", null, "Doctor Assistant"),
					h("span", { className: "user" }, user)
				),
				h("form", { action: "/logout", method: "post" },
					h("button", { className: "logout", type: "submit" }, "Sign out")
				)
			),
			h("main", { className: "workspace" },
				h("section", { className: "chat-panel" },
					h("div", { className: "conversation-bar" },
						h("label", { htmlFor: "conversationId" }, "Conversation"),
						h("input", {
							id: "conversationId",
							value: conversationId,
							onChange: (event) => setConversationId(event.target.value),
							maxLength: 64
						})
					),
					h("div", { className: "messages" },
						messages.length === 0
							? h("div", { className: "empty" }, "No messages")
							: messages.map((message, index) => h("article", {
								key: index,
								className: "message " + message.role
							},
								h("div", { className: "role" }, message.role === "user" ? "You" : "Assistant"),
								h("p", null, message.content)
							)),
						loading && h("article", { className: "message assistant pending" },
							h("div", { className: "role" }, "Assistant"),
							h("p", null, "Thinking...")
						)
					),
					h("form", { className: "composer", onSubmit: sendMessage },
						h("textarea", {
							value: question,
							onChange: (event) => setQuestion(event.target.value),
							onKeyDown: handleComposerKeyDown,
							placeholder: "Ask about symptoms or request an appointment",
							rows: 3
						}),
						h("button", { type: "submit", disabled: !canSend }, loading ? "Sending" : "Send")
					)
				),
				h("aside", { className: "side-panel" },
					h("div", { className: "panel-heading" },
						h("h2", null, "Appointments"),
						h("button", { type: "button", onClick: loadAppointments }, "Refresh")
					),
					h("table", null,
						h("thead", null, h("tr", null,
							admin && h("th", null, "Owner"),
							h("th", null, "Patient"),
							h("th", null, "Reason"),
							h("th", null, "Preferred"),
							h("th", null, "Status"),
							admin && h("th", null, "Action")
						)),
						h("tbody", null, appointmentRows.length ? appointmentRows : h("tr", null,
							h("td", { colSpan: admin ? 6 : 4 }, "No appointments")
						))
					)
				)
			)
		);
	}

	ReactDOM.createRoot(document.getElementById("root")).render(h(App));
})();
