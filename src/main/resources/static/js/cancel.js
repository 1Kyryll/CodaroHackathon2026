(function () {
    var dialog = document.getElementById("cancel-dialog");
    var form = document.getElementById("cancel-dialog-form");
    var dismiss = document.getElementById("cancel-dialog-dismiss");
    var error = document.getElementById("cancel-reason-error");
    var otherWrap = document.getElementById("other-note-wrap");
    if (!dialog || !form) {
        return;
    }

    function selectedReason() {
        return form.querySelector("input[name='reason']:checked");
    }

    function refreshOther() {
        var selected = selectedReason();
        var showOther = selected && selected.getAttribute("data-other") === "true";
        if (otherWrap) {
            otherWrap.hidden = !showOther;
        }
    }

    document.querySelectorAll(".js-open-cancel").forEach(function (button) {
        button.addEventListener("click", function (event) {
            event.preventDefault();
            var action = button.getAttribute("data-action");
            if (!action) {
                return;
            }
            form.setAttribute("action", action);
            form.querySelectorAll("input[name='reason']").forEach(function (input) {
                input.checked = false;
            });
            if (form.otherNote) {
                form.otherNote.value = "";
            }
            if (error) {
                error.hidden = true;
            }
            refreshOther();
            if (typeof dialog.showModal === "function") {
                dialog.showModal();
            } else {
                dialog.setAttribute("open", "open");
            }
        });
    });

    form.querySelectorAll("input[name='reason']").forEach(function (input) {
        input.addEventListener("change", refreshOther);
    });

    if (dismiss) {
        dismiss.addEventListener("click", function () {
            if (typeof dialog.close === "function") {
                dialog.close();
            } else {
                dialog.removeAttribute("open");
            }
        });
    }

    form.addEventListener("submit", function (event) {
        if (!selectedReason()) {
            event.preventDefault();
            if (error) {
                error.hidden = false;
                error.textContent = "Choose a cancellation reason";
            }
        }
    });
})();
