(function () {
    var dateForm = document.getElementById("slot-date-form");
    var dateInput = document.getElementById("date");
    if (dateForm && dateInput) {
        dateInput.addEventListener("change", function () {
            if (dateInput.value) {
                dateForm.submit();
            }
        });
    }

    var form = document.getElementById("booking-form");
    if (!form) {
        return;
    }

    function parseHm(value) {
        var parts = value.split(":");
        return parseInt(parts[0], 10) * 60 + parseInt(parts[1], 10);
    }

    function formatHm(mins) {
        var hours = Math.floor(mins / 60);
        var minutes = mins % 60;
        return String(hours).padStart(2, "0") + ":" + String(minutes).padStart(2, "0");
    }

    function showError(name, message) {
        var error = form.querySelector("[data-error-for='" + name + "']");
        if (error) {
            error.hidden = false;
            error.textContent = message;
        }
    }

    function clearErrors() {
        form.querySelectorAll("[data-error-for]").forEach(function (el) {
            el.hidden = true;
            el.textContent = "";
        });
    }

    function refreshDuration() {
        var start = form.querySelector("input[name='startTime']:checked");
        var select = form.querySelector("#durationHours");
        var preview = document.getElementById("end-preview");
        var closing = parseHm(form.getAttribute("data-closing") || "22:00");
        if (!select) {
            return;
        }
        var startMins = start ? parseHm(start.value) : null;
        Array.from(select.options).forEach(function (opt) {
            var hours = parseInt(opt.value, 10);
            var endMins = startMins == null ? 0 : startMins + hours * 60;
            opt.disabled = startMins != null && endMins > closing;
        });
        if (select.selectedOptions[0] && select.selectedOptions[0].disabled) {
            var first = Array.from(select.options).find(function (opt) {
                return !opt.disabled;
            });
            if (first) {
                select.value = first.value;
            }
        }
        if (preview && start && select.value && !(select.selectedOptions[0] && select.selectedOptions[0].disabled)) {
            preview.textContent = "Ends at " + formatHm(parseHm(start.value) + parseInt(select.value, 10) * 60);
        } else if (preview) {
            preview.textContent = "Pick a start time";
        }
    }

    form.querySelectorAll("input[name='startTime']").forEach(function (input) {
        input.addEventListener("change", refreshDuration);
    });
    var durationSelect = form.querySelector("#durationHours");
    if (durationSelect) {
        durationSelect.addEventListener("change", refreshDuration);
    }
    refreshDuration();

    function refreshPartySize() {
        var requires = form.getAttribute("data-requires-party") === "true";
        var wrap = document.getElementById("party-size-field");
        var select = document.getElementById("partySize");
        if (wrap) {
            wrap.hidden = !requires;
        }
        if (!select) {
            return;
        }
        select.required = requires;
        if (!requires) {
            select.value = "";
            return;
        }
        var min = parseInt(form.getAttribute("data-min-party") || "1", 10);
        var max = parseInt(form.getAttribute("data-max-party") || "1", 10);
        var plusOnMax = form.getAttribute("data-plus-on-max") === "true";
        var current = select.value;
        select.innerHTML = "";
        var blank = document.createElement("option");
        blank.value = "";
        blank.textContent = "How many people";
        select.appendChild(blank);
        for (var n = min; n <= max; n++) {
            var opt = document.createElement("option");
            opt.value = String(n);
            opt.textContent = (plusOnMax && n === max) ? String(n) + "+" : String(n);
            select.appendChild(opt);
        }
        if (current && parseInt(current, 10) >= min && parseInt(current, 10) <= max) {
            select.value = current;
        }
    }

    refreshPartySize();
    var partySelect = document.getElementById("partySize");
    if (partySelect) {
        partySelect.addEventListener("change", refreshQuote);
    }
    form.querySelectorAll("input[name='extraIds']").forEach(function (box) {
        box.addEventListener("change", refreshQuote);
    });
    form.querySelectorAll("input[name='coachId']").forEach(function (input) {
        input.addEventListener("change", refreshQuote);
    });
    var skillSelect = document.getElementById("skillLevel");
    if (skillSelect) {
        skillSelect.addEventListener("change", function () {
            refreshCoachFilter();
            refreshQuote();
        });
        refreshCoachFilter();
    }

    function refreshCoachFilter() {
        var list = document.getElementById("coach-option-list");
        var empty = document.getElementById("coach-empty");
        var hint = document.getElementById("coach-level-hint");
        if (!list) {
            return;
        }
        var selectedLevel = skillSelect ? skillSelect.value : "";
        var hasLevel = selectedLevel !== "";
        if (hint) {
            hint.hidden = hasLevel;
        }
        list.hidden = !hasLevel;
        var visible = 0;
        list.querySelectorAll(".coach-option").forEach(function (option) {
            var radio = option.querySelector("input[name='coachId']");
            if (option.classList.contains("is-none")) {
                option.hidden = !hasLevel;
                if (radio) {
                    radio.disabled = !hasLevel;
                }
                return;
            }
            var levels = (option.getAttribute("data-levels") || "").split(",").filter(Boolean);
            var show = hasLevel && levels.indexOf(selectedLevel) !== -1;
            option.hidden = !show;
            if (radio) {
                radio.disabled = !show;
            }
            if (show) {
                visible += 1;
            } else if (radio && radio.checked) {
                radio.checked = false;
                var none = list.querySelector(".coach-option.is-none input[name='coachId']");
                if (none && hasLevel) {
                    none.disabled = false;
                    none.checked = true;
                }
            }
        });
        if (empty) {
            empty.hidden = !hasLevel || visible > 0;
        }
        if (hasLevel && visible === 0) {
            var noneOnly = list.querySelector(".coach-option.is-none input[name='coachId']");
            if (noneOnly) {
                noneOnly.disabled = false;
                noneOnly.checked = true;
            }
        }
    }

    function refreshQuote() {
        var preview = document.getElementById("price-preview");
        if (!preview) {
            return;
        }
        var dateField = form.querySelector("input[name='date']");
        var start = form.querySelector("input[name='startTime']:checked");
        var duration = form.querySelector("#durationHours");
        var resourceId = form.querySelector("input[name='resourceId']");
        if (!dateField || !dateField.value || !start || !duration || !duration.value || !resourceId || !resourceId.value) {
            preview.textContent = "Pick a start time to see the amount due";
            return;
        }
        var url = "/resources/" + encodeURIComponent(resourceId.value)
            + "/quote?date=" + encodeURIComponent(dateField.value)
            + "&start=" + encodeURIComponent(start.value)
            + "&durationHours=" + encodeURIComponent(duration.value);
        var kindField = form.querySelector("input[name='kind']");
        if (kindField && kindField.value) {
            url += "&kind=" + encodeURIComponent(kindField.value);
        }
        var peopleParam = "";
        if (form.getAttribute("data-requires-party") === "true") {
            var partyField = form.querySelector("#partySize");
            var parsedPeople = partyField ? parseInt(partyField.value, 10) : NaN;
            if (parsedPeople) {
                peopleParam = "&people=" + encodeURIComponent(String(parsedPeople));
            }
        } else {
            peopleParam = "&people=1";
        }
        url += peopleParam;
        form.querySelectorAll("input[name='extraIds']:checked").forEach(function (box) {
            url += "&extraIds=" + encodeURIComponent(box.value);
        });
        var coachField = form.querySelector("input[name='coachId']:checked");
        if (coachField && coachField.value) {
            url += "&coachId=" + encodeURIComponent(coachField.value);
        }
        fetch(url, { headers: { Accept: "application/json" } })
            .then(function (res) {
                return res.ok ? res.json() : null;
            })
            .then(function (data) {
                if (!data || data.amount == null) {
                    preview.textContent = "Pick a start time to see the amount due";
                    return;
                }
                preview.textContent = "You will pay " + Number(data.amount).toFixed(2) + " PLN";
            })
            .catch(function () {
                preview.textContent = "Pick a start time to see the amount due";
            });
    }

    form.querySelectorAll("input[name='startTime']").forEach(function (input) {
        input.addEventListener("change", refreshQuote);
    });
    if (durationSelect) {
        durationSelect.addEventListener("change", refreshQuote);
    }
    refreshQuote();

    form.addEventListener("submit", function (event) {
        clearErrors();
        var selected = form.querySelector("input[name='startTime']:checked");
        var dateField = form.querySelector("input[name='date']");
        var durationField = form.querySelector("#durationHours");
        var valid = true;
        if (!dateField || !dateField.value) {
            showError("startTime", "Choose a date");
            valid = false;
        } else {
            var today = new Date();
            today.setHours(0, 0, 0, 0);
            var chosen = new Date(dateField.value + "T00:00:00");
            if (chosen < today) {
                showError("startTime", "Date cannot be in the past");
                valid = false;
            }
        }
        if (!selected) {
            showError("startTime", "Choose a start time");
            valid = false;
        }
        var hours = durationField ? parseInt(durationField.value, 10) : 0;
        if (!hours || hours < 1 || hours > 4) {
            showError("durationHours", "Choose a duration between 1 and 4 hours");
            valid = false;
        } else if (selected) {
            var closing = parseHm(form.getAttribute("data-closing") || "22:00");
            if (parseHm(selected.value) + hours * 60 > closing) {
                showError("durationHours", "That duration runs past closing time");
                valid = false;
            }
        }
        var note = form.querySelector("#note");
        if (note && note.value.length > 300) {
            showError("startTime", "Note must be 300 characters or fewer");
            valid = false;
        }
        var phone = form.querySelector("#phone");
        if (phone) {
            var phoneValue = phone.value.trim();
            var phonePattern = /^[+]?[0-9\s().-]{7,20}$/;
            if (!phoneValue) {
                showError("phone", "Phone is required to complete this booking");
                valid = false;
            } else if (!phonePattern.test(phoneValue)) {
                showError("phone", "Enter a valid phone number");
                valid = false;
            }
        }
        if (form.getAttribute("data-requires-party") === "true") {
            var partyField = form.querySelector("#partySize");
            var minParty = parseInt(form.getAttribute("data-min-party") || "1", 10);
            var maxParty = parseInt(form.getAttribute("data-max-party") || "1", 10);
            var partySize = partyField ? parseInt(partyField.value, 10) : NaN;
            if (!partySize || partySize < minParty || partySize > maxParty) {
                showError("partySize", "Choose how many people are coming");
                valid = false;
            }
        }
        var payment = form.querySelector("input[name='paymentMethod']:checked");
        if (!payment) {
            showError("paymentMethod", "Choose a payment method");
            valid = false;
        }
        if (!valid) {
            event.preventDefault();
        }
    });
})();
